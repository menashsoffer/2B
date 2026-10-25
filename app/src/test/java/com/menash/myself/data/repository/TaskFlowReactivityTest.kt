package com.menash.myself.data.repository

import com.menash.myself.core.data.repository.impl.FirestoreTaskRepository
import com.menash.myself.core.data.session.DefaultHouseholdSession
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.Household
import com.menash.myself.data.models.TaskItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Blocking 1:
 * observe* flows (tasks, dreams, inbox) derive from householdSession.currentHouseholdId with flatMapLatest.
 * When the id is null, emit emptyList() (never emptyFlow()).
 * Re-subscribe when the household changes.
 * Test: subscribe before the household is set -> receives items after it's set.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskFlowReactivityTest {

    @Test
    fun observeTasks_subscribedBeforeHouseholdIsSet_emitsEmptyListThenReceivesItemsWhenSet() = runTest {
        val householdTasksFlow = MutableStateFlow(
            listOf(
                TaskItem(
                    id = "t1",
                    title = "Task in Household 1",
                    space = "shared",
                    householdId = "hh_1"
                )
            )
        )

        val fakeLifeHubRepo = object : LifeHubRepository(db = null, uidProvider = { "uid_1" }) {
            override fun observeMergedTasks(household: Household?): Flow<List<TaskItem>> {
                return if (household?.id == "hh_1") householdTasksFlow else flowOf(emptyList())
            }
        }

        val session = DefaultHouseholdSession() // Initially null!
        val repo = FirestoreTaskRepository(fakeLifeHubRepo, session)

        val emissions = mutableListOf<List<Task>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repo.observeTasks(VisibilityFilter.ALL).collect { tasks ->
                emissions.add(tasks)
            }
        }

        // 1. Before household is set: must immediately emit emptyList() (NOT hang on emptyFlow())
        assertEquals(1, emissions.size)
        assertTrue("Initial emission when household is null must be emptyList()", emissions[0].isEmpty())

        // 2. Set household ID -> flatMapLatest re-subscribes and emits items for hh_1
        session.setHouseholdId("hh_1")
        assertEquals(2, emissions.size)
        assertEquals(1, emissions[1].size)
        assertEquals("Task in Household 1", emissions[1][0].title)

        job.cancel()
    }

    @Test
    fun observeTasks_resubscribesWhenHouseholdChanges() = runTest {
        val hh1Tasks = listOf(TaskItem(id = "t1", title = "HH1 Task", space = "mine", householdId = "hh_1"))
        val hh2Tasks = listOf(TaskItem(id = "t2", title = "HH2 Task", space = "shared", householdId = "hh_2"))

        val fakeLifeHubRepo = object : LifeHubRepository(db = null, uidProvider = { "uid_1" }) {
            override fun observeMergedTasks(household: Household?): Flow<List<TaskItem>> {
                return when (household?.id) {
                    "hh_1" -> flowOf(hh1Tasks)
                    "hh_2" -> flowOf(hh2Tasks)
                    else -> flowOf(emptyList())
                }
            }
        }

        val session = DefaultHouseholdSession()
        val repo = FirestoreTaskRepository(fakeLifeHubRepo, session)

        val emissions = mutableListOf<List<Task>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repo.observeTasks(VisibilityFilter.ALL).collect { emissions.add(it) }
        }

        assertEquals(0, emissions.last().size)

        session.setHouseholdId("hh_1")
        assertEquals("HH1 Task", emissions.last().single().title)

        session.setHouseholdId("hh_2")
        assertEquals("HH2 Task", emissions.last().single().title)

        session.setHouseholdId(null)
        assertTrue(emissions.last().isEmpty())

        job.cancel()
    }
}
