package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.LineFilter
import com.menash.myself.feature.money.importer.MerchantDecision
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.search.LineSearchScope
import com.menash.myself.feature.money.search.SearchPeriod
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
import java.time.LocalDate
import java.time.YearMonth

/** Search across months and the search box inside one month, on the transactions view model. */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsSearchTest {

    private val october = YearMonth.of(2026, 10)
    private val november = YearMonth.of(2026, 11)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val repo = FakeTransactionRepository().apply {
        lines.value = listOf("max_nov.xlsx", "hapoalim_oct.xlsx").flatMap { ImportFixtures.parse(it).rows }.map { StoredTransaction(it) }
    }

    private fun viewModel() = TransactionsViewModel(
        repo, FakeMoneyRepository(MoneySamples.filled), DefaultErrorReporter(), today = { LocalDate.of(2026, 11, 3) },
    )

    @Test
    fun searchOpensWithoutAMonth_andCloses() {
        val vm = viewModel()
        vm.openSearch()
        assertTrue(vm.ui.value.open)
        assertTrue(vm.ui.value.searching)
        assertNull(vm.ui.value.lines)
        assertEquals(repo.lines.value.size, vm.ui.value.results!!.count)
        vm.close()
        assertFalse(vm.ui.value.open)
    }

    @Test
    fun textNarrowsAcrossMonths() {
        val vm = viewModel()
        vm.openSearch()
        vm.setText("שופרסל")
        val results = vm.ui.value.results!!
        assertTrue(results.count > 0)
        assertTrue(results.lines.all { it.row.description.contains("שופרסל") })
    }

    @Test
    fun quickPeriodLimitsMonths() {
        val vm = viewModel()
        vm.openSearch()
        vm.selectPeriod(SearchPeriod.THIS_MONTH)
        assertEquals(SearchPeriod.THIS_MONTH, vm.ui.value.query.search!!.period)
        assertTrue(vm.ui.value.results!!.months.all { it.month == november })
        vm.selectPeriod(SearchPeriod.ALL)
        assertTrue(vm.ui.value.results!!.months.any { it.month == october })
    }

    @Test
    fun aRangeFromThePeriodScreen_listsOnlyThoseMonths() {
        val vm = viewModel()
        vm.openRange(october, october)
        val search = vm.ui.value.query.search!!
        assertEquals(SearchPeriod.CUSTOM, search.period)
        val results = vm.ui.value.results!!
        assertTrue(results.count > 0)
        assertTrue(results.months.all { it.month == october })
    }

    @Test
    fun aMonthFromResults_returnsToTheSameSearchOnBack() {
        val vm = viewModel()
        vm.openSearch(LineSearchScope(onlyUnassigned = true), text = "קפה")
        vm.openMonthFromSearch(november)
        assertEquals(november, vm.ui.value.query.month)
        assertFalse(vm.ui.value.searching)
        vm.close()
        assertTrue(vm.ui.value.searching)
        assertEquals("קפה", vm.ui.value.query.text)
        assertTrue(vm.ui.value.query.search!!.onlyUnassigned)
    }

    @Test
    fun aLineFoundBySearch_canBeCategorised() {
        val vm = viewModel()
        vm.openSearch(text = "קפה גרג")
        val coffee = vm.ui.value.results!!.lines.first()
        vm.pick(coffee.id)
        vm.decide(coffee.id, MerchantDecision.Categorise("current-9"))
        assertEquals("current-9", repo.lines.value.first { it.id == coffee.id }.categoryId)
    }

    @Test
    fun searchBoxInsideAMonth_combinesWithTheFilter() {
        val vm = viewModel()
        vm.open(november, LineFilter.UNASSIGNED)
        val all = vm.ui.value.shown!!.size
        vm.setText("קפה")
        val shown = vm.ui.value.shown!!
        assertTrue(shown.size in 1 until all)
        assertTrue(shown.all { it.unassigned && it.row.description.contains("קפה") })
    }
}
