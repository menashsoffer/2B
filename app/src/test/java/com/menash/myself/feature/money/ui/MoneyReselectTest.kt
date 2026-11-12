package com.menash.myself.feature.money.ui

import android.os.Looper
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.feature.money.CarViewModel
import com.menash.myself.feature.money.CategoryTrendUi
import com.menash.myself.feature.money.CategoryTrendViewModel
import com.menash.myself.feature.money.FakeMoneyRepository
import com.menash.myself.feature.money.FakeTransactionRepository
import com.menash.myself.feature.money.GoalsViewModel
import com.menash.myself.feature.money.ImportViewModel
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.MonthCloseUi
import com.menash.myself.feature.money.MonthCloseViewModel
import com.menash.myself.feature.money.MonthSummaryUi
import com.menash.myself.feature.money.MonthSummaryViewModel
import com.menash.myself.feature.money.PensionViewModel
import com.menash.myself.feature.money.PeriodViewModel
import com.menash.myself.feature.money.SubscriptionsViewModel
import com.menash.myself.feature.money.TitheViewModel
import com.menash.myself.feature.money.TransactionsUi
import com.menash.myself.feature.money.TransactionsViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A second tap on כסף closes the money screens, but asks first when a form has unsaved changes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class MoneyReselectTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reselect_withHalfTypedGoal_asksAndKeepHoldsTheDraft() {
        val vms = reselectVms()
        vms.goals.open()
        vms.goals.startAdding()
        vms.goals.updateDraft { it.copy(name = "דירה") }
        idle()
        val bus = MoneyReselectBus()
        var scrolled = 0
        setHarness(vms, bus, dirty = { vms.goals.ui.value.editor?.let { it.draft != it.opened } == true }) { scrolled++ }

        bus.send()
        idle()
        composeRule.onNodeWithTag("discard_confirm").assertExists()
        assertEquals("דירה", vms.goals.ui.value.editor?.draft?.name)

        composeRule.onNodeWithTag("discard_keep").performClick()
        idle()
        assertEquals(0, composeRule.onAllNodesWithTag("discard_confirm").fetchSemanticsNodes().size)
        assertTrue(vms.goals.ui.value.open)
        assertNotNull(vms.goals.ui.value.editor)
        assertEquals(0, scrolled)
    }

    @Test
    fun reselect_withHalfTypedGoal_leaveClosesEverything() {
        val vms = reselectVms()
        vms.goals.open()
        vms.goals.startAdding()
        vms.goals.updateDraft { it.copy(name = "דירה") }
        idle()
        val bus = MoneyReselectBus()
        var discarded = 0
        var scrolled = 0
        setHarness(vms, bus, dirty = { vms.goals.ui.value.editor?.let { it.draft != it.opened } == true }, onDiscard = { discarded++ }) { scrolled++ }

        bus.send()
        idle()
        composeRule.onNodeWithTag("discard_leave").performClick()
        idle()
        assertEquals(1, discarded)
        assertFalse(vms.goals.ui.value.open)
        assertNull(vms.goals.ui.value.editor)
        assertEquals(1, scrolled)
        composeRule.onNodeWithTag("screen_root").assertExists()
    }

    @Test
    fun reselect_clean_closesWithoutAsking() {
        val vms = reselectVms()
        vms.goals.open()
        idle()
        val bus = MoneyReselectBus()
        var scrolled = 0
        setHarness(vms, bus, dirty = { false }) { scrolled++ }

        bus.send()
        idle()
        assertEquals(0, composeRule.onAllNodesWithTag("discard_confirm").fetchSemanticsNodes().size)
        assertFalse(vms.goals.ui.value.open)
        assertEquals(1, scrolled)
    }

    private fun setHarness(
        vms: MoneyNavVms,
        bus: MoneyReselectBus,
        dirty: () -> Boolean,
        onDiscard: () -> Unit = {},
        onScrollTop: () -> Unit,
    ) {
        composeRule.setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalMoneyReselect provides bus) {
                    val goals = vms.goals.ui.collectAsStateWithLifecycle().value
                    val destination = rememberMoneyDestination(
                        inputs = MoneyNavInputs(
                            false, TransactionsUi(), goals.open, false, false, false,
                            MonthCloseUi(), false, false, MonthSummaryUi(), CategoryTrendUi(),
                        ),
                        vms = vms,
                        shownMonth = remember { MutableStateFlow<YearMonth?>(null) },
                        onRestoreMonth = {},
                        onScrollTop = onScrollTop,
                        dirty = dirty,
                        onDiscard = onDiscard,
                    )
                    val tag = if (destination == MoneyDestination.Root) "screen_root" else "screen_sub"
                    Text(destination.javaClass.simpleName, modifier = Modifier.testTag(tag))
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
    }
}

private fun reselectVms(): MoneyNavVms {
    val money = FakeMoneyRepository(MoneySamples.filled)
    val transactions = FakeTransactionRepository()
    val rate = MutableStateFlow(MoneySamples.rate)
    val today = { LocalDate.of(2026, 10, 30) }
    val error = DefaultErrorReporter()
    return MoneyNavVms(
        TransactionsViewModel(transactions, money, error),
        MonthCloseViewModel(money, transactions, rate, today, error),
        MonthSummaryViewModel(money, rate, error),
        CategoryTrendViewModel(money, rate, today, error),
        GoalsViewModel(money, rate, today, error),
        TitheViewModel(money, transactions, today, error),
        CarViewModel(money, transactions, today, error),
        PensionViewModel(money, today, error),
        SubscriptionsViewModel(money, transactions, rate, today, error),
        PeriodViewModel(money, rate, today, error),
        ImportViewModel(transactions, money, error),
    )
}
