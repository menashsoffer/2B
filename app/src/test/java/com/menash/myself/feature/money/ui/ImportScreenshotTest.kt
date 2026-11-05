package com.menash.myself.feature.money.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.ImportUi
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.importer.ImportFile
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.reviewImport
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Import goldens, light/dark at font 1.0 and 1.3: the review of a Max file whose total is off plus a
 * refused file, and the category list for a new merchant. Data is the synthetic fixtures only.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class ImportScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val categories = defaultCategories()
    private val review = reviewImport(
        listOf("max_mismatch.xlsx", "encrypted.xls").map { ImportFile(it, ImportFixtures.bytes(it)) },
        emptyMap(), ImportMemory(), categories,
    )

    @Test fun importReview_light_font10() = summary(dark = false, font = 1.0f)
    @Test fun importReview_light_font13() = summary(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun importReview_dark_font10() = summary(dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun importReview_dark_font13() = summary(dark = true, font = 1.3f)

    @Test fun importCategories_light_font10() = picker(dark = false, font = 1.0f)
    @Test fun importCategories_light_font13() = picker(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun importCategories_dark_font10() = picker(dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun importCategories_dark_font13() = picker(dark = true, font = 1.3f)

    private fun summary(dark: Boolean, font: Float) {
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { ImportContent(ui = ImportUi.Review(review, categories), actions = ImportActions()) }
        }
    }

    private fun picker(dark: Boolean, font: Float) {
        val merchant = review.merchants.single { it.name == "שופרסל דיל 123" }
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.appColors.card)) {
                    ImportCategoryList(merchant = merchant, categories = categories, onDecide = {})
                }
            }
        }
    }
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
