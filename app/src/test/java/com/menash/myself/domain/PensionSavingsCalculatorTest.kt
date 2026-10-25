package com.menash.myself.domain

import com.menash.myself.feature.calculators.pension.domain.PensionFundInput
import com.menash.myself.feature.calculators.pension.domain.PensionSavingsCalculator
import org.junit.Assert.*
import org.junit.Test

class PensionSavingsCalculatorTest {

    @Test
    fun calculateFund_standardCalculation() {
        val fund = PensionFundInput(
            fundType = "קרן השתלמות",
            provider = "מיטב דש",
            currentValue = 180000.0,
            totalDeposits = 135000.0,
            feeOnBalancePercent = 0.35,
            feeOnDepositsPercent = 0.0
        )

        val result = PensionSavingsCalculator.calculateFund(fund)
        assertEquals(45000.0, result.profit, 0.001)
        val expectedPercent = (45000.0 / 135000.0) * 100.0
        assertEquals(expectedPercent, result.profitPercent, 0.001)
        assertEquals(630.0, result.yearlyBalanceFeeEstimate, 0.001)
    }

    @Test
    fun calculateFund_zeroDeposits_locksZeroProfitPercent() {
        // Locked by CharacterizationTests (KNOWN_ISSUES § 2.1)
        val fund = PensionFundInput(
            currentValue = 10000.0,
            totalDeposits = 0.0
        )
        val result = PensionSavingsCalculator.calculateFund(fund)
        assertEquals(10000.0, result.profit, 0.001)
        assertEquals(0.0, result.profitPercent, 0.0001)
    }

    @Test
    fun calculatePortfolioSummary_aggregatesTotalsAndTracksHighestFees() {
        val funds = listOf(
            PensionFundInput(provider = "מנורה", currentValue = 300000.0, totalDeposits = 200000.0, feeOnBalancePercent = 0.19, feeOnDepositsPercent = 1.49),
            PensionFundInput(provider = "אלטשולר", currentValue = 150000.0, totalDeposits = 100000.0, feeOnBalancePercent = 0.45, feeOnDepositsPercent = 0.0),
            PensionFundInput(provider = "ילין לפידות", currentValue = 50000.0, totalDeposits = 50000.0, feeOnBalancePercent = 0.60, feeOnDepositsPercent = 0.0)
        )

        val summary = PensionSavingsCalculator.calculatePortfolioSummary(funds)
        assertEquals(500000.0, summary.totalValue, 0.001)
        assertEquals(350000.0, summary.totalDeposits, 0.001)
        assertEquals(150000.0, summary.totalProfit, 0.001)
        assertEquals(42.85714, summary.totalProfitPercent, 0.001)

        assertEquals(0.60, summary.highestBalanceFeePercent, 0.001)
        assertEquals("ילין לפידות", summary.highestBalanceFeeProvider)

        assertEquals(1.49, summary.highestDepositFeePercent, 0.001)
        assertEquals("מנורה", summary.highestDepositFeeProvider)
    }
}
