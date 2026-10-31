package com.menash.myself.core.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** One-off feedback; events are not replayed when a screen is recreated. */
sealed interface UiEvent {
    data object MONEY_SAVED : UiEvent
    data object DONATION_SAVED : UiEvent
    data object DELETED : UiEvent
    data class ImportDone(val written: Int) : UiEvent
    data class CategoryAssigned(val name: String) : UiEvent
    data class MonthClosed(val month: java.time.YearMonth) : UiEvent
}

class UiEvents {
    private val mutableEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events = mutableEvents.asSharedFlow()

    fun send(event: UiEvent) { mutableEvents.tryEmit(event) }
}
