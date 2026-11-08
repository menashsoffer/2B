package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.importer.ImportFile
import com.menash.myself.feature.money.importer.ImportFixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class PlanImportViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val money = FakeMoneyRepository(MoneySamples.filled)
    private val transactions = FakeTransactionRepository()
    private val vm = ImportViewModel(transactions, money, DefaultErrorReporter(), io = dispatcher, today = { LocalDate.of(2026, 11, 8) })

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun paamonim() = ImportFile("רישומון.xls", ImportFixtures.bytes("paamonim_plan.xls"))

    @Test fun aPaamonimFileOpensThePlanReviewWithChangesSelected() {
        vm.load(listOf(paamonim()))
        val review = vm.ui.value as ImportUi.PlanReview
        assertEquals(YearMonth.of(2026, 11), review.draft.from)
        assertEquals(review.draft.selectableKeys, review.selected)
        assertTrue(transactions.applied.isEmpty())
    }

    @Test fun confirmWritesOnlyTheSelectedLines() {
        vm.load(listOf(paamonim()))
        val review = vm.ui.value as ImportUi.PlanReview
        val rent = review.draft.lines.single { it.name == "שכר דירה" }
        vm.togglePlanLine(rent.key)
        vm.confirmPlan()
        val done = vm.ui.value as ImportUi.PlanDone
        assertEquals(review.selected.size - 1, done.saved)
        val savedIds = money.saved.map { it.id }.toSet()
        assertTrue("fx-rent" !in savedIds)
        assertTrue(money.saved.filterIsInstance<MoneyEntry.VariableBudgetEntry>().any { it.value.id == "var-food" })
    }

    @Test fun lineThatCannotChangeIsNotToggled() {
        vm.load(listOf(paamonim()))
        val review = vm.ui.value as ImportUi.PlanReview
        val subscriptions = review.draft.lines.single { it.status == PlanLineStatus.SUBSCRIPTIONS }
        vm.togglePlanLine(subscriptions.key)
        assertEquals(review.selected, (vm.ui.value as ImportUi.PlanReview).selected)
    }

    @Test fun emptySelectionWritesNothing() {
        vm.load(listOf(paamonim()))
        (vm.ui.value as ImportUi.PlanReview).selected.forEach(vm::togglePlanLine)
        vm.confirmPlan()
        assertTrue(vm.ui.value is ImportUi.PlanReview)
        assertTrue(money.saved.isEmpty())
    }

    @Test fun bankFilesStillGoToTheStatementReview() {
        vm.load(listOf(ImportFile("max.xlsx", ImportFixtures.bytes("max_nov.xlsx"))))
        assertTrue(vm.ui.value is ImportUi.Review)
    }

    @Test fun aPlanFileWithAStatementIsReadAsStatementsOnly() {
        vm.load(listOf(paamonim(), ImportFile("max.xlsx", ImportFixtures.bytes("max_nov.xlsx"))))
        assertTrue(vm.ui.value is ImportUi.Review)
    }
}
