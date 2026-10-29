package com.menash.myself.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.external.boi.BoiRate
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.data.MoneyRepository
import com.menash.myself.feature.money.data.categoriesToSeed
import com.menash.myself.feature.money.domain.defaultCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
)

class MoneyViewModel(
    private val repository: MoneyRepository,
    usdRates: Flow<BigDecimal?>,
    private val today: () -> LocalDate,
    errorReporter: ErrorReporter,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : BaseViewModel(errorReporter) {

    private var seeded = false
    private var latest = MoneyData()
    private var lastCardId: String? = null

    val state: StateFlow<MoneyScreenState> = combine(repository.observe(), usdRates) { data, rate ->
        latest = data
        val day = today()
        MoneyScreenState(money = buildMoneyMonth(data, YearMonth.from(day), day, rate), data = data)
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

    fun startEditing(entry: MoneyEntry) {
        _editor.value = EditorState(draftOf(entry, month, today()), isNew = false)
    }

    fun updateDraft(transform: (ItemDraft) -> ItemDraft) {
        _editor.update { it?.copy(draft = transform(it.draft), error = null) }
    }

    fun dismissEditor() {
        _editor.value = null
    }

    fun dismissScope() {
        _editor.update { it?.copy(askScope = false) }
    }

    /** [scope] is passed only after the user answered the one question. */
    fun save(scope: AmountScope? = null) {
        val current = _editor.value ?: return
        when (val result = buildEntries(current.draft, latest, month, today(), scope, newId)) {
            EditResult.NeedsScope -> _editor.value = current.copy(askScope = true)
            is EditResult.Invalid -> _editor.value = current.copy(error = result.field, askScope = false)
            is EditResult.Ok -> {
                current.draft.cardId?.let { lastCardId = it }
                result.entries.filterIsInstance<MoneyEntry.CardEntry>().firstOrNull()?.let { lastCardId = it.id }
                _editor.value = null
                safeLaunch("saveMoney") { result.entries.forEach { repository.save(it) } }
            }
        }
    }

    fun requestDelete() {
        _editor.update { it?.copy(confirmDelete = true) }
    }

    fun cancelDelete() {
        _editor.update { it?.copy(confirmDelete = false) }
    }

    fun delete(entry: MoneyEntry) {
        _editor.value = null
        safeLaunch("deleteMoney") { repository.delete(entry.collection, entry.id) }
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
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MoneyViewModel(repository, usdRates, today, errorReporter) as T
            }
        }
    }
}
