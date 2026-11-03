package com.menash.myself.feature.money.search

import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.search.SearchQuery
import com.menash.myself.core.domain.search.search
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.sumMoney
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.MonthLine
import com.menash.myself.feature.money.importer.MonthLines
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.monthLines
import java.time.YearMonth

/** The quick periods of the search screen; CUSTOM is a range handed in by another screen. */
enum class SearchPeriod { ALL, THIS_MONTH, LAST_3, THIS_YEAR, CUSTOM }

/** The months a quick period covers, ending with today's month; null ends mean open-ended. */
fun searchBounds(period: SearchPeriod, today: YearMonth): Pair<YearMonth?, YearMonth?> = when (period) {
    SearchPeriod.ALL, SearchPeriod.CUSTOM -> null to null
    SearchPeriod.THIS_MONTH -> today to today
    SearchPeriod.LAST_3 -> today.minusMonths(2) to today
    SearchPeriod.THIS_YEAR -> YearMonth.of(today.year, 1) to today
}

/** Which lines a search looks at. Null ends mean open-ended; [categoryId] narrows to one category. */
data class LineSearchScope(
    val period: SearchPeriod = SearchPeriod.ALL,
    val from: YearMonth? = null,
    val to: YearMonth? = null,
    val categoryId: String? = null,
    val onlyUnassigned: Boolean = false,
) {
    fun contains(month: YearMonth): Boolean = (from == null || month >= from) && (to == null || month <= to)
}

/** Matches grouped by billing month, newest month first, each month's lines in [monthLines] order. */
data class LineSearchResult(val months: List<MonthLines>) {
    val lines: List<MonthLine> get() = months.flatMap { it.lines }
    val count: Int get() = months.sumOf { it.lines.size }
    /** Counted spending among the matches; income and uncounted lines are found but not summed. */
    val spent: Money get() = sumMoney(lines.filter { it.counted }.mapNotNull { it.row.charge })
}

/**
 * Searches every stored line through the same [monthLines] the month and transactions screens use, so a
 * line's category and counting never differ between search and its month. Found by: the bank's text, the
 * household's alias, the details, and its category name; numbers also match the charge and original amount.
 */
fun searchLines(
    stored: List<StoredTransaction>,
    memory: ImportMemory,
    categories: List<Category>,
    text: String,
    scope: LineSearchScope = LineSearchScope(),
    incomeIds: Set<String> = emptySet(),
): LineSearchResult {
    val query = SearchQuery.parse(text)
    val months = stored.map { it.row.billingMonth }.distinct().filter(scope::contains).sortedDescending()
    return LineSearchResult(
        months.map { month -> monthLines(stored, month, memory, categories, incomeIds).matching(query, memory, categories, scope) }
            .filter { it.lines.isNotEmpty() },
    )
}

/** The month's lines that match [text]; used by the search box inside one month's transactions. */
fun MonthLines.matching(text: String, memory: ImportMemory, categories: List<Category>): MonthLines =
    matching(SearchQuery.parse(text), memory, categories, LineSearchScope())

private fun MonthLines.matching(query: SearchQuery, memory: ImportMemory, categories: List<Category>, scope: LineSearchScope): MonthLines {
    val names = categories.associate { it.id to it.name }
    val kept = lines
        .filter { !scope.onlyUnassigned || it.unassigned }
        .filter { scope.categoryId == null || it.categoryId == scope.categoryId }
        .search(
            query,
            texts = { listOf(it.row.description, memory.displayName(it.row.description), it.row.details, names[it.categoryId]) },
            amounts = { listOfNotNull(it.row.charge?.agorot, it.row.originalAmount?.agorot) },
        )
    return copy(lines = kept)
}
