package com.menash.myself

import com.menash.myself.data.entities.AiSubscriptionEntity
import com.menash.myself.data.entities.CarEntity
import com.menash.myself.data.entities.PensionFundEntity
import com.menash.myself.ui.calculators.car.CarCalculationVerbatimHelpers
import org.junit.Assert.*
import org.junit.Test
import java.text.NumberFormat
import java.util.Locale

/**
 * STEP 0 — CHARACTERIZATION TESTS
 *
 * These tests lock the EXACT current outputs and edge cases of the calculator algorithms.
 * They MUST pass before any code moves, and after every refactoring step.
 * Any documented quirks or edge cases are also tracked in docs/KNOWN_ISSUES.md.
 */
class CharacterizationTests {

    private val localeFmt = NumberFormat.getNumberInstance(Locale.US)

    // =========================================================================
    // 1. CAR COMPARISON CALCULATOR TESTS
    // =========================================================================

    @Test
    fun car_standard5YearCost_locksCurrentOutput() {
        // Inputs
        val car = CarEntity(
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

        // Locked Output Assertions:
        // yearlyRunningCost = 3500 + 5200 + 650 + 1200 + 2400 = 12950.0
        assertEquals(12950.0, car.yearlyRunningCost, 0.001)

        // monthlyRunningCost = 12950 / 12 = 1079.1666...
        assertEquals(12950.0 / 12.0, car.monthlyRunningCost, 0.001)

        // fiveYearRunningCost = 12950 * 5 = 64750.0
        assertEquals(64750.0, car.fiveYearRunningCost, 0.001)

        // total5YearCost = 220000 + 64750 + 2800 = 287550.0
        assertEquals(287550.0, car.total5YearCost, 0.001)

        // net5YearCost = 287550 - 110000 = 177550.0
        assertEquals(177550.0, car.net5YearCost, 0.001)

        // netMonthlyCost = 177550 / 60 = 2959.1666...
        assertEquals(177550.0 / 60.0, car.netMonthlyCost, 0.001)
    }

    @Test
    fun car_netCostClampedToZero_whenResaleExceedsTotalCost() {
        // KNOWN ISSUE LOCK (docs/KNOWN_ISSUES.md § 1.1):
        // When resaleValueAfter5Years >= total5YearCost, net5YearCost <= 0.
        // Current implementation clamps netMonthlyCost to 0.0 instead of negative.
        val profitableCar = CarEntity(
            id = 2L,
            name = "פורשה קלאסית",
            purchasePrice = 100000.0,
            resaleValueAfter5Years = 200000.0, // Resale is much higher than total cost
            energyCostYearly = 2000.0,
            insuranceYearly = 3000.0,
            trackerYearly = 500.0,
            maintenanceYearly = 2000.0,
            annualTestYearly = 1500.0,
            tires5YearsTotal = 2000.0
        )

        // total5YearCost = 100000 + (9000 * 5) + 2000 = 147000.0
        assertEquals(147000.0, profitableCar.total5YearCost, 0.001)

        // net5YearCost = 147000 - 200000 = -53000.0 (negative)
        assertEquals(-53000.0, profitableCar.net5YearCost, 0.001)

        // LOCKED BEHAVIOR: netMonthlyCost is clamped to 0.0 (not -53000/60)
        assertEquals(0.0, profitableCar.netMonthlyCost, 0.0001)
    }

    @Test
    fun car_energyHelper_fuelCalculation_locksCurrentOutput() {
        // Inputs: 20000 km, 16.0 km/liter, 7.50 NIS/liter
        val calculated = CarCalculationVerbatimHelpers.calculateEnergyCostVerbatim(
            isFuel = true,
            annualKm = 20000.0,
            kmPerLiter = 16.0,
            fuelPrice = 7.50,
            kwhPer100Km = 0.0,
            homeKwhPrice = 0.0,
            publicSessionsCount = 0.0,
            publicSessionCost = 0.0
        )

        // Expected: (20000 / 16.0) * 7.50 = 1250 * 7.50 = 9375.0
        assertEquals(9375.0, calculated, 0.001)
    }

    @Test
    fun car_energyHelper_fuelZeroKmPerLiter_locksZeroFallback() {
        // KNOWN ISSUE LOCK (docs/KNOWN_ISSUES.md § 1.3):
        // When kmPerLiter == 0.0, returns 0.0 instead of error/infinity.
        val calculated = CarCalculationVerbatimHelpers.calculateEnergyCostVerbatim(
            isFuel = true,
            annualKm = 15000.0,
            kmPerLiter = 0.0,
            fuelPrice = 7.50,
            kwhPer100Km = 0.0,
            homeKwhPrice = 0.0,
            publicSessionsCount = 0.0,
            publicSessionCost = 0.0
        )

        assertEquals(0.0, calculated, 0.0001)
    }

    @Test
    fun car_energyHelper_emptyKmPerLiterFallbackToOne_locksInflatedFuelCost() {
        // KNOWN ISSUE LOCK (docs/KNOWN_ISSUES.md § 1.4):
        // In CarComparisonScreen, empty input string kmPerLiter evaluates as:
        // val kml = kmPerLiter.toDoubleOrNull() ?: 1.0
        // When user clears the field (""), kml becomes 1.0, and cost is calculated as:
        // (annualKm / 1.0) * fuelPrice -> 20,000 * 7.50 = 150,000 NIS/year (inflated).
        val inputStr = ""
        val kml = inputStr.toDoubleOrNull() ?: 1.0
        assertEquals(1.0, kml, 0.0001)

        val calculated = CarCalculationVerbatimHelpers.calculateEnergyCostVerbatim(
            isFuel = true,
            annualKm = 20000.0,
            kmPerLiter = kml,
            fuelPrice = 7.50,
            kwhPer100Km = 0.0,
            homeKwhPrice = 0.0,
            publicSessionsCount = 0.0,
            publicSessionCost = 0.0
        )

        // LOCKED BEHAVIOR: 20000 / 1.0 * 7.50 = 150,000.0 NIS
        assertEquals(150000.0, calculated, 0.001)
    }

    @Test
    fun car_energyHelper_evCalculation_locksCurrentOutput() {
        // Inputs: 18000 km, 17.5 kWh/100km, 0.62 NIS/kWh, 10 public sessions at 55.0 NIS each
        val calculated = CarCalculationVerbatimHelpers.calculateEnergyCostVerbatim(
            isFuel = false,
            annualKm = 18000.0,
            kmPerLiter = 0.0,
            fuelPrice = 0.0,
            kwhPer100Km = 17.5,
            homeKwhPrice = 0.62,
            publicSessionsCount = 10.0,
            publicSessionCost = 55.0
        )

        // homeTotal = (18000 / 100) * 17.5 * 0.62 = 180 * 10.85 = 1953.0
        // publicTotal = 10 * 55 = 550.0
        // total = 1953.0 + 550.0 = 2503.0
        assertEquals(2503.0, calculated, 0.001)
    }

    @Test
    fun car_baselineDiff_savingTruncationTowardZero_locksCurrentOutput() {
        // KNOWN ISSUE LOCK (docs/KNOWN_ISSUES.md § 1.2):
        // Baseline difference monthly is truncated toward zero via .toInt(), not rounded.
        // Diff = 144500 (baseline) - 143545 (car) = 955.0
        // diffMonthly = 955.0 / 60.0 = 15.91666... -> .toInt() = 15 (NOT rounded to 16!)
        val result = CarCalculationVerbatimHelpers.calculateBaselineDiffVerbatim(
            carNet5YearCost = 143545.0,
            baselineNet5YearCost = 144500.0,
            baselineCarName = "קורולה בסיס",
            fmt = localeFmt
        )

        assertTrue(result.isSaving)
        assertEquals(955.0, result.diffFromBaseline, 0.001)
        // LOCKED: 15.9166 truncates to 15
        assertEquals(15, result.diffMonthly)
        assertEquals("חיסכון של ₪955 מול קורולה בסיס (₪15/חודש)", result.text)
    }

    @Test
    fun car_baselineDiff_expensiveTruncationTowardZero_locksCurrentOutput() {
        // KNOWN ISSUE LOCK (docs/KNOWN_ISSUES.md § 1.2):
        // Negative difference: car is more expensive than baseline.
        // Diff = 144500 (baseline) - 145455 (car) = -955.0
        // diffMonthly = -955.0 / 60.0 = -15.91666... -> .toInt() = -15
        val result = CarCalculationVerbatimHelpers.calculateBaselineDiffVerbatim(
            carNet5YearCost = 145455.0,
            baselineNet5YearCost = 144500.0,
            baselineCarName = "קורולה בסיס",
            fmt = localeFmt
        )

        assertFalse(result.isSaving)
        assertEquals(-955.0, result.diffFromBaseline, 0.001)
        // LOCKED: -15.9166 truncates to -15
        assertEquals(-15, result.diffMonthly)
        assertEquals("יקר ב-₪955 מול קורולה בסיס (₪15/חודש)", result.text)
    }

    // =========================================================================
    // 2. AI SUBSCRIPTIONS CALCULATOR TESTS
    // =========================================================================

    @Test
    fun aiSubscription_usdMonthly_locksCurrentOutput() {
        val sub = AiSubscriptionEntity(
            id = 1L,
            name = "Claude Pro",
            category = "LLM",
            isActive = true,
            billingCycle = "MONTHLY",
            currency = "USD",
            price = 20.0
        )
        val usdRate = 3.75

        // priceInIls = 20 * 3.75 = 75.0
        assertEquals(75.0, sub.priceInIls(usdRate), 0.001)
        // monthlyCostIls = 75.0
        assertEquals(75.0, sub.monthlyCostIls(usdRate), 0.001)
        // yearlyCostIls = 75 * 12 = 900.0
        assertEquals(900.0, sub.yearlyCostIls(usdRate), 0.001)
    }

    @Test
    fun aiSubscription_usdYearly_locksCurrentOutput() {
        val sub = AiSubscriptionEntity(
            id = 2L,
            name = "Midjourney Pro",
            category = "Image",
            isActive = true,
            billingCycle = "YEARLY",
            currency = "USD",
            price = 360.0
        )
        val usdRate = 3.60

        // priceInIls = 360 * 3.60 = 1296.0
        assertEquals(1296.0, sub.priceInIls(usdRate), 0.001)
        // monthlyCostIls = 1296 / 12 = 108.0
        assertEquals(108.0, sub.monthlyCostIls(usdRate), 0.001)
        // yearlyCostIls = 1296.0
        assertEquals(1296.0, sub.yearlyCostIls(usdRate), 0.001)
    }

    @Test
    fun aiSubscription_ilsMonthlyAndYearly_locksCurrentOutput() {
        val subMonthly = AiSubscriptionEntity(
            id = 3L,
            name = "ישראל AI",
            category = "Productivity",
            isActive = true,
            billingCycle = "MONTHLY",
            currency = "ILS",
            price = 50.0
        )
        assertEquals(50.0, subMonthly.monthlyCostIls(4.0), 0.001)
        assertEquals(600.0, subMonthly.yearlyCostIls(4.0), 0.001)

        val subYearly = AiSubscriptionEntity(
            id = 4L,
            name = "ארכיון ענן",
            category = "Storage",
            isActive = true,
            billingCycle = "YEARLY",
            currency = "ILS",
            price = 240.0
        )
        assertEquals(20.0, subYearly.monthlyCostIls(4.0), 0.001)
        assertEquals(240.0, subYearly.yearlyCostIls(4.0), 0.001)
    }

    @Test
    fun aiSubscription_activeAggregation_locksCurrentOutput() {
        val rate = 3.5
        val list = listOf(
            AiSubscriptionEntity(id = 1, name = "A", category = "AI", isActive = true, billingCycle = "MONTHLY", currency = "USD", price = 20.0), // 70/mo
            AiSubscriptionEntity(id = 2, name = "B", category = "AI", isActive = true, billingCycle = "YEARLY", currency = "ILS", price = 1200.0), // 100/mo
            AiSubscriptionEntity(id = 3, name = "C", category = "AI", isActive = false, billingCycle = "MONTHLY", currency = "USD", price = 100.0) // inactive!
        )

        val active = list.filter { it.isActive }
        assertEquals(2, active.size)
        val sumMonthly = active.sumOf { it.monthlyCostIls(rate) }
        // 70 + 100 = 170
        assertEquals(170.0, sumMonthly, 0.001)
    }

    // =========================================================================
    // 3. PENSION & SAVINGS CALCULATOR TESTS
    // =========================================================================

    @Test
    fun pension_standardCalculations_locksCurrentOutput() {
        val fund = PensionFundEntity(
            id = 10L,
            fundType = "קרן השתלמות",
            provider = "מיטב דש",
            currentValue = 180000.0,
            totalDeposits = 135000.0,
            feeOnBalancePercent = 0.35,
            feeOnDepositsPercent = 0.0,
            notes = "מסלול מניות"
        )

        // profit = 180000 - 135000 = 45000.0
        assertEquals(45000.0, fund.profit, 0.001)

        // profitPercent = (45000 / 135000) * 100 = 33.3333...
        val expectedPercent = (45000.0 / 135000.0) * 100.0
        assertEquals(expectedPercent, fund.profitPercent, 0.001)

        // yearlyBalanceFeeEstimate = 180000 * (0.35 / 100) = 630.0
        assertEquals(630.0, fund.yearlyBalanceFeeEstimate, 0.001)
    }

    @Test
    fun pension_zeroDeposits_locksZeroProfitPercent() {
        // KNOWN ISSUE LOCK (docs/KNOWN_ISSUES.md § 2.1):
        // When totalDeposits == 0.0, profitPercent returns 0.0 rather than division by zero
        val fund = PensionFundEntity(
            id = 11L,
            fundType = "קופת גמל",
            provider = "הראל",
            currentValue = 10000.0,
            totalDeposits = 0.0,
            feeOnBalancePercent = 0.5,
            feeOnDepositsPercent = 0.0
        )

        assertEquals(10000.0, fund.profit, 0.001)
        // LOCKED: returns 0.0 when deposits == 0.0
        assertEquals(0.0, fund.profitPercent, 0.0001)
    }

    @Test
    fun pension_totalsAndHighestFeeTracking_locksCurrentOutput() {
        val funds = listOf(
            PensionFundEntity(id = 1, fundType = "קרן פנסיה", provider = "מנורה", currentValue = 300000.0, totalDeposits = 200000.0, feeOnBalancePercent = 0.19, feeOnDepositsPercent = 1.49),
            PensionFundEntity(id = 2, fundType = "קרן השתלמות", provider = "אלטשולר", currentValue = 150000.0, totalDeposits = 100000.0, feeOnBalancePercent = 0.45, feeOnDepositsPercent = 0.0),
            PensionFundEntity(id = 3, fundType = "קופת גמל", provider = "ילין לפידות", currentValue = 50000.0, totalDeposits = 50000.0, feeOnBalancePercent = 0.60, feeOnDepositsPercent = 0.0)
        )

        // Total Value = 300000 + 150000 + 50000 = 500000.0
        val totalValue = funds.sumOf { it.currentValue }
        assertEquals(500000.0, totalValue, 0.001)

        // Total Deposits = 200000 + 100000 + 50000 = 350000.0
        val totalDeposits = funds.sumOf { it.totalDeposits }
        assertEquals(350000.0, totalDeposits, 0.001)

        // Total Profit = 500000 - 350000 = 150000.0
        val totalProfit = totalValue - totalDeposits
        assertEquals(150000.0, totalProfit, 0.001)

        // Total Profit Percent = (150000 / 350000) * 100 = 42.85714...
        val totalProfitPercent = (totalProfit / totalDeposits) * 100.0
        assertEquals(42.85714, totalProfitPercent, 0.001)

        // Highest fees
        val maxBalanceFee = funds.maxOfOrNull { it.feeOnBalancePercent } ?: 0.0
        val highestBalanceFund = funds.firstOrNull { it.feeOnBalancePercent == maxBalanceFee }
        assertEquals(0.60, maxBalanceFee, 0.001)
        assertEquals("ילין לפידות", highestBalanceFund?.provider)

        val maxDepositFee = funds.maxOfOrNull { it.feeOnDepositsPercent } ?: 0.0
        val highestDepositFund = funds.firstOrNull { it.feeOnDepositsPercent == maxDepositFee }
        assertEquals(1.49, maxDepositFee, 0.001)
        assertEquals("מנורה", highestDepositFund?.provider)
    }
}
