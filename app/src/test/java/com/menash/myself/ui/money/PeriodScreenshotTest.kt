package com.menash.myself.ui.money

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.PeriodPreset
import com.menash.myself.feature.money.PeriodUi
import com.menash.myself.feature.money.periodView
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.MonthSnapshot
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.YearMonth

/** "סיכום תקופה" goldens: this year with a missing month, and the empty state. Data: [MoneySamples] plus closes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class PeriodScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 11, 2)

    private fun close(month: Int, income: Long, expense: Long, food: Long, balance: Long? = null): ClosedMonth {
        val m = YearMonth.of(2026, month)
        return ClosedMonth(
            m, MonthSnapshot(m, Money(income * 100), Money(expense * 100)),
            mapOf("fixed-3" to Money(520_000), "current-1" to Money(food * 100), "current-3" to Money(85_000), null to Money(40_000)),
            emptyMap(), balance?.let { mapOf("asset-bank" to Money(it * 100)) } ?: emptyMap(), emptyMap(), m.plusMonths(1).atDay(2),
        )
    }

    private val closes = listOf(
        close(5, 18_000, 13_400, 2_900, 52_000), close(6, 18_200, 14_100, 3_300), close(7, 18_000, 18_600, 3_600),
        close(9, 18_400, 13_900, 3_100), close(10, 18_000, 13_300, 2_800, 61_000),
    )

    private fun lastYear(month: Int, expense: Long, food: Long): ClosedMonth {
        val m = YearMonth.of(2025, month)
        return ClosedMonth(
            m, MonthSnapshot(m, Money(1_750_000), Money(expense * 100)), mapOf("fixed-3" to Money(500_000), "current-1" to Money(food * 100)),
            emptyMap(), emptyMap(), emptyMap(), m.plusMonths(1).atDay(2),
        )
    }

    private val previousYear = listOf(lastYear(10, 12_900, 2_600), lastYear(11, 13_200, 2_700), lastYear(12, 13_600, 2_900))

    private fun shoot(preset: PeriodPreset, withCloses: Boolean, dark: Boolean, font: Float, compare: Boolean = false) {
        val data = if (withCloses) MoneySamples.filled.copy(closedMonths = closes + if (compare) previousYear else emptyList()) else MoneySamples.filled
        val ui = PeriodUi(open = true, view = periodView(data, preset, today, MoneySamples.rate))
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { PeriodContent(ui = ui, actions = PeriodActions()) }
        }
    }

    @Test fun periodYear_light_font10() = shoot(PeriodPreset.THIS_YEAR, withCloses = true, dark = false, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun periodYear_dark_font13() = shoot(PeriodPreset.THIS_YEAR, withCloses = true, dark = true, font = 1.3f)
    @Test fun periodCompare_light_font10() = shoot(PeriodPreset.THIS_YEAR, withCloses = true, dark = false, font = 1.0f, compare = true)
    @Test fun periodEmpty_light_font10() = shoot(PeriodPreset.LAST_YEAR, withCloses = true, dark = false, font = 1.0f)
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
