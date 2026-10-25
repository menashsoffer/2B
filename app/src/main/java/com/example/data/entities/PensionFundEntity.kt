package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val profit: Double
        get() = currentValue - totalDeposits

    val profitPercent: Double
        get() = if (totalDeposits > 0.0) ((currentValue - totalDeposits) / totalDeposits) * 100.0 else 0.0

    val yearlyBalanceFeeEstimate: Double
        get() = currentValue * (feeOnBalancePercent / 100.0)
}
