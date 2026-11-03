package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.ImportCheck
import com.menash.myself.feature.money.domain.suggestCategory
import java.time.LocalDate

/** One file as the user picked or shared it. The name is shown only; it never decides anything. */
class ImportFile(val name: String, val bytes: ByteArray, val readError: Boolean = false)

/** What the summary shows for one file. A refused file has [problem] and nothing else. */
data class FileSummary(
    val name: String,
    val source: Source? = null,
    val problem: FileProblem? = null,
    val rows: Int = 0,
    val firstDate: LocalDate? = null,
    val lastDate: LocalDate? = null,
    /** Max and Cal only; a Mismatch is shown with its gap and never fixed. */
    val total: ImportCheck? = null,
    val skippedRows: List<Int> = emptyList(),
    /** Bank files: the row where the running balance stops adding up, if any. */
    val balanceBreakRow: Int? = null,
    val newCount: Int = 0,
    val existingCount: Int = 0,
    val readError: Boolean = false,
)

/** A merchant seen for the first time; the user gives it a category once and the memory learns it. */
data class MerchantChoice(
    val key: String,
    val name: String,
    val total: Money,
    val count: Int,
    /** Max's own category name, shown as a hint. */
    val hint: String?,
    /** Category offered first: the one whose name matches the hint. */
    val suggested: String?,
    /** A transfer can instead be marked as the household's own account. */
    val transfer: Boolean,
)

sealed interface MerchantDecision {
    data class Categorise(val categoryId: String) : MerchantDecision
    object OwnTransfer : MerchantDecision
    object Later : MerchantDecision
}

data class ImportReview(val files: List<FileSummary>, val plan: ImportPlan, val merchants: List<MerchantChoice>) {
    val nothingToSave: Boolean get() = plan.toWrite.isEmpty() && plan.toDelete.isEmpty()
}

data class MemoryUpdate(val merchantKey: String, val categoryId: String?, val ownTransfer: Boolean)

/** Reads every file; a refused file stays in the summary with its reason and adds nothing. */
fun reviewImport(
    files: List<ImportFile>,
    existing: Map<String, StoredTransaction>,
    memory: ImportMemory,
    categories: List<Category>,
): ImportReview {
    val read = files.map { file ->
        try {
            file.name to if (file.readError) null else parseStatement(file.bytes)
        } catch (e: ImportException) {
            file.name to e.problem
        }
    }
    val statements = read.mapNotNull { it.second as? ParsedStatement }
    val plan = planImport(existing, statements)
    val summaries = read.map { (name, result) ->
        when (result) {
            is ParsedStatement -> FileSummary(
                name, result.source, null, result.rows.size, result.firstDate, result.lastDate, totalCheck(result),
                result.warnings.filterIsInstance<ImportWarning.SkippedRow>().map { it.rowNumber },
                result.warnings.filterIsInstance<ImportWarning.BalanceBreak>().firstOrNull()?.rowNumber,
                newCount = planImport(existing, listOf(result)).newCount,
                existingCount = result.rows.count { it.id in existing },
            )
            null -> FileSummary(name, readError = true)
            else -> FileSummary(name, problem = result as FileProblem)
        }
    }
    return ImportReview(summaries, plan, newMerchants(plan, existing, memory, categories))
}

private val askedKinds = setOf(RowKind.EXPENSE, RowKind.STANDING_ORDER, RowKind.TRANSFER)

/** Only lines new to the household, of a kind that needs a category, whose merchant the memory does not know. */
internal fun newMerchants(
    plan: ImportPlan,
    existing: Map<String, StoredTransaction>,
    memory: ImportMemory,
    categories: List<Category>,
): List<MerchantChoice> = plan.toWrite
    .filter { it.id !in existing && it.categoryId == null }
    .map { it.row }
    .filter { row ->
        val kind = classify(row, memory)
        kind in askedKinds && (kind != RowKind.TRANSFER || (row.charge ?: Money.ZERO).isPositive()) &&
            suggestCategory(row.description, memory.merchants) == null
    }
    .groupBy { ImportIds.merchantKey(it.description) }
    .filterKeys { it.isNotEmpty() }
    .map { (key, rows) ->
        val hint = rows.firstNotNullOfOrNull { it.hint }
        MerchantChoice(
            key = key,
            name = memory.aliases[key] ?: rows.first().description,
            total = Money(rows.sumOf { (it.charge ?: it.originalAmount ?: Money.ZERO).agorot }),
            count = rows.size,
            hint = hint,
            suggested = hint?.let { categoryForHint(it, categories) },
            transfer = rows.any { classify(it, memory) == RowKind.TRANSFER },
        )
    }
    .sortedByDescending { it.total.agorot }

/**
 * A current-spending category sharing a word (3+ letters) with the card company's category name, e.g.
 * "מזון וצריכה" → "מזון". Fixed bills are never suggested ("חשמל ומחשבים" is a shop, not the electricity
 * bill), and a hint matching more than one category suggests nothing.
 */
internal fun categoryForHint(hint: String, categories: List<Category>): String? {
    val words = headerKey(hint).split(' ', ',', '/').filter { it.length >= 3 }
    return categories.filter { category ->
        val name = headerKey(category.name)
        category.klass == ExpenseClass.CURRENT &&
            words.any { word -> name.contains(word) || name.split(' ').any { it.length >= 3 && word.contains(it) } }
    }.singleOrNull()?.id
}

/** The plan with the user's categories on the new lines, and what the merchant memory should learn. */
fun applyDecisions(review: ImportReview, decisions: Map<String, MerchantDecision>): Pair<ImportPlan, List<MemoryUpdate>> {
    val asked = review.merchants.map { it.key }.toSet()
    val toWrite = review.plan.toWrite.map { stored ->
        val key = ImportIds.merchantKey(stored.row.description)
        val decision = decisions[key] as? MerchantDecision.Categorise
        if (key in asked && decision != null && stored.categoryId == null) stored.copy(categoryId = decision.categoryId) else stored
    }
    val updates = decisions.filterKeys { it in asked }.mapNotNull { (key, decision) ->
        when (decision) {
            is MerchantDecision.Categorise -> MemoryUpdate(key, decision.categoryId, false)
            MerchantDecision.OwnTransfer -> MemoryUpdate(key, null, true)
            MerchantDecision.Later -> null
        }
    }
    return review.plan.copy(toWrite = toWrite) to updates
}
