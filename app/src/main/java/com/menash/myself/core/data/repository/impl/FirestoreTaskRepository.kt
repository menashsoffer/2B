package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.Household
import com.menash.myself.data.models.TaskItem
import kotlinx.coroutines.flow.Flow

class FirestoreTaskRepository(private val lifeHub: LifeHubRepository) : TaskRepository {
    override fun observeTasks(spaceFilter: String?, household: Household?): Flow<List<TaskItem>> =
        lifeHub.observeTasks(spaceFilter, household)

    override suspend fun createTask(
        title: String,
        nextAction: String,
        isTop3: Boolean,
        isFiveBefore11: Boolean,
        dueDate: String,
        space: String,
        household: Household?
    ) = lifeHub.createTask(title, nextAction, isTop3, isFiveBefore11, dueDate, space, household)

    override suspend fun toggleTaskDone(task: TaskItem) = lifeHub.toggleTaskDone(task)

    override suspend fun setTaskTop3(task: TaskItem, isTop3: Boolean) = lifeHub.setTaskTop3(task, isTop3)

    override suspend fun setTaskFiveBefore11(task: TaskItem, isFive: Boolean) = lifeHub.setTaskFiveBefore11(task, isFive)

    override suspend fun updateTaskDate(taskId: String, date: String, householdId: String) =
        lifeHub.updateTaskDate(taskId, date, householdId)

    override suspend fun deleteTask(taskId: String, householdId: String) =
        lifeHub.deleteTask(taskId, householdId)
}
