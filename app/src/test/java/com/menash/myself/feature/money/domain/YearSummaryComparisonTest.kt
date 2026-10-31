package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class YearSummaryComparisonTest {
    private fun close(year: Int, month: Int, income: Long, expenses: Long, categories: Map<String?, Money> = emptyMap()): ClosedMonth {
        val m = YearMonth.of(year, month)
        return ClosedMonth(m, MonthSnapshot(m, Money(income), Money(expenses)), categories, emptyMap(), emptyMap(), emptyMap(), m.atEndOfMonth())
    }
    @Test fun differentCountsAndCategoryUnion() {
        val rows = listOf(close(2025, 1, 100, 50, mapOf("old" to Money(10))), close(2025, 2, 200, 50),
            close(2026, 1, 200, 70, mapOf("new" to Money(20))))
        val result = compareYears(rows, 2026, emptyMap())!!
        assertEquals(1, result.monthsThis)
        assertEquals(2, result.monthsPrevious)
        assertEquals(Money(50), result.income.difference)
        assertEquals(Money(20), result.expenses.difference)
        assertEquals(Money(30), result.saving.difference)
        assertEquals(Money(20), result.categories.first { it.categoryId == "new" }.averages.difference)
        assertEquals(Money(-5), result.categories.first { it.categoryId == "old" }.averages.difference)
        assertNull(compareYears(rows, 2027, emptyMap()))
    }
    @Test fun differenceRoundsOnlyAfterSubtractingAverages() {
        val rows = listOf(close(2025, 1, 0, 0), close(2025, 2, 1, 0), close(2026, 1, 1, 0), close(2026, 2, 1, 0))
        assertEquals(Money(1), compareYears(rows, 2026, emptyMap())!!.income.difference)
    }
}
