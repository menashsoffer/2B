package com.menash.myself.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.HeroCard
import com.menash.myself.core.designsystem.component.MetricCard
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.r2.HomeContentAtFixedClock
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.collectTexts
import com.menash.myself.r2.dateLineText
import com.menash.myself.r2.hasNumeralDigit
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
 * An empty home or card shows no metric. The Gregorian date line may contain the day number;
 * the spec requires that date. Every other semantics text must have no digit.
 * Scanned: ASCII 0-9, Arabic-Indic, Eastern Arabic-Indic. Hebrew letters are not digits.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class HomeNoFakeNumberTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun emptyHome_onlyTheDateLineContainsADigit() {
        composeRule.setContent {
            MyApplicationTheme {
                HomeContentAtFixedClock(dreamsActive = null)
            }
        }
        val dateLine = dateLineText(context)
        val withDigits = composeRule.onRoot().fetchSemanticsNode().collectTexts()
            .filter { text -> text.hasNumeralDigit() }
        assertEquals(listOf(dateLine), withDigits)
    }

    @Test
    fun emptyHomeCards_haveNoDigit() {
        composeRule.setContent {
            MyApplicationTheme {
                HomeContentAtFixedClock(dreamsActive = null)
            }
        }
        val money = context.getString(R.string.nav_money)
        val message = context.getString(R.string.home_money_message)
        val dreams = context.getString(R.string.nav_dreams)
        val shopping = context.getString(R.string.nav_shopping)
        val tracking = context.getString(R.string.nav_tracking)
        val noData = context.getString(R.string.home_no_data)
        assertEquals("כסף", money)
        assertEquals("יתחיל לעבוד אחרי שנגדיר את החודש הראשון", message)
        assertEquals("חלומות", dreams)
        assertEquals("קניות", shopping)
        assertEquals("מעקב", tracking)
        assertEquals("עוד אין נתונים", noData)
        listOf(
            hasText(money) and hasText(message),
            hasText(dreams) and hasText(noData),
            hasText(shopping) and hasText(noData),
            hasText(tracking) and hasText(noData),
        ).forEach { matcher ->
            composeRule.onNode(matcher).fetchSemanticsNode().collectTexts().forEach { text ->
                assertFalse("card text \"$text\" contains a digit", text.hasNumeralDigit())
            }
        }
    }

    @Test
    fun standaloneEmptyHeroAndMetric_haveNoDigit() {
        composeRule.setContent {
            MyApplicationTheme {
                Column {
                    HeroCard(
                        label = context.getString(R.string.nav_money),
                        message = context.getString(R.string.home_money_message),
                        value = null,
                    )
                    MetricCard(
                        label = context.getString(R.string.nav_shopping),
                        value = null,
                        detail = context.getString(R.string.home_no_data),
                    )
                }
            }
        }
        val texts = composeRule.onRoot().fetchSemanticsNode().collectTexts()
        assertTrue(texts.isNotEmpty())
        texts.forEach { text ->
            assertFalse("standalone card \"$text\" contains a digit", text.hasNumeralDigit())
        }
    }
}
