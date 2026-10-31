package com.menash.myself.core.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** One-off feedback; events are not replayed when a screen is recreated. */
sealed interface UiEvent {
    data object MONEY_SAVED : UiEvent
    data object DONATION_SAVED : UiEvent
    data object DELETED : UiEvent
    data class ImportDone(val written: Int, val unassignedMonth: java.time.YearMonth?) : UiEvent
    data class CategoryAssigned(val name: String) : UiEvent
    data class SaveError(val message: String, val retry: () -> Unit) : UiEvent
    data class MonthClosed(val month: java.time.YearMonth) : UiEvent
}

class UiEvents {
    private val mutableEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events = mutableEvents.asSharedFlow()
    private val mutableUnassigned = kotlinx.coroutines.flow.MutableStateFlow<java.time.YearMonth?>(null)
    val unassigned = mutableUnassigned.asStateFlow()
    fun openUnassigned(month: java.time.YearMonth) { mutableUnassigned.value = month }
    fun clearUnassigned() { mutableUnassigned.value = null }

    fun send(event: UiEvent) { mutableEvents.tryEmit(event) }
}
