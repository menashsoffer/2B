package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.model.ItemLocation
import com.menash.myself.core.data.session.HouseholdSession
import com.menash.myself.core.data.legacy.firestore.LifeHubRepository

/**
 * Where an item of the legacy pillars lives (household or private), shared by the task, dream and
 * inbox repositories. A failure is reported under [action] and then rethrown.
 */
internal fun LifeHubRepository.locationFor(
    action: String,
    isPrivate: Boolean,
    householdSession: HouseholdSession,
): ItemLocation = try {
    ItemLocation.resolve(isPrivate, householdSession) { currentUid() }
} catch (e: Exception) {
    reportError(action, e)
    throw e
}
