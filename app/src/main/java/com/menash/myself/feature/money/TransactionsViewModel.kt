package com.menash.myself.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.core.ui.UiEvent
import com.menash.myself.core.ui.UiEvents
import com.menash.myself.feature.money.data.MoneyRepository
import com.menash.myself.feature.money.data.TransactionRepository
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.Income
import com.menash.myself.feature.money.importer.ImportIds
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.RowKind
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.classify
import com.menash.myself.feature.money.importer.LineFilter
import com.menash.myself.feature.money.importer.MerchantDecision
import com.menash.myself.feature.money.importer.MonthLines
import com.menash.myself.feature.money.importer.monthLines
import com.menash.myself.feature.money.importer.teachesMemory
import com.menash.myself.feature.money.importer.MonthLine
import com.menash.myself.feature.money.search.LineSearchResult
import com.menash.myself.feature.money.search.LineSearchScope
import com.menash.myself.feature.money.search.matching
import com.menash.myself.feature.money.search.SearchPeriod
import com.menash.myself.feature.money.search.searchBounds
import com.menash.myself.feature.money.search.searchLines
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.yield
import java.time.YearMonth

/**
 * What the user is looking at. A [month] shows that month's lines; a [search] (with no month) looks
 * across months. Both null means the screen is closed. [text] filters either view.
 */
data class TransactionsQuery(
    val month: YearMonth? = null,
    val filter: LineFilter = LineFilter.ALL,
    val text: String = "",
    val search: LineSearchScope? = null,
    /** A month opened from search results; going back returns to that search. */
    val backToSearch: TransactionsQuery? = null,
    /** The line whose category sheet is open. */
    val picking: String? = null,
    /** The pending line waiting for "להסיר?". */
    val removing: String? = null,
)

data class TransactionsUi(
    val query: TransactionsQuery = TransactionsQuery(),
    /** Null means the selected month has not loaded. */
    val lines: MonthLines? = null,
    val categories: List<Category> = emptyList(),
    /** Income sources an income line can be linked to, for maaser. */
    val incomes: List<Income> = emptyList(),
    /** Merchant aliases and "ask each time" marks; the memory itself drives the categories in [lines]. */
    val memory: ImportMemory = ImportMemory(),
    /** Every stored line of the household, for pinning categories when a merchant stops learning. */
    val stored: List<StoredTransaction> = emptyList(),
    val loadError: Boolean = false,
    /** The month's lines after the filter and the search box; null until loaded. */
    val shown: List<MonthLine>? = null,
    /** Matches across months while searching; null until loaded or outside search. */
    val results: LineSearchResult? = null,
) {
    val open: Boolean get() = query.month != null || query.search != null
    val searching: Boolean get() = query.month == null && query.search != null
    /** Every line on screen, so a sheet opened from the month or from search finds its line. */
    val allLines: List<MonthLine> get() = lines?.lines.orEmpty() + results?.lines.orEmpty()
}

/** The month's imported lines: look, give a category, or remove a cancelled pending line. */
class TransactionsViewModel(
    private val transactions: TransactionRepository,
    private val money: MoneyRepository,
    errorReporter: ErrorReporter,
    private val events: UiEvents? = null,
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
    private val today: () -> java.time.LocalDate = { java.time.LocalDate.now(java.time.ZoneId.of("Asia/Jerusalem")) },
) : BaseViewModel(errorReporter) {

    private val query = MutableStateFlow(TransactionsQuery())

    private val retry = MutableStateFlow(0)
    private val inputs = screenLoad(retry) {
        combine(transactions.observeTransactionsForScreen(), transactions.observeMemoryForScreen(), money.observeForScreen()) { stored, memory, data ->
            Triple(stored, memory, data)
        }
    }
    val ui: StateFlow<TransactionsUi> = combine(query, inputs) { q, input ->
        when (input) {
            ScreenLoad.Loading -> TransactionsUi(query = q)
            ScreenLoad.Error -> TransactionsUi(query = q, loadError = true)
            is ScreenLoad.Ready -> {
                val (stored, memory, data) = input.value
                val incomes = data.items.incomes
                val incomeIds = incomes.map { income -> income.id }.toSet()
                val lines = q.month?.let { monthLines(stored, it, memory, data.categories, incomeIds) }
                val shown = lines?.let { MonthLines(it.month, it.filtered(q.filter)).matching(q.text, memory, data.categories).lines }
                val results = q.search?.takeIf { q.month == null }?.let { searchLines(stored, memory, data.categories, q.text, it, incomeIds) }
                TransactionsUi(q, lines, data.categories, incomes, memory, stored, shown = shown, results = results)
            }
        }
    }.let { upstream ->
        var displayedMonth: YearMonth? = null
        upstream.transform { value ->
            if (value.query.month != displayedMonth) {
                displayedMonth = value.query.month
                if (displayedMonth != null && value.lines != null) {
                    emit(value.copy(lines = null))
                    yield()
                }
            }
            emit(value)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TransactionsUi())

    fun retry() { retry.update { it + 1 } }

    fun open(month: YearMonth, filter: LineFilter = LineFilter.ALL) {
        query.value = TransactionsQuery(month = month, filter = filter)
    }

    fun close() {
        query.value = query.value.backToSearch ?: TransactionsQuery()
    }

    /** Opens search across months, optionally narrowed (a period, a category, only unassigned lines). */
    fun openSearch(scope: LineSearchScope = LineSearchScope(), text: String = "") {
        query.value = TransactionsQuery(search = scope, text = text)
    }

    /** All lines of a month range (the period screen); the text box narrows them further. */
    fun openRange(from: YearMonth, to: YearMonth) {
        openSearch(LineSearchScope(SearchPeriod.CUSTOM, from, to))
    }

    fun setText(text: String) {
        query.update { it.copy(text = text) }
    }

    fun setSearchScope(scope: LineSearchScope) {
        query.update { it.copy(search = scope) }
    }

    /** A quick period; CUSTOM is only set by [openSearch] from another screen, so it is ignored here. */
    fun selectPeriod(period: SearchPeriod) {
        if (period == SearchPeriod.CUSTOM) return
        val (from, to) = searchBounds(period, YearMonth.from(today()))
        query.update { q -> q.copy(search = q.search?.copy(period = period, from = from, to = to)) }
    }

    /** From a search result to its whole month; back returns to the same search. */
    fun openMonthFromSearch(month: YearMonth) {
        val search = query.value.takeIf { it.search != null && it.month == null } ?: return
        query.value = TransactionsQuery(month = month, backToSearch = search.copy(picking = null, removing = null))
    }

    fun showMonth(month: YearMonth) {
        query.update { it.copy(month = month, picking = null, removing = null) }
    }

    fun setFilter(filter: LineFilter) {
        query.update { it.copy(filter = filter) }
    }

    fun pick(lineId: String?) {
        query.update { it.copy(picking = lineId) }
    }

    fun askRemove(lineId: String?) {
        query.update { it.copy(removing = lineId) }
    }

    /**
     * A category for one line is kept by later re-imports. Unless [thisLineOnly], or the merchant asks each
     * time, the merchant is learnt so its other uncategorised lines and future ones follow.
     * Card bills and cash are never learnt.
     */
    fun decide(lineId: String, decision: MerchantDecision, thisLineOnly: Boolean = false) {
        val line = ui.value.allLines.firstOrNull { it.id == lineId } ?: return
        query.update { it.copy(picking = null) }
        val key = ImportIds.merchantKey(line.row.description)
        val learn = teachesMemory(line) && !thisLineOnly && !ui.value.memory.asksEachTime(line.row.description)
        when (decision) {
            is MerchantDecision.Categorise -> safeLaunch("setCategory") {
                transactions.setCategory(line.id, decision.categoryId)
                if (learn) transactions.remember(key, decision.categoryId, false)
                ui.value.categories.firstOrNull { it.id == decision.categoryId }?.let { events?.send(UiEvent.CategoryAssigned(it.name)) }
            }
            MerchantDecision.OwnTransfer -> safeLaunch("markOwnTransfer") { transactions.remember(key, null, true) }
            MerchantDecision.Later -> Unit
        }
    }

    /** Links an income line to its source for maaser; the payer is remembered so next month's line follows. */
    fun linkIncome(lineId: String, incomeId: String) {
        val line = ui.value.allLines.firstOrNull { it.id == lineId } ?: return
        query.update { it.copy(picking = null) }
        if (!line.income) return
        safeLaunch("linkIncome") {
            transactions.setCategory(line.id, incomeId)
            transactions.remember(ImportIds.merchantKey(line.row.description), incomeId, false)
        }
    }

    /** Adds a category from the line's sheet and gives it to the line, as [decide] would. */
    fun createCategory(lineId: String, name: String, klass: com.menash.myself.feature.money.domain.ExpenseClass, thisLineOnly: Boolean = false) {
        val category = com.menash.myself.feature.money.domain.newCategory(name, klass, ui.value.categories, newId) ?: return
        val isNew = ui.value.categories.none { it.id == category.id }
        safeLaunch("createCategory") {
            if (isNew) money.save(com.menash.myself.feature.money.data.MoneyEntry.CategoryEntry(category))
            decide(lineId, MerchantDecision.Categorise(category.id), thisLineOnly)
        }
    }

    /** The household's name for this line's merchant, shown instead of the bank's text. Blank removes it. */
    fun setAlias(lineId: String, alias: String) {
        val line = ui.value.allLines.firstOrNull { it.id == lineId } ?: return
        val clean = alias.trim().take(com.menash.myself.feature.money.data.ImportDocuments.MAX_ALIAS)
        val key = ImportIds.merchantKey(line.row.description)
        if (clean == ui.value.memory.aliases[key].orEmpty() || (clean == line.row.description && key !in ui.value.memory.aliases)) return
        safeLaunch("setMerchantAlias") { transactions.setAlias(key, clean.takeIf { it.isNotEmpty() && it != line.row.description }) }
    }

    /**
     * Marks a generic merchant (ביט, העברה) to ask on every line. Lines that followed the learnt category
     * keep it as their own first, so closed and past months do not change.
     */
    fun setAskEachTime(lineId: String, ask: Boolean) {
        val line = ui.value.allLines.firstOrNull { it.id == lineId } ?: return
        val memory = ui.value.memory
        val key = ImportIds.merchantKey(line.row.description)
        if (ask == (key in memory.askEachTime)) return
        val learnt = memory.merchants.categories[key]
        val followers = if (ask && learnt != null) ui.value.stored.filter {
            it.categoryId == null && ImportIds.merchantKey(it.row.description) == key &&
                classify(it.row, memory) !in setOf(RowKind.CARD_TOTAL, RowKind.CASH_WITHDRAWAL)
        } else emptyList()
        safeLaunch("setMerchantAskEachTime") {
            followers.forEach { transactions.setCategory(it.id, learnt) }
            transactions.setAskEachTime(key, ask)
        }
    }

    fun remove(lineId: String) {
        val line = ui.value.allLines.firstOrNull { it.id == lineId }
        query.update { it.copy(removing = null) }
        if (line == null || !line.removable) return
        safeLaunch("deleteTransaction") { transactions.delete(line.id) }
    }

    companion object {
        fun factory(
            transactions: TransactionRepository,
            money: MoneyRepository,
            errorReporter: ErrorReporter,
            events: UiEvents? = null,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TransactionsViewModel(transactions, money, errorReporter, events) as T
        }
    }
}
