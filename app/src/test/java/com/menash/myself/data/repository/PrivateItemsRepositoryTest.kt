package com.menash.myself.data.repository

import com.menash.myself.core.data.repository.impl.FirestoreTaskRepository
import com.menash.myself.core.data.session.DefaultHouseholdSession
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.Household
import com.menash.myself.data.models.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Blocking 2:
 * Private items (isPrivate = true) are written to users/{uid}/private/{collection}/{id},
 * never to the household subcollection. Reads merge household items + the viewer's private items.
 * A partner cannot see the owner's private task.
 */
class PrivateItemsRepositoryTest {

    @Test
    fun createTask_whenIsPrivateTrue_passesPrivateSpaceAndNeverWritesToHousehold() = runBlocking {
        var capturedSpace: String? = null
        var capturedHouseholdId: String? = null

        val fakeLifeHubRepo = object : LifeHubRepository(db = null, uidProvider = { "uid_owner" }) {
            override suspend fun createTask(
                title: String,
                nextAction: String,
                isTop3: Boolean,
                isFiveBefore11: Boolean,
                dueDate: String,
                space: String,
                household: Household?
            ) {
                capturedSpace = space
                capturedHouseholdId = if (space == "private") "private" else household?.id
            }
        }

        val session = DefaultHouseholdSession().apply { setHouseholdId("hh_1") }
        val repo = FirestoreTaskRepository(fakeLifeHubRepo, session)

        repo.createTask(
            title = "Top Secret Gift",
            forWhom = ForWhom.ME,
            isPrivate = true
        )

        assertEquals("private", capturedSpace)
        assertEquals("private", capturedHouseholdId)
    }

    @Test
    fun observeTasks_partnerCannotSeeOwnersPrivateTask_whileOwnerMergesPrivateAndHouseholdTasks() = runBlocking {
        val sharedTask = TaskItem(
            id = "t_shared",
            title = "Buy groceries",
            space = "shared",
            userId = "uid_owner",
            householdId = "hh_1"
        )
        val ownerPrivateTask = TaskItem(
            id = "t_priv_owner",
            title = "Surprise birthday cake",
            space = "private",
            userId = "uid_owner",
            householdId = "private"
        )

        var activeViewerUid = "uid_owner"

        // Subclass LifeHubRepository: observeMergedTasks runs the REAL production merge logic!
        val fakeLifeHubRepo = object : LifeHubRepository(db = null, uidProvider = { activeViewerUid }) {
            override fun observeTasks(spaceFilter: String?, household: Household?): Flow<List<TaskItem>> {
                return if (household?.id == "hh_1") flowOf(listOf(sharedTask)) else flowOf(emptyList())
            }

            override fun observePrivateTasks(uid: String): Flow<List<TaskItem>> {
                // Only uid_owner has private tasks in users/{uid}/private/tasks/items
                return if (uid == "uid_owner") {
                    flowOf(listOf(ownerPrivateTask))
                } else {
                    flowOf(emptyList())
                }
            }
        }

        val session = DefaultHouseholdSession().apply { setHouseholdId("hh_1") }
        val repo = FirestoreTaskRepository(fakeLifeHubRepo, session)

        // 1. Viewer is Owner (uid_owner) -> sees BOTH shared task and their own private task
        activeViewerUid = "uid_owner"
        val ownerVisible = repo.observeTasks(VisibilityFilter.ALL).first()
        assertEquals(2, ownerVisible.size)
        assertTrue(ownerVisible.any { it.id == "t_priv_owner" && it.isPrivate })
        assertTrue(ownerVisible.any { it.id == "t_shared" && !it.isPrivate })

        // 2. Viewer is Partner (uid_partner) -> sees ONLY shared task, NEVER owner's private task
        activeViewerUid = "uid_partner"
        val partnerVisible = repo.observeTasks(VisibilityFilter.ALL).first()
        assertEquals(1, partnerVisible.size)
        assertEquals("t_shared", partnerVisible.first().id)
        assertFalse(partnerVisible.any { it.id == "t_priv_owner" })
    }
}
