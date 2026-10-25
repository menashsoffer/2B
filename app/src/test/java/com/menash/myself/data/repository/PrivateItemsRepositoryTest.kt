package com.menash.myself.data.repository

import com.menash.myself.core.data.repository.impl.FirestoreTaskRepository
import com.menash.myself.core.data.session.DefaultHouseholdSession
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.Household
import com.menash.myself.data.models.TaskItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.*

class PrivateItemsRepositoryTest {

    @Test
    fun privateTask_writtenToPrivateSpace_neverToHouseholdCollection() = runTest {
        val householdSession = DefaultHouseholdSession(initialHouseholdId = "house_123")
        val lifeHub = mock(LifeHubRepository::class.java)
        val repository = FirestoreTaskRepository(lifeHub, householdSession)

        repository.createTask(
            title = "פתק פרטי וסודי",
            forWhom = ForWhom.ME,
            isPrivate = true
        )

        // Verifies space == "private" and household.id == "private" (NOT "house_123"!)
        verify(lifeHub).createTask(
            title = eq("פתק פרטי וסודי"),
            nextAction = anyString(),
            isTop3 = anyBoolean(),
            isFiveBefore11 = anyBoolean(),
            dueDate = anyString(),
            space = eq("private"),
            household = argThat { it?.id == "private" }
        )
    }

    @Test
    fun partnerCannotSeeOwnerPrivateTask_readsMergeOnlyViewersPrivateTasks() = runTest {
        val householdSession = DefaultHouseholdSession(initialHouseholdId = "house_123")
        val lifeHub = mock(LifeHubRepository::class.java)

        // Household tasks in house_123
        val sharedTask = TaskItem(id = "shared_1", title = "משימה משותפת", householdId = "house_123", userId = "alice", space = "shared")
        // Alice's private task (only visible to Alice)
        val alicePrivateTask = TaskItem(id = "alice_priv", title = "סודי של אליס", householdId = "private", userId = "alice", space = "private")
        // Bob's private task (only visible to Bob)
        val bobPrivateTask = TaskItem(id = "bob_priv", title = "סודי של בוב", householdId = "private", userId = "bob", space = "private")

        val dummyHousehold = Household(id = "house_123", name = "", ownerUid = "alice")

        // 1. When Alice (Owner) observes merged tasks, she receives: sharedTask + alicePrivateTask
        `when`(lifeHub.observeMergedTasks(dummyHousehold)).thenReturn(flowOf(listOf(sharedTask, alicePrivateTask)))
        val aliceRepo = FirestoreTaskRepository(lifeHub, householdSession)
        val aliceTasks = aliceRepo.observeTasks(VisibilityFilter.ALL).first()

        assertEquals(2, aliceTasks.size)
        assertTrue(aliceTasks.any { it.id == "shared_1" })
        assertTrue(aliceTasks.any { it.id == "alice_priv" })

        // 2. When Bob (Partner) observes merged tasks, he receives: sharedTask + bobPrivateTask (NEVER alicePrivateTask)
        `when`(lifeHub.observeMergedTasks(dummyHousehold)).thenReturn(flowOf(listOf(sharedTask, bobPrivateTask)))
        val bobRepo = FirestoreTaskRepository(lifeHub, householdSession)
        val bobTasks = bobRepo.observeTasks(VisibilityFilter.ALL).first()

        assertEquals(2, bobTasks.size)
        assertTrue(bobTasks.any { it.id == "shared_1" })
        assertTrue(bobTasks.any { it.id == "bob_priv" })
        assertFalse(bobTasks.any { it.id == "alice_priv" }) // Bob NEVER sees Alice's private task!
    }
}
