package com.menash.myself.feature.money.data

import com.menash.myself.feature.money.CarCompareDraft
import com.menash.myself.feature.money.CarPlan
import com.menash.myself.feature.money.domain.Included

/** car/settings: the car categories and the last comparison as typed (text, so a half-filled form survives). */
internal object CarDocuments {
    private val horizons = setOf(3, 5, 7)

    fun encode(plan: CarPlan): Map<String, Any> = mapOf(
        "categoryIds" to plan.categoryIds.sorted(),
        "horizonYears" to plan.compare.horizonYears.toLong(),
        "included" to plan.compare.leaseIncluded.map { it.name }.sorted(),
        "fields" to fields(plan.compare),
    )

    private fun fields(d: CarCompareDraft): Map<String, String> = mapOf(
        "kmPerYear" to d.kmPerYear, "price" to d.price, "tradeIn" to d.tradeIn, "resale" to d.resale,
        "lostReturn" to d.lostReturn, "loanDown" to d.loanDown, "loanRate" to d.loanRate, "loanMonths" to d.loanMonths,
        "dealerList" to d.dealerList, "dealerRate" to d.dealerRate, "dealerMonths" to d.dealerMonths,
        "dealerBalloon" to d.dealerBalloon, "leaseMonthly" to d.leaseMonthly, "leaseUpfront" to d.leaseUpfront,
        "leaseMonths" to d.leaseMonths, "leaseKmLimit" to d.leaseKmLimit, "leaseExtraKm" to d.leaseExtraKm,
    ).filterValues { it.isNotBlank() }

    fun plan(id: String, data: Map<String, Any?>): CarPlan {
        require(id == CarPlan.ID) { "Unexpected car settings id '$id'" }
        val doc = MoneyDoc(data)
        val f = (data["fields"] as? Map<*, *>).orEmpty().entries
            .mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? String)?.let { key to it } } }.toMap()
        fun t(key: String) = f[key].orEmpty()
        val categories = (data["categoryIds"] as? List<*>)?.filterIsInstance<String>()?.toSet()
        val included = (data["included"] as? List<*>)?.filterIsInstance<String>()
            ?.mapNotNull { name -> Included.entries.firstOrNull { it.name == name } }?.toSet()
        return CarPlan(
            categoryIds = categories ?: CarPlan().categoryIds,
            compare = CarCompareDraft(
                horizonYears = doc.longOrNull("horizonYears")?.toInt()?.takeIf { it in horizons } ?: 5,
                kmPerYear = t("kmPerYear"), price = t("price"), tradeIn = t("tradeIn"), resale = t("resale"),
                lostReturn = t("lostReturn"), loanDown = t("loanDown"), loanRate = t("loanRate"), loanMonths = t("loanMonths"),
                dealerList = t("dealerList"), dealerRate = t("dealerRate"), dealerMonths = t("dealerMonths"),
                dealerBalloon = t("dealerBalloon"), leaseMonthly = t("leaseMonthly"), leaseUpfront = t("leaseUpfront"),
                leaseMonths = t("leaseMonths"), leaseKmLimit = t("leaseKmLimit"), leaseExtraKm = t("leaseExtraKm"),
                leaseIncluded = included ?: Included.entries.toSet(),
            ),
        )
    }
}
