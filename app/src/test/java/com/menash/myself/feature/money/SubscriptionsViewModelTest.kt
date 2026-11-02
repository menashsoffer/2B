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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionsViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun viewAppearsOnlyWhenOpenAndHouseholdReady() {
        val repo = FakeMoneyRepository(MoneySamples.filled.copy(householdReady = false))
        val vm = SubscriptionsViewModel(repo, FakeTransactionRepository(), MutableStateFlow<BigDecimal?>(null), { LocalDate.of(2026, 10, 1) }, DefaultErrorReporter())
        assertFalse(vm.ui.value.open)
        assertNull(vm.ui.value.view)
        vm.open()
        assertTrue(vm.ui.value.open)
        assertNull(vm.ui.value.view)
        repo.data.value = repo.data.value.copy(householdReady = true)
        assertNotNull(vm.ui.value.view)
        vm.close()
        assertFalse(vm.ui.value.open)
        assertNull(vm.ui.value.view)
    }
}
