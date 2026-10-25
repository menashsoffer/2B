package com.menash.myself.feature.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.FitnessRepository
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.FitnessEntry
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FitnessViewModel(
    private val fitnessRepository: FitnessRepository,
    errorReporter: ErrorReporter? = null
) : BaseViewModel(errorReporter) {

    val todayFitness: StateFlow<FitnessEntry?> = fitnessRepository.observeTodayFitness()
        .catch { e ->
            handleException("observeTodayFitness", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weeklyLogs: StateFlow<List<FitnessEntry>?> = fitnessRepository.observeWeeklyFitness()
        .map<List<FitnessEntry>, List<FitnessEntry>?> { it }
        .catch { e ->
            handleException("observeWeeklyFitness", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<UiState<List<FitnessEntry>>> = combine(weeklyLogs, _errorState) { logs, error ->
        when {
            error != null && logs.isNullOrEmpty() -> UiState.Error(error)
            logs == null -> UiState.Loading
            logs.isEmpty() -> UiState.Empty
            else -> UiState.Content(logs)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun saveTodayFitness(moved: Boolean, minutes: Int, type: String) {
        safeLaunch("saveTodayFitness") {
            fitnessRepository.saveTodayFitness(moved, minutes, type)
        }
    }

    companion object {
        fun factory(
            fitnessRepository: FitnessRepository,
            errorReporter: ErrorReporter? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FitnessViewModel(fitnessRepository, errorReporter) as T
            }
        }
    }
}
