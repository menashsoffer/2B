package com.menash.myself.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.repository.FitnessRepository
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.data.repository.InboxRepository
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.FitnessEntry
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.domain.model.InboxItem
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

@OptIn(ExperimentalCoroutinesApi::class)
class LifeHomeViewModel(
    private val taskRepository: TaskRepository,
    private val dreamRepository: DreamRepository,
    private val inboxRepository: InboxRepository,
    private val fitnessRepository: FitnessRepository,
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

    val dreams: StateFlow<List<Dream>?> = _visibilityFilter
        .flatMapLatest { filter ->
            dreamRepository.observeDreams(listFilter = null, filter = filter)
                .map<List<Dream>, List<Dream>?> { it }
        }
        .catch { e ->
            handleException("observeDreams", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val inboxItems: StateFlow<List<InboxItem>?> = inboxRepository.observeInbox()
        .map<List<InboxItem>, List<InboxItem>?> { it }
        .catch { e ->
            handleException("observeInbox", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val todayFitness: StateFlow<FitnessEntry?> = fitnessRepository.observeTodayFitness()
        .catch { e ->
            handleException("observeTodayFitness", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weeklyFitness: StateFlow<List<FitnessEntry>?> = fitnessRepository.observeWeeklyFitness()
        .map<List<FitnessEntry>, List<FitnessEntry>?> { it }
        .catch { e ->
            handleException("observeWeeklyFitness", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<UiState<List<Task>>> = combine(tasks, dreams, _errorState) { taskList, dreamList, error ->
        when {
            error != null && taskList.isNullOrEmpty() && dreamList.isNullOrEmpty() -> UiState.Error(error)
            taskList == null || dreamList == null -> UiState.Loading
            taskList.isEmpty() && dreamList.isEmpty() -> UiState.Empty
            else -> UiState.Content(taskList)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun setVisibilityFilter(filter: VisibilityFilter) {
        _visibilityFilter.value = filter
    }

    fun toggleTaskDone(task: Task) {
        safeLaunch("toggleTaskDone") {
            taskRepository.toggleTaskDone(task.id, task.isDone, task.isPrivate)
        }
    }

    fun saveTodayFitness(moved: Boolean, minutes: Int, type: String) {
        safeLaunch("saveTodayFitness") {
            fitnessRepository.saveTodayFitness(moved, minutes, type)
        }
    }

    companion object {
        fun factory(
            taskRepository: TaskRepository,
            dreamRepository: DreamRepository,
            inboxRepository: InboxRepository,
            fitnessRepository: FitnessRepository,
            householdRepository: HouseholdRepositoryInterface,
            errorReporter: ErrorReporter? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LifeHomeViewModel(
                    taskRepository,
                    dreamRepository,
                    inboxRepository,
                    fitnessRepository,
                    householdRepository,
                    errorReporter
                ) as T
            }
        }
    }
}
