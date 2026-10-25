package com.menash.myself.domain

import com.menash.myself.core.domain.model.Car
import com.menash.myself.core.domain.model.Money
import com.menash.myself.data.entities.CarEntity
import com.menash.myself.feature.calculators.car.domain.CarCostCalculator
import com.menash.myself.feature.calculators.car.domain.CarCostInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CarMathParityTest {

    @Test
    fun car_standard5YearCost_moneyParityWithCharacterizationDouble() {
        // Double entity from CharacterizationTests: car_standard5YearCost_locksCurrentOutput
        val entity = CarEntity(
            id = 1L,
            name = "יונדאי איוניק 5",
            purchasePrice = 220000.0,
            resaleValueAfter5Years = 110000.0,
            energyCostYearly = 3500.0,
            insuranceYearly = 5200.0,
            trackerYearly = 650.0,
            maintenanceYearly = 1200.0,
            annualTestYearly = 2400.0,
            tires5YearsTotal = 2800.0,
            isBaseline = false
        )

        // Domain Car initialized with same inputs as Money
        val domainCar = Car(
            id = 1L,
            name = entity.name,
            purchasePrice = Money.fromShekels(entity.purchasePrice),
            resaleValueAfter5Years = Money.fromShekels(entity.resaleValueAfter5Years),
            energyCostYearly = Money.fromShekels(entity.energyCostYearly),
            insuranceYearly = Money.fromShekels(entity.insuranceYearly),
            trackerYearly = Money.fromShekels(entity.trackerYearly),
            maintenanceYearly = Money.fromShekels(entity.maintenanceYearly),
            annualTestYearly = Money.fromShekels(entity.annualTestYearly),
            tires5YearsTotal = Money.fromShekels(entity.tires5YearsTotal),
            isBaseline = entity.isBaseline
        )

        // Verify delegation: domainCar.costResult equals CarCostCalculator.calculate5YearCost(domainCar)
        assertEquals(CarCostCalculator.calculate5YearCost(domainCar), domainCar.costResult)

        // Parity assertions:
        // 1. yearlyRunningCost
        assertEquals(entity.yearlyRunningCost, domainCar.yearlyRunningCost.shekels, 0.001)

        // 2. monthlyRunningCost (12950 / 12 = 1079.1666... vs ₪1079.17 - sub-agora difference < 0.01 NIS)
        val monthlyDiff = abs(entity.monthlyRunningCost - domainCar.monthlyRunningCost.shekels)
        assertTrue("Sub-agora difference expected to be < 0.01 NIS, was $monthlyDiff", monthlyDiff < 0.01)
        assertEquals(1079.17, domainCar.monthlyRunningCost.shekels, 0.001)

        // 3. fiveYearRunningCost
        assertEquals(entity.fiveYearRunningCost, domainCar.fiveYearRunningCost.shekels, 0.001)

        // 4. total5YearCost
        assertEquals(entity.total5YearCost, domainCar.total5YearCost.shekels, 0.001)

        // 5. net5YearCost
        assertEquals(entity.net5YearCost, domainCar.net5YearCost.shekels, 0.001)

        // 6. netMonthlyCost (177550 / 60 = 2959.1666... vs ₪2959.17 - sub-agora difference < 0.01 NIS)
        val netMonthlyDiff = abs(entity.netMonthlyCost - domainCar.netMonthlyCost.shekels)
        assertTrue("Sub-agora difference expected to be < 0.01 NIS, was $netMonthlyDiff", netMonthlyDiff < 0.01)
        assertEquals(2959.17, domainCar.netMonthlyCost.shekels, 0.001)
    }

    @Test
    fun car_clampedToZero_moneyParityWithCharacterization() {
        // Profitable car from CharacterizationTests: car_netCostClampedToZero_whenResaleExceedsTotalCost
        val profitableCar = Car(
            id = 2L,
            name = "פורשה קלאסית",
            purchasePrice = Money.fromShekels(100000.0),
            resaleValueAfter5Years = Money.fromShekels(200000.0),
            energyCostYearly = Money.fromShekels(2000.0),
            insuranceYearly = Money.fromShekels(3000.0),
            trackerYearly = Money.fromShekels(500.0),
            maintenanceYearly = Money.fromShekels(2000.0),
            annualTestYearly = Money.fromShekels(1500.0),
            tires5YearsTotal = Money.fromShekels(2000.0)
        )

        // total5YearCost = 100000 + 45000 + 2000 = 147000
        assertEquals(147000.0, profitableCar.total5YearCost.shekels, 0.001)

        // net5YearCost = 147000 - 200000 = -53000 (negative)
        assertEquals(-53000.0, profitableCar.net5YearCost.shekels, 0.001)

        // Locked clamp: netMonthlyCost is ZERO
        assertEquals(Money.ZERO, profitableCar.netMonthlyCost)
    }
}
