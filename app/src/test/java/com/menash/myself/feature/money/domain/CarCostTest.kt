package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class CarCostTest {
    private fun car(energy: Energy) = CarProfile("car", "car", energy)
    @Test fun fuelHandCalculationAndAllRunningItems() {
        // 12000 km / 15 km/l * 7.50 ILS = 6000 ILS energy.
        val c = car(Energy.Fuel(12000, BigDecimal(15), Money(750))).copy(
            insuranceYearly = Money(120000), licenseAndTestYearly = Money(12000),
            maintenanceYearly = Money(24000), otherYearly = Money(1200), tiresPerYear = Money(36000),
        )
        assertEquals(Money(793200), yearlyRunning(c))
        assertEquals(Money(66100), monthlyRunning(c))
    }
    @Test fun electricMixedHomePublicCharging() {
        // 10000 * 20/100 = 2000 kWh; 75% at 0.60 and 25% at 2 ILS = 0.95 ILS/kWh.
        val e = Energy.Electric(10000, BigDecimal(20), Money(60), BigDecimal("0.25"), Money(200))
        assertEquals(Money(190000), yearlyRunning(car(e)))
        assertEquals(Money(120000), yearlyRunning(car(e.copy(publicShare = BigDecimal.ZERO))))
        assertEquals(Money(400000), yearlyRunning(car(e.copy(publicShare = BigDecimal.ONE))))
    }
    @Test fun knownPassthroughAndZeroDistance() {
        assertEquals(Money(12345), yearlyRunning(car(Energy.Known(Money(12345)))))
        assertEquals(Money.ZERO, yearlyRunning(car(Energy.Fuel(0, BigDecimal.ONE, Money(700)))))
    }
    @Test fun monthlyRoundsOnceAndHalfUp() {
        val c = car(Energy.Fuel(164, BigDecimal(10), Money(1))) // 16.4/12 -> 1, not 2
        assertEquals(Money(16), yearlyRunning(c))
        assertEquals(Money(1), monthlyRunning(c))
        assertEquals(Money(2), monthlyRunning(car(Energy.Known(Money(18)))))
    }
    @Test fun invalidConsumptionPricesDistanceAndShareRejected() {
        for (rate in listOf(BigDecimal.ZERO, BigDecimal(-1))) {
            assertThrows(IllegalArgumentException::class.java) { Energy.Fuel(100, rate, Money(1)) }
            assertThrows(IllegalArgumentException::class.java) { Energy.Electric(100, rate, Money(1), BigDecimal.ZERO, Money(1)) }
        }
        assertThrows(IllegalArgumentException::class.java) { Energy.Fuel(-1, BigDecimal.ONE, Money(1)) }
        assertThrows(IllegalArgumentException::class.java) { Energy.Fuel(1, BigDecimal.ONE, Money(-1)) }
        assertThrows(IllegalArgumentException::class.java) { Energy.Known(Money(-1)) }
        assertThrows(IllegalArgumentException::class.java) { car(Energy.Known(Money.ZERO)).copy(tiresPerYear = Money(-1)) }
        for (share in listOf(BigDecimal("-0.01"), BigDecimal("1.01"))) {
            assertThrows(IllegalArgumentException::class.java) { Energy.Electric(100, BigDecimal.ONE, Money(1), share, Money(1)) }
        }
    }
    @Test fun overflowFailsInsteadOfWrapping() {
        assertThrows(ArithmeticException::class.java) {
            yearlyRunning(car(Energy.Known(Money(Long.MAX_VALUE))).copy(otherYearly = Money(1)))
        }
    }
}
