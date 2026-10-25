package com.menash.myself.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared base ViewModel providing [safeLaunch] and [handleException] to route
 * action/flow errors into [errorState] and [ErrorReporter] while always rethrowing
 * [CancellationException].
 */
abstract class BaseViewModel(
    protected val errorReporter: ErrorReporter? = null
) : ViewModel() {

    protected val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    open fun clearError() {
        _errorState.value = null
        errorReporter?.clearError()
    }

    protected fun handleException(action: String, e: Throwable) {
        if (e is CancellationException) throw e
        val msg = e.localizedMessage ?: e.message ?: "שגיאה ב-$action"
        _errorState.value = msg
        errorReporter?.reportError(action, e)
    }

    protected fun safeLaunch(
        action: String,
        onError: (Throwable) -> Unit = {},
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                handleException(action, e)
                onError(e)
            }
        }
    }
}
