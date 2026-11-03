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
        vm.showMonth(current.minusMonths(MonthsBack + 1))
        assertEquals(current.minusMonths(3), vm.state.value.money!!.month)
        vm.showMonth(current)
        assertEquals(current, vm.state.value.money!!.month)
    }

    @Test fun pickerShowsClosedImportedAndEmpty() {
        val current = YearMonth.of(2026, 11)
        val lines = ImportFixtures.parse("max_nov.xlsx").rows.map { StoredTransaction(it) }
        val cells = pickerMonths(current, setOf(current.minusMonths(1)), lines)
        assertEquals(MonthCellState.IMPORTED, cells[0].state)
        assertEquals(MonthCellState.CLOSED, cells[1].state)
        assertEquals(MonthCellState.EMPTY, cells[2].state)
        assertEquals(37, cells.size)
    }
}
