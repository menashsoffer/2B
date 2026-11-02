package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenLoadingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 11, 3)
    private val month = YearMonth.of(2026, 10)

    private class FailingMoneyRepository : MoneyRepository by FakeMoneyRepository() {
        var fail = true
        var ready = true
        override fun observeForScreen(): Flow<MoneyData> = flow {
            if (fail) throw IllegalStateException("offline")
            emit(MoneyData(householdReady = ready))
        }
    }

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun transactionsLoadErrorAndRetry() = runTest(dispatcher) {
        val repo = FailingMoneyRepository()
        val vm = TransactionsViewModel(FakeTransactionRepository(), repo, DefaultErrorReporter())
        vm.open(month)
        assertNull(vm.ui.value.lines)
        assertFalse(vm.ui.value.loadError)
        advanceUntilIdle()
        assertTrue(vm.ui.value.loadError)
        repo.fail = false
        vm.retry()
        advanceUntilIdle()
        assertFalse(vm.ui.value.loadError)
        assertNotNull(vm.ui.value.lines)
    }

    @Test fun closeLoadErrorAndRetry() = runTest(dispatcher) {
        val repo = FailingMoneyRepository()
        val vm = MonthCloseViewModel(repo, FakeTransactionRepository(), flowOf(null), { today }, DefaultErrorReporter())
        vm.open()
        assertNull(vm.ui.value.view)
        assertFalse(vm.ui.value.loadError)
        advanceUntilIdle()
        assertTrue(vm.ui.value.loadError)
        repo.fail = false
        vm.retry()
        advanceUntilIdle()
        assertFalse(vm.ui.value.loadError)
        assertNotNull(vm.ui.value.view)
    }

    @Test fun subscriptionsLoadErrorAndRetry() = runTest(dispatcher) {
        val repo = FailingMoneyRepository()
        val vm = SubscriptionsViewModel(repo, FakeTransactionRepository(), flowOf(null), { today }, DefaultErrorReporter())
        vm.open()
        assertNull(vm.ui.value.view)
        assertFalse(vm.ui.value.loadError)
        advanceUntilIdle()
        assertTrue(vm.ui.value.loadError)
        repo.fail = false
        vm.retry()
        advanceUntilIdle()
        assertFalse(vm.ui.value.loadError)
        assertNotNull(vm.ui.value.view)
    }

    @Test fun missingHouseholdDoesNotRemainLoading() = runTest(dispatcher) {
        val repo = FailingMoneyRepository().apply { fail = false; ready = false }
        val transactions = FakeTransactionRepository()
        val close = MonthCloseViewModel(repo, transactions, flowOf(null), { today }, DefaultErrorReporter())
        val subscriptions = SubscriptionsViewModel(repo, transactions, flowOf(null), { today }, DefaultErrorReporter())
        close.open()
        subscriptions.open()
        advanceUntilIdle()
        assertTrue(close.ui.value.noHousehold)
        assertTrue(subscriptions.ui.value.noHousehold)
    }
}
