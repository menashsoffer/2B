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
    val lines: MonthLines? = null,
    val categories: List<Category> = emptyList(),
    /** Income sources an income line can be linked to, for maaser. */
    val incomes: List<Income> = emptyList(),
) {
    val open: Boolean get() = query.month != null
}

/** The month's imported lines: look, give a category, or remove a cancelled pending line. */
class TransactionsViewModel(
    private val transactions: TransactionRepository,
    money: MoneyRepository,
    errorReporter: ErrorReporter,
    private val events: UiEvents? = null,
) : BaseViewModel(errorReporter) {

    private val query = MutableStateFlow(TransactionsQuery())

    val ui: StateFlow<TransactionsUi> = combine(
        query,
        transactions.observeTransactions(),
        transactions.observeMemory(),
        money.observe(),
    ) { q, stored, memory, data ->
        val incomes = data.items.incomes
        val lines = q.month?.let { monthLines(stored, it, memory, data.categories, incomes.map { income -> income.id }.toSet()) }
        TransactionsUi(q, lines, data.categories, incomes)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TransactionsUi())

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
     * A category for one line is kept by later re-imports, and the merchant is learnt so its other
     * uncategorised lines and future ones follow. Card bills and cash are never learnt.
     */
    fun decide(lineId: String, decision: MerchantDecision) {
        val line = ui.value.lines?.lines?.firstOrNull { it.id == lineId } ?: return
        query.update { it.copy(picking = null) }
        val key = ImportIds.merchantKey(line.row.description)
        when (decision) {
            is MerchantDecision.Categorise -> safeLaunch("setCategory") {
                transactions.setCategory(line.id, decision.categoryId)
                if (teachesMemory(line)) transactions.remember(key, decision.categoryId, false)
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
