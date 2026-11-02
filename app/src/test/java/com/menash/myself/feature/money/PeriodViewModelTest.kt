package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeriodViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun januaryOpensOnLastYearAndReopeningResetsSelection() {
        val repo = FakeMoneyRepository(MoneySamples.filled)
        val vm = PeriodViewModel(repo, MutableStateFlow<BigDecimal?>(null), { LocalDate.of(2026, 1, 2) }, DefaultErrorReporter())
        assertNull(vm.ui.value.view)
        vm.open()
        assertEquals(PeriodPreset.LAST_YEAR, vm.ui.value.view!!.preset)
        vm.select(PeriodPreset.LAST_3)
        assertEquals(PeriodPreset.LAST_3, vm.ui.value.view!!.preset)
        vm.close()
        assertNull(vm.ui.value.view)
        vm.open()
        assertEquals(PeriodPreset.LAST_YEAR, vm.ui.value.view!!.preset)
    }

    @Test fun waitsForHouseholdBeforeShowingView() {
        val repo = FakeMoneyRepository(MoneySamples.filled.copy(householdReady = false))
        val vm = PeriodViewModel(repo, MutableStateFlow<BigDecimal?>(null), { LocalDate.of(2026, 10, 1) }, DefaultErrorReporter())
        vm.open()
        assertTrue(vm.ui.value.open)
        assertNull(vm.ui.value.view)
        repo.data.value = repo.data.value.copy(householdReady = true)
        assertEquals(PeriodPreset.THIS_YEAR, vm.ui.value.view!!.preset)
    }
}
