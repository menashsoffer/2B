package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth

data class NetWorthChange(val start: Money, val end: Money, val change: Money, val startFrom: YearMonth)

fun periodicAtYearEnd(closed: List<ClosedMonth>, year: Int): Map<String, PeriodicStatus> =
    yearCloses(closed, year).lastOrNull()?.periodic ?: emptyMap()

fun netWorthChange(closed: List<ClosedMonth>, year: Int): NetWorthChange? {
    val rows = yearCloses(closed, year).filter { it.balances.isNotEmpty() || it.liabilities.isNotEmpty() }
    if (rows.isEmpty()) return null
    val previous = yearCloses(closed, Math.subtractExact(year, 1))
        .lastOrNull { it.balances.isNotEmpty() || it.liabilities.isNotEmpty() }
    val first = previous ?: rows.first()
    fun worth(row: ClosedMonth) = subtractMoney(sumMoney(row.balances.values), sumMoney(row.liabilities.values))
    val start = worth(first)
    val end = worth(rows.last())
    return NetWorthChange(start, end, subtractMoney(end, start), first.month)
}
