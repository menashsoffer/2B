package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.MonthSnapshot
import java.math.BigDecimal
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonthSummaryViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun waitsForDataAndOnlyShowsClosedMonth() {
        val month = YearMonth.of(2026, 10)
        val repo = FakeMoneyRepository(MoneySamples.filled.copy(householdReady = false, closedMonths = emptyList()))
        val vm = MonthSummaryViewModel(repo, MutableStateFlow<BigDecimal?>(null), DefaultErrorReporter())
        vm.open(month)
        assertTrue(vm.ui.value.open)
        assertNull(vm.ui.value.view)
        repo.data.value = repo.data.value.copy(householdReady = true)
        assertNull(vm.ui.value.view)
        val closed = ClosedMonth(month, MonthSnapshot(month, Money(10000), Money(5000)), emptyMap(), emptyMap(), emptyMap(), emptyMap(), month.plusMonths(1).atDay(2))
        repo.data.value = repo.data.value.copy(closedMonths = listOf(closed))
        assertNotNull(vm.ui.value.view)
        assertEquals(Money(5000), vm.ui.value.view!!.left)
        vm.close()
        assertFalse(vm.ui.value.open)
        assertNull(vm.ui.value.view)
    }
}
