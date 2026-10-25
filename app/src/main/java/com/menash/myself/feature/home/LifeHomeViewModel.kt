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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LifeHomeViewModel(
    private val taskRepository: TaskRepository,
    private val dreamRepository: DreamRepository,
    private val inboxRepository: InboxRepository,
    private val fitnessRepository: FitnessRepository,
    private val householdRepository: HouseholdRepositoryInterface,
    private val errorReporter: ErrorReporter? = null
) : ViewModel() {

    private val _visibilityFilter = MutableStateFlow(VisibilityFilter.MINE_AND_BOTH)
    val visibilityFilter: StateFlow<VisibilityFilter> = _visibilityFilter.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    val household: StateFlow<Household?> = householdRepository.observeMyHousehold()
        .catch { e -> handleException("observeHousehold", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tasks: StateFlow<List<Task>> = _visibilityFilter
        .flatMapLatest { filter -> taskRepository.observeTasks(filter) }
        .catch { e ->
            handleException("observeTasks", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val dreams: StateFlow<List<Dream>> = _visibilityFilter
        .flatMapLatest { filter -> dreamRepository.observeDreams(listFilter = null, filter = filter) }
        .catch { e ->
            handleException("observeDreams", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val inboxItems: StateFlow<List<InboxItem>> = inboxRepository.observeInbox()
        .catch { e ->
            handleException("observeInbox", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayFitness: StateFlow<FitnessEntry?> = fitnessRepository.observeTodayFitness()
        .catch { e ->
            handleException("observeTodayFitness", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weeklyFitness: StateFlow<List<FitnessEntry>> = fitnessRepository.observeWeeklyFitness()
        .catch { e ->
            handleException("observeWeeklyFitness", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<UiState<List<Task>>> = combine(tasks, dreams, _errorState) { taskList, dreamList, error ->
        when {
            error != null && taskList.isEmpty() && dreamList.isEmpty() -> UiState.Error(error)
            taskList.isEmpty() && dreamList.isEmpty() -> UiState.Empty
            else -> UiState.Content(taskList)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun setVisibilityFilter(filter: VisibilityFilter) {
        _visibilityFilter.value = filter
    }

    fun clearError() {
        _errorState.value = null
        errorReporter?.clearError()
    }

    private fun handleException(action: String, e: Throwable) {
        if (e is CancellationException) throw e
        val msg = e.localizedMessage ?: e.message ?: "שגיאה ב-$action"
        _errorState.value = msg
        errorReporter?.reportError(action, e)
    }

    private fun launchCatching(action: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException(action, e)
            }
        }
    }

    fun toggleTaskDone(task: Task) {
        launchCatching("toggleTaskDone") {
            taskRepository.toggleTaskDone(task.id, task.isDone, task.isPrivate)
        }
    }

    fun saveTodayFitness(moved: Boolean, minutes: Int, type: String) {
        launchCatching("saveTodayFitness") {
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
