package com.menash.myself.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.menash.myself.feature.calculators.pension.domain.PensionFundInput
import com.menash.myself.feature.calculators.pension.domain.PensionSavingsCalculator

@Entity(tableName = "pension_funds")
data class PensionFundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fundType: String, // קרן פנסיה, קרן השתלמות, קופת גמל, גמל להשקעה
    val provider: String, // מנורה מבטחים, אלטשולר שחם, הראל, מיטב, וכו'
    val currentValue: Double = 0.0,
    val totalDeposits: Double = 0.0,
    val feeOnBalancePercent: Double = 0.0, // דמי ניהול מצבירה באחוזים (לדוגמה 0.15)
    val feeOnDepositsPercent: Double = 0.0, // דמי ניהול מהפקדה באחוזים (לדוגמה 1.49)
    val notes: String = ""
) {
    private fun fundResult() = PensionSavingsCalculator.calculateFund(
        PensionFundInput(
            fundType = fundType,
            provider = provider,
            currentValue = currentValue,
            totalDeposits = totalDeposits,
            feeOnBalancePercent = feeOnBalancePercent,
            feeOnDepositsPercent = feeOnDepositsPercent
        )
    )

    val profit: Double
        get() = fundResult().profit

    val profitPercent: Double
        get() = fundResult().profitPercent

    val yearlyBalanceFeeEstimate: Double
        get() = fundResult().yearlyBalanceFeeEstimate
}
