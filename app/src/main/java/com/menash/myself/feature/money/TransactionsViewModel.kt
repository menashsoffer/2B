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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.yield
import java.time.YearMonth

/** What the user is looking at; null month means the screen is closed. */
data class TransactionsQuery(
    val month: YearMonth? = null,
    val filter: LineFilter = LineFilter.ALL,
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
) {
    val open: Boolean get() = query.month != null
}

/** The month's imported lines: look, give a category, or remove a cancelled pending line. */
class TransactionsViewModel(
    private val transactions: TransactionRepository,
    private val money: MoneyRepository,
    errorReporter: ErrorReporter,
    private val events: UiEvents? = null,
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
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
                val lines = q.month?.let { monthLines(stored, it, memory, data.categories, incomes.map { income -> income.id }.toSet()) }
                TransactionsUi(q, lines, data.categories, incomes, memory, stored)
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
        query.value = TransactionsQuery()
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
        val line = ui.value.lines?.lines?.firstOrNull { it.id == lineId } ?: return
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
        val line = ui.value.lines?.lines?.firstOrNull { it.id == lineId } ?: return
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
        val line = ui.value.lines?.lines?.firstOrNull { it.id == lineId } ?: return
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
        val line = ui.value.lines?.lines?.firstOrNull { it.id == lineId } ?: return
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
        val line = ui.value.lines?.lines?.firstOrNull { it.id == lineId }
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
