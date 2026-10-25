package com.menash.myself.feature.calculators.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.AiSubscriptionRepository
import com.menash.myself.core.data.repository.SettingsRepository
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.AiSubscription
import com.menash.myself.core.domain.model.CalculatorSettings
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

class AiCalculatorViewModel(
    private val aiSubscriptionRepository: AiSubscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val errorReporter: ErrorReporter? = null
) : ViewModel() {

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    val settings: StateFlow<CalculatorSettings?> = settingsRepository.getSettingsFlow()
        .catch { e ->
            handleException("getSettingsFlow", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subscriptions: StateFlow<List<AiSubscription>> = aiSubscriptionRepository.getAllAiSubscriptions()
        .catch { e ->
            handleException("getAllAiSubscriptions", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<UiState<List<AiSubscription>>> = combine(subscriptions, _errorState) { list, error ->
        when {
            error != null && list.isEmpty() -> UiState.Error(error)
            list.isEmpty() -> UiState.Empty
            else -> UiState.Content(list)
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

    fun insertAiSubscription(sub: AiSubscription) {
        launchCatching("insertAiSubscription") {
            aiSubscriptionRepository.insertAiSubscription(sub)
        }
    }

    fun insertAiSubscriptions(subs: List<AiSubscription>) {
        launchCatching("insertAiSubscriptions") {
            aiSubscriptionRepository.insertAiSubscriptions(subs)
        }
    }

    fun updateAiSubscription(sub: AiSubscription) {
        launchCatching("updateAiSubscription") {
            aiSubscriptionRepository.updateAiSubscription(sub)
        }
    }

    fun deleteAiSubscription(sub: AiSubscription) {
        launchCatching("deleteAiSubscription") {
            aiSubscriptionRepository.deleteAiSubscription(sub)
        }
    }

    companion object {
        fun factory(
            aiSubscriptionRepository: AiSubscriptionRepository,
            settingsRepository: SettingsRepository,
            errorReporter: ErrorReporter? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AiCalculatorViewModel(aiSubscriptionRepository, settingsRepository, errorReporter) as T
            }
        }
    }
}
