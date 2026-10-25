package com.menash.myself.core.data.repository

import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun observeTasks(spaceFilter: String? = null): Flow<List<Task>>

    suspend fun createTask(
        title: String,
        nextAction: String = "",
        isTop3: Boolean = false,
        isFiveBefore11: Boolean = false,
        dueDate: LocalDate? = null,
        forWhom: ForWhom = ForWhom.ME,
        isPrivate: Boolean = false,
        tag: String = ""
    )

    suspend fun toggleTaskDone(taskId: String, currentDone: Boolean)
    suspend fun setTaskTop3(taskId: String, isTop3: Boolean)
    suspend fun setTaskFiveBefore11(taskId: String, isFive: Boolean)
    suspend fun updateTaskDate(taskId: String, date: LocalDate?)
    suspend fun deleteTask(taskId: String)
}
