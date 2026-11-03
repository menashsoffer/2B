package com.menash.myself.feature.money

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
    ) : ImportUi {
        /** The decision shown for a merchant: the user's, else the suggested category. */
        fun decisionFor(key: String): MerchantDecision? = decisions[key]
            ?: review.merchants.firstOrNull { it.key == key }?.suggested?.let { MerchantDecision.Categorise(it) }
    }

    object Saving : ImportUi
    data class Done(val written: Int, val removed: Int) : ImportUi
}

class ImportViewModel(
    private val transactions: TransactionRepository,
    private val money: MoneyRepository,
    errorReporter: ErrorReporter,
    private val io: CoroutineDispatcher = Dispatchers.Default,
    private val events: UiEvents? = null,
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
) : BaseViewModel(errorReporter) {

    private var loadedMemory = ImportMemory()
    private var loadedCategories: List<Category> = emptyList()
    private var pickedFiles: List<ImportFile> = emptyList()
    private var loadJob: Job? = null

    private val _ui = MutableStateFlow<ImportUi>(ImportUi.Idle)
    val ui: StateFlow<ImportUi> = _ui.asStateFlow()

    /** Reads and compares the files with what is stored. Parsing runs off the main thread. */
    fun load(files: List<ImportFile>) {
        if (files.isEmpty()) return
        pickedFiles = files.toList()
        loadJob?.cancel()
        _ui.value = ImportUi.Reading
        loadJob = viewModelScope.launch {
            try {
                val (existing, memory, categories) = combine(
                    transactions.observeTransactions(), transactions.observeMemory(), money.observe(),
                ) { lines, memory, data -> Triple(lines, memory, data.categories) }.first()
                val review = withContext(io) { reviewImport(files, existing.associateBy { it.id }, memory, categories) }
                loadedMemory = memory
                loadedCategories = categories
                _ui.value = ImportUi.Review(review, categories)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException("loadImport", e)
                _ui.value = ImportUi.Error
            }
        }
    }

    fun retry() = load(pickedFiles)

    fun openPicker(merchantKey: String) {
        _ui.update { (it as? ImportUi.Review)?.copy(picking = merchantKey) ?: it }
    }

    fun closePicker() {
        _ui.update { (it as? ImportUi.Review)?.copy(picking = null) ?: it }
    }

    fun decide(merchantKey: String, decision: MerchantDecision) {
        _ui.update { state ->
            (state as? ImportUi.Review)?.copy(decisions = state.decisions + (merchantKey to decision), picking = null) ?: state
        }
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
                    decisions = state.decisions + (merchantKey to MerchantDecision.Categorise(category.id)),
                    picking = null,
                ) ?: state
            }
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
            updates.filter { it.ownTransfer || it.merchantKey !in loadedMemory.askEachTime }
                .forEach { transactions.remember(it.merchantKey, it.categoryId, it.ownTransfer) }
            _ui.value = ImportUi.Done(plan.toWrite.size, plan.toDelete.size)
            val month = plan.toWrite.map { it.row.billingMonth }.distinct().sortedDescending().firstOrNull { month ->
                com.menash.myself.feature.money.importer.monthLines(plan.toWrite, month, loadedMemory, loadedCategories).unassignedCount > 0
            }
            events?.send(UiEvent.ImportDone(plan.toWrite.size, month))
        }
    }

    fun close() {
        loadJob?.cancel()
        pickedFiles = emptyList()
        _ui.value = ImportUi.Idle
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
