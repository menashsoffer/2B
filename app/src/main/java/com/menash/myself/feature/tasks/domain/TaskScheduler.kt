package com.menash.myself.feature.tasks.domain

import com.menash.myself.core.domain.model.ForWhom
import java.time.LocalTime

data class TaskItemDomain(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val tag: String = "",
    val energyLevel: String = "", // "LOW", "MEDIUM", "HIGH" or blank
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
    val matchedTasks: List<TaskItemDomain>,
    val isEmpty: Boolean
)

object TaskScheduler {

    private val ELEVEN_AM = LocalTime.of(11, 0)

    /**
     * Filters tasks for "DO NOW".
     * Shows empty state when nothing matches.
     * Respects energy level filtering if provided.
     */
    fun filterDoNow(
        tasks: List<TaskItemDomain>,
        userEnergyLevel: String? = null,
        currentTime: LocalTime = LocalTime.now()
    ): DoNowFilterResult {
        val pending = tasks.filter { !it.isCompleted }

        val filtered = pending.filter { task ->
            // If task has specific energy requirement and user has an active energy state, filter by it
            val energyMatches = if (!userEnergyLevel.isNullOrBlank() && task.energyLevel.isNotBlank()) {
                task.energyLevel.equals(userEnergyLevel, ignoreCase = true)
            } else {
                true
            }

            // Anchors appear only in their time window
            val anchorWindowMatches = if (task.isAnchor && task.anchorTimeWindowStart != null && task.anchorTimeWindowEnd != null) {
                !currentTime.isBefore(task.anchorTimeWindowStart) && !currentTime.isAfter(task.anchorTimeWindowEnd)
            } else {
                true
            }

            // Five Before 11 is hidden after 11:00
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

    /**
     * Determines whether the Five Before 11 card should be visible on Home.
     * Hidden strictly after 11:00.
     */
    fun shouldShowFiveBefore11(currentTime: LocalTime): Boolean {
        return currentTime.isBefore(ELEVEN_AM)
    }
}
