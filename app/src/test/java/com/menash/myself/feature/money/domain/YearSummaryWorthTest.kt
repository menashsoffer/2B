package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class YearSummaryWorthTest {
    private fun close(year: Int, month: Int, balance: Long?, liability: Long = 0): ClosedMonth {
        val m = YearMonth.of(year, month)
        return ClosedMonth(m, MonthSnapshot(m, Money.ZERO, Money.ZERO), emptyMap(), emptyMap(),
            balance?.let { mapOf("asset" to Money(it)) } ?: emptyMap(),
            if (balance == null) emptyMap() else mapOf("debt" to Money(liability)), m.atEndOfMonth())
    }
    @Test fun previousYearAndGrowingLiability() {
        val result = netWorthChange(listOf(close(2025, 12, 100, 10), close(2026, 2, 100, 30), close(2026, 3, null)), 2026)!!
        assertEquals(YearMonth.of(2025, 12), result.startFrom)
        assertEquals(Money(-20), result.change)
        assertEquals(Money(70), result.end)
    }
    @Test fun firstKnownMonthAndUnknowns() {
        val first = close(2026, 2, 100)
        assertEquals(Money.ZERO, netWorthChange(listOf(close(2026, 1, null), first), 2026)!!.change)
        assertEquals(YearMonth.of(2026, 2), netWorthChange(listOf(first, close(2026, 3, 130)), 2026)!!.startFrom)
        assertNull(netWorthChange(listOf(close(2026, 1, null)), 2026))
        assertEquals(emptyMap<String, PeriodicStatus>(), periodicAtYearEnd(listOf(first), 2026))
    }
}
