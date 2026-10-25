package com.menash.myself.domain

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.dreams.domain.DreamSavingsCalculator
import com.menash.myself.feature.dreams.domain.DreamSavingsInput
import org.junit.Assert.*
import org.junit.Test

class DreamSavingsCalculatorTest {

    @Test
    fun calculateSavingsProgress_ongoingDream() {
        val input = DreamSavingsInput(
            targetAmount = Money.fromShekels(50000.0),
            savedAmount = Money.fromShekels(20000.0),
            monthlySavingsCapacity = Money.fromShekels(2500.0)
        )

        val result = DreamSavingsCalculator.calculateSavingsProgress(input)
        assertEquals(30000.0, result.remainingAmount.shekels, 0.001)
        assertEquals(40.0, result.progressPercent, 0.001)
        assertEquals(12, result.monthsNeeded)
        assertFalse(result.isCompleted)
    }

    @Test
    fun calculateSavingsProgress_completedDream() {
        val input = DreamSavingsInput(
            targetAmount = Money.fromShekels(10000.0),
            savedAmount = Money.fromShekels(10000.0),
            monthlySavingsCapacity = Money.fromShekels(1000.0)
        )

        val result = DreamSavingsCalculator.calculateSavingsProgress(input)
        assertEquals(0L, result.remainingAmount.agorot)
        assertEquals(100.0, result.progressPercent, 0.001)
        assertEquals(0, result.monthsNeeded)
        assertTrue(result.isCompleted)
    }
}
