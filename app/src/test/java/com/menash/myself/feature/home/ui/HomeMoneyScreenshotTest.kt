package com.menash.myself.feature.home.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MonthTask
import com.menash.myself.feature.money.SourceFreshness
import com.menash.myself.feature.money.StatementSource
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.testing.DarkQualifiers
import com.menash.myself.testing.FixedClock
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
 * Home with this month's money (stage 4): "נשאר לשוטפות" first, how fresh the files are, and the next step,
 * light/dark at font 1.0 and 1.3.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class HomeMoneyScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val october = YearMonth.of(2026, 10)
    private val state = HomeUiState(
        firstName = "דנה",
        moneySurplus = Money(410_000),
        moneyLeftForCurrent = Money(126_500),
        moneyTasks = listOf(
            MonthTask.Assign(october, 4),
            MonthTask.Close(october),
            MonthTask.Tithe(Money(42_000)),
        ),
        moneyFreshness = SourceFreshness(StatementSource(Source.HAPOALIM, "5678"), LocalDate.of(2026, 10, 22)),
    )

    @Test fun homeMoney_light_font10() = capture(dark = false, font = 1.0f)
    @Test fun homeMoney_light_font13() = capture(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun homeMoney_dark_font10() = capture(dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun homeMoney_dark_font13() = capture(dark = true, font = 1.3f)

    private fun capture(dark: Boolean, font: Float) {
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HomeContent(state = state, epochMillis = FixedClock.epochMillis,
                    onOpenRoute = {}, onOpenAccount = {}, debug = false)
            }
        }
    }
}
