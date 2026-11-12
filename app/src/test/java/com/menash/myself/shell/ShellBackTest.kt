package com.menash.myself.shell

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.feature.money.ui.LocalMoneyReselect
import com.menash.myself.feature.money.ui.MoneyReselectBus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController

/** B5, B6, B8 and B16 on the shell: tab-root Back, exit from בית, reselect, month kept. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ShellBackTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun b6_homeRootBack_exits() {
        val exited = mutableStateOf(false)
        composeRule.setContent { BackHarness(exited) }
        composeRule.onNodeWithTag("screen_home").assertExists()
        composeRule.onNodeWithTag("press_back").performClick()
        assertTrue(exited.value)
    }

    @Test
    fun b5_moneyRootBack_returnsHomeAndKeepsMonth() {
        val exited = mutableStateOf(false)
        composeRule.setContent { BackHarness(exited) }
        composeRule.onNodeWithTag("nav_money").performClick()
        composeRule.onNodeWithTag("pick_month").performClick()
        composeRule.onNodeWithTag("press_back").performClick()
        composeRule.onNodeWithTag("screen_home").assertExists()
        composeRule.onNodeWithTag("nav_money").performClick()
        composeRule.onNodeWithTag("money_month").assertTextEquals("2026-09")
        assertEquals(false, exited.value)
    }

    @Test
    fun b16_monthSurvivesTabSwitch() {
        val exited = mutableStateOf(false)
        composeRule.setContent { BackHarness(exited) }
        composeRule.onNodeWithTag("nav_money").performClick()
        composeRule.onNodeWithTag("pick_month").performClick()
        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.onNodeWithTag("nav_money").performClick()
        composeRule.onNodeWithTag("money_month").assertTextEquals("2026-09")
    }

    @Test
    fun b8_reselectMoney_closesSubScreen() {
        val exited = mutableStateOf(false)
        composeRule.setContent { BackHarness(exited) }
        composeRule.onNodeWithTag("nav_money").performClick()
        composeRule.onNodeWithTag("open_sub").performClick()
        composeRule.onNodeWithTag("money_sub").assertTextEquals("פתוח")
        composeRule.onNodeWithTag("nav_money").performClick()
        composeRule.onNodeWithTag("money_sub").assertTextEquals("סגור")
    }
}

class MonthHolder : ViewModel() {
    var month by mutableStateOf("2026-11")
    var subOpen by mutableStateOf(false)
}

@Composable
private fun BackHarness(exited: androidx.compose.runtime.MutableState<Boolean>) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val reselect = remember { MoneyReselectBus() }
    DisposableEffect(context) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { exited.value = true }
        }
        (context as ComponentActivity).onBackPressedDispatcher.addCallback(callback)
        onDispose { callback.remove() }
    }
    val screens = remember(navController, reselect) {
        backScreens(reselect)
    }
    val dispatcher = (context as ComponentActivity).onBackPressedDispatcher
    MyApplicationTheme {
        Box {
        CompositionLocalProvider(
            LocalLeaveTab provides { navController.openTab(ShellRoute.Home) },
            LocalMoneyReselect provides reselect,
        ) {
            AppShellContent(
                state = AppShellState(selectedRoute = ""),
                onDismissError = {},
                onSelectTab = { navController.openTab(it) { reselect.send() } },
            ) { modifier ->
                AppNavHost(
                    navController = navController,
                    session = null,
                    onOpenRoute = { navController.openTab(it) { reselect.send() } },
                    onRequestSignIn = {},
                    onSignedOut = {},
                    modifier = modifier,
                    screens = screens,
                )
            }
        }
        TextButton(
            onClick = { dispatcher.onBackPressed() },
            modifier = Modifier.testTag("press_back"),
        ) { Text("חזרה") }
        }
    }
}

private fun backScreens(reselect: MoneyReselectBus): ShellScreens = ShellScreens(
    home = { _, _, _ ->
        Text("בית", modifier = Modifier.testTag("screen_home"))
    },
    money = {
        val holder: MonthHolder = viewModel()
        LaunchedEffect(reselect) { reselect.ticks.collect { holder.subOpen = false } }
        TabRootBack()
        Column(modifier = Modifier.testTag("screen_money")) {
            Text(holder.month, modifier = Modifier.testTag("money_month"))
            Text(if (holder.subOpen) "פתוח" else "סגור", modifier = Modifier.testTag("money_sub"))
            TextButton(onClick = { holder.month = "2026-09" }, modifier = Modifier.testTag("pick_month")) {
                Text("חודש")
            }
            TextButton(onClick = { holder.subOpen = true }, modifier = Modifier.testTag("open_sub")) {
                Text("מסך")
            }
        }
    },
    me = { _, _, _, _, _ ->
        TabRootBack()
        Text("אני", modifier = Modifier.testTag("screen_me"))
    },
    signIn = { _, _ -> Text("התחברות") },
)
