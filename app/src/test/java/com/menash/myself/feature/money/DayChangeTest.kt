package com.menash.myself.feature.money

import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.feature.home.HomeViewModel
import com.menash.myself.feature.home.ui.HomeUiState
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** The app left open across a month end: the steps move to the new month without any data change. */
@OptIn(ExperimentalCoroutinesApi::class)
class DayChangeTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val october = YearMonth.of(2026, 10)
    private val days = MutableStateFlow(LocalDate.of(2026, 10, 31))
    private val transactions = FakeTransactionRepository().apply {
        val date = LocalDate.of(2026, 10, 10)
        lines.value = listOf(StoredTransaction(
            StatementRow(id = "r-1", source = Source.MAX, sourceId = "1234", date = date, chargeDate = date,
                description = "שופרסל", charge = Money(10_000), status = RowStatus.COUNTED),
            categoryId = "current-1",
        ))
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun closesOctober(tasks: List<MonthTask>) = tasks.any {
        it is MonthTask.Close && it.month == october || it is MonthTask.UpdateBalance && it.month == october
    }

    @Test fun monthTabStepsFollowTheDate() {
        val vm = MoneyViewModel(FakeMoneyRepository(MoneySamples.filled), MutableStateFlow(MoneySamples.rate), { days.value },
            DefaultErrorReporter(), transactions = transactions.observeTransactions(), memory = transactions.observeMemory(), days = days)
        assertTrue(vm.state.value.tasks.none { closesOctober(listOf(it)) })
        assertEquals(october, vm.state.value.money?.current)
        days.value = LocalDate.of(2026, 11, 1)
        assertTrue(closesOctober(vm.state.value.tasks))
        assertEquals(YearMonth.of(2026, 11), vm.state.value.money?.current)
    }

    private val noDreams = object : DreamRepository {
        override fun observeDreams(listFilter: String?, filter: VisibilityFilter): Flow<List<Dream>> = flowOf(emptyList())
        override suspend fun createDream(dream: Dream) = Unit
        override suspend fun updateDreamStatus(dreamId: String, status: String, isPrivate: Boolean) = Unit
        override suspend fun updateDreamSavings(dreamId: String, savedAmount: Money, isPrivate: Boolean) = Unit
        override suspend fun updateDreamAppIdea(dreamId: String, questions: Map<String, String>, nextAction: String, isPrivate: Boolean) = Unit
        override suspend fun deleteDream(dreamId: String, isPrivate: Boolean) = Unit
    }

    @Test fun homeStepsFollowTheDate() {
        val vm = HomeViewModel(noDreams, DefaultErrorReporter(), FakeMoneyRepository(MoneySamples.filled),
            MutableStateFlow(MoneySamples.rate), { days.value }, transactions, days)
        val collected = mutableListOf<HomeUiState>()
        val job = CoroutineScope(Dispatchers.Main).launch { vm.uiState.collect { collected += it } }
        assertTrue(!closesOctober(collected.last().moneyTasks))
        days.value = LocalDate.of(2026, 11, 1)
        assertTrue(closesOctober(collected.last().moneyTasks))
        job.cancel()
    }

    @Test fun dayChangesEmitsOnlyANewDate() = runTest {
        var clock = LocalDate.of(2026, 10, 31)
        val seen = mutableListOf<LocalDate>()
        val job = launch { dayChanges({ clock }, every = 1_000, context = kotlin.coroutines.EmptyCoroutineContext).take(2).toList(seen) }
        advanceTimeBy(1)
        assertEquals(listOf(LocalDate.of(2026, 10, 31)), seen)
        advanceTimeBy(5_000) // same day: nothing new
        assertEquals(1, seen.size)
        clock = LocalDate.of(2026, 11, 1)
        advanceTimeBy(1_000)
        assertEquals(listOf(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1)), seen)
        job.join()
    }
}
