package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class PeriodSummaryTest {
    private fun ym(year: Int, month: Int) = YearMonth.of(year, month)

    private fun close(
        year: Int, month: Int, income: Long = 100, expense: Long = 50,
        byCategory: Map<String?, Money> = emptyMap(), balances: Map<String, Money> = emptyMap(),
        liabilities: Map<String, Money> = emptyMap(), periodic: Map<String, PeriodicStatus> = emptyMap(),
    ): ClosedMonth {
        val m = ym(year, month)
        return ClosedMonth(m, MonthSnapshot(m, Money(income), Money(expense)), byCategory, periodic, balances, liabilities, m.atEndOfMonth())
    }

    @Test fun rangeHelpers() {
        assertEquals(MonthRange(ym(2026, 8), ym(2026, 10)), MonthRange.last(ym(2026, 10), 3))
        assertEquals(MonthRange(ym(2025, 11), ym(2026, 1)), MonthRange.last(ym(2026, 1), 3))
        assertEquals(MonthRange(ym(2026, 1), ym(2026, 10)), MonthRange.year(2026, ym(2026, 10)))
        assertEquals(MonthRange(ym(2025, 1), ym(2025, 12)), MonthRange.year(2025, ym(2026, 10)))
        assertEquals(MonthRange(ym(2025, 1), ym(2025, 12)), MonthRange.year(2025))
        assertEquals(listOf(ym(2025, 12), ym(2026, 1)), MonthRange(ym(2025, 12), ym(2026, 1)).months)
        assertThrows(IllegalArgumentException::class.java) { MonthRange(ym(2026, 2), ym(2026, 1)) }
        assertThrows(IllegalArgumentException::class.java) { MonthRange.last(ym(2026, 2), 0) }
    }

    @Test fun totalsAveragesAndSeriesAcrossYears() {
        val rows = listOf(close(2026, 1, 300, 100), close(2025, 11, 100, 120), close(2025, 12, 200, 50), close(2025, 10, 999, 1))
        val result = periodSummary(rows, MonthRange.last(ym(2026, 1), 3))!!
        assertEquals(listOf(ym(2025, 11), ym(2025, 12), ym(2026, 1)), result.closedMonths)
        assertTrue(result.missingMonths.isEmpty())
        assertEquals(Money(600), result.income)
        assertEquals(Money(270), result.expenses)
        assertEquals(Money(330), result.saving)
        assertEquals(BigDecimal("55.0"), result.savingPercent)
        assertEquals(Money(200), result.averageMonthlyIncome)
        assertEquals(Money(90), result.averageMonthlyExpenses)
        assertEquals(Money(110), result.averageMonthlySaving)
        assertEquals(listOf(Money(-20), Money(150), Money(200)), result.months.map { it.saving })
        assertEquals(ym(2026, 1), result.bestMonth)
        assertEquals(ym(2025, 11), result.worstMonth)
    }

    @Test fun missingMonthsAreReportedNotZero() {
        val rows = listOf(close(2026, 2, 100, 40), close(2026, 5, 100, 60))
        val result = periodSummary(rows, MonthRange(ym(2026, 1), ym(2026, 6)))!!
        assertEquals(listOf(ym(2026, 1), ym(2026, 3), ym(2026, 4), ym(2026, 6)), result.missingMonths)
        assertEquals(Money(50), result.averageMonthlyExpenses)
        assertEquals(2, result.months.size)
        assertNull(periodSummary(rows, MonthRange(ym(2026, 7), ym(2026, 9))))
    }

    @Test fun roundingZeroIncomeAndDuplicates() {
        val result = periodSummary(listOf(close(2026, 1, 0, 1), close(2026, 2, 0, 2)), MonthRange.year(2026, ym(2026, 2)))!!
        assertEquals(Money(2), result.averageMonthlyExpenses) // 1.5 rounds half up
        assertEquals(Money(-2), result.averageMonthlySaving) // -1.5 rounds half up away from zero
        assertNull(result.savingPercent)
        assertThrows(IllegalArgumentException::class.java) {
            periodSummary(listOf(close(2026, 1), close(2026, 1)), MonthRange.year(2026))
        }
    }

    @Test fun categoriesOnlyOverClosedMonthsOfTheRange() {
        val food = "current-1"
        val rows = listOf(
            close(2026, 3, byCategory = mapOf(food to Money(120), null to Money(5))),
            close(2026, 4, byCategory = mapOf(food to Money(80))),
            close(2026, 6, byCategory = mapOf(food to Money(1000))),
        )
        val plans = (3..6).associate { ym(2026, it) to mapOf<String?, Money>(food to Money(100)) }
        val result = categoryPeriod(rows, MonthRange(ym(2026, 3), ym(2026, 5)), plans)
        val foodRow = result.single { it.categoryId == food }
        assertEquals(Money(200), foodRow.planned) // May has a plan but no close
        assertEquals(Money(200), foodRow.actual)
        assertEquals(Money(0), foodRow.diff)
        assertNull(result.single { it.categoryId == null }.diff)
        assertEquals(categoryYear(rows, 2026, plans), categoryPeriod(rows, MonthRange.year(2026), plans))
    }

    @Test fun periodicAtEnd() {
        val status = PeriodicStatus(Money(10), Money(4), Money(110))
        val rows = listOf(close(2026, 1, periodic = mapOf("p" to status)), close(2026, 3))
        assertEquals(mapOf("p" to status), periodicAtPeriodEnd(rows, MonthRange(ym(2026, 1), ym(2026, 2))))
        assertTrue(periodicAtPeriodEnd(rows, MonthRange(ym(2026, 3), ym(2026, 4))).isEmpty())
        assertTrue(periodicAtPeriodEnd(rows, MonthRange(ym(2026, 5), ym(2026, 6))).isEmpty())
    }

    @Test fun netWorthAgainstLastKnownBeforeTheRange() {
        val rows = listOf(
            close(2025, 9, balances = mapOf("a" to Money(500))),
            close(2025, 12), // balances unknown (retro close)
            close(2026, 1, balances = mapOf("a" to Money(700)), liabilities = mapOf("d" to Money(100))),
            close(2026, 2),
        )
        val change = netWorthChange(rows, MonthRange(ym(2025, 12), ym(2026, 2)))!!
        assertEquals(ym(2025, 9), change.startFrom)
        assertEquals(Money(500), change.start)
        assertEquals(Money(600), change.end)
        assertEquals(Money(100), change.change)

        val inside = netWorthChange(rows, MonthRange(ym(2025, 9), ym(2026, 1)))!!
        assertEquals(ym(2025, 9), inside.startFrom)
        assertEquals(Money(100), inside.change)
        assertNull(netWorthChange(rows, MonthRange(ym(2025, 12), ym(2025, 12))))
    }
}
