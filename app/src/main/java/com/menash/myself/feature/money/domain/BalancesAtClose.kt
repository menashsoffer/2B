package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money

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

fun ClosedMonth.hasSuspectBalances(): Boolean =
    closedOn > month.plusMonths(1).atEndOfMonth() && (balances.isNotEmpty() || liabilities.isNotEmpty())
