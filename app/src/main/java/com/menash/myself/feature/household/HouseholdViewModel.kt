package com.menash.myself.feature.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HouseholdViewModel(
    private val householdRepository: HouseholdRepositoryInterface,
    errorReporter: ErrorReporter? = null
) : BaseViewModel(errorReporter) {

    val household: StateFlow<Household?> = householdRepository.observeMyHousehold()
        .catch { e -> handleException("observeHousehold", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<UiState<Household?>> = combine(household, _errorState) { h, error ->
        when {
            error != null && h == null -> UiState.Error(error)
            else -> UiState.Content(h)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Content(null))

    fun invitePartner(householdId: String, partnerEmail: String) {
        safeLaunch("invitePartner") {
            householdRepository.invitePartner(householdId, partnerEmail)
        }
    }

    fun ensureHouseholdExists() {
        safeLaunch("ensureHouseholdExists") {
            householdRepository.ensureHouseholdExists()
        }
    }

    companion object {
        fun factory(
            householdRepository: HouseholdRepositoryInterface,
            errorReporter: ErrorReporter? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HouseholdViewModel(householdRepository, errorReporter) as T
                }
            }
    }
}
