package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.ActualIncome
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.CharityBalance
import com.menash.myself.feature.money.domain.Donation
import com.menash.myself.feature.money.domain.TitheRate
import com.menash.myself.feature.money.domain.TitheSettings
import com.menash.myself.feature.money.domain.UnsubmittedDonations
import com.menash.myself.feature.money.domain.amountFor
import com.menash.myself.feature.money.domain.charityBalance
import com.menash.myself.feature.money.domain.sumMoney
import com.menash.myself.feature.money.domain.unsubmittedThisYear
import java.time.YearMonth

/** The portfolio's part: 20% of value minus deposits. Negative while the portfolio is at a loss. */
data class PortfolioPart(val value: Money, val deposits: Money, val owed: Money)

/**
 * The maaser and chomesh balance. [balance] is signed: positive is still owed, negative is donated ahead.
 * Income counts by its planned net amount in each month from the start month to [TitheView.month].
 */
data class TitheView(
    val settings: TitheSettings,
    val month: YearMonth,
    val fromIncome: Money,
    val portfolio: PortfolioPart?,
    val donated: Money,
    val balance: Money,
    /** Income sources marked maaser or chomesh; zero means nothing is counted yet. */
    val countedIncomes: Int,
    /** Newest first; only donations from the start month on count. */
    val donations: List<Donation>,
    val unsubmitted: UnsubmittedDonations,
)

private fun signed(balance: CharityBalance): Money = when (balance) {
    is CharityBalance.Owed -> balance.amount
    is CharityBalance.Credit -> Money(-balance.amount.agorot)
    CharityBalance.Settled -> Money.ZERO
}

/** The planned net of every maaser or chomesh income, month by month from [from] to [to]. */
fun plannedTitheIncome(data: MoneyData, from: YearMonth, to: YearMonth): List<ActualIncome> {
    val rated = data.items.incomes.filter { it.titheRate != TitheRate.NONE }
    return generateSequence(from) { it.plusMonths(1) }.takeWhile { it <= to }.flatMap { month ->
        rated.mapNotNull { income -> amountFor(income, month)?.let { ActualIncome(income.id, month, it) } }
    }.toList()
}

/** Null until the household sets a start month. [importedDeposits] are portfolio deposits found in imports since then. */
fun titheView(data: MoneyData, month: YearMonth, importedDeposits: Money = Money.ZERO): TitheView? {
    val settings = data.tithe ?: return null
    val incomes = data.items.incomes
    val fromIncome = signed(charityBalance(incomes, plannedTitheIncome(data, settings.startMonth, month), Money.ZERO, Money.ZERO, emptyList()))
    val portfolio = data.assets.firstOrNull { it.id == settings.portfolioAssetId && it.kind == AssetKind.ASSET }?.let { asset ->
        val deposits = settings.depositsBefore + importedDeposits
        PortfolioPart(asset.balance, deposits, signed(charityBalance(emptyList(), emptyList(), asset.balance, deposits, emptyList())))
    }
    val counted = data.donations.filter { !it.date.isBefore(settings.startMonth.atDay(1)) }.sortedByDescending { it.date }
    val donated = sumMoney(counted.map { it.amount })
    return TitheView(
        settings = settings,
        month = month,
        fromIncome = fromIncome,
        portfolio = portfolio,
        donated = donated,
        balance = settings.opening + fromIncome + (portfolio?.owed ?: Money.ZERO) - donated,
        countedIncomes = incomes.count { it.titheRate != TitheRate.NONE },
        donations = counted,
        unsubmitted = unsubmittedThisYear(data.donations, month.year),
    )
}
