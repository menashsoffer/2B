package com.menash.myself.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.model.VisibilityFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(
    private val taskRepository: TaskRepository,
    private val householdRepository: HouseholdRepositoryInterface
) : ViewModel() {

    private val _visibilityFilter = MutableStateFlow(VisibilityFilter.ALL)
    val visibilityFilter: StateFlow<VisibilityFilter> = _visibilityFilter.asStateFlow()

    val household: StateFlow<Household?> = householdRepository.observeHousehold()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tasks: StateFlow<List<Task>> = _visibilityFilter
        .flatMapLatest { filter -> taskRepository.observeTasks(filter) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setVisibilityFilter(filter: VisibilityFilter) {
        _visibilityFilter.value = filter
    }

    fun createTask(
        title: String,
        nextAction: String,
        isTop3: Boolean,
        isFiveBefore11: Boolean,
        dueDateStr: String,
        space: String
    ) {
        viewModelScope.launch {
            val (forWhom, isPrivate) = ModelMappers.legacySpaceToForWhom(space)
            val parsedDate = ModelMappers.parseLocalDate(dueDateStr)
            taskRepository.createTask(
                title = title,
                nextAction = nextAction,
                isTop3 = isTop3,
                isFiveBefore11 = isFiveBefore11,
                dueDate = parsedDate,
                forWhom = forWhom,
                isPrivate = isPrivate
            )
        }
    }

    fun toggleTaskDone(task: Task) {
        viewModelScope.launch {
            taskRepository.toggleTaskDone(task.id, task.isDone, task.isPrivate)
        }
    }

    fun setTaskTop3(task: Task, isTop3: Boolean) {
        viewModelScope.launch {
            taskRepository.setTaskTop3(task.id, isTop3, task.isPrivate)
        }
    }

    fun setTaskFiveBefore11(task: Task, isFive: Boolean) {
        viewModelScope.launch {
            taskRepository.setTaskFiveBefore11(task.id, isFive, task.isPrivate)
        }
    }

    fun updateTaskDate(task: Task, dateStr: String) {
        viewModelScope.launch {
            val parsedDate = ModelMappers.parseLocalDate(dateStr)
            taskRepository.updateTaskDate(task.id, parsedDate, task.isPrivate)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task.id, task.isPrivate)
        }
    }

    companion object {
        fun factory(
            taskRepository: TaskRepository,
            householdRepository: HouseholdRepositoryInterface
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TasksViewModel(taskRepository, householdRepository) as T
            }
        }
    }
}
