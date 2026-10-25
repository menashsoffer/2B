package com.menash.myself.domain

import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.feature.tasks.domain.TaskItemDomain
import com.menash.myself.feature.tasks.domain.TaskScheduler
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class TaskSchedulerTest {

    @Test
    fun untaggedTask_defaultsToShotef() {
        val taskWithNoTag = TaskItemDomain(id = "1", title = "לתקן ברז במטבח", tag = "")
        assertEquals("שוטף", taskWithNoTag.effectiveTag)

        val taskWithBlankTag = TaskItemDomain(id = "2", title = "לקנות חלב", tag = "   ")
        assertEquals("שוטף", taskWithBlankTag.effectiveTag)

        val taskWithCustomTag = TaskItemDomain(id = "3", title = "השקעות", tag = "פיננסים")
        assertEquals("פיננסים", taskWithCustomTag.effectiveTag)
    }

    @Test
    fun filterDoNow_emptyStateWhenNothingMatches() {
        val tasks = listOf(
            TaskItemDomain(id = "1", title = "משימה שהושלמה", isCompleted = true),
            TaskItemDomain(id = "2", title = "משימה באנרגיה גבוהה", energyLevel = "HIGH")
        )

        val result = TaskScheduler.filterDoNow(tasks, userEnergyLevel = "LOW")
        assertTrue(result.isEmpty)
        assertEquals(0, result.matchedTasks.size)
    }

    @Test
    fun filterDoNow_matchesPendingWithMatchingEnergy() {
        val tasks = listOf(
            TaskItemDomain(id = "1", title = "משימת אנרגיה נמוכה", energyLevel = "LOW"),
            TaskItemDomain(id = "2", title = "משימת אנרגיה גבוהה", energyLevel = "HIGH")
        )

        val result = TaskScheduler.filterDoNow(tasks, userEnergyLevel = "LOW")
        assertFalse(result.isEmpty)
        assertEquals(1, result.matchedTasks.size)
        assertEquals("1", result.matchedTasks.first().id)
    }

    @Test
    fun fiveBefore11_hiddenAfter11AM() {
        assertTrue(TaskScheduler.shouldShowFiveBefore11(LocalTime.of(10, 59)))
        assertFalse(TaskScheduler.shouldShowFiveBefore11(LocalTime.of(11, 0)))
        assertFalse(TaskScheduler.shouldShowFiveBefore11(LocalTime.of(14, 30)))

        val tasks = listOf(
            TaskItemDomain(id = "1", title = "חמש לפני 11", isFiveBefore11 = true)
        )

        // Before 11:00 -> matched
        val morningResult = TaskScheduler.filterDoNow(tasks, currentTime = LocalTime.of(9, 30))
        assertEquals(1, morningResult.matchedTasks.size)

        // After 11:00 -> excluded
        val afternoonResult = TaskScheduler.filterDoNow(tasks, currentTime = LocalTime.of(11, 15))
        assertEquals(0, afternoonResult.matchedTasks.size)
        assertTrue(afternoonResult.isEmpty)
    }

    @Test
    fun anchorTasks_filteredByTimeWindow() {
        val anchorTask = TaskItemDomain(
            id = "anchor1",
            title = "איסוף ילדים מבית ספר",
            isAnchor = true,
            anchorTimeWindowStart = LocalTime.of(13, 0),
            anchorTimeWindowEnd = LocalTime.of(14, 0)
        )
        val list = listOf(anchorTask)

        // Inside window -> matches
        val inWindow = TaskScheduler.filterDoNow(list, currentTime = LocalTime.of(13, 30))
        assertEquals(1, inWindow.matchedTasks.size)

        // Outside window -> excluded
        val outWindow = TaskScheduler.filterDoNow(list, currentTime = LocalTime.of(12, 30))
        assertEquals(0, outWindow.matchedTasks.size)
        assertTrue(outWindow.isEmpty)
    }
}
