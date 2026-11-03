package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.domain.hasSuspectBalances
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class MonthCloseViewModelTest {

    private val today = LocalDate.of(2026, 11, 3)
    private val october = YearMonth.of(2026, 10)
    private val bank = Asset("asset-bank", "עו\"ש", "bank", Money(3_000_000), AssetKind.ASSET, true, LocalDate.of(2026, 11, 1))
    private val data = MoneyData(householdReady = true, assets = listOf(bank), categories = defaultCategories())

    private fun line(id: String, date: LocalDate, card: String = "1234") = StoredTransaction(
        StatementRow(id, Source.MAX, card, date, date, "שופרסל", charge = Money(25_000)),
        categoryId = "current-1",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repo: FakeMoneyRepository, lines: List<StoredTransaction>): MonthCloseViewModel {
        val transactions = FakeTransactionRepository()
        transactions.lines.value = lines
        return MonthCloseViewModel(repo, transactions, flowOf(null), { today }, DefaultErrorReporter())
    }

    @Test
    fun opensSelectedPastMonthAndUsesDefaultForCurrentOrFuture() {
        val vm = viewModel(FakeMoneyRepository(data), listOf(line("a", LocalDate.of(2026, 10, 5))))
        vm.open(october.minusMonths(2))
        assertEquals(october.minusMonths(2), vm.ui.value.view!!.month)
        vm.open(YearMonth.from(today))
        assertEquals(october, vm.ui.value.view!!.month)
        vm.open(YearMonth.from(today).plusMonths(1))
        assertEquals(october, vm.ui.value.view!!.month)
    }

    @Test
    fun opensOnLastMonth_closes_andStaysOnIt() {
        val repo = FakeMoneyRepository(data)
        val vm = viewModel(repo, listOf(line("a", LocalDate.of(2026, 10, 5))))
        assertNull(vm.ui.value.view)
        vm.open()
        val view = vm.ui.value.view!!
        assertEquals(october, view.month)
        assertTrue(view.canClose)

        vm.closeMonth()
        val saved = repo.saved.single() as MoneyEntry.ClosedMonthEntry
        assertEquals(Money(25_000), saved.value.snapshot.actualExpenses)
        assertEquals(today, saved.value.closedOn)
        repo.data.value = data.copy(closedMonths = listOf(saved.value))
        assertEquals(october, vm.ui.value.view!!.month)
        assertEquals(saved.value, vm.ui.value.view!!.existing)
    }

    @Test
    fun lastMonth_keepsTodaysBalancesWithoutAStep() {
        val vm = viewModel(FakeMoneyRepository(data), listOf(line("a", LocalDate.of(2026, 10, 5))))
        vm.open()
        assertEquals(october, vm.ui.value.view!!.month)
        assertFalse(vm.ui.value.showBalancesStep)
        assertEquals(mapOf(bank.id to bank.balance), vm.ui.value.view!!.preview.balances)
    }

    @Test
    fun historicalBalancesAreExplicitAndResetBetweenMonths() {
        val repo = FakeMoneyRepository(data)
        val vm = viewModel(repo, listOf(line("a", LocalDate.of(2026, 9, 5))))
        vm.open()
        vm.previousMonth()
        vm.previousMonth()
        assertEquals(YearMonth.of(2026, 9), vm.ui.value.view!!.month)
        assertTrue(vm.ui.value.showBalancesStep)
        assertTrue(vm.ui.value.balanceDraft.values.isEmpty())
        assertTrue(vm.ui.value.view!!.preview.balances.isEmpty())
        vm.enterBalances(true)
        vm.setBalance(bank.id, "12.34")
        assertEquals(mapOf(bank.id to Money(1234)), vm.ui.value.view!!.preview.balances)
        vm.setBalance(bank.id, "-1")
        vm.closeMonth()
        assertTrue(repo.saved.isEmpty())
        vm.nextMonth()
        assertFalse(vm.ui.value.showBalancesStep)
        assertEquals(mapOf(bank.id to bank.balance), vm.ui.value.view!!.preview.balances)
        vm.previousMonth()
        assertTrue(vm.ui.value.view!!.preview.balances.isEmpty())
    }

    @Test
    fun enteredBalancesSaveOnlyFilledAssetAndLiabilityIds() {
        val loan = bank.copy(id = "loan", kind = AssetKind.LIABILITY)
        val untouched = bank.copy(id = "untouched")
        val repo = FakeMoneyRepository(data.copy(assets = listOf(bank, loan, untouched)))
        val vm = viewModel(repo, listOf(line("a", LocalDate.of(2026, 9, 5))))
        vm.open()
        vm.previousMonth()
        vm.previousMonth()
        vm.enterBalances(true)
        vm.setBalance(bank.id, "0")
        vm.setBalance(loan.id, "100")
        vm.closeMonth()
        val saved = (repo.saved.single() as MoneyEntry.ClosedMonthEntry).value
        assertEquals(mapOf(bank.id to Money.ZERO), saved.balances)
        assertEquals(mapOf(loan.id to Money(10000)), saved.liabilities)
        assertTrue(saved.balancesEntered)
        assertFalse(saved.hasSuspectBalances())
    }

    @Test
    fun explicitRepairPreservesEveryOtherStoredField() {
        val original = closeView(data, october, today, null, emptyList(),
            com.menash.myself.feature.money.importer.ImportMemory()).preview.copy(
                balances = mapOf(bank.id to bank.balance), closedOn = LocalDate.of(2026, 12, 1))
        val repo = FakeMoneyRepository(data.copy(closedMonths = listOf(original)))
        val vm = viewModel(repo, emptyList())
        vm.open()
        vm.previousMonth()
        assertTrue(repo.saved.isEmpty())
        vm.markBalancesUnknown()
        assertEquals(original.copy(balances = emptyMap(), liabilities = emptyMap()),
            (repo.saved.single() as MoneyEntry.ClosedMonthEntry).value)
    }

    @Test
    fun reclosing_asksFirst() {
        val repo = FakeMoneyRepository(data)
        val vm = viewModel(repo, listOf(line("a", LocalDate.of(2026, 10, 5))))
        vm.open()
        vm.closeMonth()
        repo.data.value = data.copy(closedMonths = listOf((repo.saved.single() as MoneyEntry.ClosedMonthEntry).value))

        vm.closeMonth()
        assertTrue(vm.ui.value.confirmReclose)
        assertEquals(1, repo.saved.size)
        vm.cancelReclose()
        assertFalse(vm.ui.value.confirmReclose)
        vm.closeMonth()
        vm.closeMonth()
        assertEquals(2, repo.saved.size)
    }

    @Test
    fun missingCard_blocksUntilMarkedEmpty() {
        val repo = FakeMoneyRepository(data)
        val vm = viewModel(repo, listOf(line("a", LocalDate.of(2026, 9, 5), card = "9876"), line("b", LocalDate.of(2026, 10, 5))))
        vm.open()
        val missing = vm.ui.value.view!!.missingSources.single()
        vm.closeMonth()
        assertTrue(repo.saved.isEmpty())
        vm.markNoLines(missing)
        assertTrue(vm.ui.value.view!!.canClose)
    }

    @Test
    fun arrows_stopAtThisMonth() {
        val vm = viewModel(FakeMoneyRepository(data), listOf(line("a", LocalDate.of(2026, 10, 5))))
        vm.open()
        vm.nextMonth()
        assertEquals(YearMonth.of(2026, 11), vm.ui.value.view!!.month)
        assertFalse(vm.ui.value.view!!.canClose)
        vm.nextMonth()
        assertEquals(YearMonth.of(2026, 11), vm.ui.value.view!!.month)
        vm.previousMonth()
        vm.previousMonth()
        assertEquals(YearMonth.of(2026, 9), vm.ui.value.view!!.month)
        vm.close()
        assertFalse(vm.ui.value.open)
    }
}
