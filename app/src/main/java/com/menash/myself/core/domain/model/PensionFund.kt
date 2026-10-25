package com.menash.myself.core.domain.model

data class PensionFund(
    val id: Long = 0,
    val fundType: String,
    val provider: String,
    val currentValue: Money = Money.ZERO,
    val totalDeposits: Money = Money.ZERO,
    val feeOnBalancePercent: Double = 0.0,
    val feeOnDepositsPercent: Double = 0.0,
    val notes: String = ""
) {
    val profit: Money
        get() = currentValue - totalDeposits

    val profitPercent: Double
        get() = if (totalDeposits.isPositive()) {
            (profit.shekels / totalDeposits.shekels) * 100.0
        } else {
            0.0
        }

    val yearlyBalanceFeeEstimate: Money
        get() = currentValue * (feeOnBalancePercent / 100.0)
}
