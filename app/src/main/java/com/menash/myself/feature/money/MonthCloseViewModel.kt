package com.menash.myself.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.core.ui.UiEvent
import com.menash.myself.core.ui.UiEvents
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.data.MoneyRepository
import com.menash.myself.feature.money.data.TransactionRepository
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.monthLines
import com.menash.myself.feature.money.domain.MISC_CATEGORY_NAME
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.newCategory
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.hasSuspectBalances
import com.menash.myself.feature.money.domain.needsHistoricalBalances
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** [month] null follows [closeTarget]; the arrows pin a month. [noLines] holds sources the user said had nothing. */
private data class CloseQuery(
    val open: Boolean = false,
    val month: YearMonth? = null,
    val noLines: Set<String> = emptySet(),
    val confirming: Boolean = false,
    val confirmingMisc: Boolean = false,
    val saving: Boolean = false,
    val saveError: String? = null,
    val balanceDraft: CloseBalanceDraft = CloseBalanceDraft(),
    val successfullyClosed: YearMonth? = null,
)

data class MonthCloseUi(
    val open: Boolean = false,
    /** Null until money data arrives. */
    val view: CloseView? = null,
    /** "לסגור מחדש?" is showing for a month that is already closed. */
    val confirmReclose: Boolean = false,
    val confirmMisc: Boolean = false,
    /** The latest month that can be shown: this month. */
    val currentMonth: YearMonth? = null,
    /** The earliest month that can be shown, matching the money screen's history boundary. */
    val earliestMonth: YearMonth? = currentMonth?.minusMonths(12),
    val showBalancesStep: Boolean = false,
    val assets: List<Asset> = emptyList(),
    val balanceDraft: CloseBalanceDraft = CloseBalanceDraft(),
    val balancesValid: Boolean = true,
    val saving: Boolean = false,
    val saveError: String? = null,
    val loadError: Boolean = false,
    val noHousehold: Boolean = false,
    val nextPending: YearMonth? = null,
    /** Sources the user marked "אין חיובים החודש". */
    val noLines: Set<String> = emptySet(),
)

/** "לסגור את החודש": what is still missing, what closing would store, and the close itself. */
class MonthCloseViewModel(
    private val money: MoneyRepository,
    private val transactions: TransactionRepository,
    usdRates: Flow<BigDecimal?>,
    private val today: () -> LocalDate,
    errorReporter: ErrorReporter,
    private val events: UiEvents? = null,
) : BaseViewModel(errorReporter) {

    private val query = MutableStateFlow(CloseQuery())
    private var closing = false

    private val retry = MutableStateFlow(0)
    private val inputs = screenLoad(retry) {
        combine(money.observeForScreen(), transactions.observeTransactionsForScreen(), transactions.observeMemoryForScreen(), usdRates, transactions.observeSourceClosures()) {
            data, stored, memory, rate, sourceClosures -> CloseInputs(data, stored, memory, rate, sourceClosures)
        }
    }

    val ui: StateFlow<MonthCloseUi> = combine(query, inputs) { q, input ->
        val day = today()
        val current = YearMonth.from(day)
        if (input !is ScreenLoad.Ready) return@combine MonthCloseUi(open = q.open, currentMonth = current, loadError = input is ScreenLoad.Error)
        val input = input.value
        val earliest = earliestMonth(current, input.stored, input.data.closedMonths.map { it.month }.toSet())
        if (!q.open || !input.data.householdReady) return@combine MonthCloseUi(q.open, currentMonth = current, earliestMonth = earliest, noHousehold = !input.data.householdReady)
        val month = q.month?.takeIf { it in earliest..<current } ?: closeTarget(day, input.stored, input.data.closedMonths)
        MonthCloseUi(
            open = true,
            view = closeView(input.data, month, day, input.rate, input.stored, input.memory, q.noLines, q.balanceDraft.balances(input.data.assets), input.sourceClosures),
            showBalancesStep = needsHistoricalBalances(month, day),
            assets = input.data.assets,
            balanceDraft = q.balanceDraft,
            balancesValid = q.balanceDraft.valid(input.data.assets),
            confirmReclose = q.confirming,
            confirmMisc = q.confirmingMisc,
            currentMonth = current,
            earliestMonth = earliest,
            saving = q.saving,
            saveError = q.saveError,
            nextPending = if (q.successfullyClosed == month) monthsAwaitingClose(day, input.stored, input.data.closedMonths).firstOrNull { it != month } else null,
            noLines = q.noLines,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MonthCloseUi())

    fun retry() { retry.update { it + 1 } }

    fun open(month: YearMonth? = null) {
        val current = YearMonth.from(today())
        val earliest = ui.value.earliestMonth ?: current.minusMonths(12)
        val target = month?.takeIf { it in earliest..<current }
        query.value = CloseQuery(open = true, month = target, saving = closing)
    }

    fun close() {
        query.value = CloseQuery()
    }

    fun previousMonth() = shift(-1)

    fun nextMonth() = shift(1)

    private fun shift(by: Long) {
        val current = YearMonth.from(today())
        val earliest = ui.value.earliestMonth ?: current.minusMonths(12)
        val shown = ui.value.view?.month ?: return
        val target = shown.plusMonths(by)
        if (target !in earliest..<current) return
        query.update { it.copy(month = target, noLines = emptySet(), confirming = false, confirmingMisc = false, balanceDraft = CloseBalanceDraft(), successfullyClosed = null) }
    }

    fun openNextPending() {
        val target = ui.value.nextPending ?: return
        query.update { it.copy(month = target, noLines = emptySet(), confirming = false, confirmingMisc = false,
            balanceDraft = CloseBalanceDraft(), successfullyClosed = null) }
    }

    /** "אין חיובים החודש" on a missing card or account. */
    fun markNoLines(source: StatementSource) {
        query.update { it.copy(noLines = it.noLines + source.key) }
    }

    fun setSourceClosed(source: StatementSource, closed: Boolean) {
        val month = ui.value.view?.month ?: return
        safeLaunch("setSourceClosed", onError = { error ->
            query.update { it.copy(saveError = com.menash.myself.core.ui.ErrorMapper.toFriendlyMessage(error)) }
        }) { transactions.setSourceClosedFrom(source, if (closed) month else null) }
    }

    fun askClassifyRest() { query.update { it.copy(confirmingMisc = true) } }

    fun cancelClassifyRest() { query.update { it.copy(confirmingMisc = false) } }

    fun classifyRest() {
        val month = ui.value.view?.month ?: return
        query.update { it.copy(confirmingMisc = false, saveError = null) }
        safeLaunch("classifyRest", onError = { error ->
            query.update { it.copy(saveError = com.menash.myself.core.ui.ErrorMapper.toFriendlyMessage(error)) }
        }) {
            val input = (inputs.first { it is ScreenLoad.Ready } as ScreenLoad.Ready<CloseInputs>).value
            val lines = monthLines(input.stored, month, input.memory, input.data.categories).lines.filter { it.unassigned }
            if (lines.isEmpty()) return@safeLaunch
            val category = input.data.categories.firstOrNull { it.name == MISC_CATEGORY_NAME }
                ?: newCategory(MISC_CATEGORY_NAME, ExpenseClass.CURRENT, input.data.categories) { java.util.UUID.randomUUID().toString() }
                ?: return@safeLaunch
            if (input.data.categories.none { it.id == category.id }) money.save(MoneyEntry.CategoryEntry(category))
            // The same per-line write as decide(thisLineOnly = true): never update merchant memory.
            lines.forEach { transactions.setCategory(it.id, category.id) }
        }
    }

    /** Saves the preview. A month already closed asks once before it is replaced. */
    fun closeMonth() {
        if (closing) return
        val view = ui.value.view ?: return
        if (!view.canClose || !ui.value.balancesValid) return
        if (view.existing != null && !query.value.confirming) {
            query.update { it.copy(confirming = true) }
            return
        }
        // Pinned, so the screen stays on the month just closed instead of moving on to the next target.
        closing = true
        query.update { it.copy(month = view.month, confirming = false, saving = true, saveError = null) }
        safeLaunch("closeMonth", onError = { error -> val message = com.menash.myself.core.ui.ErrorMapper.toFriendlyMessage(error); closing = false; query.update { it.copy(saving = false, saveError = message) }; events?.send(UiEvent.SaveError(message) { if (ui.value.open && ui.value.view?.month == view.month) closeMonth() }) }, reportError = false) {
            val acknowledged = withTimeoutOrNull(3_000) { money.save(MoneyEntry.ClosedMonthEntry(view.preview)); true } == true
            closing = false
            query.update { it.copy(saving = false, successfullyClosed = if (acknowledged) view.month else null) }
            events?.send(if (acknowledged) UiEvent.MonthClosed(view.month) else UiEvent.SavedOffline)
        }
    }

    fun enterBalances(entering: Boolean) {
        if (!closing) query.update { it.copy(balanceDraft = it.balanceDraft.copy(entering = entering), confirming = false) }
    }

    fun setBalance(id: String, text: String) {
        if (!closing) query.update { it.copy(balanceDraft = it.balanceDraft.copy(values = it.balanceDraft.values + (id to text)), confirming = false) }
    }

    fun markBalancesUnknown() {
        if (closing) return
        val existing = ui.value.view?.existing ?: return
        if (!existing.hasSuspectBalances()) return
        closing = true
        query.update { it.copy(saving = true, saveError = null) }
        safeLaunch("markBalancesUnknown", onError = { error ->
            closing = false
            query.update { it.copy(saving = false, saveError = com.menash.myself.core.ui.ErrorMapper.toFriendlyMessage(error)) }
        }) {
            val acknowledged = withTimeoutOrNull(3_000) {
                money.save(MoneyEntry.ClosedMonthEntry(existing.copy(balances = emptyMap(), liabilities = emptyMap())))
                true
            } == true
            closing = false
            query.update { it.copy(saving = false) }
            events?.send(if (acknowledged) UiEvent.MonthClosed(existing.month) else UiEvent.SavedOffline)
        }
    }

    fun cancelReclose() {
        query.update { it.copy(confirming = false) }
    }

    private data class CloseInputs(
        val data: MoneyData,
        val stored: List<StoredTransaction>,
        val memory: ImportMemory,
        val rate: BigDecimal?,
        val sourceClosures: Map<StatementSource, YearMonth>,
    )

    companion object {
        fun factory(
            money: MoneyRepository,
            transactions: TransactionRepository,
            usdRates: Flow<BigDecimal?>,
            today: () -> LocalDate,
            errorReporter: ErrorReporter,
            events: UiEvents? = null,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MonthCloseViewModel(money, transactions, usdRates, today, errorReporter, events) as T
        }
    }
}
