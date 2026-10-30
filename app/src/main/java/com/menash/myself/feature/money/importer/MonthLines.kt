package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.sumMoney
import java.time.YearMonth

/**
 * One imported line of a month as the transactions screen shows it. [counted] and [categoryId] come from
 * the same toTransactions the month's "בפועל" uses, so the list and the month never disagree.
 */
data class MonthLine(
    val stored: StoredTransaction,
    val kind: RowKind,
    /** Counted as spending this month. */
    val counted: Boolean,
    /** Counted as income this month. */
    val income: Boolean,
    /** The user's choice, else what the merchant memory suggests. Null when nothing is known. */
    val categoryId: String?,
    /** In "לא משויך": counted spending without a known category. */
    val unassigned: Boolean,
    /** Income lines only: the income source it is linked to, for maaser. */
    val incomeId: String? = null,
) {
    val id: String get() = stored.id
    val row: StatementRow get() = stored.row
    val pending: Boolean get() = row.status == RowStatus.PENDING
    /** Only counted spending gets a category; income, trading and own transfers stay out of the budget. */
    val canCategorise: Boolean get() = counted
    /** A pending card line that was cancelled stays stored and uncounted until the user removes it. */
    val removable: Boolean get() = pending
}

enum class LineFilter { ALL, UNASSIGNED, PENDING }

data class MonthLines(val month: YearMonth, val lines: List<MonthLine>) {
    val spent: Money get() = sumMoney(lines.filter { it.counted }.mapNotNull { it.row.charge })
    val income: Money get() = Money(-lines.filter { it.income }.sumOf { it.row.charge!!.agorot })
    val unassignedCount: Int get() = lines.count { it.unassigned }
    val pendingCount: Int get() = lines.count { it.pending }

    fun filtered(filter: LineFilter, categoryId: String? = null): List<MonthLine> = lines.filter { line ->
        when (filter) {
            LineFilter.ALL -> categoryId == null || line.categoryId == categoryId
            LineFilter.UNASSIGNED -> line.unassigned
            LineFilter.PENDING -> line.pending
        }
    }
}

/** Every stored line billed in [month], newest first. */
fun monthLines(
    stored: List<StoredTransaction>,
    month: YearMonth,
    memory: ImportMemory,
    categories: List<Category>,
    incomeIds: Set<String> = emptySet(),
): MonthLines {
    val known = categories.map { it.id }.toSet()
    val counted = toTransactions(stored, memory).associateBy { it.id }
    val lines = stored.filter { it.row.billingMonth == month }.map { s ->
        val kind = classify(s.row, memory)
        val tx = counted[s.id]
        val charge = s.row.charge
        val income = tx == null && s.row.status == RowStatus.COUNTED && charge != null && charge.agorot < 0 &&
            kind in setOf(RowKind.INCOME, RowKind.TRANSFER)
        val category = tx?.categoryId ?: s.categoryId
        val source = if (income) incomeSourceOf(s, memory, incomeIds) else null
        MonthLine(s, kind, tx != null, income, category, tx != null && (category == null || category !in known), source)
    }
    return MonthLines(month, lines.sortedWith(compareByDescending<MonthLine> { it.row.date }.thenBy { it.row.description }))
}

/** What the category list needs to ask about one line; the hint and suggestion work as in the import. */
fun choiceFor(line: MonthLine, categories: List<Category>): MerchantChoice = MerchantChoice(
    key = ImportIds.merchantKey(line.row.description),
    name = line.row.description,
    total = line.row.charge ?: Money.ZERO,
    count = 1,
    hint = line.row.hint,
    suggested = line.categoryId?.takeIf { id -> categories.any { it.id == id } }
        ?: line.row.hint?.let { categoryForHint(it, categories) },
    transfer = line.kind == RowKind.TRANSFER,
)

/** Card bills and cash are one-off lines: their category is never taught to the merchant memory. */
fun teachesMemory(line: MonthLine): Boolean = line.kind !in setOf(RowKind.CARD_TOTAL, RowKind.CASH_WITHDRAWAL)
