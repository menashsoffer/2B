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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTransactionRepository : TransactionRepository {
    val lines = MutableStateFlow<List<StoredTransaction>>(emptyList())
    val memory = MutableStateFlow(ImportMemory())
    val applied = mutableListOf<ImportPlan>()
    var failApply = false

    override fun observeTransactions(): Flow<List<StoredTransaction>> = lines
    override fun observeMemory(): Flow<ImportMemory> = memory

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
        memory.value = ImportMemory(
            if (categoryId != null) MerchantMemory(m.merchants.categories + (merchantKey to categoryId)) else m.merchants,
            if (ownTransfer) m.ownTransfers + merchantKey else m.ownTransfers,
        )
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
    fun nothingIsWrittenBeforeConfirm() {
        val repo = FakeTransactionRepository()
        val vm = viewModel(repo)
        vm.load(listOf(file("max_nov.xlsx")))
        val review = vm.ui.value as ImportUi.Review
        assertEquals(7, review.review.plan.newCount)
        assertTrue(repo.applied.isEmpty())
        vm.close()
        assertEquals(ImportUi.Idle, vm.ui.value)
        assertTrue(repo.applied.isEmpty())
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
        assertEquals(ImportUi.Done(7, 0), vm.ui.value)
        assertEquals(7, repo.lines.value.size)
        assertEquals(2, repo.lines.value.count { it.categoryId == "current-9" })
        // The suggested category counts as chosen: Shufersal is learnt as "מזון".
        assertEquals("current-1", repo.memory.value.merchants.categories[ImportIds.merchantKey("שופרסל דיל 123")])
        assertEquals("current-9", repo.memory.value.merchants.categories[coffee])
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
