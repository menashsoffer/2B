package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.mapper.TaskMappers
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.data.session.HouseholdSession
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.Household
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreTaskRepository(
    private val lifeHub: LifeHubRepository,
    private val householdSession: HouseholdSession
) : TaskRepository {

    override fun observeTasks(filter: VisibilityFilter): Flow<List<Task>> {
        return householdSession.currentHouseholdId.flatMapLatest { hid ->
            if (hid.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                val dummyHousehold = Household(id = hid, name = "", ownerUid = "")
                lifeHub.observeMergedTasks(dummyHousehold)
                    .map { list ->
                        list.map { TaskMappers.taskToDomain(it) }
                            .filter { task ->
                                when (filter) {
                                    VisibilityFilter.ALL -> true
                                    VisibilityFilter.MINE_AND_BOTH ->
                                        task.forWhom == ForWhom.ME || task.forWhom == ForWhom.BOTH
                                }
                            }
                    }
            }
        }
    }

    override suspend fun createTask(
        title: String,
        nextAction: String,
        isTop3: Boolean,
        isFiveBefore11: Boolean,
        dueDate: LocalDate?,
        forWhom: ForWhom,
        isPrivate: Boolean,
        tag: String
    ) {
        val location = lifeHub.locationFor("createTask", isPrivate, householdSession)
        val legacySpace = ModelMappers.forWhomToLegacySpace(forWhom, isPrivate)
        lifeHub.createTask(
            title = title,
            nextAction = nextAction,
            isTop3 = isTop3,
            isFiveBefore11 = isFiveBefore11,
            dueDate = ModelMappers.formatLocalDate(dueDate),
            space = legacySpace,
            location = location
        )
    }

    override suspend fun toggleTaskDone(taskId: String, currentDone: Boolean, isPrivate: Boolean) {
        val location = lifeHub.locationFor("toggleTaskDone", isPrivate, householdSession)
        lifeHub.toggleTaskDone(taskId, currentDone, location)
    }

    override suspend fun setTaskTop3(taskId: String, isTop3: Boolean, isPrivate: Boolean) {
        val location = lifeHub.locationFor("setTaskTop3", isPrivate, householdSession)
        lifeHub.setTaskTop3(taskId, isTop3, location)
    }

    override suspend fun setTaskFiveBefore11(taskId: String, isFive: Boolean, isPrivate: Boolean) {
        val location = lifeHub.locationFor("setTaskFiveBefore11", isPrivate, householdSession)
        lifeHub.setTaskFiveBefore11(taskId, isFive, location)
    }

    override suspend fun updateTaskDate(taskId: String, date: LocalDate?, isPrivate: Boolean) {
        val location = lifeHub.locationFor("updateTaskDate", isPrivate, householdSession)
        lifeHub.updateTaskDate(
            taskId = taskId,
            date = ModelMappers.formatLocalDate(date),
            location = location
        )
    }

    override suspend fun deleteTask(taskId: String, isPrivate: Boolean) {
        val location = lifeHub.locationFor("deleteTask", isPrivate, householdSession)
        lifeHub.deleteTask(taskId = taskId, location = location)
    }
}
