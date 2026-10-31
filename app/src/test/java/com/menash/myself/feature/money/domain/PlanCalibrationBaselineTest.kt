package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class PlanCalibrationBaselineTest {
    private val end = YearMonth.of(2026, 12)
    private fun close(month: Int, amount: Long? = null): ClosedMonth {
        val m = YearMonth.of(2026, month)
        return ClosedMonth(m, MonthSnapshot(m, Money.ZERO, Money.ZERO),
            if (amount == null) emptyMap() else mapOf("a" to Money(amount)),
            emptyMap(), emptyMap(), emptyMap(), m.atEndOfMonth())
    }
    @Test fun gapsAreNotZero() { assertEquals(Money(200), categoryBaseline(listOf(close(1, 100), close(3, 300)), end).rows.single().average) }
    @Test fun absentCategoryIsZero() { assertEquals(Money(50), categoryBaseline(listOf(close(1, 100), close(2)), end).rows.single().average) }
    @Test fun medianRoundsHalfUp() { assertEquals(Money(2), categoryBaseline(listOf(close(1, 1), close(2, 2)), end).rows.single().median) }
    @Test fun windowLimitsHistory() { assertEquals(Money(200), categoryBaseline(listOf(close(1, 100), close(2, 200)), end, 1).rows.single().average) }
    @Test fun futureIgnored() { assertEquals(Money(100), categoryBaseline(listOf(close(1, 100), close(2, 200)), YearMonth.of(2026, 1)).rows.single().average) }
    @Test fun duplicatesRejected() { assertThrows(IllegalArgumentException::class.java) { categoryBaseline(listOf(close(1), close(1)), end) } }
    @Test fun emptyInput() { assertTrue(categoryBaseline(emptyList(), end).rows.isEmpty()) }
}
