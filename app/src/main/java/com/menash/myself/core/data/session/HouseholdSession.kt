package com.menash.myself.core.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the active household session.
 * Repositories read the active household internally.
 * If no household is set, requireHouseholdId throws an explicit IllegalStateException,
 * guaranteeing no write to an empty path.
 */
interface HouseholdSession {
    val currentHouseholdId: StateFlow<String?>
    fun setHouseholdId(id: String?)

    fun requireHouseholdId(): String {
        return currentHouseholdId.value?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Active household session required. User must be in a household.")
    }
}

class DefaultHouseholdSession(initialHouseholdId: String? = null) : HouseholdSession {
    private val _currentHouseholdId = MutableStateFlow<String?>(initialHouseholdId)
    override val currentHouseholdId: StateFlow<String?> = _currentHouseholdId.asStateFlow()

    override fun setHouseholdId(id: String?) {
        _currentHouseholdId.value = id?.trim()?.ifEmpty { null }
    }
}
