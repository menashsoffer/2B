package com.menash.myself.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.core.ui.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(
    private val taskRepository: TaskRepository,
    private val householdRepository: HouseholdRepositoryInterface,
    errorReporter: ErrorReporter? = null
) : BaseViewModel(errorReporter) {

    private val _visibilityFilter = MutableStateFlow(VisibilityFilter.MINE_AND_BOTH)
    val visibilityFilter: StateFlow<VisibilityFilter> = _visibilityFilter.asStateFlow()

    val household: StateFlow<Household?> = householdRepository.observeMyHousehold()
        .catch { e -> handleException("observeHousehold", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tasks: StateFlow<List<Task>?> = _visibilityFilter
        .flatMapLatest { filter ->
            taskRepository.observeTasks(filter).map<List<Task>, List<Task>?> { it }
        }
        .catch { e ->
            handleException("observeTasks", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<UiState<List<Task>>> = combine(tasks, _errorState) { list, error ->
        when {
            error != null && list.isNullOrEmpty() -> UiState.Error(error)
            list == null -> UiState.Loading
            list.isEmpty() -> UiState.Empty
            else -> UiState.Content(list)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun setVisibilityFilter(filter: VisibilityFilter) {
        _visibilityFilter.value = filter
    }

    fun createTask(
        title: String,
        nextAction: String = "",
        isTop3: Boolean = false,
        isFiveBefore11: Boolean = false,
        dueDate: LocalDate? = null,
        forWhom: ForWhom = ForWhom.BOTH,
        isPrivate: Boolean = false,
        tag: String = ""
    ) {
        safeLaunch("createTask") {
            taskRepository.createTask(
                title = title,
                nextAction = nextAction,
                isTop3 = isTop3,
                isFiveBefore11 = isFiveBefore11,
                dueDate = dueDate,
                forWhom = forWhom,
                isPrivate = isPrivate,
                tag = tag
            )
        }
    }

    fun toggleTaskDone(task: Task) {
        safeLaunch("toggleTaskDone") {
            taskRepository.toggleTaskDone(task.id, task.isDone, task.isPrivate)
        }
    }

    fun setTaskTop3(task: Task, isTop3: Boolean) {
        safeLaunch("setTaskTop3") {
            taskRepository.setTaskTop3(task.id, isTop3, task.isPrivate)
        }
    }

    fun setTaskFiveBefore11(task: Task, isFive: Boolean) {
        safeLaunch("setTaskFiveBefore11") {
            taskRepository.setTaskFiveBefore11(task.id, isFive, task.isPrivate)
        }
    }

    fun updateTaskDate(task: Task, dueDate: LocalDate?) {
        safeLaunch("updateTaskDate") {
            taskRepository.updateTaskDate(task.id, dueDate, task.isPrivate)
        }
    }

    fun deleteTask(task: Task) {
        safeLaunch("deleteTask") {
            taskRepository.deleteTask(task.id, task.isPrivate)
        }
    }

    companion object {
        fun factory(
            taskRepository: TaskRepository,
            householdRepository: HouseholdRepositoryInterface,
            errorReporter: ErrorReporter? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TasksViewModel(taskRepository, householdRepository, errorReporter) as T
            }
        }
    }
}
