package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class CarActualTest {
    private val october = YearMonth.of(2026, 10)
    private val car = setOf("fuel", "car-insurance")
    private fun tx(id: String, month: YearMonth, agorot: Long, category: String?) = Transaction(
        id, month.atDay(5), month, "merchant", Money(agorot), PaymentMethod.CREDIT_CARD, "1234", category,
    )

    @Test fun sumsTheTwelveMonthsEndingAtEndMonth_refundsReduce() {
        val rows = listOf(
            tx("a", october, 40_000, "fuel"),
            tx("b", october.minusMonths(11), 250_000, "car-insurance"),
            tx("c", october.minusMonths(3), -5_000, "fuel"),
            tx("d", october, 99_000, "food"),
            tx("e", october, 1_000, null),
        )
        assertEquals(Money(285_000), carActualYearly(rows, car, october))
    }

    @Test fun linesOutsideTheWindowDoNotCount() {
        val rows = listOf(tx("a", october.minusMonths(12), 40_000, "fuel"), tx("b", october.plusMonths(1), 30_000, "fuel"), tx("c", october, 10_000, "fuel"))
        assertEquals(Money(10_000), carActualYearly(rows, car, october))
    }

    @Test fun noCarLineInTheWindow_isNull() {
        assertNull(carActualYearly(emptyList(), car, october))
        assertNull(carActualYearly(listOf(tx("a", october, 10_000, "food")), car, october))
        assertNull(carActualYearly(listOf(tx("a", october.minusMonths(12), 10_000, "fuel")), car, october))
    }
}
