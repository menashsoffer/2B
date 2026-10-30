package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.CloseCheck
import com.menash.myself.feature.money.domain.CloseCheckKind
import com.menash.myself.feature.money.domain.CloseInput
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.averageMonthlyExpenses
import com.menash.myself.feature.money.domain.canClose
import com.menash.myself.feature.money.domain.closeChecks
import com.menash.myself.feature.money.domain.closeMonth
import com.menash.myself.feature.money.domain.monthlySaving
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.actualIncome
import com.menash.myself.feature.money.importer.monthLines
import com.menash.myself.feature.money.importer.toTransactions
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** One bank account or card, as the close checklist names it: the institution and the last four digits. */
data class StatementSource(val source: Source, val sourceId: String) {
    val key: String get() = "${source.key}:$sourceId"
}

private fun StatementRow.statementSource() = StatementSource(source, sourceId)

/** The month the close screen opens on: last month while it has lines and is not closed, else this month. */
fun closeTarget(today: LocalDate, stored: List<StoredTransaction>, closed: List<ClosedMonth>): YearMonth {
    val current = YearMonth.from(today)
    val previous = current.minusMonths(1)
    val pending = closed.none { it.month == previous } && stored.any { it.row.billingMonth == previous }
    return if (pending) previous else current
}

/**
 * The close screen for [month]. [checks] come from the engine's closeChecks; [preview] is what closing now
 * would store, so the screen shows the same numbers it saves. [existing] is the stored close, if any.
 */
data class CloseView(
    val month: YearMonth,
    val checks: List<CloseCheck>,
    val missingSources: List<StatementSource>,
    val staleAssets: List<Asset>,
    val preview: ClosedMonth,
    val existing: ClosedMonth?,
) {
    val canClose: Boolean get() = canClose(checks)
    val saving: Money get() = preview.snapshot.actualIncomes - preview.snapshot.actualExpenses
}

/**
 * "Expected" sources are those with a line in the three months before [month]. A source the user said has no
 * lines this month ([noLines], by key) stops being expected. Pending lines warn; they never block.
 */
fun closeView(
    data: MoneyData,
    month: YearMonth,
    today: LocalDate,
    usdToIls: BigDecimal?,
    stored: List<StoredTransaction>,
    memory: ImportMemory,
    noLines: Set<String> = emptySet(),
): CloseView {
    val lines = monthLines(stored, month, memory, data.categories)
    val thisMonth = stored.filter { it.row.billingMonth == month }.map { it.row.statementSource() }.toSet()
    val window = month.minusMonths(3)..month.minusMonths(1)
    val expected = stored.filter { it.row.billingMonth in window }.map { it.row.statementSource() }
        .filter { it.key !in noLines }.toSet()
    val checks = closeChecks(
        CloseInput(
            month = month,
            today = today,
            sourcesThisMonth = thisMonth.map { it.key }.toSet(),
            sourcesExpected = expected.map { it.key }.toSet(),
            unassignedCount = lines.unassignedCount,
            pendingCount = lines.pendingCount,
            assets = data.assets,
        )
    )
    val byKey = (thisMonth + expected).associateBy { it.key }
    val assets = data.assets.associateBy { it.id }
    return CloseView(
        month = month,
        checks = checks,
        missingSources = checks.filter { it.kind == CloseCheckKind.MISSING_SOURCE }.mapNotNull { byKey[it.detail] },
        staleAssets = checks.filter { it.kind == CloseCheckKind.STALE_BALANCE }.mapNotNull { assets[it.detail] },
        preview = closeMonth(
            month, data.items, toTransactions(stored, memory), actualIncome(stored, month, memory), data.assets, usdToIls, today,
        ),
        existing = data.closedMonths.firstOrNull { it.month == month },
    )
}

/** What the big picture and "האם אפשר?" take from closed months; both null until a month is closed. */
data class ClosedFigures(val monthlySaving: Money?, val averageExpenses: Money?)

fun closedFigures(closed: List<ClosedMonth>): ClosedFigures =
    ClosedFigures(monthlySaving(closed), averageMonthlyExpenses(closed))
