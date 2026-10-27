package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.LocalDate

fun netWorth(assets: List<Asset>): Money {
    val positive = sumMoney(assets.filter { it.kind == AssetKind.ASSET }.map { it.balance })
    val debt = sumMoney(assets.filter { it.kind == AssetKind.LIABILITY }.map { it.balance })
    return Money(Math.subtractExact(positive.agorot, debt.agorot))
}

fun cushionMonths(assets: List<Asset>, averageMonthlyExpenses: Money?): Double? {
    if (averageMonthlyExpenses == null || averageMonthlyExpenses <= Money.ZERO) return null
    val liquid = sumMoney(assets.filter { it.kind == AssetKind.ASSET && it.liquid }.map { it.balance })
    return liquid.agorot.toDouble() / averageMonthlyExpenses.agorot
}

/** Monthly monetary saving, not a percentage; latest three distinct month closes. */
fun savingRate(snapshots: List<MonthSnapshot>): Money? {
    require(snapshots.map { it.month }.distinct().size == snapshots.size) { "One snapshot per month is required" }
    if (snapshots.isEmpty()) return null
    val latest = snapshots.sortedByDescending { it.month }.take(3)
    return sumMoney(latest.map { Money(Math.subtractExact(it.actualIncomes.agorot, it.actualExpenses.agorot)) }) / latest.size
}

/** Exactly three calendar months is fresh; only dates after that boundary are stale. */
fun isStale(asset: Asset, today: LocalDate): Boolean = today > asset.lastUpdated.plusMonths(3)
