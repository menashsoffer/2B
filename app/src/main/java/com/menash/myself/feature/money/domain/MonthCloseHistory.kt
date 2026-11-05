package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth

/** New wins its month; the last existing entry wins every other duplicated month. */
fun upsert(closed: List<ClosedMonth>, new: ClosedMonth): List<ClosedMonth> =
    (closed.associateBy { it.month } + (new.month to new)).values.sortedBy { it.month }

/** Selected closed months, irrespective of calendar gaps; rounds once to agorot, HALF_UP. */
fun averageMonthlyExpenses(closed: List<ClosedMonth>, averagesFrom: YearMonth? = null): Money? {
    require(closed.map { it.month }.distinct().size == closed.size) { "One close per month is required" }
    val ordered = closed.sortedByDescending { it.month }
    val latest = if (averagesFrom == null) ordered.take(12) else ordered.filter { it.month >= averagesFrom }
    if (latest.isEmpty()) return null
    return sumMoney(latest.map { it.snapshot.actualExpenses }) / latest.size
}

fun monthlySaving(closed: List<ClosedMonth>, averagesFrom: YearMonth? = null): Money? = savingRate(closed.map { it.snapshot }, averagesFrom)
