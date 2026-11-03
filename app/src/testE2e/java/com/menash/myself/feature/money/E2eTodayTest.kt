package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.time.TodayProvider
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
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class E2eTodayTest {
    @Before fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun cleanup() = Dispatchers.resetMain()

    @Test fun overrideDeterminesCurrentMonth() {
        TodayProvider.setForE2e(LocalDate.of(2026, 10, 20))
        val vm = MoneyViewModel(FakeMoneyRepository(), MutableStateFlow(null), TodayProvider::today, DefaultErrorReporter())
        assertEquals(YearMonth.of(2026, 10), vm.state.value.money!!.current)
        TodayProvider.setForE2e(LocalDate.of(2026, 11, 3))
        val next = MoneyViewModel(FakeMoneyRepository(), MutableStateFlow(null), TodayProvider::today, DefaultErrorReporter())
        assertEquals(YearMonth.of(2026, 11), next.state.value.money!!.current)
    }
}
