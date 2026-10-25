package com.menash.myself.feature.calculators.pension.domain

data class PensionFundInput(
    val fundType: String = "",
    val provider: String = "",
    val currentValue: Double = 0.0,
    val totalDeposits: Double = 0.0,
    val feeOnBalancePercent: Double = 0.0,
    val feeOnDepositsPercent: Double = 0.0
)

data class PensionFundResult(
    val profit: Double,
    val profitPercent: Double,
    val yearlyBalanceFeeEstimate: Double
)

data class PensionPortfolioSummaryResult(
    val totalValue: Double,
    val totalDeposits: Double,
    val totalProfit: Double,
    val totalProfitPercent: Double,
    val highestBalanceFeePercent: Double,
    val highestBalanceFeeProvider: String?,
    val highestDepositFeePercent: Double,
    val highestDepositFeeProvider: String?
)

object PensionSavingsCalculator {

    /**
     * Calculates profit and fees for a single fund.
     * Locked by CharacterizationTests:
     * - pension_standardCalculations_locksCurrentOutput
     * - pension_zeroDeposits_locksZeroProfitPercent (KNOWN_ISSUES § 2.1)
     */
    fun calculateFund(fund: PensionFundInput): PensionFundResult {
        val profit = fund.currentValue - fund.totalDeposits
        val profitPercent = if (fund.totalDeposits > 0.0) {
            (profit / fund.totalDeposits) * 100.0
        } else {
            0.0
        }
        val yearlyBalanceFeeEstimate = fund.currentValue * (fund.feeOnBalancePercent / 100.0)

        return PensionFundResult(
            profit = profit,
            profitPercent = profitPercent,
            yearlyBalanceFeeEstimate = yearlyBalanceFeeEstimate
        )
    }

    /**
     * Calculates total portfolio value, profits, and identifies the highest fees across providers.
     * Locked by CharacterizationTests: pension_totalsAndHighestFeeTracking_locksCurrentOutput
     */
    fun calculatePortfolioSummary(funds: List<PensionFundInput>): PensionPortfolioSummaryResult {
        val totalValue = funds.sumOf { it.currentValue }
        val totalDeposits = funds.sumOf { it.totalDeposits }
        val totalProfit = totalValue - totalDeposits
        val totalProfitPercent = if (totalDeposits > 0.0) {
            (totalProfit / totalDeposits) * 100.0
        } else {
            0.0
        }

        val highestBalanceFund = funds.maxByOrNull { it.feeOnBalancePercent }
        val highestDepositFund = funds.maxByOrNull { it.feeOnDepositsPercent }

        return PensionPortfolioSummaryResult(
            totalValue = totalValue,
            totalDeposits = totalDeposits,
            totalProfit = totalProfit,
            totalProfitPercent = totalProfitPercent,
            highestBalanceFeePercent = highestBalanceFund?.feeOnBalancePercent ?: 0.0,
            highestBalanceFeeProvider = highestBalanceFund?.provider,
            highestDepositFeePercent = highestDepositFund?.feeOnDepositsPercent ?: 0.0,
            highestDepositFeeProvider = highestDepositFund?.provider
        )
    }
}
