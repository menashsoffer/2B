package com.menash.myself.feature.calculators.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.repository.AiSubscriptionRepository
import com.menash.myself.core.data.repository.SettingsRepository
import com.menash.myself.data.entities.AiSubscriptionEntity
import com.menash.myself.data.entities.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AiCalculatorViewModel(
    private val aiSubscriptionRepository: AiSubscriptionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val subscriptions: StateFlow<List<AiSubscriptionEntity>> = aiSubscriptionRepository.getAllAiSubscriptions()
        .map { list -> list.map { ModelMappers.aiSubscriptionToEntity(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertAiSubscription(sub: AiSubscriptionEntity) {
        viewModelScope.launch {
            aiSubscriptionRepository.insertAiSubscription(ModelMappers.aiSubscriptionToDomain(sub))
        }
    }

    fun insertAiSubscriptions(subs: List<AiSubscriptionEntity>) {
        viewModelScope.launch {
            aiSubscriptionRepository.insertAiSubscriptions(subs.map { ModelMappers.aiSubscriptionToDomain(it) })
        }
    }

    fun updateAiSubscription(sub: AiSubscriptionEntity) {
        viewModelScope.launch {
            aiSubscriptionRepository.updateAiSubscription(ModelMappers.aiSubscriptionToDomain(sub))
        }
    }

    fun deleteAiSubscription(sub: AiSubscriptionEntity) {
        viewModelScope.launch {
            aiSubscriptionRepository.deleteAiSubscription(ModelMappers.aiSubscriptionToDomain(sub))
        }
    }

    companion object {
        fun factory(
            aiSubscriptionRepository: AiSubscriptionRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AiCalculatorViewModel(aiSubscriptionRepository, settingsRepository) as T
            }
        }
    }
}
