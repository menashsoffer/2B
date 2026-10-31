package com.menash.myself.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.di.AppContainer
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.ui.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    private val appContainer: AppContainer
) : BaseViewModel(appContainer.errorReporter) {

    val errorFlow: Flow<String?> = appContainer.errorFlow

    val household: StateFlow<Household?> = appContainer.householdRepository.observeMyHousehold()
        .catch { e -> if (e is Exception) handleException("observeHousehold", e) else throw e }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun initializeHouseholdForUser() {
        safeLaunch("initializeHouseholdForUser") {
            appContainer.householdRepository.ensureHouseholdExists()
        }
        safeLaunch("refreshExternalDataOnStartup") {
            runCatching { appContainer.exchangeRateRepository.refreshRateIfNeeded() }
            runCatching { appContainer.hebcalRepository.refreshDataIfNeeded() }
        }
    }

    fun clearErrors() {
        clearError()
        appContainer.clearErrors()
    }

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(appContainer) as T
                }
            }
    }
}
