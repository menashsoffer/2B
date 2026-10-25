package com.menash.myself.feature.calculators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.repository.AiSubscriptionRepository
import com.menash.myself.core.data.repository.CarRepository
import com.menash.myself.core.data.repository.PensionFundRepository
import com.menash.myself.core.data.repository.SettingsRepository
import com.menash.myself.data.entities.AiSubscriptionEntity
import com.menash.myself.data.entities.AppSettings
import com.menash.myself.data.entities.CarEntity
import com.menash.myself.data.entities.PensionFundEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalculatorsHubViewModel(
    private val carRepository: CarRepository,
    private val aiSubscriptionRepository: AiSubscriptionRepository,
    private val pensionFundRepository: PensionFundRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val cars: StateFlow<List<CarEntity>> = carRepository.getAllCars()
        .map { list -> list.map { ModelMappers.carToEntity(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiSubs: StateFlow<List<AiSubscriptionEntity>> = aiSubscriptionRepository.getAllAiSubscriptions()
        .map { list -> list.map { ModelMappers.aiSubscriptionToEntity(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pensionFunds: StateFlow<List<PensionFundEntity>> = pensionFundRepository.getAllPensionFunds()
        .map { list -> list.map { ModelMappers.pensionFundToEntity(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveUsdRate(newRate: Double) {
        viewModelScope.launch {
            val current = settings.value ?: AppSettings()
            settingsRepository.saveSettings(current.copy(usdToIlsRate = newRate))
        }
    }

    companion object {
        fun factory(
            carRepository: CarRepository,
            aiSubscriptionRepository: AiSubscriptionRepository,
            pensionFundRepository: PensionFundRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CalculatorsHubViewModel(
                    carRepository,
                    aiSubscriptionRepository,
                    pensionFundRepository,
                    settingsRepository
                ) as T
            }
        }
    }
}
