package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class PlanCalibrationPeriodicTest {
    private val end = YearMonth.of(2026, 12)
    private val budget = PeriodicBudget("b", "a", "A", Money(100), end.minusMonths(11))
    private fun close(month: YearMonth, amount: Long = 1) = ClosedMonth(month, MonthSnapshot(month, Money.ZERO, Money.ZERO), mapOf("a" to Money(amount)), emptyMap(), emptyMap(), emptyMap(), month.atEndOfMonth())
    private fun year() = (0L..11L).map { close(end.minusMonths(it)) }
    @Test fun fullYear() { assertEquals(Money(12), suggestPeriodic(year(), listOf(budget), end).single().spentLast12) }
    @Test fun partialYear() { assertNull(suggestPeriodic(year().drop(1), listOf(budget), end).single().spentLast12) }
    @Test fun outsideIgnored() { assertEquals(Money(12), suggestPeriodic(year() + close(end.minusMonths(12), 500), listOf(budget), end).single().spentLast12) }
    @Test fun roundedUp() { assertEquals(Money(10000), suggestPeriodic(year(), listOf(budget), end).single().suggested) }
}
