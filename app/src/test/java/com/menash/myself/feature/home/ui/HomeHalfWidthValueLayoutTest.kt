package com.menash.myself.feature.home.ui

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.HomeContentAtFixedClock
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.ProvideR2FontScale
import com.menash.myself.r2.collectTexts
import com.menash.myself.r2.containsBounds
import com.menash.myself.r2.hasNumeralDigit
import com.menash.myself.r2.textLayout
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
 * Half-width MetricCard values at fontScale 1.3 stay on one line inside the card.
 * `@Config(fontScale = 1.3f)` sets Configuration.fontScale. [ProvideR2FontScale] sets the
 * same absolute LocalDensity font scale. They are not multiplied. See R2ScreenshotCapture.
 *
 * Shopping has no value. The dreams count is the only half-width value, and it is checked.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers, fontScale = 1.3f)
class HomeHalfWidthValueLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dreams4_light_staysOnOneLine() = assertHalfWidthValues(dreams = 4, darkTheme = false)

    @Test
    fun dreams128_light_staysOnOneLine() = assertHalfWidthValues(dreams = 128, darkTheme = false)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers, fontScale = 1.3f)
    fun dreams128_dark_staysOnOneLine() = assertHalfWidthValues(dreams = 128, darkTheme = true)

    private fun assertHalfWidthValues(dreams: Int, darkTheme: Boolean) {
        composeRule.setContent {
            ProvideR2FontScale(fontScale = 1.3f) {
                MyApplicationTheme(darkTheme = darkTheme) {
                    HomeContentAtFixedClock(dreamsActive = dreams)
                }
            }
        }
        val value = dreams.toString()
        val dreamDetail = if (dreams == 1) "חלום פעיל" else "חלומות פעילים"
        assertValueOnOneLine(label = "חלומות", detail = dreamDetail, value = value)
        assertCardHasNoValue(label = "קניות", detail = "עוד אין נתונים")
        val dreamCard = composeRule.onNode(hasText("חלומות") and hasText(value)).fetchSemanticsNode()
        val shopCard = composeRule.onNode(hasText("קניות") and hasText("עוד אין נתונים")).fetchSemanticsNode()
        val dreamWidth = dreamCard.boundsInRoot.width
        val shopWidth = shopCard.boundsInRoot.width
        assertEquals(dreamWidth, shopWidth, 1f)
        assertTrue(dreamCard.boundsInRoot.left > shopCard.boundsInRoot.left)
    }

    private fun assertValueOnOneLine(label: String, detail: String, value: String) {
        val card = composeRule.onNode(hasText(label) and hasText(value)).fetchSemanticsNode()
        val valueNode = composeRule.onNodeWithText(value, useUnmergedTree = true)
        val layout = valueNode.textLayout()
        val valueBounds = valueNode.fetchSemanticsNode().boundsInRoot
        val lineWidth = layout.getLineRight(0) - layout.getLineLeft(0)
        // GetTextLayoutResult rebuilds MultiParagraph at the max constraint width while
        // size stays the tight glyph size, so hasVisualOverflow is true for any value
        // narrower than the card. Ellipsis, line count, glyph width, and bounds are the
        // clip signals.
        val detailText = "size=${layout.size} lineWidth=$lineWidth lineCount=${layout.lineCount} " +
            "ellipsis=${layout.isLineEllipsized(0)} overflowH=${layout.didOverflowHeight} " +
            "value=$valueBounds card=${card.boundsInRoot}"
        assertEquals(detailText, 1, layout.lineCount)
        assertFalse(detailText, layout.isLineEllipsized(0))
        assertFalse(detailText, layout.didOverflowHeight)
        assertTrue(detailText, lineWidth <= valueBounds.width + 0.5f)
        assertTrue(detailText, card.boundsInRoot.containsBounds(valueBounds))
        val extra = card.collectTexts().filter { text -> text != label && text != detail && text != value }
        assertTrue("unexpected text on $label card: $extra", extra.isEmpty())
    }

    private fun assertCardHasNoValue(label: String, detail: String) {
        val card: SemanticsNode = composeRule.onNode(hasText(label) and hasText(detail)).fetchSemanticsNode()
        val texts = card.collectTexts()
        val values = texts.filter { text -> text != label && text != detail }
        assertTrue("$label rendered a value: $values", values.isEmpty())
        texts.forEach { text ->
            assertFalse("$label card contains a digit in \"$text\"", text.hasNumeralDigit())
        }
    }
}
