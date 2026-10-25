package com.menash.myself.domain

import com.menash.myself.feature.calculators.ai.domain.AiSubscriptionCalculator
import com.menash.myself.feature.calculators.ai.domain.AiSubscriptionInput
import org.junit.Assert.*
import org.junit.Test

class AiSubscriptionCalculatorTest {

    @Test
    fun calculateCost_usdMonthly() {
        val sub = AiSubscriptionInput(
            name = "Claude Pro",
            price = 20.0,
            currency = "USD",
            billingCycle = "MONTHLY"
        )
        val result = AiSubscriptionCalculator.calculateCost(sub, usdToIlsRate = 3.75)
        assertEquals(75.0, result.priceInIls, 0.001)
        assertEquals(75.0, result.monthlyCostIls, 0.001)
        assertEquals(900.0, result.yearlyCostIls, 0.001)
    }

    @Test
    fun calculateCost_usdYearly() {
        val sub = AiSubscriptionInput(
            name = "Midjourney Pro",
            price = 360.0,
            currency = "USD",
            billingCycle = "YEARLY"
        )
        val result = AiSubscriptionCalculator.calculateCost(sub, usdToIlsRate = 3.60)
        assertEquals(1296.0, result.priceInIls, 0.001)
        assertEquals(108.0, result.monthlyCostIls, 0.001)
        assertEquals(1296.0, result.yearlyCostIls, 0.001)
    }

    @Test
    fun calculateSummary_filtersInactiveSubscriptions() {
        val rate = 3.5
        val list = listOf(
            AiSubscriptionInput(name = "A", category = "AI", isActive = true, billingCycle = "MONTHLY", currency = "USD", price = 20.0), // 70/mo
            AiSubscriptionInput(name = "B", category = "Productivity", isActive = true, billingCycle = "YEARLY", currency = "ILS", price = 1200.0), // 100/mo
            AiSubscriptionInput(name = "C", category = "AI", isActive = false, billingCycle = "MONTHLY", currency = "USD", price = 100.0) // inactive!
        )

        val summary = AiSubscriptionCalculator.calculateSummary(list, rate)
        assertEquals(2, summary.activeCount)
        assertEquals(170.0, summary.totalMonthlyCostIls, 0.001)
        assertEquals(2040.0, summary.totalYearlyCostIls, 0.001)
        assertEquals(70.0, summary.categoryBreakdown["AI"] ?: 0.0, 0.001)
        assertEquals(100.0, summary.categoryBreakdown["Productivity"] ?: 0.0, 0.001)
    }
}
