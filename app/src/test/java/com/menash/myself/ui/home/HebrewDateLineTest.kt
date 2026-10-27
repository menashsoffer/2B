package com.menash.myself.ui.home

import android.icu.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HebrewDateLineTest {

    @Test
    fun formatsGregorianHebrewWithoutYear() {
        val calendar = Calendar.getInstance()
        calendar.set(2026, Calendar.OCTOBER, 27, 21, 14, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        assertEquals("יום שלישי, 27 באוקטובר", hebrewDateLine(calendar.timeInMillis))
    }
}
