package com.menash.myself.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.repository.FitnessRepository
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.data.repository.InboxRepository
import com.menash.myself.core.data.repository.TaskRepository
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.FitnessEntry
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.domain.model.InboxItem
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.model.VisibilityFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LifeHomeViewModel(
    private val householdRepository: HouseholdRepositoryInterface,
    private val taskRepository: TaskRepository,
    private val dreamRepository: DreamRepository,
    private val inboxRepository: InboxRepository,
    private val fitnessRepository: FitnessRepository
) : ViewModel() {

    private val _household = MutableStateFlow<Household?>(null)
    val household: StateFlow<Household?> = _household.asStateFlow()

    val allTasks: StateFlow<List<Task>> = taskRepository.observeTasks(VisibilityFilter.ALL)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDreams: StateFlow<List<Dream>> = dreamRepository.observeDreams(null, VisibilityFilter.ALL)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inboxItems: StateFlow<List<InboxItem>> = inboxRepository.observeInbox()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayFitness: StateFlow<FitnessEntry?> = fitnessRepository.observeTodayFitness()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weeklyFitness: StateFlow<List<FitnessEntry>> = fitnessRepository.observeWeeklyFitness()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            runCatching {
                _household.value = householdRepository.getOrCreateHousehold()
            }
            householdRepository.observeHousehold().collect { h ->
                if (h != null) _household.value = h
            }
        }
    }

    fun toggleTaskDone(task: Task) {
        viewModelScope.launch {
            taskRepository.toggleTaskDone(task.id, task.isDone, task.isPrivate)
        }
    }

    fun saveTodayFitness(moved: Boolean, minutes: Int, type: String) {
        viewModelScope.launch {
            fitnessRepository.saveTodayFitness(moved = moved, minutes = minutes, type = type)
        }
    }

    companion object {
        fun factory(
            householdRepository: HouseholdRepositoryInterface,
            taskRepository: TaskRepository,
            dreamRepository: DreamRepository,
            inboxRepository: InboxRepository,
            fitnessRepository: FitnessRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LifeHomeViewModel(
                    householdRepository,
                    taskRepository,
                    dreamRepository,
                    inboxRepository,
                    fitnessRepository
                ) as T
            }
        }
    }
}
