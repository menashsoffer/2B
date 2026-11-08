package com.menash.myself.feature.money

import kotlinx.coroutines.flow.first
import com.menash.myself.core.data.session.DefaultErrorReporter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoneyFactoriesTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun factoriesCreateWorkingMoneyScreens() = runTest {
        val repo = FakeMoneyRepository(MoneySamples.filled)
        val tx = FakeTransactionRepository()
        val rate = MutableStateFlow<BigDecimal?>(BigDecimal("3.5"))
        val today = { LocalDate.of(2026, 10, 1) }
        val error = DefaultErrorReporter()
        val car = CarViewModel.factory(repo, tx, today, error).create(CarViewModel::class.java)
        car.open(); runCurrent(); assertTrue(car.ui.value.open)
        val goals = GoalsViewModel.factory(repo, rate, today, error).create(GoalsViewModel::class.java)
        goals.open(); runCurrent(); assertTrue(goals.ui.value.open)
        val close = MonthCloseViewModel.factory(repo, tx, rate, today, error).create(MonthCloseViewModel::class.java)
        close.open(); runCurrent(); assertTrue(close.ui.value.open)
        val pension = PensionViewModel.factory(repo, today, error).create(PensionViewModel::class.java)
        pension.open(); runCurrent(); assertTrue(pension.ui.value.open)
        val tithe = TitheViewModel.factory(repo, tx, today, error).create(TitheViewModel::class.java)
        tithe.open(); runCurrent(); assertTrue(tithe.ui.value.open)
        val period = PeriodViewModel.factory(repo, rate, today, error).create(PeriodViewModel::class.java)
        period.open(); runCurrent(); assertTrue(period.ui.value.open)
        val subscriptions = SubscriptionsViewModel.factory(repo, tx, rate, today, error).create(SubscriptionsViewModel::class.java)
        subscriptions.open(); runCurrent(); assertTrue(subscriptions.ui.value.open)
        val summary = MonthSummaryViewModel.factory(repo, rate, error).create(MonthSummaryViewModel::class.java)
        summary.open(YearMonth.of(2026, 10)); runCurrent(); assertTrue(summary.ui.value.open)
    }

    @Test fun factoriesConnectTrendTransactionsAndImportToInputs() = runTest {
        val repo = FakeMoneyRepository(MoneySamples.filled)
        val tx = FakeTransactionRepository()
        val rate = MutableStateFlow<BigDecimal?>(BigDecimal("3.5"))
        val today = { LocalDate.of(2026, 10, 1) }
        val error = DefaultErrorReporter()
        val trend = CategoryTrendViewModel.factory(repo, rate, today, error).create(CategoryTrendViewModel::class.java)
        trend.open("current-1"); runCurrent(); assertEquals("current-1", trend.ui.value.categoryId)
        val transactions = TransactionsViewModel.factory(tx, repo, error).create(TransactionsViewModel::class.java)
        transactions.open(YearMonth.of(2026, 10)); runCurrent(); assertTrue(transactions.ui.value.open)
        val importer = ImportViewModel.factory(tx, repo, error).create(ImportViewModel::class.java)
        importer.load(emptyList()); assertEquals(ImportUi.Idle, importer.ui.value)
        val money = MoneyViewModel.factory(repo, rate, today, error, tx).create(MoneyViewModel::class.java)
        // The factory builds the month on Dispatchers.Default, so the state arrives off the test scheduler.
        val state = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            kotlinx.coroutines.withTimeout(10_000) { money.state.first { it.data.items == repo.data.value.items } }
        }
        assertEquals(repo.data.value.items, state.data.items)
    }
}
