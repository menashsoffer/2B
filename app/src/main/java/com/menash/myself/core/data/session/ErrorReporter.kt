package com.menash.myself.core.data.session

import android.util.Log
import com.menash.myself.core.crash.CrashlyticsNonFatal
import com.menash.myself.core.ui.ErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared error state reporter for ViewModels and repositories.
 * Ensures exceptions from async actions are captured into observable UI state instead of crashing.
 */
interface ErrorReporter {
    val errorState: StateFlow<String?>
    val lastTechnicalError: String?
    fun reportError(message: String)
    fun reportError(action: String, throwable: Throwable)
    fun clearError()
}

class DefaultErrorReporter : ErrorReporter {
    private val _errorState = MutableStateFlow<String?>(null)
    override val errorState: StateFlow<String?> = _errorState.asStateFlow()

    override var lastTechnicalError: String? = null
        private set

    override fun reportError(message: String) {
        _errorState.value = message
    }

    override fun reportError(action: String, throwable: Throwable) {
        lastTechnicalError = throwable.message
        runCatching {
            Log.e("ErrorReporter", "Action '$action' failed: ${throwable.message}", throwable)
        }
        CrashlyticsNonFatal.record(throwable)
        _errorState.value = ErrorMapper.toFriendlyMessage(throwable)
    }

    override fun clearError() {
        _errorState.value = null
        lastTechnicalError = null
    }
}
