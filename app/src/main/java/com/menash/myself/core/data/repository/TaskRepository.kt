package com.menash.myself.core.data.repository

import com.menash.myself.data.models.Household
import com.menash.myself.data.models.TaskItem
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(spaceFilter: String? = null, household: Household? = null): Flow<List<TaskItem>>
    suspend fun createTask(
        title: String,
        nextAction: String = "",
        isTop3: Boolean = false,
        isFiveBefore11: Boolean = false,
        dueDate: String = "",
        space: String = "shared",
        household: Household? = null
    )
    suspend fun toggleTaskDone(task: TaskItem)
    suspend fun setTaskTop3(task: TaskItem, isTop3: Boolean)
    suspend fun setTaskFiveBefore11(task: TaskItem, isFive: Boolean)
    suspend fun updateTaskDate(taskId: String, date: String, householdId: String = "")
    suspend fun deleteTask(taskId: String, householdId: String = "")
}
