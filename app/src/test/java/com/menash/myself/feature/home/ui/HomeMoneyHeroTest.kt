package com.menash.myself.feature.home.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.menash.myself.R
import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.designsystem.component.MoneyFormatter
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.feature.home.HomeViewModel
import com.menash.myself.feature.money.FakeMoneyRepository
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.domain.MonthPlan
import com.menash.myself.feature.money.domain.monthPlan
import com.menash.myself.testing.FixedClock
import com.menash.myself.testing.LightQualifiers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The money hero shows the engine's expected surplus once the month is planned, and no number before. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class HomeMoneyHeroTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val noDreams = object : DreamRepository {
        override fun observeDreams(listFilter: String?, filter: VisibilityFilter): Flow<List<Dream>> = flowOf(emptyList())
        override suspend fun createDream(dream: Dream) = Unit
        override suspend fun updateDreamStatus(dreamId: String, status: String, isPrivate: Boolean) = Unit
        override suspend fun updateDreamSavings(dreamId: String, savedAmount: Money, isPrivate: Boolean) = Unit
        override suspend fun updateDreamAppIdea(dreamId: String, questions: Map<String, String>, nextAction: String, isPrivate: Boolean) = Unit
        override suspend fun deleteDream(dreamId: String, isPrivate: Boolean) = Unit
    }

    private fun viewModel(repository: FakeMoneyRepository) = HomeViewModel(
        dreamRepository = noDreams,
        errorReporter = DefaultErrorReporter(),
        moneyRepository = repository,
        usdRates = MutableStateFlow(MoneySamples.rate),
        today = { MoneySamples.today },
    )

    @Test
    fun viewModel_surplusIsTheEngineWhenPlanned_nullWhenEmpty() {
        val repository = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repository)
        val engine = monthPlan(MoneySamples.month, MoneySamples.filled.items, MoneySamples.rate) as MonthPlan.Planned
        val collected = mutableListOf<HomeUiState>()
        val job = CoroutineScope(Dispatchers.Main).launch { vm.uiState.collect { collected += it } }
        assertEquals(engine.expectedSurplus, collected.last().moneySurplus)
        repository.data.value = MoneySamples.empty
        assertNull(collected.last().moneySurplus)
        job.cancel()
    }

    @Test
    fun heroShowsTheSurplusAndThePlannedLabel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val surplus = Money(123_400)
        composeRule.setContent {
            MyApplicationTheme {
                HomeContent(
                    state = HomeUiState(firstName = "דנה", moneySurplus = surplus),
                    epochMillis = FixedClock.epochMillis,
                    onOpenRoute = {},
                    onOpenAccount = {},
                )
            }
        }
        composeRule.onNodeWithText(MoneyFormatter.format(surplus), substring = true).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.home_money_planned), substring = true).assertExists()
    }

    @Test
    fun viewModel_leadsWithLeftForCurrentAndSharesTheSteps() {
        val stored = com.menash.myself.feature.money.FakeTransactionRepository()
        val date = MoneySamples.today
        stored.lines.value = listOf(
            com.menash.myself.feature.money.importer.StoredTransaction(
                com.menash.myself.feature.money.importer.StatementRow(id = "r-1", source = com.menash.myself.feature.money.importer.Source.MAX,
                    sourceId = "1234", date = date, chargeDate = date, description = "שופרסל", charge = Money(10_000),
                    status = com.menash.myself.feature.money.importer.RowStatus.COUNTED),
                categoryId = null,
            ),
        )
        val vm = HomeViewModel(noDreams, DefaultErrorReporter(), FakeMoneyRepository(MoneySamples.filled),
            MutableStateFlow(MoneySamples.rate), { MoneySamples.today }, stored)
        val collected = mutableListOf<HomeUiState>()
        val job = CoroutineScope(Dispatchers.Main).launch { vm.uiState.collect { collected += it } }
        val state = collected.last()
        assertEquals(Money(380_000), state.moneyLeftForCurrent) // food 3,000 + fuel 800; the line has no category
        assertEquals(com.menash.myself.feature.money.MonthTask.Assign(MoneySamples.month, 1), state.moneyTasks.last())
        assertEquals("1234", state.moneyFreshness?.source?.sourceId)
        job.cancel()
    }

    @Test
    fun heroLeadsWithLeftForCurrentAndShowsSteps() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var tapped: com.menash.myself.feature.money.MonthTask? = null
        val task = com.menash.myself.feature.money.MonthTask.Assign(MoneySamples.month, 2)
        composeRule.setContent {
            MyApplicationTheme {
                HomeContent(
                    state = HomeUiState(firstName = "דנה", moneySurplus = Money(500_000), moneyLeftForCurrent = Money(120_000), moneyTasks = listOf(task)),
                    epochMillis = FixedClock.epochMillis,
                    onOpenRoute = {},
                    onOpenAccount = {},
                    onMoneyTask = { tapped = it },
                )
            }
        }
        composeRule.onNodeWithText(MoneyFormatter.format(Money(120_000)), substring = true).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.status_left_for_current), substring = true).assertExists()
        composeRule.onNode(androidx.compose.ui.test.hasTestTag("next_step_first")).performClick()
        assertEquals(task, tapped)
    }
}
