package com.menash.myself.feature.home.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.testing.collectHeadings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The greeting is one heading. The line and leaf are decoration and are not read again. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class GreetingLineTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reducedMotion_isOneHeadingWithTheFullSentence() {
        val text = greetingLine("טוב לראות אותך שוב", "דנה")
        composeRule.setContent {
            MyApplicationTheme { GreetingLine(text = text, animate = false) }
        }
        val root = composeRule.onRoot().fetchSemanticsNode()
        val headings = root.collectHeadings()
        assertEquals(1, headings.size)
        val spoken = headings.first().config.getOrNull(SemanticsProperties.Text)
            .orEmpty()
            .joinToString(separator = "") { it.text }
        assertEquals(text, spoken)
        composeRule.onNodeWithTag("home_greeting").assertExists()
    }
}
