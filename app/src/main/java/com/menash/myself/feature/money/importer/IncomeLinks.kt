package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.ActualIncome
import java.time.YearMonth

/** Money in that is not the household's own: the same lines [actualIncome] sums. */
fun isIncomeLine(stored: StoredTransaction, memory: ImportMemory): Boolean {
    val charge = stored.row.charge ?: return false
    return stored.row.status == RowStatus.COUNTED && charge.agorot < 0 &&
        classify(stored.row, memory) in setOf(RowKind.INCOME, RowKind.TRANSFER)
}

/**
 * The income source an income line belongs to: the user's choice for this line, else the one remembered for
 * the same payer. Null when neither names one of [incomeIds].
 */
fun incomeSourceOf(stored: StoredTransaction, memory: ImportMemory, incomeIds: Set<String>): String? =
    (stored.categoryId ?: memory.merchants.categories[ImportIds.merchantKey(stored.row.description)])?.takeIf { it in incomeIds }

/** What the imports say about income: per source and month, and how many income lines are not linked yet. */
data class ImportedIncome(val actual: List<ActualIncome>, val unlinked: Map<YearMonth, Int>)

fun importedIncome(rows: List<StoredTransaction>, memory: ImportMemory, incomeIds: Set<String>): ImportedIncome {
    val lines = rows.filter { isIncomeLine(it, memory) }
    val (linked, unlinked) = lines.partition { incomeSourceOf(it, memory, incomeIds) != null }
    val actual = linked.groupBy { incomeSourceOf(it, memory, incomeIds)!! to it.row.billingMonth }
        .map { (key, group) -> ActualIncome(key.first, key.second, Money(-group.sumOf { it.row.charge!!.agorot })) }
        .sortedWith(compareBy({ it.month }, { it.incomeId }))
    return ImportedIncome(actual, unlinked.groupingBy { it.row.billingMonth }.eachCount())
}
