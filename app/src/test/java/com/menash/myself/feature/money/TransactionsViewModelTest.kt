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
    fun thisLineOnly_keepsTheOtherLinesOfTheMerchant() {
        val repo = repo("max_nov.xlsx")
        val vm = viewModel(repo)
        vm.open(november)
        val coffees = vm.ui.value.lines!!.lines.filter { it.row.description == "קפה גרג" }
        assertTrue(coffees.size > 1)
        vm.decide(coffees.first().id, MerchantDecision.Categorise("current-9"), thisLineOnly = true)
        assertEquals("current-9", repo.lines.value.single { it.id == coffees.first().id }.categoryId)
        assertNull(repo.memory.value.merchants.categories[ImportIds.merchantKey("קפה גרג")])
        assertTrue(vm.ui.value.lines!!.lines.single { it.id == coffees[1].id }.unassigned)
    }

    @Test
    fun askEachTime_pinsFollowersAndNeverLearnsAgain() {
        val repo = repo("max_nov.xlsx")
        val vm = viewModel(repo)
        vm.open(november)
        val coffees = vm.ui.value.lines!!.lines.filter { it.row.description == "קפה גרג" }
        vm.decide(coffees.first().id, MerchantDecision.Categorise("current-9"))
        val key = ImportIds.merchantKey("קפה גרג")
        assertNull(repo.lines.value.single { it.id == coffees[1].id }.categoryId)

        vm.setAskEachTime(coffees[1].id, true)
        // The line that followed the memory keeps the category as its own; nothing moves.
        assertEquals("current-9", repo.lines.value.single { it.id == coffees[1].id }.categoryId)
        assertTrue(key in repo.memory.value.askEachTime)
        assertNull(repo.memory.value.merchants.categories[key])

        vm.decide(coffees[1].id, MerchantDecision.Categorise("current-2"))
        assertEquals("current-2", repo.lines.value.single { it.id == coffees[1].id }.categoryId)
        assertEquals("current-9", repo.lines.value.single { it.id == coffees.first().id }.categoryId)
        assertNull(repo.memory.value.merchants.categories[key])
    }

    @Test
    fun newCategoryForOneLine_reusesAnExistingNameInTheSameClass() {
        val repo = repo("max_nov.xlsx")
        val money = FakeMoneyRepository(MoneySamples.filled)
        val vm = TransactionsViewModel(repo, money, DefaultErrorReporter(), newId = { "cat-new" })
        vm.open(november)
        val coffee = vm.ui.value.lines!!.lines.first { it.row.description == "קפה גרג" }
        vm.createCategory(coffee.id, "בתי קפה", com.menash.myself.feature.money.domain.ExpenseClass.CURRENT, thisLineOnly = true)
        assertEquals("cat-new", repo.lines.value.single { it.id == coffee.id }.categoryId)
        assertNull(repo.memory.value.merchants.categories[ImportIds.merchantKey("קפה גרג")])
        assertEquals(1, money.saved.size)

        val existing = MoneySamples.filled.categories.first()
        vm.createCategory(coffee.id, " ${existing.name.uppercase()} ", existing.klass)
        assertEquals(1, money.saved.size)
        assertEquals(existing.id, repo.lines.value.single { it.id == coffee.id }.categoryId)
    }

    @Test
    fun aliasIsShownButMatchingKeepsTheBankName() {
        val repo = repo("max_nov.xlsx")
        val vm = viewModel(repo)
        vm.open(november)
        val coffee = vm.ui.value.lines!!.lines.first { it.row.description == "קפה גרג" }
        vm.setAlias(coffee.id, "  קפה של הבוקר ")
        val key = ImportIds.merchantKey("קפה גרג")
        assertEquals("קפה של הבוקר", repo.memory.value.aliases[key])
        assertEquals("קפה של הבוקר", vm.ui.value.memory.displayName("קפה גרג"))
        assertEquals("קפה גרג", coffee.row.description)
        vm.decide(coffee.id, MerchantDecision.Categorise("current-9"))
        assertEquals("current-9", repo.memory.value.merchants.categories[key])
        vm.setAlias(coffee.id, "")
        assertNull(repo.memory.value.aliases[key])
        assertEquals("קפה גרג", vm.ui.value.memory.displayName("קפה גרג"))
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
    fun anIncomeLine_isLinkedToItsSource_andThePayerRemembered() {
        val repo = repo("hapoalim_oct.xlsx")
        val vm = viewModel(repo)
        vm.open(october)
        assertEquals(MoneySamples.filled.items.incomes, vm.ui.value.incomes)
        val allowance = vm.ui.value.lines!!.lines.single { it.income && it.row.description.contains("קצבת ילדים") }
        vm.pick(allowance.id)
        vm.linkIncome(allowance.id, MoneySamples.salary.id)
        assertNull(vm.ui.value.query.picking)
        assertEquals(MoneySamples.salary.id, repo.lines.value.single { it.id == allowance.id }.categoryId)
        assertEquals(MoneySamples.salary.id, repo.memory.value.merchants.categories[ImportIds.merchantKey(allowance.row.description)])
        assertEquals(MoneySamples.salary.id, vm.ui.value.lines!!.lines.single { it.id == allowance.id }.incomeId)
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
