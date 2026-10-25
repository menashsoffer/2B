package com.menash.myself.core.data.model

import com.menash.myself.core.data.session.HouseholdSession

/**
 * Typed storage location for a household or private item inside core/data.
 * Replaces any string-sentinel ("private") household IDs.
 */
sealed class ItemLocation {
    data class Household(val id: String) : ItemLocation()
    data class Private(val uid: String) : ItemLocation()

    companion object {
        fun resolve(
            isPrivate: Boolean,
            householdSession: HouseholdSession,
            uidProvider: () -> String?
        ): ItemLocation {
            return if (isPrivate) {
                val uid = uidProvider()?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("User not authenticated")
                Private(uid)
            } else {
                Household(householdSession.requireHouseholdId())
            }
        }
    }
}
