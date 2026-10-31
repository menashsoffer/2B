package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class PlanCalibrationSuggestionsTest {
    private fun baseline(avg: Long, latest: Long = avg, active: Int = 3) = CalibrationBaseline(
        (1..3).map { YearMonth.of(2026, it) }, listOf(CategoryBaseline("a", Money(avg), Money(avg), Money(avg), Money(avg), Money(latest), active)))
    private fun suggest(avg: Long, plan: Long?) = suggestPlan(baseline(avg), if (plan == null) emptyMap() else mapOf("a" to Money(plan)), emptyList()).single()
    @Test fun under() { assertEquals(SuggestionStatus.UNDER_PLANNED, suggest(120, 100).status) }
    @Test fun over() { assertEquals(SuggestionStatus.OVER_PLANNED, suggest(80, 100).status) }
    @Test fun noPlan() { assertEquals(SuggestionStatus.NO_PLAN, suggest(100, null).status) }
    @Test fun unused() { assertEquals(SuggestionStatus.UNUSED, suggestPlan(baseline(0, active = 0), mapOf("a" to Money(100)), emptyList()).single().status) }
    @Test fun boundaryOk() { assertEquals(SuggestionStatus.OK, suggest(110, 100).status) }
    @Test fun fixedUsesLatest() { assertEquals(Money(10000), suggestPlan(baseline(100, 6000), emptyMap(), listOf(Category("a", "A", ExpenseClass.FIXED))).single().suggested) }
    @Test fun roundsUp() { assertEquals(Money(5000), suggest(1, null).suggested) }
    @Test fun periodicExcluded() { assertTrue(suggestPlan(baseline(100), emptyMap(), listOf(Category("a", "A", ExpenseClass.PERIODIC))).isEmpty()) }
    @Test fun nullExcluded() { assertTrue(suggestPlan(CalibrationBaseline(baseline(1).months, listOf(baseline(1).rows.single().copy(categoryId = null))), emptyMap(), emptyList()).isEmpty()) }
    @Test fun insufficient() { assertEquals(SuggestionStatus.NOT_ENOUGH_DATA, suggestPlan(baseline(100).copy(months = emptyList()), emptyMap(), emptyList()).single().status) }
}
