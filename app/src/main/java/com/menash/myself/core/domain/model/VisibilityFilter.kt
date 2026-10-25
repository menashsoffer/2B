package com.menash.myself.core.domain.model

/**
 * Filter for controlling visible items across personal and shared household spaces.
 *
 * - MINE_AND_BOTH: Cognitive-load reduction view showing only items relevant to the viewer (default).
 * - ALL: Complete household view including items belonging to the partner.
 */
enum class VisibilityFilter {
    MINE_AND_BOTH,
    ALL
}
