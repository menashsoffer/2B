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
import com.menash.myself.core.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AiCalculatorViewModel(
    private val aiSubscriptionRepository: AiSubscriptionRepository,
    private val settingsRepository: SettingsRepository,
    errorReporter: ErrorReporter
) : BaseViewModel(errorReporter) {

    val settings: StateFlow<CalculatorSettings?> = settingsRepository.getSettingsFlow()
        .catch { e ->
            if (e is Exception) handleException("getSettingsFlow", e) else throw e
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subscriptions: StateFlow<List<AiSubscription>?> = aiSubscriptionRepository.getAllAiSubscriptions()
        .map<List<AiSubscription>, List<AiSubscription>?> { it }
        .catch { e ->
            if (e is Exception) handleException("getAllAiSubscriptions", e) else throw e
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<UiState<List<AiSubscription>>> = combine(subscriptions, _errorState) { list, error ->
        when {
            error != null && list.isNullOrEmpty() -> UiState.Error(error)
            list == null -> UiState.Loading
            list.isEmpty() -> UiState.Empty
            else -> UiState.Content(list)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun insertAiSubscription(sub: AiSubscription) {
        safeLaunch("insertAiSubscription") {
            aiSubscriptionRepository.insertAiSubscription(sub)
        }
    }

    fun insertAiSubscriptions(subs: List<AiSubscription>) {
        safeLaunch("insertAiSubscriptions") {
            aiSubscriptionRepository.insertAiSubscriptions(subs)
        }
    }

    fun updateAiSubscription(sub: AiSubscription) {
        safeLaunch("updateAiSubscription") {
            aiSubscriptionRepository.updateAiSubscription(sub)
        }
    }

    fun deleteAiSubscription(sub: AiSubscription) {
        safeLaunch("deleteAiSubscription") {
            aiSubscriptionRepository.deleteAiSubscription(sub)
        }
    }

    companion object {
        fun factory(
            aiSubscriptionRepository: AiSubscriptionRepository,
            settingsRepository: SettingsRepository,
            errorReporter: ErrorReporter
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AiCalculatorViewModel(aiSubscriptionRepository, settingsRepository, errorReporter) as T
            }
        }
    }
}
