package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.data.session.HouseholdSession
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Task
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.Household
import com.menash.myself.data.models.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FirestoreTaskRepository(
    private val lifeHub: LifeHubRepository,
    private val householdSession: HouseholdSession
) : TaskRepository {

    override fun observeTasks(spaceFilter: String?): Flow<List<Task>> {
        val hid = householdSession.currentHouseholdId.value
        if (hid.isNullOrBlank()) {
            return emptyFlow()
        }
        val dummyHousehold = Household(id = hid, name = "", ownerUid = "")
        return lifeHub.observeTasks(spaceFilter = spaceFilter, household = dummyHousehold)
            .map { list -> list.map { ModelMappers.taskToDomain(it) } }
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
        val hid = householdSession.requireHouseholdId()
        val legacySpace = ModelMappers.forWhomToLegacySpace(forWhom, isPrivate)
        val dummyHousehold = Household(id = hid, name = "", ownerUid = "")
        lifeHub.createTask(
            title = title,
            nextAction = nextAction,
            isTop3 = isTop3,
            isFiveBefore11 = isFiveBefore11,
            dueDate = ModelMappers.formatLocalDate(dueDate),
            space = legacySpace,
            household = dummyHousehold
        )
    }

    override suspend fun toggleTaskDone(taskId: String, currentDone: Boolean) {
        val hid = householdSession.requireHouseholdId()
        val stub = TaskItem(id = taskId, householdId = hid, isDone = currentDone)
        lifeHub.toggleTaskDone(stub)
    }

    override suspend fun setTaskTop3(taskId: String, isTop3: Boolean) {
        val hid = householdSession.requireHouseholdId()
        val stub = TaskItem(id = taskId, householdId = hid)
        lifeHub.setTaskTop3(stub, isTop3)
    }

    override suspend fun setTaskFiveBefore11(taskId: String, isFive: Boolean) {
        val hid = householdSession.requireHouseholdId()
        val stub = TaskItem(id = taskId, householdId = hid)
        lifeHub.setTaskFiveBefore11(stub, isFive)
    }

    override suspend fun updateTaskDate(taskId: String, date: LocalDate?) {
        val hid = householdSession.requireHouseholdId()
        lifeHub.updateTaskDate(taskId = taskId, date = ModelMappers.formatLocalDate(date), householdId = hid)
    }

    override suspend fun deleteTask(taskId: String) {
        val hid = householdSession.requireHouseholdId()
        lifeHub.deleteTask(taskId = taskId, householdId = hid)
    }
}
