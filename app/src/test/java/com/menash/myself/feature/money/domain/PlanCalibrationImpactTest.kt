package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class PlanCalibrationImpactTest {
    private val plan = mapOf("a" to Money(100))
    private val budget = PeriodicBudget("b", "p", "P", Money(1200), YearMonth.of(2026, 1))
    private val suggestion = PlanSuggestion("a", Money(100), Money(200), SuggestionStatus.UNDER_PLANNED, 3)
    private val periodic = PeriodicSuggestion("b", Money(1200), Money(2400), Money(2400), 12)
    @Test fun noneAccepted() { assertEquals(Money.ZERO, planImpact(plan, listOf(suggestion), emptySet(), listOf(periodic), emptySet(), listOf(budget)).difference) }
    @Test fun bothAccepted() { assertEquals(Money(200), planImpact(plan, listOf(suggestion), setOf("a"), listOf(periodic), setOf("b"), listOf(budget)).difference) }
    @Test fun unusedDropsToZero() { assertEquals(Money(-100), planImpact(plan, listOf(suggestion.copy(suggested = Money.ZERO, status = SuggestionStatus.UNUSED)), setOf("a"), emptyList(), emptySet(), emptyList()).difference) }
}
