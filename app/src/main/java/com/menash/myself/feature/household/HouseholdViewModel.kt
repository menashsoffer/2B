package com.menash.myself.feature.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.Household
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

class HouseholdViewModel(
    private val householdRepository: HouseholdRepositoryInterface,
    private val errorReporter: ErrorReporter? = null
) : ViewModel() {

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    val household: StateFlow<Household?> = householdRepository.observeMyHousehold()
        .catch { e -> handleException("observeHousehold", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<UiState<Household?>> = combine(household, _errorState) { h, error ->
        when {
            error != null && h == null -> UiState.Error(error)
            else -> UiState.Content(h)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Content(null))

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

    fun invitePartner(householdId: String, partnerEmail: String) {
        launchCatching("invitePartner") {
            householdRepository.invitePartner(householdId, partnerEmail)
        }
    }

    fun ensureHouseholdExists() {
        launchCatching("ensureHouseholdExists") {
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
