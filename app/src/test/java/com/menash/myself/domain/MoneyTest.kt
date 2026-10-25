package com.menash.myself.domain

import com.menash.myself.core.domain.model.Money
import org.junit.Assert.*
import org.junit.Test

class MoneyTest {

    @Test
    fun fromShekels_convertsCorrectlyToAgorot() {
        val m = Money.fromShekels(12.50)
        assertEquals(1250L, m.agorot)
        assertEquals(12.50, m.shekels, 0.001)

        val intShekels = Money.fromShekels(100L)
        assertEquals(10000L, intShekels.agorot)
        assertEquals(100.0, intShekels.shekels, 0.001)
    }

    @Test
    fun arithmeticOperators_workAccurately() {
        val a = Money.fromShekels(50.25)
        val b = Money.fromShekels(25.75)

        val sum = a + b
        assertEquals(7600L, sum.agorot)
        assertEquals(76.0, sum.shekels, 0.001)

        val diff = a - b
        assertEquals(2450L, diff.agorot)
        assertEquals(24.50, diff.shekels, 0.001)

        val multiplied = Money.fromShekels(10.0) * 3
        assertEquals(3000L, multiplied.agorot)

        val divided = Money.fromShekels(100.0) / 4
        assertEquals(2500L, divided.agorot)
    }

    @Test
    fun formatting_formatsHebrewShekelsProperly() {
        val m1 = Money.fromShekels(1250.0)
        assertEquals("₪1,250", m1.formatShekels(includeAgorot = false))

        val m2 = Money.fromShekels(1250.50)
        assertEquals("₪1,250.50", m2.formatShekels(includeAgorot = true))

        val negative = Money.fromShekels(-450.0)
        assertEquals("-₪450", negative.formatShekels(includeAgorot = false))
    }

    @Test
    fun compareTo_ordersCorrectly() {
        val m10 = Money.fromShekels(10.0)
        val m20 = Money.fromShekels(20.0)
        assertTrue(m10 < m20)
        assertEquals(0, m10.compareTo(Money.fromShekels(10.0)))
    }
}
