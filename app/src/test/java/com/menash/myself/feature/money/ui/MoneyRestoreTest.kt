package com.menash.myself.feature.money.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.feature.money.CarViewModel
import com.menash.myself.feature.money.CategoryTrendViewModel
import com.menash.myself.feature.money.FakeMoneyRepository
import com.menash.myself.feature.money.FakeTransactionRepository
import com.menash.myself.feature.money.GoalsViewModel
import com.menash.myself.feature.money.ImportViewModel
import com.menash.myself.feature.money.MoneyFrame
import com.menash.myself.feature.money.MoneyPlace
import com.menash.myself.feature.money.MonthCloseViewModel
import com.menash.myself.feature.money.MonthSummaryViewModel
import com.menash.myself.feature.money.PensionViewModel
import com.menash.myself.feature.money.PeriodViewModel
import com.menash.myself.feature.money.SubscriptionsViewModel
import com.menash.myself.feature.money.TitheViewModel
import com.menash.myself.feature.money.TransactionsViewModel
import com.menash.myself.feature.money.data.MoneyData
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** B12 and the close that follows it: restore waits on the clock, a normal close does not. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MoneyRestoreTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backReachesRootOnTheNextFrame() {
        composeRule.mainClock.autoAdvance = false
        val log = mutableListOf<String>()
        composeRule.setContent {
            var frame by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) { withFrameNanos { frame = 1 } }
            val open = if (frame == 0) listOf(frame(MoneyPlace.Transactions)) else emptyList()
            val shown = restoredMoneyStack(open) {}
            val label = stackLabel(shown)
            SideEffect { log += "$frame:$label" }
        }
        composeRule.mainClock.advanceTimeByFrame()
        val atClose = log.lastOrNull { it.startsWith("1:") }
        assertEquals(log.joinToString(), "1:root", atClose)
    }

    @Test
    fun b12_lateFlagsInFramesOrder_keepHistoryAndReplay() {
        composeRule.mainClock.autoAdvance = false
        val tester = StateRestorationTester(composeRule)
        val boots = AtomicInteger(0)
        val replayed = mutableListOf<MoneyPlace>()
        val log = mutableListOf<String>()
        tester.setContent { LateFlags(boots, replayed, log) }
        composeRule.mainClock.advanceTimeByFrame()
        assertTrue(log.joinToString(" | "), log.last().endsWith("Transactions-Close"))
        composeRule.mainClock.autoAdvance = true
        tester.emulateSavedInstanceStateRestore()
        composeRule.mainClock.autoAdvance = false
        assertEquals(log.joinToString(" | "), listOf(MoneyPlace.Transactions, MoneyPlace.Close), replayed.toList())
        assertTrue(log.joinToString(" | "), log.any { it.startsWith("b2:") && it.endsWith("Transactions-Close") })
        assertTrue(log.none { it.endsWith("Close-Transactions") })
    }

    @Composable
    private fun LateFlags(
        boots: AtomicInteger,
        replayed: MutableList<MoneyPlace>,
        log: MutableList<String>,
    ) {
        val boot = remember { boots.incrementAndGet() }
        val vms = remember { restoreVms() }
        var step by remember { mutableIntStateOf(0) }
        var publish by remember { mutableStateOf(false) }
        LaunchedEffect(boot) {
            if (boot == 1) withFrameNanos { step = 1 }
            else {
                delay(500)
                publish = true
            }
        }
        val open = when {
            boot == 1 && step == 0 -> listOf(frame(MoneyPlace.Transactions))
            boot == 1 -> listOf(frame(MoneyPlace.Transactions), frame(MoneyPlace.Close))
            publish -> listOf(frame(MoneyPlace.Close), frame(MoneyPlace.Transactions))
            else -> emptyList()
        }
        val shown = restoredMoneyStack(open) { missing ->
            missing.forEach { frame ->
                replayed += frame.place
                vms.replay(frame)
            }
        }
        val label = stackLabel(shown)
        SideEffect { log += "b$boot:$label" }
    }
}

private fun frame(place: MoneyPlace) = MoneyFrame(place, month = "2026-09")

private fun stackLabel(shown: List<MoneyFrame>): String =
    shown.joinToString("-") { it.place.name }.ifEmpty { "root" }

private fun restoreVms(): MoneyNavVms {
    val money = FakeMoneyRepository(MoneyData(householdReady = true))
    val transactions = FakeTransactionRepository()
    val rate = MutableStateFlow<BigDecimal?>(null)
    val today = { LocalDate.of(2026, 11, 12) }
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
