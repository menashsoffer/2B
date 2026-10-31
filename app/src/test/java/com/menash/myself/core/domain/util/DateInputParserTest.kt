package com.menash.myself.core.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Due dates typed by hand in the task and dream dialogs. */
class DateInputParserTest {

    @Test
    fun parsesIsoAndDayMonthYear() {
        assertEquals(LocalDate.of(2026, 10, 31), DateInputParser.parseOrNull("2026-10-31"))
        assertEquals(LocalDate.of(2026, 3, 5), DateInputParser.parseOrNull(" 5/3/2026 "))
        assertEquals(LocalDate.of(2026, 3, 5), DateInputParser.parseOrNull("05/03/2026"))
    }

    @Test
    fun blankOrUnreadableGivesNull() {
        assertNull(DateInputParser.parseOrNull(null))
        assertNull(DateInputParser.parseOrNull("  "))
        assertNull(DateInputParser.parseOrNull("31.10.2026"))
        assertNull(DateInputParser.parseOrNull("2026-02-30"))
    }

    @Test
    fun parseWithRaw_keepsTheTextItCouldNotRead() {
        assertEquals(LocalDate.of(2026, 1, 2) to null, DateInputParser.parseWithRaw("2026-01-02"))
        assertEquals(null to "next week", DateInputParser.parseWithRaw(" next week "))
        assertEquals(null to null, DateInputParser.parseWithRaw(""))
    }

    @Test
    fun formatting_isIsoWithAnOptionalRawFallback() {
        assertEquals("2026-10-31", DateInputParser.formatIso(LocalDate.of(2026, 10, 31)))
        assertEquals("", DateInputParser.formatIso(null))
        assertEquals("next week", DateInputParser.formatDate(null, "next week"))
    }
}
