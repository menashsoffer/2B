package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.ImportIds
import com.menash.myself.feature.money.importer.LineFilter
import com.menash.myself.feature.money.importer.MerchantDecision
import com.menash.myself.feature.money.importer.RowKind
import com.menash.myself.feature.money.importer.StoredTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val october = YearMonth.of(2026, 10)
    private val november = YearMonth.of(2026, 11)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repo(vararg files: String) = FakeTransactionRepository().apply {
        lines.value = files.flatMap { ImportFixtures.parse(it).rows }.map { StoredTransaction(it) }
    }

    private fun viewModel(repo: FakeTransactionRepository) =
        TransactionsViewModel(repo, FakeMoneyRepository(MoneySamples.filled), DefaultErrorReporter())

    @Test
    fun opensOnAMonthWithAFilter_andCloses() {
        val vm = viewModel(repo("max_nov.xlsx"))
        assertFalse(vm.ui.value.open)
        vm.open(november, LineFilter.UNASSIGNED)
        assertEquals(LineFilter.UNASSIGNED, vm.ui.value.query.filter)
        assertEquals(7, vm.ui.value.lines!!.lines.size)
        vm.showMonth(october)
        assertTrue(vm.ui.value.lines!!.lines.isEmpty())
        vm.close()
        assertFalse(vm.ui.value.open)
        assertNull(vm.ui.value.lines)
    }

    @Test
    fun categoryForALine_isKeptAndTheMerchantLearnt() {
        val repo = repo("max_nov.xlsx")
        val vm = viewModel(repo)
        vm.open(november)
        val coffee = vm.ui.value.lines!!.lines.first { it.row.description == "קפה גרג" }
        vm.pick(coffee.id)
        assertEquals(coffee.id, vm.ui.value.query.picking)
        vm.decide(coffee.id, MerchantDecision.Categorise("current-9"))
        assertNull(vm.ui.value.query.picking)
        assertEquals("current-9", repo.lines.value.single { it.id == coffee.id }.categoryId)
        assertEquals("current-9", repo.memory.value.merchants.categories[ImportIds.merchantKey("קפה גרג")])
        // The second coffee line follows through the memory.
        assertTrue(vm.ui.value.lines!!.lines.filter { it.row.description == "קפה גרג" }.all { it.categoryId == "current-9" && !it.unassigned })
    }

    @Test
    fun cardBillCategoryIsNotLearnt_transferCanBeOwn() {
        val repo = repo("hapoalim_oct.xlsx")
        val vm = viewModel(repo)
        vm.open(october)
        val lines = vm.ui.value.lines!!.lines
        val bill = lines.first { it.kind == RowKind.CARD_TOTAL }
        vm.decide(bill.id, MerchantDecision.Categorise("current-1"))
        assertEquals("current-1", repo.lines.value.single { it.id == bill.id }.categoryId)
        assertTrue(repo.memory.value.merchants.categories.isEmpty())

        val transfer = lines.first { it.kind == RowKind.TRANSFER && it.counted }
        vm.decide(transfer.id, MerchantDecision.OwnTransfer)
        assertTrue(ImportIds.merchantKey(transfer.row.description) in repo.memory.value.ownTransfers)
        assertFalse(vm.ui.value.lines!!.lines.single { it.id == transfer.id }.counted)
    }

    @Test
    fun onlyAPendingLineIsRemoved_afterTheQuestion() {
        val repo = repo("cal_nov.xlsm")
        val vm = viewModel(repo)
        vm.open(october, LineFilter.PENDING)
        val pending = vm.ui.value.lines!!.filtered(LineFilter.PENDING).single()
        vm.askRemove(pending.id)
        assertEquals(pending.id, vm.ui.value.query.removing)
        vm.askRemove(null)
        assertTrue(repo.lines.value.any { it.id == pending.id })
        vm.askRemove(pending.id)
        vm.remove(pending.id)
        assertNull(vm.ui.value.query.removing)
        assertFalse(repo.lines.value.any { it.id == pending.id })

        vm.showMonth(november)
        val counted = vm.ui.value.lines!!.lines.first { it.counted }
        val before = repo.lines.value.size
        vm.remove(counted.id)
        assertEquals(before, repo.lines.value.size)
    }
}
