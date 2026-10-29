package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.ImportCheck
import java.time.YearMonth

/**
 * What one import (one or more files, shared together) will write. Nothing is written before the user
 * confirms, and a re-import only ever adds missing lines or refreshes the bank's own fields.
 */
data class ImportPlan(
    /** New lines and refreshed lines (pending → charged, FX amount arrived). Categories already chosen are kept. */
    val toWrite: List<StoredTransaction>,
    /** Pending lines replaced by a new version of the same purchase (amount changed when it was charged). */
    val toDelete: List<String>,
    val newCount: Int,
    val updatedCount: Int,
    val alreadyImported: Int,
    /** The same line in two files of this batch, kept once. */
    val duplicatesInBatch: Int,
    /** New or refreshed lines that are still pending or waiting for a rate; never counted. */
    val waiting: Int,
    val checks: List<FileCheck>,
)

data class FileCheck(val source: Source, val rows: Int, val total: ImportCheck?, val warnings: List<ImportWarning>)

private fun StatementRow.preferable(other: StatementRow): Boolean =
    status == RowStatus.COUNTED && other.status != RowStatus.COUNTED

/**
 * [existing] is every stored line of the household by id. Lines of the batch are first deduplicated
 * by id (a charged version beats a pending one), then compared with what is stored:
 * - unknown id → new;
 * - same id, identical bank fields → already imported, nothing written;
 * - same id, different bank fields → refreshed, the stored categoryId kept.
 * A stored line that is not counted yet (pending / waiting) and is absent from the batch is deleted only
 * when the batch holds another line of the same card, date and merchant: the purchase was re-issued with
 * its final amount. A pending line with no successor stays, uncounted, so nothing disappears silently.
 */
fun planImport(existing: Map<String, StoredTransaction>, files: List<ParsedStatement>): ImportPlan {
    val batch = linkedMapOf<String, StatementRow>()
    var duplicates = 0
    files.flatMap { it.rows }.forEach { row ->
        val kept = batch[row.id]
        if (kept == null) batch[row.id] = row
        else {
            duplicates++
            if (row.preferable(kept)) batch[row.id] = row
        }
    }
    var new = 0
    var updated = 0
    var already = 0

    val toWrite = mutableListOf<StoredTransaction>()
    batch.values.forEach { row ->
        val stored = existing[row.id]
        when {
            stored == null -> { new++; toWrite += StoredTransaction(row) }
            stored.row == row -> already++
            // A stored charged line is never turned back into pending by an older file.
            stored.row.preferable(row) -> already++
            else -> { updated++; toWrite += stored.copy(row = row) }
        }
    }
    val waiting = toWrite.count { it.row.status != RowStatus.COUNTED }
    fun samePurchase(row: StatementRow) = Triple(row.sourceId, row.date, ImportIds.merchantKey(row.description))
    val successors = batch.values.map(::samePurchase).toSet()
    val toDelete = existing.values
        .filter { it.row.status != RowStatus.COUNTED && it.id !in batch && samePurchase(it.row) in successors }
        .map { it.id }
    val checks = files.map { FileCheck(it.source, it.rows.size, totalCheck(it), it.warnings) }
    return ImportPlan(toWrite, toDelete, new, updated, already, duplicates, waiting, checks)
}

/** The bank's card bill for [month] next to the sum of that card's imported purchases. */
data class CardReconciliation(val issuer: Issuer, val month: YearMonth, val bankTotal: Money, val purchasesTotal: Money?) {
    /** bank − purchases; null when that card's file for the month was not imported yet. */
    val gap: Money? get() = purchasesTotal?.let { Money(bankTotal.agorot - it.agorot) }
}

fun reconcileCards(rows: List<StoredTransaction>, memory: ImportMemory = ImportMemory()): List<CardReconciliation> {
    val bills = rows.map { it.row }
        .filter { it.status == RowStatus.COUNTED && it.charge != null && classify(it, memory) == RowKind.CARD_TOTAL }
        .groupBy { issuerOf(it.description)!! to it.billingMonth }
    val purchases = rows.map { it.row }
        .filter { it.status == RowStatus.COUNTED && it.charge != null && issuerOf(it.source) != null }
        .groupBy { issuerOf(it.source)!! to it.billingMonth }
    return bills.map { (key, lines) ->
        CardReconciliation(
            key.first, key.second, Money(lines.sumOf { it.charge!!.agorot }),
            purchases[key]?.let { list -> Money(list.sumOf { it.charge!!.agorot }) },
        )
    }.sortedWith(compareBy({ it.month }, { it.issuer }))
}
