package com.menash.myself.core.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** One-off feedback; events are not replayed when a screen is recreated. */
enum class UiEvent { MONEY_SAVED, DONATION_SAVED, MONTH_CLOSED }

class UiEvents {
    private val mutableEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events = mutableEvents.asSharedFlow()

    fun send(event: UiEvent) { mutableEvents.tryEmit(event) }
}
