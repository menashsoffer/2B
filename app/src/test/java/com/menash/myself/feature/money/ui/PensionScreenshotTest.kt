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
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.FundDraft
import com.menash.myself.feature.money.FundEditor
import com.menash.myself.feature.money.FundField
import com.menash.myself.feature.money.PensionUi
import com.menash.myself.feature.money.fundDraftOf
import com.menash.myself.feature.money.pensionView
import com.menash.myself.feature.money.domain.FundKind
import com.menash.myself.feature.money.domain.PensionFund
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.LocalDate

/**
 * "פנסיה וחיסכון" goldens, light/dark at font 1.0 and 1.3: three funds with the priciest by shekels, a stale
 * hishtalmut that is already liquid and a gemel; the empty screen; and the fund sheet new and with an error.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class PensionScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 30)
    private val funds = listOf(
        PensionFund(
            "p", FundKind.PENSION, "הראל", Money(31_250_000), Money(24_000_000), Money(420_000),
            BigDecimal("0.22"), BigDecimal("1.49"), null, LocalDate.of(2026, 10, 1),
        ),
        PensionFund(
            "h", FundKind.HISHTALMUT, "מיטב", Money(12_480_000), Money(10_200_000), Money(157_000),
            BigDecimal("0.7"), BigDecimal.ZERO, LocalDate.of(2019, 6, 1), LocalDate.of(2026, 5, 12),
        ),
        PensionFund(
            "g", FundKind.GEMEL, "אלטשולר", Money(1_830_000), Money(2_000_000), Money(0),
            BigDecimal("0.6"), BigDecimal.ZERO, null, LocalDate.of(2026, 9, 20),
        ),
    )

    @Test fun pensionFunds_light_font10() = screen(PensionUi(true, pensionView(funds, today)), dark = false, font = 1.0f)
    @Test fun pensionFunds_light_font13() = screen(PensionUi(true, pensionView(funds, today)), dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun pensionFunds_dark_font10() = screen(PensionUi(true, pensionView(funds, today)), dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun pensionFunds_dark_font13() = screen(PensionUi(true, pensionView(funds, today)), dark = true, font = 1.3f)

    @Test fun pensionEmpty_light_font10() = screen(PensionUi(true, pensionView(emptyList(), today)), dark = false, font = 1.0f)

    @Test fun pensionFormNew_light_font13() = sheet(dark = false, font = 1.3f) {
        FundForm(FundEditor(FundDraft(kind = FundKind.HISHTALMUT, provider = "מיטב"), isNew = true), today, {}, {}, {})
    }

    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun pensionFormError_dark_font10() = sheet(dark = true, font = 1.0f) {
        val draft = fundDraftOf(funds[1]).copy(feeOnBalance = "14")
        FundForm(FundEditor(draft, isNew = false, invalid = FundField.FEE_BALANCE), today, {}, {}, {})
    }

    private fun screen(ui: PensionUi, dark: Boolean, font: Float) {
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { PensionContent(ui = ui, actions = PensionActions()) }
        }
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
