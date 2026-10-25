package com.menash.myself.domain

import com.menash.myself.feature.calculators.car.domain.CarCostCalculator
import com.menash.myself.feature.calculators.car.domain.CarCostInput
import com.menash.myself.feature.calculators.car.domain.EnergyCostInput
import org.junit.Assert.*
import org.junit.Test
import java.text.NumberFormat
import java.util.Locale

class CarCostCalculatorTest {

    private val fmt = NumberFormat.getNumberInstance(Locale.US)

    @Test
    fun calculate5YearCost_standardCalculation_matchesCharacterization() {
        val input = CarCostInput(
            purchasePrice = 220000.0,
            resaleValueAfter5Years = 110000.0,
            energyCostYearly = 3500.0,
            insuranceYearly = 5200.0,
            trackerYearly = 650.0,
            maintenanceYearly = 1200.0,
            annualTestYearly = 2400.0,
            tires5YearsTotal = 2800.0
        )

        val result = CarCostCalculator.calculate5YearCost(input)

        // yearlyRunningCost = 3500 + 5200 + 650 + 1200 + 2400 = 12950.0
        assertEquals(12950.0, result.yearlyRunningCost, 0.001)
        assertEquals(12950.0 / 12.0, result.monthlyRunningCost, 0.001)
        assertEquals(64750.0, result.fiveYearRunningCost, 0.001)
        assertEquals(287550.0, result.total5YearCost, 0.001)
        assertEquals(177550.0, result.net5YearCost, 0.001)
        assertEquals(177550.0 / 60.0, result.netMonthlyCost, 0.001)
    }

    @Test
    fun calculate5YearCost_clampedToZero_whenResaleExceedsTotalCost() {
        // Locked by CharacterizationTests (KNOWN_ISSUES § 1.1)
        val profitableCar = CarCostInput(
            purchasePrice = 100000.0,
            resaleValueAfter5Years = 200000.0,
            energyCostYearly = 2000.0,
            insuranceYearly = 3000.0,
            trackerYearly = 500.0,
            maintenanceYearly = 2000.0,
            annualTestYearly = 1500.0,
            tires5YearsTotal = 2000.0
        )

        val result = CarCostCalculator.calculate5YearCost(profitableCar)
        assertEquals(147000.0, result.total5YearCost, 0.001)
        assertEquals(-53000.0, result.net5YearCost, 0.001)
        // Net monthly is clamped to 0.0
        assertEquals(0.0, result.netMonthlyCost, 0.0001)
    }

    @Test
    fun energyCalculation_fuelAndEv() {
        // Fuel
        val fuelInput = EnergyCostInput(
            isFuel = true,
            annualKm = 20000.0,
            kmPerLiter = 16.0,
            fuelPrice = 7.50
        )
        val fuelCost = CarCostCalculator.calculateEnergyCost(fuelInput)
        assertEquals(9375.0, fuelCost, 0.001)

        // EV
        val evInput = EnergyCostInput(
            isFuel = false,
            annualKm = 18000.0,
            kwhPer100Km = 17.5,
            homeKwhPrice = 0.62,
            publicSessionsCount = 10.0,
            publicSessionCost = 55.0
        )
        val evCost = CarCostCalculator.calculateEnergyCost(evInput)
        assertEquals(2503.0, evCost, 0.001)
    }

    @Test
    fun parseKmPerLiter_emptyFallbackToOne_locksKnownIssue() {
        assertEquals(1.0, CarCostCalculator.parseKmPerLiter(""), 0.0001)
        assertEquals(1.0, CarCostCalculator.parseKmPerLiter("abc"), 0.0001)
        assertEquals(15.5, CarCostCalculator.parseKmPerLiter("15.5"), 0.0001)
    }

    @Test
    fun baselineComparison_truncationTowardZero_matchesCharacterization() {
        // Saving
        val saving = CarCostCalculator.calculateBaselineComparison(
            carNet5YearCost = 143545.0,
            baselineNet5YearCost = 144500.0,
            baselineCarName = "קורולה בסיס",
            fmt = fmt
        )
        assertTrue(saving.isSaving)
        assertEquals(955.0, saving.diffFromBaseline, 0.001)
        assertEquals(15, saving.diffMonthly)
        assertEquals("חיסכון של ₪955 מול קורולה בסיס (₪15/חודש)", saving.text)

        // Expensive
        val expensive = CarCostCalculator.calculateBaselineComparison(
            carNet5YearCost = 145455.0,
            baselineNet5YearCost = 144500.0,
            baselineCarName = "קורולה בסיס",
            fmt = fmt
        )
        assertFalse(expensive.isSaving)
        assertEquals(-955.0, expensive.diffFromBaseline, 0.001)
        assertEquals(-15, expensive.diffMonthly)
        assertEquals("יקר ב-₪955 מול קורולה בסיס (₪15/חודש)", expensive.text)
    }
}
