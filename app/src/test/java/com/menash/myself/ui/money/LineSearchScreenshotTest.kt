package com.menash.myself.ui.money

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
import com.menash.myself.feature.money.search.LineSearchScope
import com.menash.myself.feature.money.search.searchLines
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Search goldens: the hint before typing, results across two months, and no results. Synthetic files only. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class LineSearchScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val categories = defaultCategories()
    private val stored = listOf("max_nov.xlsx", "hapoalim_oct.xlsx").flatMap { ImportFixtures.parse(it).rows }
        .map { StoredTransaction(it, categoryId = if (it.description == "שופרסל דיל 123") "current-1" else null) }

    @Test fun searchHint_light_font10() = shoot("", dark = false, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun searchHint_dark_font13() = shoot("", dark = true, font = 1.3f)
    @Test fun searchResults_light_font10() = shoot("שופרסל", dark = false, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun searchResults_dark_font10() = shoot("שופרסל", dark = true, font = 1.0f)
    @Test fun searchResults_light_font13() = shoot("שופרסל", dark = false, font = 1.3f)
    @Test fun searchUnassigned_light_font10() = shoot("", dark = false, font = 1.0f, scope = LineSearchScope(onlyUnassigned = true))
    @Test fun searchNoResults_light_font10() = shoot("רמי לוי", dark = false, font = 1.0f)

    private fun shoot(text: String, dark: Boolean, font: Float, scope: LineSearchScope = LineSearchScope()) {
        val ui = TransactionsUi(
            query = TransactionsQuery(search = scope, text = text),
            categories = categories,
            results = searchLines(stored, ImportMemory(), categories, text, scope),
        )
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { TransactionsContent(ui = ui, actions = TransactionsActions()) }
        }
    }
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
