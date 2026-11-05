package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.StoredTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class MonthPickerTest {
    @Before fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun showMonthRejectsOutOfBounds() {
        val vm = MoneyViewModel(FakeMoneyRepository(MoneySamples.filled), MutableStateFlow(MoneySamples.rate),
            { MoneySamples.today }, DefaultErrorReporter())
        val current = MoneySamples.month
        vm.showMonth(current.minusMonths(3))
        assertEquals(current.minusMonths(3), vm.state.value.money!!.month)
        vm.showMonth(current.plusMonths(1))
        vm.showMonth(current.minusMonths(13))
        assertEquals(current.minusMonths(3), vm.state.value.money!!.month)
        vm.showMonth(current)
        assertEquals(current, vm.state.value.money!!.month)
    }

    @Test fun pickerIncludesOldImportedMonths() {
        val current = YearMonth.of(2031, 1)
        val lines = ImportFixtures.parse("max_nov.xlsx").rows.map { StoredTransaction(it) }
        val cells = pickerMonths(current, emptySet(), lines)
        assertEquals(lines.minOf { it.row.billingMonth }, cells.last().month)
    }

    @Test fun oldDataIsReachableByPickerSelectionAndArrows() {
        val current = YearMonth.of(2027, 3)
        val january = YearMonth.of(2024, 1)
        val example = ImportFixtures.parse("max_nov.xlsx").rows.first()
        val line = StoredTransaction(example.copy(date = january.atDay(5), chargeDate = january.atDay(5)))
        val vm = MoneyViewModel(FakeMoneyRepository(MoneySamples.empty), MutableStateFlow(MoneySamples.rate),
            { current.atDay(15) }, DefaultErrorReporter(), transactions = MutableStateFlow(listOf(line)))
        assertEquals(january, vm.state.value.months.last().month)
        vm.showMonth(january)
        assertEquals(january, vm.state.value.money!!.month)
        vm.showMonth(current)
        repeat(50) { vm.showPreviousMonth() }
        assertEquals(january, vm.state.value.money!!.month)
        assertEquals(false, vm.state.value.money!!.canGoBack)
        assertEquals(current.minusMonths(12), earliestMonth(current, emptyList(), emptySet()))
    }

    @Test fun pickerShowsClosedImportedAndEmpty() {
        val current = YearMonth.of(2026, 11)
        val lines = ImportFixtures.parse("max_nov.xlsx").rows.map { StoredTransaction(it) }
        val cells = pickerMonths(current, setOf(current.minusMonths(1)), lines)
        assertEquals(MonthCellState.IMPORTED, cells[0].state)
        assertEquals(MonthCellState.CLOSED, cells[1].state)
        assertEquals(MonthCellState.EMPTY, cells[2].state)
        assertEquals(13, cells.size)
    }
}
