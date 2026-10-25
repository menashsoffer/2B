package com.menash.myself.feature.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.FitnessRepository
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.FitnessEntry
import com.menash.myself.core.domain.model.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FitnessViewModel(
    private val fitnessRepository: FitnessRepository,
    private val errorReporter: ErrorReporter? = null
) : ViewModel() {

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    val todayFitness: StateFlow<FitnessEntry?> = fitnessRepository.observeTodayFitness()
        .catch { e ->
            handleException("observeTodayFitness", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weeklyLogs: StateFlow<List<FitnessEntry>> = fitnessRepository.observeWeeklyFitness()
        .catch { e ->
            handleException("observeWeeklyFitness", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<UiState<List<FitnessEntry>>> = combine(weeklyLogs, _errorState) { logs, error ->
        when {
            error != null && logs.isEmpty() -> UiState.Error(error)
            logs.isEmpty() -> UiState.Empty
            else -> UiState.Content(logs)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

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

    fun saveTodayFitness(moved: Boolean, minutes: Int, type: String) {
        launchCatching("saveTodayFitness") {
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
