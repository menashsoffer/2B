package com.menash.myself.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.menash.myself.data.entities.AiSubscriptionEntity
import com.menash.myself.data.entities.CarEntity
import com.menash.myself.data.entities.PensionFundEntity
import com.menash.myself.feature.calculators.ai.domain.AiSubscriptionCalculator
import com.menash.myself.feature.calculators.ai.domain.AiSubscriptionInput
import com.menash.myself.feature.calculators.car.domain.CarCostCalculator
import com.menash.myself.feature.calculators.car.domain.CarCostInput
import com.menash.myself.feature.calculators.car.domain.EnergyCostInput
import com.menash.myself.feature.calculators.pension.domain.PensionFundInput
import com.menash.myself.feature.calculators.pension.domain.PensionSavingsCalculator
import com.menash.myself.ui.calculators.car.CarCalculationVerbatimHelpers
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.NumberFormat
import java.util.Locale

/**
 * PARITY TESTS:
 * Verifies that for all Characterization-Test inputs, domain calculator results
 * strictly equal previous entity computed getters and verbatim helper outputs.
 */
@RunWith(RobolectricTestRunner::class)
class CalculatorParityTest {

    private val fmt = NumberFormat.getNumberInstance(Locale.US)
    private val testContext: Context by lazy { ApplicationProvider.getApplicationContext() }

    // =========================================================================
    // 1. CAR CALCULATOR PARITY
    // =========================================================================

    @Test
    fun car_standard5YearCost_parity() {
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
            tires5YearsTotal = 2800.0
        )

        val input = CarCostInput(
            purchasePrice = car.purchasePrice,
            resaleValueAfter5Years = car.resaleValueAfter5Years,
            energyCostYearly = car.energyCostYearly,
            insuranceYearly = car.insuranceYearly,
            trackerYearly = car.trackerYearly,
            maintenanceYearly = car.maintenanceYearly,
            annualTestYearly = car.annualTestYearly,
            tires5YearsTotal = car.tires5YearsTotal
        )

        val domainResult = CarCostCalculator.calculate5YearCost(input)

        // Strict parity between domain function and entity getters:
        assertEquals(car.yearlyRunningCost, domainResult.yearlyRunningCost, 0.0001)
        assertEquals(car.monthlyRunningCost, domainResult.monthlyRunningCost, 0.0001)
        assertEquals(car.fiveYearRunningCost, domainResult.fiveYearRunningCost, 0.0001)
        assertEquals(car.total5YearCost, domainResult.total5YearCost, 0.0001)
        assertEquals(car.net5YearCost, domainResult.net5YearCost, 0.0001)
        assertEquals(car.netMonthlyCost, domainResult.netMonthlyCost, 0.0001)

        // Parity with characterization constants:
        assertEquals(12950.0, domainResult.yearlyRunningCost, 0.0001)
        assertEquals(12950.0 / 12.0, domainResult.monthlyRunningCost, 0.0001)
        assertEquals(64750.0, domainResult.fiveYearRunningCost, 0.0001)
        assertEquals(287550.0, domainResult.total5YearCost, 0.0001)
        assertEquals(177550.0, domainResult.net5YearCost, 0.0001)
        assertEquals(177550.0 / 60.0, domainResult.netMonthlyCost, 0.0001)
    }

    @Test
    fun car_netCostClampedToZero_parity() {
        val profitableCar = CarEntity(
            id = 2L,
            name = "פורשה קלאסית",
            purchasePrice = 100000.0,
            resaleValueAfter5Years = 200000.0,
            energyCostYearly = 2000.0,
            insuranceYearly = 3000.0,
            trackerYearly = 500.0,
            maintenanceYearly = 2000.0,
            annualTestYearly = 1500.0,
            tires5YearsTotal = 2000.0
        )

        val input = CarCostInput(
            purchasePrice = profitableCar.purchasePrice,
            resaleValueAfter5Years = profitableCar.resaleValueAfter5Years,
            energyCostYearly = profitableCar.energyCostYearly,
            insuranceYearly = profitableCar.insuranceYearly,
            trackerYearly = profitableCar.trackerYearly,
            maintenanceYearly = profitableCar.maintenanceYearly,
            annualTestYearly = profitableCar.annualTestYearly,
            tires5YearsTotal = profitableCar.tires5YearsTotal
        )

        val domainResult = CarCostCalculator.calculate5YearCost(input)

        assertEquals(profitableCar.total5YearCost, domainResult.total5YearCost, 0.0001)
        assertEquals(profitableCar.net5YearCost, domainResult.net5YearCost, 0.0001)
        assertEquals(0.0, domainResult.netMonthlyCost, 0.0001)
        assertEquals(profitableCar.netMonthlyCost, domainResult.netMonthlyCost, 0.0001)
    }

    @Test
    fun car_energyCostAndParsing_parity() {
        val fuelInput = EnergyCostInput(
            isFuel = true,
            annualKm = 20000.0,
            kmPerLiter = 16.0,
            fuelPrice = 7.50
        )
        val domainFuel = CarCostCalculator.calculateEnergyCost(fuelInput)
        val helperFuel = CarCalculationVerbatimHelpers.calculateEnergyCostVerbatim(
            isFuel = true,
            annualKm = 20000.0,
            kmPerLiter = 16.0,
            fuelPrice = 7.50,
            kwhPer100Km = 0.0,
            homeKwhPrice = 0.0,
            publicSessionsCount = 0.0,
            publicSessionCost = 0.0
        )
        assertEquals(domainFuel, helperFuel, 0.0001)
        assertEquals(9375.0, domainFuel, 0.0001)

        // String parsing parity:
        for (inputStr in listOf("", "   ", "abc", "0", "16.5")) {
            val domainParsed = CarCostCalculator.parseKmPerLiter(inputStr)
            val helperParsed = CarCalculationVerbatimHelpers.parseKmPerLiterVerbatim(inputStr)
            assertEquals(domainParsed, helperParsed, 0.0001)
        }

        // Baseline diff parity:
        val domainDiff = CarCostCalculator.calculateBaselineComparison(143545.0, 144500.0)
        val helperDiff = CarCalculationVerbatimHelpers.calculateBaselineDiffVerbatim(testContext, 143545.0, 144500.0, "קורולה", fmt)
        assertEquals(domainDiff.diffFromBaseline, helperDiff.diffFromBaseline, 0.0001)
        assertEquals(domainDiff.isSaving, helperDiff.isSaving)
        assertEquals(domainDiff.diffMonthly, helperDiff.diffMonthly)
    }

    // =========================================================================
    // 2. AI SUBSCRIPTIONS PARITY
    // =========================================================================

    @Test
    fun aiSubscription_costs_parity() {
        val subUsdMonthly = AiSubscriptionEntity(
            id = 1L,
            name = "Claude Pro",
            category = "LLM",
            isActive = true,
            billingCycle = "MONTHLY",
            currency = "USD",
            price = 20.0
        )
        val rate = 3.75
        val input = AiSubscriptionInput(
            name = subUsdMonthly.name,
            category = subUsdMonthly.category,
            isActive = subUsdMonthly.isActive,
            billingCycle = subUsdMonthly.billingCycle,
            currency = subUsdMonthly.currency,
            price = subUsdMonthly.price
        )
        val domainCost = AiSubscriptionCalculator.calculateCost(input, rate)

        assertEquals(subUsdMonthly.priceInIls(rate), domainCost.priceInIls, 0.0001)
        assertEquals(subUsdMonthly.monthlyCostIls(rate), domainCost.monthlyCostIls, 0.0001)
        assertEquals(subUsdMonthly.yearlyCostIls(rate), domainCost.yearlyCostIls, 0.0001)

        // USD Yearly parity:
        val subUsdYearly = AiSubscriptionEntity(
            id = 2L,
            name = "Midjourney Pro",
            category = "Image",
            isActive = true,
            billingCycle = "YEARLY",
            currency = "USD",
            price = 360.0
        )
        val yearlyInput = AiSubscriptionInput(
            name = subUsdYearly.name,
            category = subUsdYearly.category,
            isActive = subUsdYearly.isActive,
            billingCycle = subUsdYearly.billingCycle,
            currency = subUsdYearly.currency,
            price = subUsdYearly.price
        )
        val yearlyCost = AiSubscriptionCalculator.calculateCost(yearlyInput, 3.60)
        assertEquals(subUsdYearly.monthlyCostIls(3.60), yearlyCost.monthlyCostIls, 0.0001)
        assertEquals(subUsdYearly.yearlyCostIls(3.60), yearlyCost.yearlyCostIls, 0.0001)
    }

    @Test
    fun aiSubscription_summaryAggregation_parity() {
        val rate = 3.5
        val entities = listOf(
            AiSubscriptionEntity(id = 1, name = "A", category = "AI", isActive = true, billingCycle = "MONTHLY", currency = "USD", price = 20.0),
            AiSubscriptionEntity(id = 2, name = "B", category = "AI", isActive = true, billingCycle = "YEARLY", currency = "ILS", price = 1200.0),
            AiSubscriptionEntity(id = 3, name = "C", category = "AI", isActive = false, billingCycle = "MONTHLY", currency = "USD", price = 100.0)
        )
        val inputs = entities.map {
            AiSubscriptionInput(it.name, it.category, it.isActive, it.billingCycle, it.currency, it.price)
        }

        val domainSummary = AiSubscriptionCalculator.calculateSummary(inputs, rate)
        val entitySum = entities.filter { it.isActive }.sumOf { it.monthlyCostIls(rate) }

        assertEquals(entitySum, domainSummary.totalMonthlyCostIls, 0.0001)
        assertEquals(170.0, domainSummary.totalMonthlyCostIls, 0.0001)
        assertEquals(2, domainSummary.activeCount)
    }

    // =========================================================================
    // 3. PENSION FUNDS PARITY
    // =========================================================================

    @Test
    fun pensionFund_calculations_parity() {
        val fund = PensionFundEntity(
            id = 10L,
            fundType = "קרן השתלמות",
            provider = "מיטב דש",
            currentValue = 180000.0,
            totalDeposits = 135000.0,
            feeOnBalancePercent = 0.35,
            feeOnDepositsPercent = 0.0
        )
        val input = PensionFundInput(
            fundType = fund.fundType,
            provider = fund.provider,
            currentValue = fund.currentValue,
            totalDeposits = fund.totalDeposits,
            feeOnBalancePercent = fund.feeOnBalancePercent,
            feeOnDepositsPercent = fund.feeOnDepositsPercent
        )
        val domainResult = PensionSavingsCalculator.calculateFund(input)

        assertEquals(fund.profit, domainResult.profit, 0.0001)
        assertEquals(fund.profitPercent, domainResult.profitPercent, 0.0001)
        assertEquals(fund.yearlyBalanceFeeEstimate, domainResult.yearlyBalanceFeeEstimate, 0.0001)

        // Zero deposits guard parity:
        val zeroFund = PensionFundEntity(fundType = "קופת גמל", provider = "הראל", currentValue = 10000.0, totalDeposits = 0.0)
        val zeroInput = PensionFundInput(fundType = "קופת גמל", provider = "הראל", currentValue = 10000.0, totalDeposits = 0.0)
        val zeroResult = PensionSavingsCalculator.calculateFund(zeroInput)
        assertEquals(zeroFund.profitPercent, zeroResult.profitPercent, 0.0001)
        assertEquals(0.0, zeroResult.profitPercent, 0.0001)
    }

    @Test
    fun pensionFund_portfolioSummary_parity() {
        val funds = listOf(
            PensionFundEntity(id = 1, fundType = "קרן פנסיה", provider = "מנורה", currentValue = 300000.0, totalDeposits = 200000.0, feeOnBalancePercent = 0.19, feeOnDepositsPercent = 1.49),
            PensionFundEntity(id = 2, fundType = "קרן השתלמות", provider = "אלטשולר", currentValue = 150000.0, totalDeposits = 100000.0, feeOnBalancePercent = 0.45, feeOnDepositsPercent = 0.0),
            PensionFundEntity(id = 3, fundType = "קופת גמל", provider = "ילין לפידות", currentValue = 50000.0, totalDeposits = 50000.0, feeOnBalancePercent = 0.60, feeOnDepositsPercent = 0.0)
        )
        val inputs = funds.map {
            PensionFundInput(it.fundType, it.provider, it.currentValue, it.totalDeposits, it.feeOnBalancePercent, it.feeOnDepositsPercent)
        }

        val domainSummary = PensionSavingsCalculator.calculatePortfolioSummary(inputs)

        assertEquals(funds.sumOf { it.currentValue }, domainSummary.totalValue, 0.0001)
        assertEquals(funds.sumOf { it.totalDeposits }, domainSummary.totalDeposits, 0.0001)
        assertEquals(funds.sumOf { it.profit }, domainSummary.totalProfit, 0.0001)
        assertEquals(funds.maxOf { it.feeOnBalancePercent }, domainSummary.highestBalanceFeePercent, 0.0001)
        assertEquals(funds.maxOf { it.feeOnDepositsPercent }, domainSummary.highestDepositFeePercent, 0.0001)
    }
}
