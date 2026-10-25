package com.menash.myself.core.domain.model

/**
 * Standard screen state representation for all screens:
 * Loading / Empty / Error / Content.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
}
