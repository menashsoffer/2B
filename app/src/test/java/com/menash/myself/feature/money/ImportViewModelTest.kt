package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.feature.money.data.TransactionRepository
import com.menash.myself.feature.money.importer.ImportFile
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.ImportIds
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.ImportPlan
import com.menash.myself.feature.money.importer.MerchantDecision
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.domain.MerchantMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.menash.myself.core.ui.UiEvent
import com.menash.myself.core.ui.UiEvents
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTransactionRepository : TransactionRepository {
    val lines = MutableStateFlow<List<StoredTransaction>>(emptyList())
    val memory = MutableStateFlow(ImportMemory())
    val sourceClosures = MutableStateFlow<Map<StatementSource, java.time.YearMonth>>(emptyMap())
    val applied = mutableListOf<ImportPlan>()
    var failApply = false

    override fun observeTransactions(): Flow<List<StoredTransaction>> = lines
    override fun observeMemory(): Flow<ImportMemory> = memory

    override fun observeSourceClosures() = sourceClosures
    override suspend fun setSourceClosedFrom(source: StatementSource, month: java.time.YearMonth?) {
        sourceClosures.value = if (month == null) sourceClosures.value - source else sourceClosures.value + (source to month)
    }

    override suspend fun apply(plan: ImportPlan) {
        if (failApply) throw IllegalStateException("offline")
        applied += plan
        lines.value = ((lines.value.associateBy { it.id } + plan.toWrite.associateBy { it.id }) - plan.toDelete.toSet()).values.toList()
    }

    override suspend fun setCategory(transactionId: String, categoryId: String?) {
        lines.value = lines.value.map { if (it.id == transactionId) it.copy(categoryId = categoryId) else it }
    }

    override suspend fun delete(transactionId: String) {
        lines.value = lines.value.filterNot { it.id == transactionId }
    }

    override suspend fun remember(merchantKey: String, categoryId: String?, ownTransfer: Boolean) {
        val m = memory.value
        memory.value = m.copy(
            merchants = if (categoryId != null) MerchantMemory(m.merchants.categories + (merchantKey to categoryId)) else m.merchants,
            ownTransfers = if (ownTransfer) m.ownTransfers + merchantKey else m.ownTransfers,
        )
    }

    override suspend fun setAlias(merchantKey: String, alias: String?) {
        val m = memory.value
        memory.value = m.copy(aliases = if (alias.isNullOrBlank()) m.aliases - merchantKey else m.aliases + (merchantKey to alias.trim()))
    }

    override suspend fun setAskEachTime(merchantKey: String, ask: Boolean) {
        val m = memory.value
        memory.value = if (ask) m.copy(askEachTime = m.askEachTime + merchantKey, merchants = MerchantMemory(m.merchants.categories - merchantKey))
        else m.copy(askEachTime = m.askEachTime - merchantKey)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun file(name: String) = ImportFile(name, ImportFixtures.bytes(name))

    private fun viewModel(repo: FakeTransactionRepository) =
        ImportViewModel(repo, FakeMoneyRepository(MoneySamples.filled), DefaultErrorReporter(), io = dispatcher)

    @Test
    fun waitsForAllSourcesBeforeReviewing() {
        val lines = MutableSharedFlow<List<StoredTransaction>>(replay = 1)
        val memory = MutableSharedFlow<ImportMemory>(replay = 1)
        val data = MutableSharedFlow<com.menash.myself.feature.money.data.MoneyData>(replay = 1)
        val repo = object : TransactionRepository by FakeTransactionRepository() {
            override fun observeTransactionsForImport(): Flow<List<StoredTransaction>> = lines
            override fun observeMemoryForImport(): Flow<ImportMemory> = memory
        }
        val money = object : com.menash.myself.feature.money.data.MoneyRepository by FakeMoneyRepository() {
            override fun observeForImport(): Flow<com.menash.myself.feature.money.data.MoneyData> = data
        }
        val vm = ImportViewModel(repo, money, DefaultErrorReporter(), io = dispatcher)
        vm.load(listOf(file("max_nov.xlsx")))
        assertEquals(ImportUi.Reading, vm.ui.value)
        lines.tryEmit(emptyList())
        memory.tryEmit(ImportMemory())
        assertEquals(ImportUi.Reading, vm.ui.value)
        data.tryEmit(MoneySamples.filled)
        assertEquals(7, (vm.ui.value as ImportUi.Review).review.plan.newCount)
    }

    @Test
    fun delayedStoredLinesDetectDuplicate() {
        val seed = viewModel(FakeTransactionRepository())
        seed.load(listOf(file("max_nov.xlsx")))
        val stored = (seed.ui.value as ImportUi.Review).review.plan.toWrite
        val lines = MutableSharedFlow<List<StoredTransaction>>(replay = 1)
        val repo = object : TransactionRepository by FakeTransactionRepository() {
            override fun observeTransactionsForImport(): Flow<List<StoredTransaction>> = lines
        }
        val vm = ImportViewModel(repo, FakeMoneyRepository(MoneySamples.filled), DefaultErrorReporter(), io = dispatcher)
        vm.load(listOf(file("max_nov.xlsx")))
        assertEquals(ImportUi.Reading, vm.ui.value)
        lines.tryEmit(stored)
        assertTrue((vm.ui.value as ImportUi.Review).review.nothingToSave)
    }

    @Test
    fun failedSourceCanRetryTheSameFiles() {
        var fail = true
        val delegate = FakeTransactionRepository()
        val repo = object : TransactionRepository by delegate {
            override fun observeTransactionsForImport(): Flow<List<StoredTransaction>> = flow {
                if (fail) throw IllegalStateException("offline")
                emit(emptyList())
            }
        }
        val vm = ImportViewModel(repo, FakeMoneyRepository(MoneySamples.filled), DefaultErrorReporter(), io = dispatcher)
        vm.load(listOf(file("max_nov.xlsx")))
        assertEquals(ImportUi.Error, vm.ui.value)
        fail = false
        vm.retry()
        assertEquals(7, (vm.ui.value as ImportUi.Review).review.plan.newCount)
    }

    @Test
    fun nothingIsWrittenBeforeConfirm() {
        val repo = FakeTransactionRepository()
        val vm = viewModel(repo)
        vm.load(listOf(file("max_nov.xlsx")))
        val review = vm.ui.value as ImportUi.Review
        assertEquals(7, review.review.plan.newCount)
        assertTrue(repo.applied.isEmpty())
        vm.close()
        assertTrue(vm.ui.value is ImportUi.Review)
        vm.resume()
        assertTrue(vm.open.value)
        vm.cancel()
        assertEquals(ImportUi.Idle, vm.ui.value)
        assertTrue(repo.applied.isEmpty())
    }

    @Test
    fun undoRestoresPreviousDecisionAndReviewSurvivesLeaving() = runTest {
        val events = UiEvents()
        val vm = ImportViewModel(FakeTransactionRepository(), FakeMoneyRepository(MoneySamples.filled), DefaultErrorReporter(), io = dispatcher, events = events)
        vm.load(listOf(file("max_nov.xlsx")))
        val coffee = (vm.ui.value as ImportUi.Review).review.merchants.single { it.name == "קפה גרג" }.key
        vm.decide(coffee, MerchantDecision.Categorise("current-1"))
        var event: UiEvent.ImportDecision? = null
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { event = events.events.first() as UiEvent.ImportDecision }
        vm.decide(coffee, MerchantDecision.Categorise("current-9"))
        event!!.undo()
        vm.close()
        vm.resume()
        assertEquals(MerchantDecision.Categorise("current-1"), (vm.ui.value as ImportUi.Review).decisionFor(coffee))
        collector.cancel()
    }

    @Test
    fun confirmWritesLinesAndLearnsMerchants() {
        val repo = FakeTransactionRepository()
        val vm = viewModel(repo)
        vm.load(listOf(file("max_nov.xlsx")))
        val coffee = (vm.ui.value as ImportUi.Review).review.merchants.single { it.name == "קפה גרג" }.key
        vm.openPicker(coffee)
        assertEquals(coffee, (vm.ui.value as ImportUi.Review).picking)
        vm.decide(coffee, MerchantDecision.Categorise("current-9"))
        assertEquals(null, (vm.ui.value as ImportUi.Review).picking)
        vm.confirm()
        assertEquals(7, (vm.ui.value as ImportUi.Done).written)
        assertTrue((vm.ui.value as ImportUi.Done).remembered >= 2)
        assertEquals(7, repo.lines.value.size)
        assertEquals(2, repo.lines.value.count { it.categoryId == "current-9" })
        // The suggested category counts as chosen: Shufersal is learnt as "מזון".
        assertEquals("current-1", repo.memory.value.merchants.categories[ImportIds.merchantKey("שופרסל דיל 123")])
        assertEquals("current-9", repo.memory.value.merchants.categories[coffee])
    }

    @Test
    fun rememberedMerchantStartsDecidedAndCanBeChanged() {
        val repo = FakeTransactionRepository()
        val key = ImportIds.merchantKey("שופרסל דיל 123")
        repo.memory.value = ImportMemory(merchants = MerchantMemory(mapOf(key to "current-9")))
        val vm = viewModel(repo)
        vm.load(listOf(file("max_nov.xlsx")))
        val review = vm.ui.value as ImportUi.Review
        assertEquals(MerchantDecision.Categorise("current-9"), review.decisionFor(key))
        vm.decide(key, MerchantDecision.Categorise("current-1"))
        vm.confirm()
        assertTrue(repo.lines.value.filter { ImportIds.merchantKey(it.row.description) == key }.all { it.categoryId == "current-1" })
        assertEquals("current-1", repo.memory.value.merchants.categories[key])
    }

    @Test
    fun newCategoryFromTheSheet_isSavedShownAndPicked() {
        val money = FakeMoneyRepository(MoneySamples.filled)
        val vm = ImportViewModel(FakeTransactionRepository(), money, DefaultErrorReporter(), io = dispatcher, newId = { "cat-new" })
        vm.load(listOf(file("max_nov.xlsx")))
        val coffee = (vm.ui.value as ImportUi.Review).review.merchants.single { it.name == "קפה גרג" }.key
        vm.openPicker(coffee)
        vm.createCategory(coffee, "  בתי קפה ", com.menash.myself.feature.money.domain.ExpenseClass.CURRENT)
        val review = vm.ui.value as ImportUi.Review
        val saved = (money.saved.single() as com.menash.myself.feature.money.data.MoneyEntry.CategoryEntry).value
        assertEquals("cat-new", saved.id)
        assertEquals("בתי קפה", saved.name)
        assertTrue(review.categories.any { it.id == "cat-new" })
        assertEquals(MerchantDecision.Categorise("cat-new"), review.decisionFor(coffee))
        assertEquals(null, review.picking)
        vm.createCategory(coffee, "   ", com.menash.myself.feature.money.domain.ExpenseClass.CURRENT)
        assertEquals(1, money.saved.size)
    }

    @Test
    fun sameFileAgainAfterSaving_nothingToSave() {
        val repo = FakeTransactionRepository()
        val vm = viewModel(repo)
        vm.load(listOf(file("max_nov.xlsx")))
        vm.confirm()
        vm.close()
        vm.load(listOf(file("max_nov.xlsx")))
        val review = vm.ui.value as ImportUi.Review
        assertTrue(review.review.nothingToSave)
        assertTrue(review.review.merchants.isEmpty())
    }

    @Test
    fun failedSaveReturnsToTheReview() {
        val repo = FakeTransactionRepository().apply { failApply = true }
        val vm = viewModel(repo)
        vm.load(listOf(file("cal_nov.xlsm")))
        val review = vm.ui.value
        vm.confirm()
        assertEquals(review, vm.ui.value)
        assertTrue(repo.lines.value.isEmpty())
    }

    @Test
    fun refusedFileIsShownNotThrown() {
        val vm = viewModel(FakeTransactionRepository())
        vm.load(listOf(file("encrypted.xls"), file("unknown.xlsx")))
        val review = vm.ui.value as ImportUi.Review
        assertEquals(2, review.review.files.count { it.problem != null })
        assertTrue(review.review.nothingToSave)
    }
}
