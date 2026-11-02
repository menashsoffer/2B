package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.BalancesAtClose

fun parseCloseBalance(text: String): Money? = runCatching {
    Money(text.trim().toBigDecimal().movePointRight(2).longValueExact()).also { require(it >= Money.ZERO) }
}.getOrNull()

data class CloseBalanceDraft(val entering: Boolean = false, val values: Map<String, String> = emptyMap()) {
    fun valid(assets: List<Asset>): Boolean = !entering || assets.all {
        values[it.id].isNullOrBlank() || parseCloseBalance(values.getValue(it.id)) != null
    }
    fun balances(assets: List<Asset>): BalancesAtClose {
        if (!entering) return BalancesAtClose.Unknown
        fun amounts(kind: AssetKind) = assets.filter { it.kind == kind }.mapNotNull { asset ->
            values[asset.id]?.let(::parseCloseBalance)?.let { asset.id to it }
        }.toMap()
        return BalancesAtClose.Entered(amounts(AssetKind.ASSET), amounts(AssetKind.LIABILITY))
    }
}
