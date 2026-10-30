package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money

/** New wins its month; the last existing entry wins every other duplicated month. */
fun upsert(closed: List<ClosedMonth>, new: ClosedMonth): List<ClosedMonth> =
    (closed.associateBy { it.month } + (new.month to new)).values.sortedBy { it.month }

/** Latest three closed months, irrespective of calendar gaps; rounds once to agorot, HALF_UP. */
fun averageMonthlyExpenses(closed: List<ClosedMonth>): Money? {
    require(closed.map { it.month }.distinct().size == closed.size) { "One close per month is required" }
    if (closed.isEmpty()) return null
    val latest = closed.sortedByDescending { it.month }.take(3)
    return sumMoney(latest.map { it.snapshot.actualExpenses }) / latest.size
}

fun monthlySaving(closed: List<ClosedMonth>): Money? = savingRate(closed.map { it.snapshot })
