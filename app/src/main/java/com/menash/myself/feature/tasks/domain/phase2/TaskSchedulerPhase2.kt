package com.menash.myself.feature.tasks.domain.phase2

import com.menash.myself.core.domain.model.ForWhom
import java.time.LocalTime

/**
 * PHASE 2 - LOCKED IN ROADMAP.MD
 * Untouched by current Phase 1 screens.
 */
enum class EnergyState {
    SHARP,
    STEADY,
    EXHAUSTED
}

enum class EnergyTier {
    DEEP,
    ROUTINE,
    LIGHT;

    companion object {
        val DEFAULT = ROUTINE
    }
}

data class Phase2TaskItem(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val tag: String = "",
    val energyTier: EnergyTier = EnergyTier.DEFAULT,
    val isAnchor: Boolean = false,
    val anchorTimeWindowStart: LocalTime? = null,
    val anchorTimeWindowEnd: LocalTime? = null,
    val isFiveBefore11: Boolean = false,
    val forWhom: ForWhom = ForWhom.ME
) {
    /**
     * Untagged tasks default to "שוטף"; tagging is optional.
     */
    val effectiveTag: String
        get() = tag.trim().ifEmpty { DEFAULT_TAG }

    companion object {
        const val DEFAULT_TAG = "שוטף"
    }
}

data class DoNowFilterResult(
    val matchedTasks: List<Phase2TaskItem>,
    val isEmpty: Boolean
)

object TaskSchedulerPhase2 {

    private val ELEVEN_AM = LocalTime.of(11, 0)

    /**
     * Filters tasks for "DO NOW".
     * Shows empty state when nothing matches.
     * Matches user's current energy state to compatible energy tiers:
     * - SHARP: DEEP, ROUTINE, LIGHT
     * - STEADY: ROUTINE, LIGHT
     * - EXHAUSTED: LIGHT only
     */
    fun filterDoNow(
        tasks: List<Phase2TaskItem>,
        userEnergyState: EnergyState? = null,
        currentTime: LocalTime = LocalTime.now()
    ): DoNowFilterResult {
        val pending = tasks.filter { !it.isCompleted }

        val filtered = pending.filter { task ->
            val energyMatches = when (userEnergyState) {
                null -> true
                EnergyState.SHARP -> true // All tiers compatible
                EnergyState.STEADY -> task.energyTier == EnergyTier.ROUTINE || task.energyTier == EnergyTier.LIGHT
                EnergyState.EXHAUSTED -> task.energyTier == EnergyTier.LIGHT
            }

            val anchorWindowMatches = if (task.isAnchor && task.anchorTimeWindowStart != null && task.anchorTimeWindowEnd != null) {
                !currentTime.isBefore(task.anchorTimeWindowStart) && !currentTime.isAfter(task.anchorTimeWindowEnd)
            } else {
                true
            }

            val fiveBefore11Matches = if (task.isFiveBefore11) {
                currentTime.isBefore(ELEVEN_AM)
            } else {
                true
            }

            energyMatches && anchorWindowMatches && fiveBefore11Matches
        }

        return DoNowFilterResult(
            matchedTasks = filtered,
            isEmpty = filtered.isEmpty()
        )
    }

    fun shouldShowFiveBefore11(currentTime: LocalTime): Boolean {
        return currentTime.isBefore(ELEVEN_AM)
    }
}
