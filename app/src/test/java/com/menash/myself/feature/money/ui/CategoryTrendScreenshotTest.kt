package com.menash.myself.feature.money.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.CategoryTrendUi
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.categoryTrend
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

/** "מגמה" goldens: food over eight closes with a gap and a suggestion, and the same before any month is closed. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class CategoryTrendScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 11, 2)

    private fun close(month: Int, food: Long): ClosedMonth {
        val m = YearMonth.of(2026, month)
        return ClosedMonth(m, MonthSnapshot(m, Money(1_800_000), Money(1_300_000)), mapOf("current-1" to Money(food * 100)), emptyMap(), emptyMap(), emptyMap(), m.plusMonths(1).atDay(2))
    }

    private val closes = listOf(close(2, 2_900), close(3, 3_100), close(4, 3_300), close(5, 2_800), close(6, 3_600), close(8, 3_500), close(9, 3_400), close(10, 3_450))

    private fun shoot(categoryId: String, dark: Boolean, font: Float, withCloses: Boolean = true) {
        val data = MoneySamples.filled.copy(closedMonths = if (withCloses) closes else emptyList())
        val ui = CategoryTrendUi(categoryId, categoryTrend(data, categoryId, today, MoneySamples.rate))
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { CategoryTrendContent(ui = ui, actions = CategoryTrendActions()) }
        }
    }

    @Test fun trendFood_light_font10() = shoot("current-1", dark = false, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun trendFood_dark_font13() = shoot("current-1", dark = true, font = 1.3f)
    @Test fun trendEmpty_light_font10() = shoot("current-1", dark = false, font = 1.0f, withCloses = false)
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
