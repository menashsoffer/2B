package com.menash.myself.domain

import com.menash.myself.core.domain.model.DateRange
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DateRangeTest {

    @Test
    fun contains_evaluatesInclusivityAccurately() {
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 1, 31)
        val range = DateRange(start, end)

        assertTrue(range.contains(start))
        assertTrue(range.contains(end))
        assertTrue(range.contains(LocalDate.of(2026, 1, 15)))
        assertFalse(range.contains(LocalDate.of(2025, 12, 31)))
        assertFalse(range.contains(LocalDate.of(2026, 2, 1)))
    }

    @Test
    fun counts_calculateDaysAndMonthsCorrectly() {
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 1, 10)
        val range = DateRange(start, end)
        assertEquals(10L, range.daysCount())

        val sixMonths = DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30))
        assertEquals(6L, sixMonths.monthsCount())
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidRange_throwsException() {
        DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1))
    }
}
