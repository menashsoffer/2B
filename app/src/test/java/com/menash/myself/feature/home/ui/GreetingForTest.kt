package com.menash.myself.feature.home.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GreetingForTest {
    private val today = LocalDate.of(2026, 11, 12)
    private val yesterday = LocalDate.of(2026, 11, 11)

    @Test
    fun textPerVisitAndSignIn() {
        assertEquals("נעים להכיר", greetingLine(GreetingVisit.First, null))
        assertEquals("נעים להכיר, דנה", greetingLine(GreetingVisit.First, "דנה כהן"))
        assertEquals("טוב לראות אותך שוב", greetingLine(GreetingVisit.Returning, null))
        assertEquals("טוב לראות אותך שוב, דנה", greetingLine(GreetingVisit.Returning, "דנה"))
        assertFalse(greetingLine(GreetingVisit.First, "דנה").contains("שלום"))
        assertFalse(greetingLine(GreetingVisit.Returning, "דנה").contains("ערב"))
    }

    @Test
    fun nameIsFirstTokenIsolatedAndEllipsized() {
        assertEquals("דנה", firstNameOf("דנה כהן"))
        assertNull(greetingName(null))
        assertNull(greetingName("   "))
        assertEquals("\u2068Dana\u2069", greetingName("Dana Cohen"))
        val long = "Abcdefghijklmnopq"
        assertEquals("\u2068Abcdefghijklmno…\u2069", greetingName(long))
        assertEquals("נעים להכיר", greetingLine(GreetingVisit.First, "  "))
    }

    @Test
    fun onceADayUsesTheCalendarDate() {
        val fresh = GreetingRecord()
        assertEquals(GreetingVisit.First, greetingVisit(fresh, today))
        assertTrue(shouldAnimateGreeting(fresh, today, reducedMotion = false))
        assertFalse(shouldAnimateGreeting(fresh, today, reducedMotion = true))
        val shown = markGreetingShown(fresh, today)
        assertEquals(today, shown.firstDate)
        assertEquals(today, shown.lastShown)
        assertEquals(GreetingVisit.First, greetingVisit(shown, today))
        assertFalse(shouldAnimateGreeting(shown, today, reducedMotion = false))
        assertEquals(GreetingVisit.Returning, greetingVisit(shown, yesterday.plusDays(2)))
        assertTrue(shouldAnimateGreeting(shown, yesterday.plusDays(2), reducedMotion = false))
    }

    @Test
    fun animationClock() {
        assertEquals(0f, greetingUnderlineFraction(0), 0.001f)
        assertEquals(0f, greetingUnderlineFraction(150), 0.001f)
        assertEquals(1f, greetingUnderlineFraction(750), 0.001f)
        assertTrue(greetingUnderlineFraction(400) in 0f..1f)
        assertEquals(0f, greetingLeafFraction(750), 0.001f)
        assertEquals(1f, greetingLeafFraction(1000), 0.001f)
        assertTrue(greetingLeafFraction(900) in 0.01f..0.99f)
    }
}
