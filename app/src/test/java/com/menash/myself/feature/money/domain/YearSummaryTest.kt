package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class YearSummaryTest {
    private fun close(year: Int, month: Int, income: Long = 100, expense: Long = 50): ClosedMonth {
        val m = YearMonth.of(year, month)
        return ClosedMonth(m, MonthSnapshot(m, Money(income), Money(expense)), emptyMap(), emptyMap(), emptyMap(), emptyMap(), m.atEndOfMonth())
    }

    @Test fun fullYearAndTies() {
        val result = yearSummary((1..12).map { close(2026, it) }.reversed(), 2026)!!
        assertEquals((1..12).map { YearMonth.of(2026, it) }, result.closedMonths)
        assertTrue(result.missingMonths.isEmpty())
        assertEquals(Money(1200), result.income)
        assertEquals(Money(600), result.expenses)
        assertEquals(Money(600), result.saving)
        assertEquals(BigDecimal("50.0"), result.savingPercent)
        assertEquals(Money(50), result.averageMonthlyExpenses)
        assertEquals(YearMonth.of(2026, 1), result.bestMonth)
        assertEquals(result.bestMonth, result.worstMonth)
    }

    @Test fun gapsAndOutsideMonths() {
        val rows = listOf(close(2026, 2), close(2026, 4), close(2025, 3))
        assertEquals(listOf(YearMonth.of(2026, 3)), yearSummary(rows, 2026)!!.missingMonths)
        assertTrue(yearSummary(listOf(close(2026, 9), close(2026, 10)), 2026)!!.missingMonths.isEmpty())
        assertNull(yearSummary(rows, 2024))
    }

    @Test fun negativeSavingZeroIncomeAndDuplicates() {
        val result = yearSummary(listOf(close(2026, 1, 0, 3)), 2026)!!
        assertEquals(Money(-3), result.saving)
        assertNull(result.savingPercent)
        assertEquals(result.bestMonth, result.worstMonth)
        assertThrows(IllegalArgumentException::class.java) { yearSummary(listOf(close(2026, 1), close(2026, 1)), 2026) }
    }
}
