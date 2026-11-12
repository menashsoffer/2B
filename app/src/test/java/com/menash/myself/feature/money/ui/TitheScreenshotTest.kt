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
import com.menash.myself.feature.money.DonationDraft
import com.menash.myself.feature.money.DonationEditor
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.MoneySamples.shekels
import com.menash.myself.feature.money.TitheUi
import com.menash.myself.feature.money.domain.ActualIncome
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.importer.ImportedIncome
import com.menash.myself.feature.money.settingsDraftOf
import com.menash.myself.feature.money.titheView
import com.menash.myself.testing.DarkQualifiers
import com.menash.myself.testing.LightQualifiers
import com.menash.myself.testing.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * "מעשרות" goldens, light/dark at font 1.0 and 1.3: September on the plan and October from a linked import with
 * one line still to link, the portfolio part and the donations; the first-time screen; and both sheets.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class TitheScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val october = YearMonth.of(2026, 10)
    private val today = LocalDate.of(2026, 10, 30)
    private val data = MoneySamples.filled.copy(donations = MoneySamples.donations, tithe = MoneySamples.tithe)
    private val imported = ImportedIncome(listOf(ActualIncome(MoneySamples.salary.id, october, shekels(17_240))), mapOf(october to 2))
    private val assets = data.assets.filter { it.kind == AssetKind.ASSET }

    @Test fun titheBalance_light_font10() = balance(dark = false, font = 1.0f)
    @Test fun titheBalance_light_font13() = balance(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun titheBalance_dark_font10() = balance(dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun titheBalance_dark_font13() = balance(dark = true, font = 1.3f)

    @Test fun titheStart_light_font10() = screen(TitheUi(true, null, october, today), dark = false, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun titheStart_dark_font13() = screen(TitheUi(true, null, october, today), dark = true, font = 1.3f)

    @Test fun titheDonation_light_font13() = donation(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun titheDonation_dark_font10() = donation(dark = true, font = 1.0f)

    @Test fun titheSettings_light_font13() = settings(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun titheSettings_dark_font10() = settings(dark = true, font = 1.0f)

    private fun balance(dark: Boolean, font: Float) =
        screen(TitheUi(true, titheView(data, october, imported, shekels(3_000)), october, today, assets), dark, font)

    private fun screen(ui: TitheUi, dark: Boolean, font: Float) {
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { TitheContent(ui = ui, actions = TitheActions()) }
        }
    }

    private fun donation(dark: Boolean, font: Float) = sheet(dark, font) {
        val editor = DonationEditor(DonationDraft(amount = "180", organisation = "קופת העיר", date = today.minusDays(3)), isNew = true)
        DonationForm(editor, today, {}, {}, {})
    }

    private fun settings(dark: Boolean, font: Float) = sheet(dark, font) {
        TitheSettingsForm(settingsDraftOf(MoneySamples.tithe, october), october, assets, {}, {})
    }

    private fun sheet(dark: Boolean, font: Float, content: @Composable () -> Unit) {
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.appColors.card)) { content() } }
        }
    }
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
