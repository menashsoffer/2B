package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.CloseCheckKind
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.monthLines
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * One step of "update the month". Steps are derived from the household's data on every change and never
 * stored, so they are shared: when one member finishes a step, it is gone for both.
 */
sealed interface MonthTask {
    /** Imported lines of [month] still without a category. */
    data class Assign(val month: YearMonth, val count: Int) : MonthTask

    /** A bank account or card that had lines in recent months has none for [month] yet. */
    data class MissingFile(val month: YearMonth, val source: StatementSource) : MonthTask

    /** A balance too old to freeze with [month]. */
    data class UpdateBalance(val month: YearMonth, val asset: Asset) : MonthTask

    /** Every blocking check of [month] passes: it can be closed. */
    data class Close(val month: YearMonth) : MonthTask

    /** Maaser owed on actual income. */
    data class Tithe(val owed: Money) : MonthTask
}

/**
 * The steps, in the order they should be done. The oldest month awaiting close comes first, with its
 * blocking checks in the close screen's order and then the close itself; later months wait until it is
 * closed, as the close screen does. Then lines of the current month without a category, then maaser owed.
 * Pending lines only warn at close, so they are not a step.
 */
fun monthTasks(
    data: MoneyData,
    today: LocalDate,
    usdToIls: BigDecimal?,
    stored: List<StoredTransaction>,
    memory: ImportMemory,
    sourceClosures: Map<StatementSource, YearMonth> = emptyMap(),
    titheOwed: Money? = null,
): List<MonthTask> = buildList {
    monthsAwaitingClose(today, stored, data.closedMonths).firstOrNull()?.let { month ->
        val view = closeView(data, month, today, usdToIls, stored, memory, sourceClosures = sourceClosures)
        view.missingSources.forEach { add(MonthTask.MissingFile(month, it)) }
        view.checks.filter { it.kind == CloseCheckKind.UNASSIGNED && it.blocking }
            .forEach { check -> add(MonthTask.Assign(month, check.detail.toIntOrNull() ?: 0)) }
        view.staleAssets.forEach { add(MonthTask.UpdateBalance(month, it)) }
        if (view.canClose) add(MonthTask.Close(month))
    }
    val current = YearMonth.from(today)
    val unassigned = monthLines(stored, current, memory, data.categories).unassignedCount
    if (unassigned > 0) add(MonthTask.Assign(current, unassigned))
    titheOwed?.takeIf { it.isPositive() }?.let { add(MonthTask.Tithe(it)) }
}
