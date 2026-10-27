package com.menash.myself.core.domain.model

/**
 * Active dreams for Home.
 *
 * Firestore stores the Hebrew [DreamStatus.displayName] written by the dream board
 * (`DreamCardComponents`) and by inbox triage (`"חלום"`). New dreams created from the
 * dream dialog or a CSV import keep the domain default `"active"` (`Dream.status`).
 * `"completed"` and `"archived"` are the English aliases named on [Dream], not the
 * board's labels. `"בוצע"` is [DreamStatus.DONE] and `"נגנז"` is [DreamStatus.ARCHIVED].
 */
fun Dream.isActiveForHome(): Boolean {
    if (isCompleted) return false
    return status !in inactiveHomeDreamStatuses
}

fun activeDreamCount(dreams: List<Dream>): Int = dreams.count { dream -> dream.isActiveForHome() }

fun dreamsActiveValue(state: UiState<List<Dream>>): Int? {
    val dreams = (state as? UiState.Content)?.data ?: return null
    return activeDreamCount(dreams).takeIf { count -> count >= 1 }
}

private val inactiveHomeDreamStatuses = setOf(
    DreamStatus.ARCHIVED.displayName,
    DreamStatus.DONE.displayName,
    "archived",
    "completed",
)
