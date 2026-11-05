package com.menash.myself.feature.home.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
import com.menash.myself.r2.FixedClock
import com.menash.myself.r2.LightQualifiers
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
                    hour = FixedClock.HOUR,
                    epochMillis = FixedClock.epochMillis,
                    onOpenRoute = {},
                    onOpenAccount = {},
                )
            }
        }
        composeRule.onNodeWithText(MoneyFormatter.format(surplus), substring = true).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.home_money_planned), substring = true).assertExists()
    }
}
