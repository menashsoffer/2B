package com.menash.myself.feature.home.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The line draws once the visit record loads, and the day is marked when that draw starts. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class GreetingAnimationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 11, 12)

    @Test
    fun beforeTheRecordLoads_noGreetingText() {
        composeRule.setContent {
            val decision = rememberGreetingDecision(null, today, "דנה", false)
            if (decision == null) GreetingReserve() else GreetingLine(decision.text, decision.animate)
        }
        assertEquals(0, composeRule.onAllNodesWithText("טוב לראות", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("נעים להכיר", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun recordLoads_drawsFromZeroAndMarksWhenTheDrawStarts() {
        composeRule.mainClock.autoAdvance = false
        val store = store()
        val marked = java.util.concurrent.atomic.AtomicReference<LocalDate?>()
        Thread {
            runCatching {
                runBlocking {
                    store.record.collect { if (it.lastShown == today) marked.set(it.lastShown) }
                }
            }
        }.apply { isDaemon = true }.start()
        Thread.sleep(100)
        composeRule.setContent {
            CompositionLocalProvider(LocalGreetingInspection provides true) {
                MyApplicationTheme {
                    val decision = rememberVisitGreeting(store, "דנה", today, reducedMotion = false)
                    if (decision == null) GreetingReserve() else GreetingLine(decision.text, decision.animate)
                }
            }
        }
        assertEquals(0, composeRule.onAllNodesWithTag("home_greeting").fetchSemanticsNodes().size)
        repeat(30) {
            if (composeRule.onAllNodesWithTag("home_greeting").fetchSemanticsNodes().isNotEmpty()) return@repeat
            composeRule.mainClock.advanceTimeByFrame()
        }
        composeRule.onNodeWithTag("home_greeting").fetchSemanticsNode()
        composeRule.mainClock.advanceTimeByFrame()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, composeRule.onAllNodesWithText("טוב לראות", substring = true).fetchSemanticsNodes().size)
        assertProgress(0f, 0f)
        composeRule.mainClock.advanceTimeBy(150)
        assertProgress(0f, 0f)
        composeRule.mainClock.advanceTimeBy(250)
        val underline = composeRule.onNodeWithTag("home_greeting").fetchSemanticsNode().config[GreetingUnderlineProgress]
        assertTrue(underline > 0f && underline < 1f)
        val started = System.currentTimeMillis()
        while (marked.get() == null && System.currentTimeMillis() - started < 2_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertEquals(today, marked.get())
        composeRule.mainClock.advanceTimeBy(350)
        assertProgress(1f, 0f)
        composeRule.mainClock.advanceTimeBy(250)
        assertProgress(1f, 1f)
        assertTrue(composeRule.onAllNodesWithText("נעים להכיר", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertEquals(0, composeRule.onAllNodesWithText("טוב לראות", substring = true).fetchSemanticsNodes().size)
    }

    private fun assertProgress(underline: Float, leaf: Float) {
        val node = composeRule.onNodeWithTag("home_greeting").fetchSemanticsNode()
        assertEquals(underline, node.config[GreetingUnderlineProgress], 0.05f)
        assertEquals(leaf, node.config[GreetingLeafProgress], 0.05f)
    }

    private fun store(): GreetingStore {
        val file = File(System.getProperty("java.io.tmpdir"), "greeting-anim-${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { file },
        )
        return GreetingStore(dataStore)
    }
}
