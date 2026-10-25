package com.menash.myself.core.domain.model

data class AiSubscription(
    val id: Long = 0,
    val name: String,
    val category: String,
    val isActive: Boolean = true,
    val billingCycle: String = "MONTHLY", // "MONTHLY" or "YEARLY"
    val currency: String = "USD", // "USD" or "ILS"
    val price: Money = Money.ZERO,
    val notes: String = ""
) {
    fun priceInIls(usdRate: Double): Money {
        return if (currency.equals("USD", ignoreCase = true)) {
            price * usdRate
        } else {
            price
        }
    }

    fun monthlyCostIls(usdRate: Double): Money {
        val inIls = priceInIls(usdRate)
        return if (billingCycle.equals("YEARLY", ignoreCase = true)) {
            inIls / 12
        } else {
            inIls
        }
    }

    fun yearlyCostIls(usdRate: Double): Money {
        val inIls = priceInIls(usdRate)
        return if (billingCycle.equals("MONTHLY", ignoreCase = true)) {
            inIls * 12L
        } else {
            inIls
        }
    }
}
