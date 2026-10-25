package com.menash.myself.data.repository

import com.menash.myself.core.data.repository.impl.FirestoreTaskRepository
import com.menash.myself.core.data.session.DefaultHouseholdSession
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.Household
import com.menash.myself.data.models.TaskItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class TaskFlowReactivityTest {

    @Test
    fun observeTasks_subscribesBeforeHouseholdSet_receivesItemsAfterSet() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val householdSession = DefaultHouseholdSession(initialHouseholdId = null)

        val lifeHub = mock(LifeHubRepository::class.java)

        val house1Flow = MutableSharedFlow<List<TaskItem>>(replay = 1)
        val dummyHouse1 = Household(id = "house_1", name = "", ownerUid = "")
        `when`(lifeHub.observeMergedTasks(dummyHouse1)).thenReturn(house1Flow)

        val repository = FirestoreTaskRepository(lifeHub, householdSession)

        val emissions = mutableListOf<List<com.menash.myself.core.domain.model.Task>>()
        val job = launch(testDispatcher) {
            repository.observeTasks(VisibilityFilter.ALL).collect {
                emissions.add(it)
            }
        }

        // 1. Initial emission when household is null: MUST be emptyList(), NOT emptyFlow()
        assertEquals(1, emissions.size)
        assertTrue(emissions[0].isEmpty())
        assertTrue(job.isActive) // Must remain active (subscribed), never terminated!

        // 2. Set household ID
        householdSession.setHouseholdId("house_1")

        // 3. Emit items for house_1
        val item1 = TaskItem(id = "t1", title = "משימה ראשונה", householdId = "house_1", userId = "u1")
        house1Flow.emit(listOf(item1))

        // 4. Subscriber must have received the newly emitted items!
        assertEquals(2, emissions.size)
        assertEquals(1, emissions[1].size)
        assertEquals("t1", emissions[1][0].id)
        assertEquals("משימה ראשונה", emissions[1][0].title)

        job.cancel()
    }

    @Test
    fun observeTasks_householdChanges_resubscribesToNewHousehold() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val householdSession = DefaultHouseholdSession(initialHouseholdId = "house_A")

        val lifeHub = mock(LifeHubRepository::class.java)

        val flowA = MutableSharedFlow<List<TaskItem>>(replay = 1)
        val flowB = MutableSharedFlow<List<TaskItem>>(replay = 1)

        val dummyA = Household(id = "house_A", name = "", ownerUid = "")
        val dummyB = Household(id = "house_B", name = "", ownerUid = "")

        `when`(lifeHub.observeMergedTasks(dummyA)).thenReturn(flowA)
        `when`(lifeHub.observeMergedTasks(dummyB)).thenReturn(flowB)

        val repository = FirestoreTaskRepository(lifeHub, householdSession)

        val emissions = mutableListOf<List<com.menash.myself.core.domain.model.Task>>()
        val job = launch(testDispatcher) {
            repository.observeTasks(VisibilityFilter.ALL).collect {
                emissions.add(it)
            }
        }

        // Emit for house A
        flowA.emit(listOf(TaskItem(id = "item_a", title = "Task A", householdId = "house_A", userId = "u1")))
        assertEquals(1, emissions.size)
        assertEquals("item_a", emissions[0][0].id)

        // Switch to house B
        householdSession.setHouseholdId("house_B")
        flowB.emit(listOf(TaskItem(id = "item_b", title = "Task B", householdId = "house_B", userId = "u1")))

        assertEquals(2, emissions.size)
        assertEquals("item_b", emissions[1][0].id)

        job.cancel()
    }
}
