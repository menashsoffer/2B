package com.menash.myself.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.menash.myself.R
import com.menash.myself.core.auth.AuthSession
import com.menash.myself.core.auth.SignInCopy
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.feature.me.ui.MeActions
import com.menash.myself.feature.me.ui.MeContent
import com.menash.myself.feature.me.ui.MeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** B13: sign-in is a route. Back and success return to «אני», and drafts on the opened screen stay. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SignInFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shellShowsNavBar_hidesSignInOnly() {
        assertTrue(shellShowsNavBar(ShellRoute.Home))
        assertTrue(shellShowsNavBar(null))
        assertFalse(shellShowsNavBar(RequireSignIn.Route))
    }

    @Test
    fun signInCopy_holdsEachLabel() {
        val copy = SignInCopy(
            wordmark = "מותג",
            body = "גוף",
            spacesTitle = "מרחבים",
            spacesSubtitle = "שלי, שלה",
            syncTitle = "סנכרון",
            syncSubtitle = "מגובה",
            privacyTitle = "פרטיות",
            privacySubtitle = "מקומי",
            button = "התחבר",
            close = "סגירה",
        )
        assertEquals("סגירה", copy.close)
        assertEquals("מותג", copy.wordmark)
        assertEquals("שלי, שלה", copy.spacesSubtitle)
    }

    @Test
    fun signInCopy_keepsCurrentWording() {
        composeRule.setContent {
            MyApplicationTheme {
                Column {
                    Text(stringResource(R.string.sign_in_spaces_subtitle), modifier = Modifier.testTag("spaces"))
                    Text(stringResource(R.string.sign_in_sync_subtitle), modifier = Modifier.testTag("sync"))
                    Text(stringResource(R.string.sign_in_close), modifier = Modifier.testTag("close"))
                    Text(stringResource(R.string.sign_in_button), modifier = Modifier.testTag("button"))
                }
            }
        }
        composeRule.onNodeWithTag("spaces").assertTextEquals("שלי, שלה, ומשותף לבית")
        composeRule.onNodeWithTag("sync").assertTextEquals("עובד תמיד, נשמר בענן ומגובה")
        composeRule.onNodeWithTag("close").assertTextEquals("סגירה")
        composeRule.onNodeWithTag("button").assertTextEquals("התחבר באמצעות חשבון Google")
    }

    @Test
    fun b13_launch_opensHomeNotSignIn() {
        composeRule.setContent { Harness() }
        composeRule.onNodeWithTag("screen_home").assertExists()
        composeRule.onAllNodesWithTag("screen_sign_in").assertCountEquals(0)
        composeRule.onNodeWithTag("nav_home").assertExists()
    }

    @Test
    fun b13_backFromSignIn_returnsMeAndKeepsDrafts() {
        val back = BackProbe()
        composeRule.setContent { Harness(back) }
        composeRule.onNodeWithTag("nav_money").performClick()
        composeRule.onNodeWithTag("edit_money_draft").performClick()
        composeRule.onNodeWithTag("nav_me").performClick()
        composeRule.onNodeWithTag("edit_me_draft").performClick()
        composeRule.onNodeWithTag("me_sign_in").performClick()
        composeRule.onNodeWithTag("screen_sign_in").assertExists()
        composeRule.onAllNodesWithTag("nav_home").assertCountEquals(0)
        composeRule.runOnIdle { back.dispatcher.onBackPressed() }
        composeRule.onNodeWithTag("me_screen").assertExists()
        composeRule.onAllNodesWithTag("screen_sign_in").assertCountEquals(0)
        composeRule.onNodeWithTag("me_saveable_draft").assertTextEquals("טיוטה")
        composeRule.onNodeWithTag("nav_money").performClick()
        composeRule.onNodeWithTag("vm_draft").assertTextEquals("נשמר")
        composeRule.onNodeWithTag("saveable_draft").assertTextEquals("נשמר")
    }

    @Test
    fun b13_closeButton_returnsMe() {
        composeRule.setContent { Harness() }
        composeRule.onNodeWithTag("nav_me").performClick()
        composeRule.onNodeWithTag("me_sign_in").performClick()
        composeRule.onNodeWithTag("sign_in_close").performClick()
        composeRule.onNodeWithTag("me_screen").assertExists()
        composeRule.onAllNodesWithTag("screen_sign_in").assertCountEquals(0)
        composeRule.onNodeWithTag("nav_me").assertExists()
    }

    @Test
    fun b13_signInSuccess_returnsMeSignedInAndKeepsDraft() {
        composeRule.setContent { Harness() }
        composeRule.onNodeWithTag("nav_me").performClick()
        composeRule.onNodeWithTag("edit_me_draft").performClick()
        composeRule.onNodeWithTag("me_sign_in").performClick()
        composeRule.onNodeWithTag("sign_in_fake_success").performClick()
        composeRule.onNodeWithTag("me_screen").assertExists()
        composeRule.onAllNodesWithTag("screen_sign_in").assertCountEquals(0)
        composeRule.onAllNodesWithTag("me_sign_in").assertCountEquals(0)
        composeRule.onNodeWithTag("me_profile").assertExists()
        composeRule.onNodeWithTag("me_saveable_draft").assertTextEquals("טיוטה")
        composeRule.onNodeWithTag("sign_in_resumed").assertTextEquals("חזר")
        composeRule.onNodeWithTag("nav_money").performClick()
        composeRule.onNodeWithTag("screen_money").assertExists()
    }
}

class OriginDraftViewModel : ViewModel() {
    var note by mutableStateOf("התחלה")
}

private class BackProbe {
    lateinit var dispatcher: OnBackPressedDispatcher
}

@Composable
private fun Harness(back: BackProbe? = null) {
    val context = LocalContext.current
    SideEffect {
        back?.dispatcher = (context as ComponentActivity).onBackPressedDispatcher
    }
    val navController = rememberNavController()
    val session = remember { mutableStateOf<AuthSession?>(null) }
    val resumed = remember { mutableStateOf(false) }
    val entry by navController.currentBackStackEntryAsState()
    val selected = shellTabs.firstOrNull { tab ->
        entry?.destination?.hierarchy?.any { it.route == tab.route } == true
    }?.route.orEmpty()
    val screens = remember(navController) {
        harnessScreens(navController, session, resumed)
    }
    MyApplicationTheme {
        AppShellContent(
            state = AppShellState(selectedRoute = selected),
            onDismissError = {},
            onSelectTab = { navController.openShellRoute(it) },
            showNavBar = shellShowsNavBar(entry?.destination?.route),
        ) { modifier ->
            AppNavHost(
                navController = navController,
                session = session.value,
                onOpenRoute = { navController.openShellRoute(it) },
                onRequestSignIn = { navController.requireSignIn(signedIn = session.value != null) },
                onSignedOut = { session.value = null },
                modifier = modifier,
                screens = screens,
            )
        }
    }
}

private fun harnessScreens(
    navController: NavHostController,
    session: MutableState<AuthSession?>,
    resumed: MutableState<Boolean>,
): ShellScreens = ShellScreens(
    home = { _, _, _ ->
        Text("בית", modifier = Modifier.testTag("screen_home"))
    },
    money = {
        val draft: OriginDraftViewModel = viewModel()
        var saved by rememberSaveable { mutableStateOf("התחלה") }
        Column(modifier = Modifier.testTag("screen_money")) {
            Text(draft.note, modifier = Modifier.testTag("vm_draft"))
            Text(saved, modifier = Modifier.testTag("saveable_draft"))
            TextButton(
                onClick = {
                    draft.note = "נשמר"
                    saved = "נשמר"
                },
                modifier = Modifier.testTag("edit_money_draft"),
            ) { Text("עריכה") }
        }
    },
    me = { current, onSignIn, _, _, _ ->
        RequireSignInEffect(navController) { resumed.value = true }
        var saved by rememberSaveable { mutableStateOf("ריק") }
        Column {
            Text(saved, modifier = Modifier.testTag("me_saveable_draft"))
            Text(if (resumed.value) "חזר" else "עוד לא", modifier = Modifier.testTag("sign_in_resumed"))
            TextButton(
                onClick = { saved = "טיוטה" },
                modifier = Modifier.testTag("edit_me_draft"),
            ) { Text("עריכה") }
            MeContent(
                state = MeState(
                    signedIn = current != null,
                    displayName = current?.displayName,
                    email = current?.email,
                    versionName = "1.0",
                    versionCode = 1,
                ),
                actions = MeActions(onSignIn = onSignIn),
            )
        }
    },
    signIn = { onSuccess, onClose ->
        Column {
            TextButton(onClick = onClose, modifier = Modifier.testTag("sign_in_close")) {
                Text(stringResource(R.string.sign_in_close))
            }
            TextButton(
                onClick = {
                    session.value = AuthSession(uid = "user-1", displayName = "דנה", email = "dana@example.com")
                    onSuccess()
                },
                modifier = Modifier.testTag("sign_in_fake_success"),
            ) { Text("הצלחה") }
        }
    },
)
