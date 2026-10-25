package com.menash.myself.feature.calculators.ai.domain

data class AiSubscriptionInput(
    val name: String = "",
    val category: String = "",
    val isActive: Boolean = true,
    val billingCycle: String = "MONTHLY",
    val currency: String = "USD",
    val price: Double = 0.0
)

data class AiSubscriptionCostResult(
    val priceInIls: Double,
    val monthlyCostIls: Double,
    val yearlyCostIls: Double
)

data class AiSubscriptionSummaryResult(
    val totalMonthlyCostIls: Double,
    val totalYearlyCostIls: Double,
    val activeCount: Int,
    val categoryBreakdown: Map<String, Double>
)

object AiSubscriptionCalculator {

    /**
     * Calculates costs in ILS for a single AI subscription.
     * Locked by CharacterizationTests:
     * - aiSubscription_usdMonthly_locksCurrentOutput
     * - aiSubscription_usdYearly_locksCurrentOutput
     * - aiSubscription_ilsMonthlyAndYearly_locksCurrentOutput
     */
    fun calculateCost(subscription: AiSubscriptionInput, usdToIlsRate: Double): AiSubscriptionCostResult {
        val priceInIls = if (subscription.currency == "USD") {
            subscription.price * usdToIlsRate
        } else {
            subscription.price
        }

        val monthlyCostIls = if (subscription.billingCycle == "YEARLY") {
            priceInIls / 12.0
        } else {
            priceInIls
        }

        val yearlyCostIls = if (subscription.billingCycle == "YEARLY") {
            priceInIls
        } else {
            priceInIls * 12.0
        }

        return AiSubscriptionCostResult(
            priceInIls = priceInIls,
            monthlyCostIls = monthlyCostIls,
            yearlyCostIls = yearlyCostIls
        )
    }

    /**
     * Aggregates active subscriptions for monthly and yearly totals.
     * Inactive subscriptions are excluded from total costs.
     * Locked by: aiSubscription_activeAggregation_locksCurrentOutput
     */
    fun calculateSummary(subscriptions: List<AiSubscriptionInput>, usdToIlsRate: Double): AiSubscriptionSummaryResult {
        val activeSubs = subscriptions.filter { it.isActive }
        var totalMonthly = 0.0
        var totalYearly = 0.0
        val categoryMap = mutableMapOf<String, Double>()

        for (sub in activeSubs) {
            val cost = calculateCost(sub, usdToIlsRate)
            totalMonthly += cost.monthlyCostIls
            totalYearly += cost.yearlyCostIls
            val cat = sub.category.ifBlank { "כללי" }
            categoryMap[cat] = (categoryMap[cat] ?: 0.0) + cost.monthlyCostIls
        }

        return AiSubscriptionSummaryResult(
            totalMonthlyCostIls = totalMonthly,
            totalYearlyCostIls = totalYearly,
            activeCount = activeSubs.size,
            categoryBreakdown = categoryMap
        )
    }
}
