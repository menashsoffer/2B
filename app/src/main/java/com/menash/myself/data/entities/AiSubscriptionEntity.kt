package com.menash.myself.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_subscriptions")
data class AiSubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val isActive: Boolean = true,
    val billingCycle: String = "MONTHLY", // "MONTHLY" or "YEARLY"
    val currency: String = "USD", // "USD" or "ILS"
    val price: Double = 0.0,
    val notes: String = ""
) {
    fun priceInIls(usdRate: Double): Double {
        return if (currency.equals("USD", ignoreCase = true)) price * usdRate else price
    }

    fun monthlyCostIls(usdRate: Double): Double {
        val inIls = priceInIls(usdRate)
        return if (billingCycle.equals("YEARLY", ignoreCase = true)) inIls / 12.0 else inIls
    }

    fun yearlyCostIls(usdRate: Double): Double {
        val inIls = priceInIls(usdRate)
        return if (billingCycle.equals("MONTHLY", ignoreCase = true)) inIls * 12.0 else inIls
    }
}
