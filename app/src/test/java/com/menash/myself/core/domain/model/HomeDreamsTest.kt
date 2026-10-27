package com.menash.myself.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDreamsTest {

    @Test
    fun storedBoardStatuses() {
        assertTrue(dream("active").isActiveForHome())
        assertTrue(dream("חלום").isActiveForHome())
        assertTrue(dream("בבדיקה").isActiveForHome())
        assertTrue(dream("חוסכים/מתכננים").isActiveForHome())
        assertFalse(dream("בוצע").isActiveForHome())
        assertFalse(dream("נגנז").isActiveForHome())
        assertFalse(dream("archived").isActiveForHome())
        assertFalse(dream("completed").isActiveForHome())
    }

    @Test
    fun savingsGoalCountsAsCompleted() {
        val funded = dream("חלום").copy(
            estimatedCost = Money.fromShekels(100.0),
            savedAmount = Money.fromShekels(100.0),
        )
        assertFalse(funded.isActiveForHome())
    }

    @Test
    fun onlyContentWithAtLeastOneActiveDreamShowsANumber() {
        val active = dream("חלום")
        val archived = dream("נגנז")
        assertNull(dreamsActiveValue(UiState.Loading))
        assertNull(dreamsActiveValue(UiState.Empty))
        assertNull(dreamsActiveValue(UiState.Error("offline")))
        assertNull(dreamsActiveValue(UiState.Content(emptyList())))
        assertNull(dreamsActiveValue(UiState.Content(listOf(archived))))
        assertEquals(1, dreamsActiveValue(UiState.Content(listOf(active, archived))))
        assertEquals(2, dreamsActiveValue(UiState.Content(listOf(active, dream("active")))))
    }

    private fun dream(status: String) = Dream(title = "sample", status = status)
}
