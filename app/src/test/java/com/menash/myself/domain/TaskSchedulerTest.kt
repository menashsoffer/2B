package com.menash.myself.domain

import com.menash.myself.feature.tasks.domain.phase2.EnergyState
import com.menash.myself.feature.tasks.domain.phase2.EnergyTier
import com.menash.myself.feature.tasks.domain.phase2.Phase2TaskItem
import com.menash.myself.feature.tasks.domain.phase2.TaskSchedulerPhase2
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class TaskSchedulerTest {

    @Test
    fun untaggedTask_defaultsToShotef() {
        val taskWithNoTag = Phase2TaskItem(id = "1", title = "לתקן ברז במטבח", tag = "")
        assertEquals("שוטף", taskWithNoTag.effectiveTag)

        val taskWithBlankTag = Phase2TaskItem(id = "2", title = "לקנות חלב", tag = "   ")
        assertEquals("שוטף", taskWithBlankTag.effectiveTag)

        val taskWithCustomTag = Phase2TaskItem(id = "3", title = "השקעות", tag = "פיננסים")
        assertEquals("פיננסים", taskWithCustomTag.effectiveTag)
    }

    @Test
    fun energyTier_defaultsToRoutine() {
        val defaultTask = Phase2TaskItem(id = "1", title = "משימה רגילה")
        assertEquals(EnergyTier.ROUTINE, defaultTask.energyTier)
    }

    @Test
    fun filterDoNow_emptyStateWhenNothingMatches() {
        val tasks = listOf(
            Phase2TaskItem(id = "1", title = "משימה שהושלמה", isCompleted = true),
            Phase2TaskItem(id = "2", title = "עבודה עמוקה", energyTier = EnergyTier.DEEP)
        )

        // When user is EXHAUSTED, DEEP tasks do not match -> empty state
        val result = TaskSchedulerPhase2.filterDoNow(tasks, userEnergyState = EnergyState.EXHAUSTED)
        assertTrue(result.isEmpty)
        assertEquals(0, result.matchedTasks.size)
    }

    @Test
    fun filterDoNow_matchesPendingWithCompatibleEnergy() {
        val tasks = listOf(
            Phase2TaskItem(id = "1", title = "סידור קל", energyTier = EnergyTier.LIGHT),
            Phase2TaskItem(id = "2", title = "תכנות מורכב", energyTier = EnergyTier.DEEP)
        )

        // When user is EXHAUSTED, only LIGHT matches
        val exhaustedResult = TaskSchedulerPhase2.filterDoNow(tasks, userEnergyState = EnergyState.EXHAUSTED)
        assertEquals(1, exhaustedResult.matchedTasks.size)
        assertEquals("1", exhaustedResult.matchedTasks.first().id)

        // When user is SHARP, both match
        val sharpResult = TaskSchedulerPhase2.filterDoNow(tasks, userEnergyState = EnergyState.SHARP)
        assertEquals(2, sharpResult.matchedTasks.size)
    }

    @Test
    fun fiveBefore11_hiddenAfter11AM() {
        assertTrue(TaskSchedulerPhase2.shouldShowFiveBefore11(LocalTime.of(10, 59)))
        assertFalse(TaskSchedulerPhase2.shouldShowFiveBefore11(LocalTime.of(11, 0)))
        assertFalse(TaskSchedulerPhase2.shouldShowFiveBefore11(LocalTime.of(14, 30)))

        val tasks = listOf(
            Phase2TaskItem(id = "1", title = "חמש לפני 11", isFiveBefore11 = true)
        )

        // Before 11:00 -> matched
        val morningResult = TaskSchedulerPhase2.filterDoNow(tasks, currentTime = LocalTime.of(9, 30))
        assertEquals(1, morningResult.matchedTasks.size)

        // After 11:00 -> excluded
        val afternoonResult = TaskSchedulerPhase2.filterDoNow(tasks, currentTime = LocalTime.of(11, 15))
        assertEquals(0, afternoonResult.matchedTasks.size)
        assertTrue(afternoonResult.isEmpty)
    }

    @Test
    fun anchorTasks_filteredByTimeWindow() {
        val anchorTask = Phase2TaskItem(
            id = "anchor1",
            title = "איסוף ילדים מבית ספר",
            isAnchor = true,
            anchorTimeWindowStart = LocalTime.of(13, 0),
            anchorTimeWindowEnd = LocalTime.of(14, 0)
        )
        val list = listOf(anchorTask)

        // Inside window -> matches
        val inWindow = TaskSchedulerPhase2.filterDoNow(list, currentTime = LocalTime.of(13, 30))
        assertEquals(1, inWindow.matchedTasks.size)

        // Outside window -> excluded
        val outWindow = TaskSchedulerPhase2.filterDoNow(list, currentTime = LocalTime.of(12, 30))
        assertEquals(0, outWindow.matchedTasks.size)
        assertTrue(outWindow.isEmpty)
    }
}
