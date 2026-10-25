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
    fun roundingPolicy_halfUpOnPointFiveCases() {
        // 5 / 2 = 2.5 -> rounds HALF_UP to 3
        val divHalf = Money.fromAgorot(5L) / 2
        assertEquals(3L, divHalf.agorot)

        // 1 * 1.5 = 1.5 -> rounds HALF_UP to 2
        val timesHalf = Money.fromAgorot(1L) * 1.5
        assertEquals(2L, timesHalf.agorot)

        // 1 * 1.4 = 1.4 -> rounds down to 1
        val timesBelowHalf = Money.fromAgorot(1L) * 1.4
        assertEquals(1L, timesBelowHalf.agorot)

        // 7 / 3 = 2.333... -> rounds to 2
        val divThird = Money.fromAgorot(7L) / 3
        assertEquals(2L, divThird.agorot)

        // 8 / 3 = 2.666... -> rounds to 3
        val divTwoThirds = Money.fromAgorot(8L) / 3
        assertEquals(3L, divTwoThirds.agorot)
    }

    @Test
    fun roundingPolicy_negativeNumbersAndHalfUp() {
        // -5 / 2 = -2.5 -> rounds HALF_UP to -3 (away from zero for .5)
        val divNegHalf = Money.fromAgorot(-5L) / 2
        assertEquals(-3L, divNegHalf.agorot)

        // -1 * 1.5 = -1.5 -> rounds HALF_UP to -2
        val timesNegHalf = Money.fromAgorot(-1L) * 1.5
        assertEquals(-2L, timesNegHalf.agorot)

        // -1 * 1.4 = -1.4 -> rounds to -1
        val timesNegBelow = Money.fromAgorot(-1L) * 1.4
        assertEquals(-1L, timesNegBelow.agorot)

        // Unary minus
        val pos = Money.fromShekels(50.0)
        val neg = -pos
        assertEquals(-5000L, neg.agorot)
        assertTrue(neg.isNegative())
        assertFalse(neg.isPositive())
        assertFalse(neg.isZero())
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
        val mNeg = Money.fromShekels(-10.0)
        val m10 = Money.fromShekels(10.0)
        val m20 = Money.fromShekels(20.0)
        assertTrue(mNeg < m10)
        assertTrue(m10 < m20)
        assertEquals(0, m10.compareTo(Money.fromShekels(10.0)))
    }
}
