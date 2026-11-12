package com.menash.myself.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The greeting node comes from a real DataStore visit record: first, returning, guest, and a long Latin name. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class GreetingVisitStoreTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 11, 12)

    @Test
    fun firstVisit_showsMeetYou() {
        assertGreeting(store(), "דנה כהן", "נעים להכיר, דנה")
    }

    @Test
    fun returningVisit_showsWelcomeBack() {
        val store = store()
        runBlocking { store.markShown(today.minusDays(3)) }
        assertGreeting(store, "דנה", "טוב לראות אותך שוב, דנה")
    }

    @Test
    fun guest_hasNoName() {
        assertGreeting(store(), null, "נעים להכיר")
    }

    @Test
    fun longLatinName_isIsolatedEllipsizedAndTwoLines() {
        val store = store()
        composeRule.setContent {
            MyApplicationTheme {
                Box(Modifier.width(32.dp).height(600.dp)) {
                    VisitGreeting(store, "Abcdefghijklmnopq Smith", narrow = true)
                }
            }
        }
        awaitGreeting()
        val text = greetingText()
        assertEquals("נעים להכיר, \u2068Abcdefghijklmno…\u2069", text)
        assertTrue(text.contains('\u2068') && text.contains('\u2069') && text.contains('…'))
        val bounds = composeRule.onNodeWithTag("home_greeting").getUnclippedBoundsInRoot()
        val greetingHeight = bounds.bottom - bounds.top
        assertTrue("greeting height $greetingHeight", greetingHeight < 96.dp)
    }

    private fun assertGreeting(store: GreetingStore, name: String?, expected: String) {
        composeRule.setContent {
            MyApplicationTheme { VisitGreeting(store, name, narrow = false) }
        }
        awaitGreeting()
        assertEquals(expected, greetingText())
    }

    @Composable
    private fun VisitGreeting(store: GreetingStore, name: String?, narrow: Boolean) {
        val decision = rememberVisitGreeting(store, name, today, reducedMotion = true)
        HomeContent(
            state = HomeUiState(firstName = firstNameOf(name)),
            hour = 0,
            epochMillis = 1_762_931_200_000,
            greeting = decision?.text,
            animateGreeting = decision?.animate == true,
            showGreeting = decision != null,
            onOpenRoute = {},
            onOpenAccount = {},
            modifier = if (narrow) Modifier.width(32.dp) else Modifier,
        )
    }

    private fun awaitGreeting() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("home_greeting").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun greetingText(): String =
        composeRule.onNodeWithTag("home_greeting").fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString("") { it.text }

    private fun store(): GreetingStore {
        val file = File(System.getProperty("java.io.tmpdir"), "greeting-${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { file },
        )
        return GreetingStore(dataStore)
    }
}
