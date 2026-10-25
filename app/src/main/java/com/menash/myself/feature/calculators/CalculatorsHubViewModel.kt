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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalculatorsHubViewModel(
    private val carRepository: CarRepository,
    private val aiSubscriptionRepository: AiSubscriptionRepository,
    private val pensionFundRepository: PensionFundRepository,
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

    val cars: StateFlow<List<Car>> = carRepository.getAllCars()
        .catch { e ->
            handleException("getAllCars", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiSubs: StateFlow<List<AiSubscription>> = aiSubscriptionRepository.getAllAiSubscriptions()
        .catch { e ->
            handleException("getAllAiSubscriptions", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pensionFunds: StateFlow<List<PensionFund>> = pensionFundRepository.getAllPensionFunds()
        .catch { e ->
            handleException("getAllPensionFunds", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<UiState<Boolean>> = combine(cars, aiSubs, pensionFunds, _errorState) { c, a, p, err ->
        when {
            err != null && c.isEmpty() && a.isEmpty() && p.isEmpty() -> UiState.Error(err)
            else -> UiState.Content(true)
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

    fun saveRate(newRate: Double) {
        viewModelScope.launch {
            try {
                val current = settings.value ?: CalculatorSettings()
                settingsRepository.saveSettings(current.copy(usdToIlsRate = newRate))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException("saveRate", e)
            }
        }
    }

    fun createAndShareBackup(context: Context) {
        viewModelScope.launch {
            try {
                settingsRepository.createAndShareBackup(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException("createAndShareBackup", e)
            }
        }
    }

    fun restoreFromJson(context: Context, jsonString: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val ok = settingsRepository.restoreFromJson(context, jsonString)
                onResult(ok)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException("restoreFromJson", e)
                onResult(false)
            }
        }
    }

    companion object {
        fun factory(
            carRepository: CarRepository,
            aiSubscriptionRepository: AiSubscriptionRepository,
            pensionFundRepository: PensionFundRepository,
            settingsRepository: SettingsRepository,
            errorReporter: ErrorReporter? = null
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
