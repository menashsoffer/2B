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
import kotlinx.coroutines.withTimeoutOrNull
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
    val saving: Boolean = false,
    val saveError: String? = null,
    val balanceDraft: CloseBalanceDraft = CloseBalanceDraft(),
)

data class MonthCloseUi(
    val open: Boolean = false,
    /** Null until money data arrives. */
    val view: CloseView? = null,
    /** "לסגור מחדש?" is showing for a month that is already closed. */
    val confirmReclose: Boolean = false,
    /** The latest month that can be shown: this month. */
    val currentMonth: YearMonth? = null,
    val showBalancesStep: Boolean = false,
    val assets: List<Asset> = emptyList(),
    val balanceDraft: CloseBalanceDraft = CloseBalanceDraft(),
    val balancesValid: Boolean = true,
    val saving: Boolean = false,
    val saveError: String? = null,
    val loadError: Boolean = false,
)

/** "לסגור את החודש": what is still missing, what closing would store, and the close itself. */
class MonthCloseViewModel(
    private val money: MoneyRepository,
    transactions: TransactionRepository,
    usdRates: Flow<BigDecimal?>,
    private val today: () -> LocalDate,
    errorReporter: ErrorReporter,
    private val events: UiEvents? = null,
) : BaseViewModel(errorReporter) {

    private val query = MutableStateFlow(CloseQuery())
    private var closing = false

    private val retry = MutableStateFlow(0)
    private val inputs = screenLoad(retry) {
        combine(money.observeForScreen(), transactions.observeTransactionsForScreen(), transactions.observeMemoryForScreen(), usdRates) {
            data, stored, memory, rate -> CloseInputs(data, stored, memory, rate)
        }
    }

    val ui: StateFlow<MonthCloseUi> = combine(query, inputs) { q, input ->
        val day = today()
        if (input !is ScreenLoad.Ready) return@combine MonthCloseUi(open = q.open, currentMonth = YearMonth.from(day), loadError = input is ScreenLoad.Error)
        val input = input.value
        if (!q.open || !input.data.householdReady) return@combine MonthCloseUi(q.open, currentMonth = YearMonth.from(day))
        val month = q.month ?: closeTarget(day, input.stored, input.data.closedMonths)
        MonthCloseUi(
            open = true,
            view = closeView(input.data, month, day, input.rate, input.stored, input.memory, q.noLines, q.balanceDraft.balances(input.data.assets)),
            showBalancesStep = needsHistoricalBalances(month, day),
            assets = input.data.assets,
            balanceDraft = q.balanceDraft,
            balancesValid = q.balanceDraft.valid(input.data.assets),
            confirmReclose = q.confirming,
            currentMonth = YearMonth.from(day),
            saving = q.saving,
            saveError = q.saveError,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MonthCloseUi())

    fun retry() { retry.update { it + 1 } }

    fun open() {
        query.value = CloseQuery(open = true, saving = closing)
    }

    fun close() {
        query.value = CloseQuery()
    }

    fun previousMonth() = shift(-1)

    fun nextMonth() = shift(1)

    private fun shift(by: Long) {
        val shown = ui.value.view?.month ?: return
        val target = shown.plusMonths(by)
        if (target > YearMonth.from(today())) return
        query.update { it.copy(month = target, noLines = emptySet(), confirming = false, balanceDraft = CloseBalanceDraft()) }
    }

    /** "אין חיובים החודש" on a missing card or account. */
    fun markNoLines(source: StatementSource) {
        query.update { it.copy(noLines = it.noLines + source.key) }
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
            query.update { it.copy(saving = false) }
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
