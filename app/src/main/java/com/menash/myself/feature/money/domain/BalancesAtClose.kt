package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.LocalDate
import java.time.YearMonth

sealed interface BalancesAtClose {
    data class Current(val assets: List<Asset>) : BalancesAtClose
    data class Entered(val balances: Map<String, Money>, val liabilities: Map<String, Money>) : BalancesAtClose
    data object Unknown : BalancesAtClose

    fun maps(): Pair<Map<String, Money>, Map<String, Money>> = when (this) {
        is Current -> {
            require(assets.map { it.id }.distinct().size == assets.size)
            assets.filter { it.kind == AssetKind.ASSET }.associate { it.id to it.balance } to
                assets.filter { it.kind == AssetKind.LIABILITY }.associate { it.id to it.balance }
        }
        is Entered -> balances.toMap() to liabilities.toMap()
        Unknown -> emptyMap<String, Money>() to emptyMap()
    }
}

/**
 * Closing [month] on [day] needs balances typed for that month only when the following month is already over.
 * Closing October during November (the usual close) keeps today's balances: they are close to October's end.
 */
fun needsHistoricalBalances(month: YearMonth, day: LocalDate): Boolean = day > month.plusMonths(1).atEndOfMonth()

/** Today's balances were frozen into a month that ended long before the close, and nobody typed them. */
fun ClosedMonth.hasSuspectBalances(): Boolean =
    !balancesEntered && needsHistoricalBalances(month, closedOn) && (balances.isNotEmpty() || liabilities.isNotEmpty())
