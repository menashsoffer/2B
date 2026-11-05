package com.menash.myself.feature.money.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.feature.money.TransactionsQuery
import com.menash.myself.feature.money.TransactionsUi
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.monthLines
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.YearMonth

/**
 * Transactions goldens, light/dark at font 1.0 and 1.3: November from the synthetic Max and Cal files with
 * one merchant categorised, and an empty month.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class TransactionsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val categories = defaultCategories()
    private val november = YearMonth.of(2026, 11)
    private val stored = listOf("max_nov.xlsx", "cal_nov.xlsm").flatMap { ImportFixtures.parse(it).rows }
        .map { StoredTransaction(it, categoryId = if (it.description == "שופרסל דיל 123") "current-1" else null) }

    @Test fun txList_light_font10() = list(dark = false, font = 1.0f)
    @Test fun txList_light_font13() = list(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun txList_dark_font10() = list(dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun txList_dark_font13() = list(dark = true, font = 1.3f)

    @Test fun txEmpty_light_font10() = empty(dark = false, font = 1.0f)
    @Test fun txEmpty_light_font13() = empty(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun txEmpty_dark_font10() = empty(dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun txEmpty_dark_font13() = empty(dark = true, font = 1.3f)

    private fun list(dark: Boolean, font: Float) = capture(dark, font, november)

    private fun empty(dark: Boolean, font: Float) = capture(dark, font, YearMonth.of(2026, 6))

    private fun capture(dark: Boolean, font: Float, month: YearMonth) {
        val ui = TransactionsUi(TransactionsQuery(month = month), monthLines(stored, month, ImportMemory(), categories), categories)
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { TransactionsContent(ui = ui, actions = TransactionsActions()) }
        }
    }
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
