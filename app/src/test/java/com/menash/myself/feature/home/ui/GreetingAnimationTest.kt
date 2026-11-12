package com.menash.myself.feature.home.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** The line draws on the first launch and the returning sentence is not shown before the record loads. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class GreetingAnimationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun frames_drawUnderlineThenLeaf() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MyApplicationTheme { GreetingLine(text = "נעים להכיר", animate = true) }
        }
        assertProgress(0f, 0f)
        composeRule.mainClock.advanceTimeBy(150)
        assertProgress(0f, 0f)
        composeRule.mainClock.advanceTimeBy(600)
        assertProgress(1f, 0f)
        composeRule.mainClock.advanceTimeBy(250)
        assertProgress(1f, 1f)
    }

    @Test
    fun beforeTheRecordLoads_noGreetingText() {
        composeRule.setContent {
            val decision = rememberGreetingDecision(null, LocalDate.of(2026, 11, 12), "דנה", false)
            if (decision == null) GreetingReserve() else GreetingLine(decision.text, decision.animate)
        }
        assertEquals(0, composeRule.onAllNodesWithText("טוב לראות", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("נעים להכיר", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun firstLaunch_showsMeetYouAndMarksOnlyAfterTheAnimation() {
        composeRule.mainClock.autoAdvance = false
        val today = LocalDate.of(2026, 11, 12)
        var marks by mutableIntStateOf(0)
        composeRule.setContent {
            val decision = rememberGreetingDecision(GreetingRecord(), today, "דנה", reducedMotion = false)
            LaunchedEffect(decision) {
                val chosen = decision ?: return@LaunchedEffect
                if (!chosen.mark) return@LaunchedEffect
                if (chosen.animate) delay(1_000)
                marks++
            }
            GreetingLine(decision!!.text, animate = decision.animate)
        }
        composeRule.onNodeWithText("נעים להכיר, דנה").fetchSemanticsNode()
        assertEquals(0, composeRule.onAllNodesWithText("טוב לראות", substring = true).fetchSemanticsNodes().size)
        composeRule.mainClock.advanceTimeBy(400)
        assertEquals(0, marks)
        composeRule.mainClock.advanceTimeBy(700)
        composeRule.onNodeWithText("נעים להכיר, דנה").fetchSemanticsNode()
        assertEquals(1, marks)
    }

    private fun assertProgress(underline: Float, leaf: Float) {
        val node = composeRule.onNodeWithTag("home_greeting").fetchSemanticsNode()
        assertEquals(underline, node.config[GreetingUnderlineProgress], 0.05f)
        assertEquals(leaf, node.config[GreetingLeafProgress], 0.05f)
    }
}
