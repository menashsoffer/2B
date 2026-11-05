package com.menash.myself.feature.home.ui

import com.menash.myself.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class GreetingForTest {

    @Test
    fun boundaries() {
        assertEquals(R.string.greeting_evening, greetingFor(4))
        assertEquals(R.string.greeting_morning, greetingFor(5))
        assertEquals(R.string.greeting_morning, greetingFor(11))
        assertEquals(R.string.greeting_afternoon, greetingFor(12))
        assertEquals(R.string.greeting_afternoon, greetingFor(16))
        assertEquals(R.string.greeting_evening, greetingFor(17))
        assertEquals(R.string.greeting_evening, greetingFor(20))
        assertEquals(R.string.greeting_evening, greetingFor(21))
        assertEquals(R.string.greeting_evening, greetingFor(0))
        assertEquals(R.string.greeting_evening, greetingFor(23))
    }

    @Test
    fun titleUsesFirstNameOnly() {
        assertEquals("ערב טוב, דנה", homeTitle("דנה", "ערב טוב"))
        assertEquals("בוקר טוב, דנה", homeTitle("  דנה  ", "בוקר טוב"))
        assertEquals("צהריים טובים", homeTitle(null, "צהריים טובים"))
        assertEquals("צהריים טובים", homeTitle("   ", "צהריים טובים"))
        assertEquals("דנה", firstNameOf("דנה דנה"))
    }

    @Test
    fun missingNameDoesNotInventAPlaceholder() {
        assertNull(firstNameOf(null))
        assertNull(firstNameOf(""))
        assertNull(firstNameOf("  "))
        assertEquals("דנה", firstNameOf("דנה"))
        assertFalse(homeTitle(null, "ערב טוב").contains("משתמש"))
    }
}
