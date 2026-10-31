package com.menash.myself.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.external.boi.BoiRate
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.core.ui.UiEvent
import com.menash.myself.core.ui.UiEvents
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.Card
import com.menash.myself.feature.money.data.MoneyRepository
import com.menash.myself.feature.money.data.TransactionRepository
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.data.categoriesToSeed
import com.menash.myself.feature.money.domain.defaultCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** The stored Bank of Israel rate as the engine wants it; null when nothing usable is stored. */
fun usdRateOf(rate: BoiRate?): BigDecimal? =
    rate?.rate?.takeIf { it > 0.0 }?.let { BigDecimal.valueOf(it) }

/** Null until the first snapshot arrives. */
data class MoneyScreenState(
    val money: MoneyMonth? = null,
    val data: MoneyData = MoneyData(),
)

/** The open add/edit sheet. [askScope] shows the one "from now on / this month only" question. */
data class EditorState(
    val draft: ItemDraft,
    val isNew: Boolean,
    val askScope: Boolean = false,
    val error: DraftField? = null,
    val confirmDelete: Boolean = false,
    val saving: Boolean = false,
    val saveError: String? = null,
    /** Reuse generated IDs if a write fails and its result is uncertain. */
    val retryEntries: List<MoneyEntry>? = null,
    /** The item sheet a "כרטיס חדש" sheet was opened from; it comes back, draft intact, when the card sheet closes. */
    val returnTo: EditorState? = null,
)

class MoneyViewModel(
    private val repository: MoneyRepository,
    usdRates: Flow<BigDecimal?>,
    private val today: () -> LocalDate,
    errorReporter: ErrorReporter,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    transactions: Flow<List<StoredTransaction>> = flowOf(emptyList()),
    memory: Flow<ImportMemory> = flowOf(ImportMemory()),
    private val events: UiEvents? = null,
) : BaseViewModel(errorReporter) {

    private var seeded = false
    private var latest = MoneyData()
    private var lastCardId: String? = null
    /** Cards saved here that the repository may not have sent back yet, so an item saved right after finds its card. */
    private val createdCards = mutableListOf<Card>()

    val state: StateFlow<MoneyScreenState> = combine(repository.observe(), usdRates, transactions, memory) { data, rate, lines, learnt ->
        latest = data
        val day = today()
        MoneyScreenState(money = buildMoneyMonth(data, YearMonth.from(day), day, rate, lines, learnt), data = data)
    }
        .onEach { seedCategoriesOnce(it.data) }
        .catch { error ->
            if (error is Exception) handleException("observeMoney", error) else throw error
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MoneyScreenState())

    private val _editor = MutableStateFlow<EditorState?>(null)
    val editor: StateFlow<EditorState?> = _editor.asStateFlow()

    private val month: YearMonth get() = YearMonth.from(today())

    private fun seedCategoriesOnce(data: MoneyData) {
        if (seeded || !data.householdReady) return
        seeded = true
        val toSeed = categoriesToSeed(data.categories, defaultCategories())
        if (toSeed.isNotEmpty()) safeLaunch("seedCategories") { repository.saveCategories(toSeed) }
    }

    fun startAdding(kind: EditKind) {
        val cardId = lastCardId?.takeIf { id -> latest.cards.any { it.id == id } } ?: latest.cards.lastOrNull()?.id
        _editor.value = EditorState(newDraft(kind, month, today(), cardId), isNew = true)
    }

    /** "להוסיף כמנוי" on a charge seen in the card files: the sheet opens filled from what was charged. */
    fun startSubscriptionFrom(charge: UnlistedCharge) {
        startAdding(EditKind.SUBSCRIPTION)
        _editor.update {
            it?.copy(draft = it.draft.copy(
                name = charge.name, amount = amountText(charge.last.amount), categoryId = charge.categoryId,
                renewalDate = charge.last.month.atDay(minOf(today().dayOfMonth, charge.last.month.lengthOfMonth())),
            ))
        }
    }

    /** "כרטיס חדש" from inside an item sheet: the item's draft waits and gets the new card when it is saved. */
    fun startNewCard() {
        if (_editor.value?.saving == true) return
        val from = _editor.value?.copy(error = null, askScope = false, confirmDelete = false)
        _editor.value = EditorState(newDraft(EditKind.CARD, month, today(), null), isNew = true, returnTo = from)
    }

    fun startEditing(entry: MoneyEntry) {
        _editor.value = EditorState(draftOf(entry, month, today()), isNew = false)
    }

    fun updateDraft(transform: (ItemDraft) -> ItemDraft) {
        _editor.update { if (it == null || it.saving) it else it.copy(draft = transform(it.draft), error = null, retryEntries = null) }
    }

    fun dismissEditor() {
        if (_editor.value?.saving != true) _editor.value = _editor.value?.returnTo
    }

    fun dismissScope() {
        _editor.update { it?.copy(askScope = false) }
    }

    /** [scope] is passed only after the user answered the one question. */
    fun save(scope: AmountScope? = null) {
        val current = _editor.value ?: return
        if (current.saving) return
        val data = latest.copy(cards = latest.cards + createdCards.filter { card -> latest.cards.none { it.id == card.id } })
        when (val result = current.retryEntries?.let { EditResult.Ok(it) } ?: buildEntries(current.draft, data, month, today(), scope, newId)) {
            EditResult.NeedsScope -> _editor.value = current.copy(askScope = true)
            is EditResult.Invalid -> _editor.value = current.copy(error = result.field, askScope = false)
            is EditResult.Ok -> {
                val card = result.entries.filterIsInstance<MoneyEntry.CardEntry>().firstOrNull()?.value
                _editor.value = current.copy(saving = true, saveError = null, askScope = false, retryEntries = result.entries)
                safeLaunch("saveMoney", onError = { error -> val message = com.menash.myself.core.ui.ErrorMapper.toFriendlyMessage(error); _editor.update { it?.copy(saving = false, saveError = message) }; events?.send(UiEvent.SaveError(message) { if (_editor.value?.retryEntries === result.entries) save() }) }) {
                    repository.saveAll(result.entries)
                    current.draft.cardId?.let { lastCardId = it }
                    if (card != null) {
                        lastCardId = card.id
                        createdCards.removeAll { it.id == card.id }
                        createdCards += card
                    }
                    _editor.value = current.returnTo?.let { back -> card?.let { back.copy(draft = back.draft.copy(cardId = it.id)) } ?: back }
                    events?.send(UiEvent.MONEY_SAVED)
                }
            }
        }
    }

    fun requestDelete() {
        if (_editor.value?.saving == true) return
        _editor.update { it?.copy(confirmDelete = true) }
    }

    fun cancelDelete() {
        _editor.update { it?.copy(confirmDelete = false) }
    }

    fun delete(entry: MoneyEntry) {
        if (_editor.value?.saving == true) return
        _editor.value = null
        safeLaunch("deleteMoney") { repository.delete(entry.collection, entry.id); events?.send(UiEvent.DELETED) }
    }

    /** The stored entry the open draft edits, for delete. */
    fun editingEntry(): MoneyEntry? {
        val id = _editor.value?.draft?.id ?: return null
        val data = latest
        val items = data.items
        return (data.cards.map { MoneyEntry.CardEntry(it) } +
            items.incomes.map { MoneyEntry.IncomeEntry(it) } +
            items.fixedExpenses.map { MoneyEntry.FixedExpenseEntry(it) } +
            items.subscriptions.map { MoneyEntry.SubscriptionEntry(it) } +
            items.variableBudgets.map { MoneyEntry.VariableBudgetEntry(it) } +
            items.installments.map { MoneyEntry.InstallmentEntry(it) } +
            items.periodicBudgets.map { MoneyEntry.PeriodicBudgetEntry(it) } +
            items.debts.map { MoneyEntry.DebtEntry(it) } +
            items.savings.map { MoneyEntry.SavingEntry(it) } +
            data.assets.map { MoneyEntry.AssetEntry(it) })
            .firstOrNull { it.id == id }
    }

    companion object {
        fun factory(
            repository: MoneyRepository,
            usdRates: Flow<BigDecimal?>,
            today: () -> LocalDate,
            errorReporter: ErrorReporter,
            transactions: TransactionRepository,
            events: UiEvents? = null,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MoneyViewModel(
                    repository, usdRates, today, errorReporter,
                    transactions = transactions.observeTransactions(), memory = transactions.observeMemory(),
                    events = events,
                ) as T
            }
        }
    }
}
