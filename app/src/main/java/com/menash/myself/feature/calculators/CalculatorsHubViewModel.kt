package com.menash.myself.feature.calculators

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.AiSubscriptionRepository
import com.menash.myself.core.data.repository.CarRepository
import com.menash.myself.core.data.repository.PensionFundRepository
import com.menash.myself.core.data.repository.SettingsRepository
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.AiSubscription
import com.menash.myself.core.domain.model.CalculatorSettings
import com.menash.myself.core.domain.model.Car
import com.menash.myself.core.domain.model.PensionFund
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CalculatorsHubViewModel(
    private val carRepository: CarRepository,
    private val aiSubscriptionRepository: AiSubscriptionRepository,
    private val pensionFundRepository: PensionFundRepository,
    private val settingsRepository: SettingsRepository,
    errorReporter: ErrorReporter
) : BaseViewModel(errorReporter) {

    val settings: StateFlow<CalculatorSettings?> = settingsRepository.getSettingsFlow()
        .catch { e ->
            if (e is Exception) handleException("getSettingsFlow", e) else throw e
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val cars: StateFlow<List<Car>?> = carRepository.getAllCars()
        .map<List<Car>, List<Car>?> { it }
        .catch { e ->
            if (e is Exception) handleException("getAllCars", e) else throw e
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val aiSubs: StateFlow<List<AiSubscription>?> = aiSubscriptionRepository.getAllAiSubscriptions()
        .map<List<AiSubscription>, List<AiSubscription>?> { it }
        .catch { e ->
            if (e is Exception) handleException("getAllAiSubscriptions", e) else throw e
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pensionFunds: StateFlow<List<PensionFund>?> = pensionFundRepository.getAllPensionFunds()
        .map<List<PensionFund>, List<PensionFund>?> { it }
        .catch { e ->
            if (e is Exception) handleException("getAllPensionFunds", e) else throw e
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<UiState<Boolean>> = combine(cars, aiSubs, pensionFunds, _errorState) { c, a, p, err ->
        when {
            err != null && c.isNullOrEmpty() && a.isNullOrEmpty() && p.isNullOrEmpty() -> UiState.Error(err)
            c == null || a == null || p == null -> UiState.Loading
            else -> UiState.Content(true)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun saveRate(newRate: Double) {
        safeLaunch("saveRate") {
            val current = settings.value ?: CalculatorSettings()
            settingsRepository.saveSettings(current.copy(usdToIlsRate = newRate))
        }
    }

    fun createAndShareBackup(context: Context) {
        safeLaunch("createAndShareBackup") {
            settingsRepository.createAndShareBackup(context)
        }
    }

    fun restoreFromJson(context: Context, jsonString: String, onResult: (Boolean) -> Unit = {}) {
        safeLaunch(
            action = "restoreFromJson",
            onError = { onResult(false) }
        ) {
            val ok = settingsRepository.restoreFromJson(context, jsonString)
            onResult(ok)
        }
    }

    companion object {
        fun factory(
            carRepository: CarRepository,
            aiSubscriptionRepository: AiSubscriptionRepository,
            pensionFundRepository: PensionFundRepository,
            settingsRepository: SettingsRepository,
            errorReporter: ErrorReporter
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CalculatorsHubViewModel(
                    carRepository,
                    aiSubscriptionRepository,
                    pensionFundRepository,
                    settingsRepository,
                    errorReporter
                ) as T
            }
        }
    }
}
