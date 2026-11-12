package com.menash.myself.shell

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.testing.FixedClock
import com.menash.myself.testing.LightQualifiers
import com.menash.myself.testing.SampleName
import com.menash.myself.testing.collectDescriptions
import com.menash.myself.testing.collectHeadings
import com.menash.myself.testing.collectTestTags
import com.menash.myself.testing.collectTexts
import com.menash.myself.testing.dateLineText
import com.menash.myself.testing.greetingText
import com.menash.myself.testing.r2HomeShell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Home shell tree at the fixed clock. No TopAppBar and no capture FAB.
 *
 * pre-fix1: MainActivity still declares HiddenCaptureFab, but captureFabEnabled() is false,
 * so the FAB is not composed. This tree is HomeContent plus the nav, which is what the home
 * route draws. The assertion stays after fix1 removes the dead composable.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class HomeChromeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeTree_hasNoTopAppBarAndNoFab() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            MyApplicationTheme {
                r2HomeShell(dreamsActive = null)
            }
        }
        val root = composeRule.onRoot().fetchSemanticsNode()
        val tags = root.collectTestTags()
        assertFalse(tags.contains("persistent_capture_fab"))
        val descriptions = root.collectDescriptions()
        val captureLabel = context.getString(R.string.ui_str_7)
        assertEquals("לכידה מהירה", captureLabel)
        assertFalse(descriptions.contains(captureLabel))
        val backLabel = context.getString(R.string.ui_str_180)
        assertFalse(descriptions.contains(backLabel))
        val headings = root.collectHeadings()
        assertEquals(1, headings.size)
        val greeting = greetingText(context, FixedClock.HOUR, SampleName)
        val headingText = headings.first().config.getOrNull(SemanticsProperties.Text)
            .orEmpty()
            .joinToString(separator = "") { text -> text.text }
        assertEquals(greeting, headingText)
        assertTrue(root.collectTexts().contains(dateLineText(context)))
    }
}
