package com.menash.myself.feature.money

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Files shared into the app (WhatsApp, mail, Files) waiting for the money tab to read them.
 * Holds content URI strings only; the money screen reads the bytes and then [take]s them.
 */
class ImportInbox {
    private val _pending = MutableStateFlow<List<String>>(emptyList())
    val pending: StateFlow<List<String>> = _pending.asStateFlow()

    fun offer(uris: List<String>) {
        if (uris.isNotEmpty()) _pending.value = (_pending.value + uris).distinct()
    }

    /** Returns what was waiting and empties the inbox, so a file is read once. */
    fun take(): List<String> {
        val taken = _pending.value
        _pending.value = emptyList()
        return taken
    }
}
