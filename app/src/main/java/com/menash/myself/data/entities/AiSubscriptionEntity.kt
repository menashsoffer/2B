package com.menash.myself.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.menash.myself.feature.calculators.ai.domain.AiSubscriptionCalculator
import com.menash.myself.feature.calculators.ai.domain.AiSubscriptionInput

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
    private fun toInput() = AiSubscriptionInput(
        name = name,
        category = category,
        isActive = isActive,
        billingCycle = billingCycle,
        currency = currency,
        price = price
    )

    fun priceInIls(usdRate: Double): Double =
        AiSubscriptionCalculator.calculateCost(toInput(), usdRate).priceInIls

    fun monthlyCostIls(usdRate: Double): Double =
        AiSubscriptionCalculator.calculateCost(toInput(), usdRate).monthlyCostIls

    fun yearlyCostIls(usdRate: Double): Double =
        AiSubscriptionCalculator.calculateCost(toInput(), usdRate).yearlyCostIls
}
