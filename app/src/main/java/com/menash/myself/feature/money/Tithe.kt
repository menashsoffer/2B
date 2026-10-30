package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.ActualIncome
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.CharityBalance
import com.menash.myself.feature.money.domain.Donation
import com.menash.myself.feature.money.domain.Income
import com.menash.myself.feature.money.domain.TitheRate
import com.menash.myself.feature.money.domain.TitheSettings
import com.menash.myself.feature.money.domain.UnsubmittedDonations
import com.menash.myself.feature.money.domain.amountFor
import com.menash.myself.feature.money.domain.charityBalance
import com.menash.myself.feature.money.domain.sumMoney
import com.menash.myself.feature.money.domain.unsubmittedThisYear
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.ImportedIncome
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.importedIncome
import com.menash.myself.feature.money.importer.portfolioDeposits
import java.time.YearMonth

/** The portfolio's part: 20% of value minus deposits. Negative while the portfolio is at a loss. */
data class PortfolioPart(val value: Money, val deposits: Money, val owed: Money)

/** One maaser or chomesh source in one month. [actual] is false when no imported line was linked to it. */
data class TitheSource(val income: Income, val net: Money, val actual: Boolean)

data class TitheMonth(
    val month: YearMonth,
    val sources: List<TitheSource>,
    /** 10% or 20% of each source. */
    val owed: Money,
    val donated: Money,
    /** Imported income lines of the month not linked to a source yet. */
    val unlinkedLines: Int,
)

/**
 * The maaser and chomesh balance. [balance] is signed: positive is still owed, negative is donated ahead.
 * Each source counts what came in that month when an imported line is linked to it, else its planned net.
 */
data class TitheView(
    val settings: TitheSettings,
    val month: YearMonth,
    /** Newest month first. */
    val months: List<TitheMonth>,
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

private fun titheMonth(month: YearMonth, rated: List<Income>, imported: ImportedIncome, donations: List<Donation>): TitheMonth {
    val sources = rated.mapNotNull { income ->
        val actual = imported.actual.filter { it.incomeId == income.id && it.month == month }
        when {
            actual.isNotEmpty() -> TitheSource(income, sumMoney(actual.map { it.net }), actual = true)
            else -> amountFor(income, month)?.let { TitheSource(income, it, actual = false) }
        }
    }
    val owed = signed(charityBalance(rated, sources.map { ActualIncome(it.income.id, month, it.net) }, Money.ZERO, Money.ZERO, emptyList()))
    val given = sumMoney(donations.filter { YearMonth.from(it.date) == month }.map { it.amount })
    return TitheMonth(month, sources, owed, given, imported.unlinked[month] ?: 0)
}

/** Null until the household sets a start month. [importedDeposits] are portfolio deposits found in imports since then. */
fun titheView(
    data: MoneyData,
    month: YearMonth,
    imported: ImportedIncome = ImportedIncome(emptyList(), emptyMap()),
    importedDeposits: Money = Money.ZERO,
): TitheView? {
    val settings = data.tithe ?: return null
    val rated = data.items.incomes.filter { it.titheRate != TitheRate.NONE }
    val counted = data.donations.filter { !it.date.isBefore(settings.startMonth.atDay(1)) }.sortedByDescending { it.date }
    val months = generateSequence(settings.startMonth) { it.plusMonths(1) }.takeWhile { it <= month }
        .map { titheMonth(it, rated, imported, counted) }.toList().reversed()
    val fromIncome = sumMoney(months.map { it.owed })
    val portfolio = data.assets.firstOrNull { it.id == settings.portfolioAssetId && it.kind == AssetKind.ASSET }?.let { asset ->
        val deposits = settings.depositsBefore + importedDeposits
        PortfolioPart(asset.balance, deposits, signed(charityBalance(emptyList(), emptyList(), asset.balance, deposits, emptyList())))
    }
    val donated = sumMoney(counted.map { it.amount })
    return TitheView(
        settings = settings,
        month = month,
        months = months,
        fromIncome = fromIncome,
        portfolio = portfolio,
        donated = donated,
        balance = settings.opening + fromIncome + (portfolio?.owed ?: Money.ZERO) - donated,
        countedIncomes = rated.size,
        donations = counted,
        unsubmitted = unsubmittedThisYear(data.donations, month.year),
    )
}

/** The balance straight from the imported lines, as the maaser screen and the big picture both show it. */
fun titheFromImports(data: MoneyData, month: YearMonth, stored: List<StoredTransaction>, memory: ImportMemory): TitheView? {
    val start = data.tithe?.startMonth ?: return null
    val incomeIds = data.items.incomes.map { it.id }.toSet()
    return titheView(data, month, importedIncome(stored, memory, incomeIds), portfolioDeposits(stored, start, memory))
}
