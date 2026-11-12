package com.menash.myself.shell

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.feature.money.CarViewModel
import com.menash.myself.feature.money.CategoryTrendViewModel
import com.menash.myself.feature.money.FakeMoneyRepository
import com.menash.myself.feature.money.FakeTransactionRepository
import com.menash.myself.feature.money.GoalsViewModel
import com.menash.myself.feature.money.ImportInbox
import com.menash.myself.feature.money.ImportViewModel
import com.menash.myself.feature.money.MonthCloseViewModel
import com.menash.myself.feature.money.MonthSummaryViewModel
import com.menash.myself.feature.money.PensionViewModel
import com.menash.myself.feature.money.PeriodViewModel
import com.menash.myself.feature.money.SubscriptionsViewModel
import com.menash.myself.feature.money.TitheViewModel
import com.menash.myself.feature.money.TransactionsViewModel
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.importer.ImportFile
import com.menash.myself.feature.money.CategoryTrendUi
import com.menash.myself.feature.money.MonthCloseUi
import com.menash.myself.feature.money.MonthSummaryUi
import com.menash.myself.feature.money.TransactionsUi
import com.menash.myself.feature.money.ui.MoneyDestination
import com.menash.myself.feature.money.ui.MoneyNavInputs
import com.menash.myself.feature.money.ui.MoneyNavVms
import com.menash.myself.feature.money.ui.MoneySubScreen
import com.menash.myself.feature.money.ui.SharedImportEffect
import com.menash.myself.feature.money.ui.rememberMoneyDestination
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** B9b: share, then Back three times through the product stack. Import, money root, בית, then the activity finishes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ShareBackTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun b9_share_backMoneyRootThenHome() {
        val exited = mutableStateOf(false)
        composeRule.setContent { ShareBackHarness(exited) }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("screen_import").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("press_back").performClick()
        composeRule.onNodeWithTag("screen_money").assertExists()
        composeRule.onNodeWithTag("press_back").performClick()
        composeRule.onNodeWithTag("screen_home").assertExists()
        composeRule.onNodeWithTag("press_back").performClick()
        assertTrue(exited.value)
    }
}

@Composable
private fun ShareBackHarness(exited: androidx.compose.runtime.MutableState<Boolean>) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val inbox = remember { ImportInbox().also { it.offer(listOf("content://bank/file")) } }
    val vms = remember { shareBackVms() }
    DisposableEffect(context) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { exited.value = true }
        }
        (context as ComponentActivity).onBackPressedDispatcher.addCallback(callback)
        onDispose { callback.remove() }
    }
    val pending by inbox.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pending.isNotEmpty()) {
        if (pending.isNotEmpty()) navController.openTab(ShellRoute.Money)
    }
    val dispatcher = (context as ComponentActivity).onBackPressedDispatcher
    MyApplicationTheme {
        Box {
            CompositionLocalProvider(LocalLeaveTab provides { navController.openTab(ShellRoute.Home) }) {
                AppNavHost(
                    navController = navController,
                    session = null,
                    onOpenRoute = { navController.openTab(it) },
                    onRequestSignIn = {},
                    onSignedOut = {},
                    screens = shareScreens(inbox, vms),
                )
            }
            TextButton(onClick = { dispatcher.onBackPressed() }, modifier = Modifier.testTag("press_back")) {
                Text("חזרה")
            }
        }
    }
}

private fun shareScreens(inbox: ImportInbox, vms: MoneyNavVms): ShellScreens = ShellScreens(
    home = { _, _, _ -> Text("בית", modifier = Modifier.testTag("screen_home")) },
    money = { ProductMoney(inbox, vms) },
    me = { _, _, _, _, _ -> Text("אני") },
    signIn = { _, _ -> Text("התחברות") },
)

@Composable
private fun ProductMoney(inbox: ImportInbox, vms: MoneyNavVms) {
    val importOpen by vms.import.open.collectAsStateWithLifecycle()
    SharedImportEffect(inbox, { false }, vms::closeAll, {}, { uris -> vms.import.load(uris.toImportFiles()) })
    val destination = rememberMoneyDestination(
        inputs = MoneyNavInputs(
            importOpen, TransactionsUi(), false, false, false, false,
            MonthCloseUi(), false, false, MonthSummaryUi(), CategoryTrendUi(),
        ),
        vms = vms,
        shownMonth = remember { MutableStateFlow<YearMonth?>(null) },
        onRestoreMonth = {},
        onScrollTop = {},
    )
    TabRootBack(enabled = destination == MoneyDestination.Root)
    when (destination) {
        MoneyDestination.Import -> MoneySubScreen(title = "ייבוא", onBack = vms.import::close) {
            Text("ייבוא", modifier = Modifier.testTag("screen_import"))
        }
        MoneyDestination.Root -> Text("כסף", modifier = Modifier.testTag("screen_money"))
        else -> Text(destination.javaClass.simpleName, modifier = Modifier.testTag("screen_other"))
    }
}

private fun shareBackVms(): MoneyNavVms {
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

private fun List<Uri>.toImportFiles() = map { uri -> ImportFile(uri.lastPathSegment ?: "file", byteArrayOf()) }
