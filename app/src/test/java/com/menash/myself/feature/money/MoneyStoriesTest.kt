package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.e2e.stories.Stories
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.MoneyItems
import com.menash.myself.feature.money.importer.MerchantDecision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class MoneyStoriesTest {
    private val reporter = DefaultErrorReporter()
    private val rate = MutableStateFlow<java.math.BigDecimal?>(null)
    @Before fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun cleanup() = Dispatchers.resetMain()

    private fun fixture(name: String, today: LocalDate): Pair<FakeMoneyRepository, FakeTransactionRepository> {
        val story = Stories.build(name, today)
        val entries = story.entries
        val data = MoneyData(householdReady = true,
            items = MoneyItems(
                incomes = entries.filterIsInstance<MoneyEntry.IncomeEntry>().map { it.value },
                fixedExpenses = entries.filterIsInstance<MoneyEntry.FixedExpenseEntry>().map { it.value },
                variableBudgets = entries.filterIsInstance<MoneyEntry.VariableBudgetEntry>().map { it.value },
                periodicBudgets = entries.filterIsInstance<MoneyEntry.PeriodicBudgetEntry>().map { it.value },
            ),
            categories = entries.filterIsInstance<MoneyEntry.CategoryEntry>().map { it.value },
            closedMonths = entries.filterIsInstance<MoneyEntry.ClosedMonthEntry>().map { it.value },
        )
        return FakeMoneyRepository(data) to FakeTransactionRepository().apply { lines.value = story.rows }
    }

    private fun money(repo: FakeMoneyRepository, tx: FakeTransactionRepository, date: LocalDate) =
        MoneyViewModel(repo, rate, { date }, reporter, transactions = tx.lines, memory = tx.memory)

    @Test fun newMonthShowsThePlan() {
        val date = LocalDate.of(2026, 10, 5)
        val (repo, tx) = fixture("plan_only", date)
        val status = (money(repo, tx, date).state.value.money!!.view as MonthView.Planned).status as MonthStatus.PlanOnly
        assertEquals(890000L, status.expectedSurplus.agorot)
    }

    @Test fun recategorisingUpdatesTheCurrentBudget() {
        val date = LocalDate.of(2026, 10, 20)
        val (repo, tx) = fixture("mid_month", date)
        val vm = money(repo, tx, date)
        fun status() = (vm.state.value.money!!.view as MonthView.Planned).status as MonthStatus.Open
        assertEquals(180000L, status().leftForCurrent.agorot)
        assertEquals(500000L, status().currentBudget.agorot)
        val screen = TransactionsViewModel(tx, repo, reporter)
        screen.open(YearMonth.from(date))
        screen.decide("story-2026-10-card-0", MerchantDecision.Categorise("story-rent"), thisLineOnly = true)
        assertEquals(196000L, status().leftForCurrent.agorot)
        assertEquals(2000000L, com.menash.myself.feature.money.importer.actualIncome(tx.lines.value, YearMonth.from(date)).agorot)
    }

    @Test fun previousMonthCanCloseWithTheExpectedResult() {
        val date = LocalDate.of(2026, 11, 3)
        val (repo, tx) = fixture("ready_to_close", date)
        val close = MonthCloseViewModel(repo, tx, rate, { date }, reporter)
        close.open()
        assertTrue(close.ui.value.view!!.canClose)
        assertEquals(1300000L, close.ui.value.view!!.saving.agorot)
        close.closeMonth()
        val saved = repo.saved.filterIsInstance<MoneyEntry.ClosedMonthEntry>().single().value
        assertEquals(YearMonth.of(2026, 10), saved.month)
        assertEquals(350000L, saved.byCategory["story-food"]!!.agorot)
    }

    @Test fun retroCloseFeedsPeriodWithAMissingMonth() {
        val date = LocalDate.of(2026, 11, 3)
        val (repo, tx) = fixture("history", date)
        val close = MonthCloseViewModel(repo, tx, rate, { date }, reporter)
        close.open()
        close.previousMonth()
        assertTrue(close.ui.value.showBalancesStep)
        close.enterBalances(false)
        close.closeMonth()
        val saved = repo.saved.filterIsInstance<MoneyEntry.ClosedMonthEntry>().single().value
        assertFalse(saved.balancesEntered)
        repo.data.value = repo.data.value.copy(closedMonths = repo.data.value.closedMonths + saved)
        val period = PeriodViewModel(repo, rate, { date }, reporter)
        period.open()
        period.select(PeriodPreset.LAST_3)
        val view = period.ui.value.view!!
        assertEquals(2, view.summary!!.closedMonths.size)
        assertEquals(3, view.range.months.size)
    }
}
