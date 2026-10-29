package com.menash.myself.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.ui.BaseViewModel
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The import flow: nothing is written before [ImportUi.Review] is confirmed. */
sealed interface ImportUi {
    object Idle : ImportUi
    object Reading : ImportUi

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
    money: MoneyRepository,
    errorReporter: ErrorReporter,
    private val io: CoroutineDispatcher = Dispatchers.Default,
) : BaseViewModel(errorReporter) {

    private val stored: StateFlow<List<StoredTransaction>> =
        transactions.observeTransactions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val memory: StateFlow<ImportMemory> =
        transactions.observeMemory().stateIn(viewModelScope, SharingStarted.Eagerly, ImportMemory())
    private val categories: StateFlow<List<Category>> =
        money.observe().map { it.categories }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _ui = MutableStateFlow<ImportUi>(ImportUi.Idle)
    val ui: StateFlow<ImportUi> = _ui.asStateFlow()

    /** Reads and compares the files with what is stored. Parsing runs off the main thread. */
    fun load(files: List<ImportFile>) {
        if (files.isEmpty()) return
        _ui.value = ImportUi.Reading
        viewModelScope.launch {
            val existing = stored.value.associateBy { it.id }
            val review = withContext(io) { reviewImport(files, existing, memory.value, categories.value) }
            _ui.value = ImportUi.Review(review, categories.value)
        }
    }

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

    /** Writes the lines and teaches the memory. Suggested categories count as chosen unless changed. */
    fun confirm() {
        val review = _ui.value as? ImportUi.Review ?: return
        val decisions = review.review.merchants.mapNotNull { m -> review.decisionFor(m.key)?.let { m.key to it } }.toMap()
        val (plan, updates) = applyDecisions(review.review, decisions)
        _ui.value = ImportUi.Saving
        safeLaunch("applyImport", onError = { _ui.value = review }) {
            transactions.apply(plan)
            updates.forEach { transactions.remember(it.merchantKey, it.categoryId, it.ownTransfer) }
            _ui.value = ImportUi.Done(plan.toWrite.size, plan.toDelete.size)
        }
    }

    fun close() {
        _ui.value = ImportUi.Idle
    }

    companion object {
        fun factory(
            transactions: TransactionRepository,
            money: MoneyRepository,
            errorReporter: ErrorReporter,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ImportViewModel(transactions, money, errorReporter) as T
        }
    }
}
