package com.menash.myself.core.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared error state reporter for ViewModels and repositories.
 * Ensures exceptions from async actions are captured into observable UI state instead of crashing.
 */
interface ErrorReporter {
    val errorState: StateFlow<String?>
    fun reportError(message: String)
    fun reportError(action: String, throwable: Throwable)
    fun clearError()
}

class DefaultErrorReporter : ErrorReporter {
    private val _errorState = MutableStateFlow<String?>(null)
    override val errorState: StateFlow<String?> = _errorState.asStateFlow()

    override fun reportError(message: String) {
        _errorState.value = message
    }

    override fun reportError(action: String, throwable: Throwable) {
        val detail = throwable.localizedMessage ?: throwable.message ?: "שגיאה לא צפויה"
        _errorState.value = "שגיאה ב-$action: $detail"
    }

    override fun clearError() {
        _errorState.value = null
    }
}
