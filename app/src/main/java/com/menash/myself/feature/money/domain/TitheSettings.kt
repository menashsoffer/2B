package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth

/**
 * Where the household's maaser/chomesh count starts. [opening] is the balance on [startMonth]: positive is still
 * owed, negative is donated ahead. [portfolioAssetId] is the asset that holds the investment portfolio, whose gains
 * owe 20%; [depositsBefore] is what was deposited into it before [startMonth] (later deposits come from imports).
 */
data class TitheSettings(
    val startMonth: YearMonth,
    val opening: Money = Money.ZERO,
    val portfolioAssetId: String? = null,
    val depositsBefore: Money = Money.ZERO,
) {
    init { require(depositsBefore >= Money.ZERO) }

    companion object {
        /** One settings document per household. */
        const val ID = "settings"
    }
}
