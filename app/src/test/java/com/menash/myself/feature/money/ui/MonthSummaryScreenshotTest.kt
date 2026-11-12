package com.menash.myself.feature.money.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.MonthSummaryUi
import com.menash.myself.feature.money.monthSummary
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.MonthSnapshot
import com.menash.myself.feature.money.domain.PeriodicStatus
import com.menash.myself.testing.DarkQualifiers
import com.menash.myself.testing.LightQualifiers
import com.menash.myself.testing.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.YearMonth

/** "סיכום החודש" goldens: October after four closes, and a first close with nothing to compare. Data: [MoneySamples]. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class MonthSummaryScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private val october = YearMonth.of(2026, 10)

    private fun close(month: Int, income: Long, expense: Long, food: Long, balance: Long? = null): ClosedMonth {
        val m = YearMonth.of(2026, month)
        return ClosedMonth(
            m, MonthSnapshot(m, Money(income * 100), Money(expense * 100)),
            mapOf("fixed-3" to Money(520_000), "current-1" to Money(food * 100), "current-3" to Money(95_000), null to Money(40_000)),
            mapOf("per-holidays" to PeriodicStatus(Money(400_000), Money(520_000), Money(0))),
            balance?.let { mapOf("asset-bank" to Money(it * 100)) } ?: emptyMap(), emptyMap(), m.plusMonths(1).atDay(2),
        )
    }

    private val closes = listOf(
        close(7, 18_000, 14_600, 3_600, 52_000), close(8, 18_200, 14_100, 3_400),
        close(9, 18_400, 13_900, 3_500), close(10, 18_000, 13_300, 3_450, 61_000),
    )

    private fun shoot(all: Boolean, dark: Boolean, font: Float) {
        val data = MoneySamples.filled.copy(closedMonths = if (all) closes else closes.takeLast(1))
        val ui = MonthSummaryUi(october, monthSummary(data, october, MoneySamples.rate))
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { MonthSummaryContent(ui = ui, onBack = {}) }
        }
    }

    @Test fun monthSummary_light_font10() = shoot(all = true, dark = false, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun monthSummary_dark_font13() = shoot(all = true, dark = true, font = 1.3f)
    @Test fun monthSummaryFirst_light_font10() = shoot(all = false, dark = false, font = 1.0f)
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
