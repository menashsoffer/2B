package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MoneySamples.shekels
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.MonthSnapshot
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.PlanGroup
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** A draft plan from the household's own ended months, reviewed like a Paamonim file. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlanFromHistoryTest {
    private val today = LocalDate.of(2026, 11, 8)
    private val data = MoneyData(householdReady = true, categories = defaultCategories())
    private fun closed(month: YearMonth, byCategory: Map<String?, Money>) =
        ClosedMonth(month, MonthSnapshot(month, Money.ZERO, Money.ZERO), byCategory, emptyMap(), emptyMap(), emptyMap(), today)

    private val months = (6 downTo 1).map { YearMonth.from(today).minusMonths(it.toLong()) } // May..October
    private val closedHistory = months.mapIndexed { i, m ->
        closed(m, mapOf(
            "fixed-3" to shekels(5_000L + 100 * i), // rent rises: the latest counts
            "current-1" to shekels(2_000L + 200 * i), // food: average 2,500
            "periodic-16" to if (i == 0) shekels(1_200) else Money.ZERO, // holidays once in six months
            "sub-1" to shekels(30), // a single subscription
            null to shekels(99), // unassigned spending
        ))
    }
    private fun line(name: String) = requireNotNull(planFromHistory(spendingHistory(data.copy(closedMonths = closedHistory), emptyList(), ImportMemory(), today), data))
        .single { it.name == name }

    @Test fun fixedTakesTheLatestMonth() {
        assertEquals(PlanGroup.FIXED, line("שכר דירה").group)
        assertEquals(shekels(5_500), line("שכר דירה").monthly)
    }

    @Test fun currentTakesTheAverageRoundedUpTo50() {
        assertEquals(shekels(2_500), line("מזון").monthly)
    }

    @Test fun periodicIsTheAverageTimesTwelveRoundedUpTo100() {
        assertEquals(shekels(2_400), line("יהדות / חגים").annual) // 1,200 over six months
        assertEquals(shekels(200), line("יהדות / חגים").monthly)
    }

    @Test fun singleSubscriptionsAndUnassignedSpendingAreLeftOut() {
        val lines = requireNotNull(planFromHistory(spendingHistory(data.copy(closedMonths = closedHistory), emptyList(), ImportMemory(), today), data))
        assertTrue(lines.none { it.name == defaultCategories().single { c -> c.id == "sub-1" }.name })
        assertEquals(3, lines.size)
    }

    @Test fun fewerThanThreeMonthsGivesNoDraft() {
        val history = spendingHistory(data.copy(closedMonths = closedHistory.takeLast(2)), emptyList(), ImportMemory(), today)
        assertEquals(2, history.size)
        assertNull(planFromHistory(history, data))
        assertNull(draftPlanFromHistory(history, data, YearMonth.from(today)) { "id" })
    }

    @Test fun usesAtMostTheLastSixMonths() {
        val older = closed(YearMonth.of(2026, 1), mapOf("current-1" to shekels(90_000)))
        val history = spendingHistory(data.copy(closedMonths = closedHistory + older), emptyList(), ImportMemory(), today)
        assertEquals(months, history.map { it.month })
    }

    private var n = 0
    private fun stored(date: LocalDate, agorot: Long, category: String?) = StoredTransaction(
        StatementRow(id = "r-${n++}", source = Source.MAX, sourceId = "1234", date = date, chargeDate = date,
            description = "שופרסל $n", charge = Money(agorot), status = RowStatus.COUNTED),
        categoryId = category,
    )

    @Test fun importedMonthsCountBeforeTheyAreClosedButNotThisMonth() {
        val lines = listOf(8, 9, 10, 11).map { m -> stored(LocalDate.of(2026, m, 5), 30_000, "current-1") }
        val history = spendingHistory(data, lines, ImportMemory(), today)
        assertEquals(listOf(8, 9, 10).map { YearMonth.of(2026, it) }, history.map { it.month })
        assertEquals(shekels(300), history.first().byCategory["current-1"])
    }

    @Test fun draftKeepsItsMonthsAndGoesThroughTheSameReview() {
        val history = spendingHistory(data.copy(closedMonths = closedHistory), emptyList(), ImportMemory(), today)
        val draft = requireNotNull(draftPlanFromHistory(history, MoneySamples.filled, YearMonth.from(today)) { "id" })
        assertEquals(PlanBasis.History(months), draft.basis)
        val rent = draft.lines.single { it.name == "שכר דירה" }
        assertEquals(PlanLineStatus.CHANGED, rent.status)
        assertEquals(shekels(5_200), rent.current)
    }

    @Test fun viewModelOpensTheHistoryDraft() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            val money = FakeMoneyRepository(MoneySamples.filled.copy(closedMonths = closedHistory))
            val vm = ImportViewModel(FakeTransactionRepository(), money, DefaultErrorReporter(), io = dispatcher, today = { today })
            vm.openHistoryPlan()
            val review = vm.ui.value as ImportUi.PlanReview
            assertTrue(review.draft.basis is PlanBasis.History)
            vm.confirmPlan()
            assertTrue(vm.ui.value is ImportUi.PlanDone)
            assertTrue(money.saved.filterIsInstance<MoneyEntry.FixedExpenseEntry>().any { it.value.id == "fx-rent" })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun viewModelWithoutHistorySaysSo() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            val vm = ImportViewModel(FakeTransactionRepository(), FakeMoneyRepository(data), DefaultErrorReporter(), io = dispatcher, today = { today })
            vm.openHistoryPlan()
            assertEquals(ImportUi.PlanNoHistory, vm.ui.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun aCategoryWithAYearlyFixedExpenseIsLeftOut() {
        val yearly = closedHistory.mapIndexed { i, c -> c.copy(byCategory = c.byCategory + ("periodic-8" to if (i == 5) shekels(3_600) else Money.ZERO)) }
        val d = MoneySamples.filled.copy(closedMonths = yearly) // car insurance: a YEARLY fixed expense in periodic-8
        val lines = requireNotNull(planFromHistory(spendingHistory(d, emptyList(), ImportMemory(), today), d))
        assertTrue(lines.none { it.name == "ביטוח רכב + טסט" })
    }

    @Test fun viewModelSaysSoWhenNoLineCanBeDrafted() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            val onlySubs = months.map { closed(it, mapOf("sub-1" to shekels(30))) }
            val vm = ImportViewModel(FakeTransactionRepository(), FakeMoneyRepository(data.copy(closedMonths = onlySubs)),
                DefaultErrorReporter(), io = dispatcher, today = { today })
            vm.openHistoryPlan()
            assertEquals(ImportUi.PlanNoHistory, vm.ui.value)
            vm.close()
            assertEquals(ImportUi.Idle, vm.ui.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
