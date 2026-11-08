package com.menash.myself.feature.money

import com.menash.myself.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.core.ui.UiEvent
import com.menash.myself.core.ui.UiEvents
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.data.MoneyRepository
import com.menash.myself.feature.money.data.TransactionRepository
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.importer.ImportFile
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.ImportReview
import com.menash.myself.feature.money.importer.MerchantDecision
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.applyDecisions
import com.menash.myself.feature.money.importer.reviewImport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** The import flow: nothing is written before [ImportUi.Review] is confirmed. */
sealed interface ImportUi {
    object Idle : ImportUi
    object Reading : ImportUi
    object Error : ImportUi

    /** [picking] is the merchant whose category sheet is open. A merchant with no decision is left for later. */
    data class Review(
        val review: ImportReview,
        val categories: List<Category>,
        val decisions: Map<String, MerchantDecision> = emptyMap(),
        val picking: String? = null,
        val recentCategories: List<String> = emptyList(),
        val frequentCategories: List<String> = emptyList(),
    ) : ImportUi {
        /** The decision shown for a merchant: the user's, else the suggested category. */
        fun decisionFor(key: String): MerchantDecision? = decisions[key]
            ?: review.merchants.firstOrNull { it.key == key }?.suggested?.let { MerchantDecision.Categorise(it) }
    }

    /** A Paamonim workbook: its budget as a draft plan. Only [selected] lines are written, and only on confirm. */
    data class PlanReview(val draft: PlanImportDraft, val selected: Set<String> = draft.selectableKeys) : ImportUi

    /** The recent months give no line a draft could use (for example only single subscriptions were categorised). */
    object PlanNoHistory : ImportUi

    /** [saved] plan lines were written; they apply from [from]. */
    data class PlanDone(val saved: Int, val from: java.time.YearMonth) : ImportUi

    object Saving : ImportUi
    data class Done(val written: Int, val removed: Int, val remembered: Int = 0, val unassignedByMonth: Map<java.time.YearMonth, Int> = emptyMap()) : ImportUi
}

/** A Paamonim plan workbook, or null for anything else (bank and card files, unreadable files). */
private fun readPlanOrNull(bytes: ByteArray): com.menash.myself.feature.money.importer.PaamonimPlan? = try {
    com.menash.myself.feature.money.importer.readPaamonimPlan(com.menash.myself.feature.money.importer.readWorkbook(bytes))
} catch (e: com.menash.myself.feature.money.importer.ImportException) {
    null
}

class ImportViewModel(
    private val transactions: TransactionRepository,
    private val money: MoneyRepository,
    errorReporter: ErrorReporter,
    private val io: CoroutineDispatcher = Dispatchers.Default,
    private val events: UiEvents? = null,
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
    private val today: () -> java.time.LocalDate = com.menash.myself.core.time.TodayProvider::today,
) : BaseViewModel(errorReporter) {

    private var loadedMemory = ImportMemory()
    private var loadedCategories: List<Category> = emptyList()
    private var pickedFiles: List<ImportFile> = emptyList()
    private var loadJob: Job? = null

    private val _ui = MutableStateFlow<ImportUi>(ImportUi.Idle)
    val ui: StateFlow<ImportUi> = _ui.asStateFlow()
    private val _open = MutableStateFlow(false)
    val open: StateFlow<Boolean> = _open.asStateFlow()

    /** Reads and compares the files with what is stored. Parsing runs off the main thread. */
    fun load(files: List<ImportFile>) {
        if (files.isEmpty()) return
        _open.value = true
        pickedFiles = files.toList()
        loadJob?.cancel()
        _ui.value = ImportUi.Reading
        loadJob = viewModelScope.launch {
            try {
                val planFile = files.singleOrNull()?.takeUnless { it.readError }?.let { file ->
                    withContext(io) { readPlanOrNull(file.bytes) }
                }
                if (planFile != null) {
                    val data = money.observeForImport().first()
                    val draft = withContext(io) { draftPlanImport(planFile, data, java.time.YearMonth.from(today()), newId) }
                    _ui.value = ImportUi.PlanReview(draft)
                    return@launch
                }
                val (existing, memory, categories) = combine(
                    transactions.observeTransactionsForImport(), transactions.observeMemoryForImport(), money.observeForImport(),
                ) { lines, memory, data -> Triple(lines, memory, data.categories) }.first()
                val review = withContext(io) { reviewImport(files, existing.associateBy { it.id }, memory, categories) }
                loadedMemory = memory
                loadedCategories = categories
                _ui.value = ImportUi.Review(review, categories, frequentCategories = existing.mapNotNull { it.categoryId }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key })
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException("loadImport", e)
                _ui.value = ImportUi.Error
            }
        }
    }

    fun retry() = if (pickedFiles.isEmpty()) openHistoryPlan() else load(pickedFiles)

    /** Opens the plan review on a draft built from the household's recent months instead of a file. */
    fun openHistoryPlan() {
        _open.value = true
        loadJob?.cancel()
        pickedFiles = emptyList()
        _ui.value = ImportUi.Reading
        loadJob = viewModelScope.launch {
            try {
                val (stored, memory, data) = combine(
                    transactions.observeTransactionsForImport(), transactions.observeMemoryForImport(), money.observeForImport(),
                ) { lines, memory, data -> Triple(lines, memory, data) }.first()
                val day = today()
                val draft = withContext(io) {
                    draftPlanFromHistory(spendingHistory(data, stored, memory, day), data, java.time.YearMonth.from(day), newId)
                }
                _ui.value = if (draft == null || draft.lines.isEmpty()) ImportUi.PlanNoHistory else ImportUi.PlanReview(draft)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException("historyPlan", e)
                _ui.value = ImportUi.Error
            }
        }
    }

    fun openPicker(merchantKey: String) {
        _ui.update { (it as? ImportUi.Review)?.copy(picking = merchantKey) ?: it }
    }

    fun closePicker() {
        _ui.update { (it as? ImportUi.Review)?.copy(picking = null) ?: it }
    }

    fun decide(merchantKey: String, decision: MerchantDecision) {
        val before = _ui.value as? ImportUi.Review ?: return
        val previous = before.decisions[merchantKey]
        _ui.update { state ->
            (state as? ImportUi.Review)?.copy(
                decisions = state.decisions + (merchantKey to decision), picking = null,
                recentCategories = (decision as? MerchantDecision.Categorise)?.let { listOf(it.categoryId) + state.recentCategories.filterNot { id -> id == it.categoryId } } ?: state.recentCategories,
            ) ?: state
        }
        val name = (decision as? MerchantDecision.Categorise)?.let { d -> before.categories.firstOrNull { it.id == d.categoryId }?.name }
        val label = when (decision) {
            MerchantDecision.OwnTransfer -> R.string.import_own_transfer
            MerchantDecision.Later -> R.string.import_later
            is MerchantDecision.Categorise -> null
        }
        events?.send(UiEvent.ImportDecision(name, label) {
            _ui.update { state ->
                if (state !is ImportUi.Review || state.decisions[merchantKey] != decision) state
                else state.copy(decisions = if (previous == null) state.decisions - merchantKey else state.decisions + (merchantKey to previous))
            }
        })
    }

    /** Adds a category from the merchant's sheet, shows it in the review, and picks it for the merchant. */
    fun createCategory(merchantKey: String, name: String, klass: com.menash.myself.feature.money.domain.ExpenseClass) {
        val review = _ui.value as? ImportUi.Review ?: return
        val category = com.menash.myself.feature.money.domain.newCategory(name, klass, review.categories, newId) ?: return
        val isNew = review.categories.none { it.id == category.id }
        safeLaunch("createCategory") {
            if (isNew) money.save(MoneyEntry.CategoryEntry(category))
            _ui.update { state ->
                (state as? ImportUi.Review)?.copy(
                    categories = if (isNew) state.categories + category else state.categories,
                ) ?: state
            }
            loadedCategories = if (isNew) loadedCategories + category else loadedCategories
            decide(merchantKey, MerchantDecision.Categorise(category.id))
        }
    }

    /** Writes the lines and teaches the memory. Suggested categories count as chosen unless changed. */
    fun confirm() {
        val review = _ui.value as? ImportUi.Review ?: return
        val decisions = review.review.merchants.mapNotNull { m -> review.decisionFor(m.key)?.let { m.key to it } }.toMap()
        val (plan, updates) = applyDecisions(review.review, decisions)
        _ui.value = ImportUi.Saving
        safeLaunch("applyImport", onError = { _ui.value = review }) {
            transactions.apply(plan)
            // A merchant that asks each time keeps this batch's category on its lines only.
            val remembered = updates.filter { it.ownTransfer || it.merchantKey !in loadedMemory.askEachTime }
            remembered.forEach { transactions.remember(it.merchantKey, it.categoryId, it.ownTransfer) }
            val effectiveMemory = loadedMemory.copy(ownTransfers = loadedMemory.ownTransfers + remembered.filter { it.ownTransfer }.map { it.merchantKey })
            val counts = plan.toWrite.map { it.row.billingMonth }.distinct().associateWith { month ->
                com.menash.myself.feature.money.importer.monthLines(plan.toWrite, month, effectiveMemory, loadedCategories).unassignedCount
            }
            val unassignedByMonth = counts.filterValues { it > 0 }.toSortedMap()
            pickedFiles = emptyList()
            _ui.value = ImportUi.Done(plan.toWrite.size, plan.toDelete.size, remembered.size, unassignedByMonth)

        }
    }

    fun togglePlanLine(key: String) {
        _ui.update { state ->
            if (state !is ImportUi.PlanReview || key !in state.draft.selectableKeys) state
            else state.copy(selected = if (key in state.selected) state.selected - key else state.selected + key)
        }
    }

    /**
     * Writes the selected lines in one batch. The draft is first matched again against the stored plan as it is
     * now and for today's month, so an edit made meanwhile (by a partner, or in another screen) is not overwritten
     * by the copy taken when the review opened. A selected line that is no longer a change is not written.
     */
    fun confirmPlan() {
        val review = _ui.value as? ImportUi.PlanReview ?: return
        if (review.draft.toSave(review.selected).isEmpty()) return
        _ui.value = ImportUi.Saving
        safeLaunch("applyPlanImport", onError = { _ui.value = review }) {
            val fresh = money.observeForImport().first()
            val draft = withContext(io) { review.draft.refreshed(fresh, java.time.YearMonth.from(today()), newId) }
            val selected = review.selected.intersect(draft.selectableKeys)
            val entries = draft.toSave(selected)
            if (entries.isNotEmpty()) money.saveAll(entries)
            pickedFiles = emptyList()
            _ui.value = ImportUi.PlanDone(selected.size, draft.from)
        }
    }

    fun close() {
        if (_ui.value is ImportUi.Done || _ui.value is ImportUi.PlanDone || _ui.value is ImportUi.PlanNoHistory) cancel() else _open.value = false
    }

    fun resume() { if (_ui.value != ImportUi.Idle) _open.value = true }

    fun cancel() {
        loadJob?.cancel()
        pickedFiles = emptyList()
        _ui.value = ImportUi.Idle
        _open.value = false
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
                ImportViewModel(transactions, money, errorReporter, events = events) as T
        }
    }
}
