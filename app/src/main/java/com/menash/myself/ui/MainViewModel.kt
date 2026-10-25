package com.menash.myself.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.di.AppContainer
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Household
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val appContainer: AppContainer
) : ViewModel() {

    val errorFlow: Flow<String?> = appContainer.errorFlow

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    val household: StateFlow<Household?> = appContainer.householdRepository.observeMyHousehold()
        .catch { e -> handleException("observeHousehold", e) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun handleException(action: String, e: Throwable) {
        if (e is CancellationException) throw e
        val msg = e.localizedMessage ?: e.message ?: "שגיאה ב-$action"
        _errorState.value = msg
        appContainer.errorReporter.reportError(action, e)
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

    fun initializeHouseholdForUser() {
        launchCatching("initializeHouseholdForUser") {
            appContainer.householdRepository.ensureHouseholdExists()
        }
    }

    fun addInboxItem(
        text: String,
        forWhom: ForWhom = ForWhom.BOTH,
        isPrivate: Boolean = false
    ) {
        launchCatching("addInboxItem") {
            appContainer.inboxRepository.addInboxItem(text, forWhom, isPrivate)
        }
    }

    fun clearErrors() {
        _errorState.value = null
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
