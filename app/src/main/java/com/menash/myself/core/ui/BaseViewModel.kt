package com.menash.myself.core.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared base ViewModel providing [safeLaunch] and [handleException] to route
 * action/flow errors into friendly Hebrew [errorState] and [ErrorReporter] while
 * always rethrowing [CancellationException] and propagating JVM [Error]s.
 */
abstract class BaseViewModel : ViewModel {

    protected val errorReporter: ErrorReporter

    constructor(errorReporter: ErrorReporter) : super() {
        this.errorReporter = errorReporter
    }

    protected constructor(errorReporter: ErrorReporter, scope: CoroutineScope) : super(scope) {
        this.errorReporter = errorReporter
    }

    protected val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    open fun clearError() {
        _errorState.value = null
        errorReporter.clearError()
    }

    protected fun handleException(action: String, e: Exception) {
        runCatching {
            Log.e("BaseViewModel", "Action '$action' failed with technical error: ${e.message}", e)
        }
        val friendlyMessage = ErrorMapper.toFriendlyMessage(e)
        _errorState.value = friendlyMessage
        errorReporter.reportError(action, e)
    }

    protected fun safeLaunch(
        action: String,
        onError: (Exception) -> Unit = {},
        reportError: Boolean = true,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (reportError) handleException(action, e)
                onError(e)
            } catch (t: Throwable) {
                throw t
            }
        }
    }
}
