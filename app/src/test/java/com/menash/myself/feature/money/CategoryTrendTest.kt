package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.MonthSnapshot
import com.menash.myself.feature.money.domain.SuggestionStatus
import com.menash.myself.feature.money.domain.amountFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** "מגמה": twelve months up to last month, closed months only, and the suggested plan in one tap. */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryTrendTest {

    @Test
    fun missingFixedPlanRequiresManualEditing() {
        val target = planTarget(MoneySamples.empty, "missing", ExpenseClass.FIXED, YearMonth.of(2026, 10))
        assertEquals(PlanTarget.Manual(0), target)
    }

    private val today = LocalDate.of(2026, 11, 2)
    private val november = YearMonth.of(2026, 11)

    private fun close(month: Int, byCategory: Map<String?, Money>) = YearMonth.of(2026, month).let { m ->
        ClosedMonth(m, MonthSnapshot(m, Money(1_800_000), Money(1_300_000)), byCategory, emptyMap(), emptyMap(), emptyMap(), m.plusMonths(1).atDay(2))
    }

    private fun closes(vararg months: Int, food: Long = 420_000, out: Long = 0) =
        months.map { close(it, mapOf("current-1" to Money(food), "current-9" to Money(out))) }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun twelveMonths_missingAreNull_planFromItems() {
        val data = MoneySamples.filled.copy(closedMonths = closes(8, 9, 10))
        val view = categoryTrend(data, "current-1", today, MoneySamples.rate)!!
        assertEquals(12, view.months.size)
        assertEquals(YearMonth.of(2025, 11), view.months.first().month)
        assertEquals(YearMonth.of(2026, 10), view.months.last().month)
        assertNull(view.months.single { it.month == YearMonth.of(2026, 7) }.actual)
        assertEquals(Money(420_000), view.months.last().actual)
        assertNull(view.months.first().planned) // the food budget starts in January 2026
        assertEquals(Money(300_000), view.months.last().planned)
        assertEquals(3, view.closedCount)
        assertEquals(Money(420_000), view.baseline!!.average)
    }

    @Test
    fun underPlanned_oneBudget_changesFromThisMonth() {
        val data = MoneySamples.filled.copy(closedMonths = closes(8, 9, 10))
        val view = categoryTrend(data, "current-1", today, MoneySamples.rate)!!
        assertEquals(SuggestionStatus.UNDER_PLANNED, view.status)
        assertEquals(Money(420_000), view.suggested)
        val entry = planChange(view.target!!, view, view.suggested!!) { "new" } as MoneyEntry.VariableBudgetEntry
        assertEquals("var-food", entry.value.id)
        assertEquals(Money(420_000), amountFor(entry.value, november))
        assertEquals(Money(300_000), amountFor(entry.value, YearMonth.of(2026, 10)))
    }

    @Test
    fun noPlan_currentCategory_opensNewBudget() {
        val data = MoneySamples.filled.copy(closedMonths = closes(8, 9, 10, out = 61_000))
        val view = categoryTrend(data, "current-9", today, MoneySamples.rate)!!
        assertEquals(SuggestionStatus.NO_PLAN, view.status)
        assertEquals(PlanTarget.NewBudget, view.target)
        val entry = planChange(view.target!!, view, view.suggested!!) { "new-id" } as MoneyEntry.VariableBudgetEntry
        assertEquals("current-9", entry.value.categoryId)
        assertEquals(view.name, entry.value.name)
        assertEquals(Money(65_000), amountFor(entry.value, november)) // 610 rounded up to 650
    }

    @Test
    fun fewerThanThreeCloses_noSuggestion() {
        val data = MoneySamples.filled.copy(closedMonths = closes(10))
        val view = categoryTrend(data, "current-1", today, MoneySamples.rate)!!
        assertEquals(SuggestionStatus.NOT_ENOUGH_DATA, view.status)
        assertNull(view.suggested)
    }

    @Test
    fun periodicCategory_hasNoTarget_unknownIsNull() {
        val data = MoneySamples.filled.copy(closedMonths = closes(8, 9, 10))
        assertNull(categoryTrend(data, "periodic-16", today, MoneySamples.rate)!!.target)
        assertNull(categoryTrend(data, "nope", today, MoneySamples.rate))
    }

    @Test
    fun accept_savesTheChange() {
        val repo = FakeMoneyRepository(MoneySamples.filled.copy(closedMonths = closes(8, 9, 10)))
        val viewModel = CategoryTrendViewModel(repo, flowOf(MoneySamples.rate), { today }, DefaultErrorReporter())
        viewModel.open("current-1")
        viewModel.acceptSuggestion()
        val saved = repo.saved.single() as MoneyEntry.VariableBudgetEntry
        assertEquals(Money(420_000), amountFor(saved.value, november))
        assertTrue(!viewModel.ui.value.saving)
    }
}
