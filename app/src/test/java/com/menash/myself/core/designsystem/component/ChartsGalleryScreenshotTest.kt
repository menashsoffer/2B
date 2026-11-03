package com.menash.myself.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.menash.myself.core.designsystem.component.charts.ChartLegend
import com.menash.myself.core.designsystem.component.charts.ChartSeries
import com.menash.myself.core.designsystem.component.charts.ChartTone
import com.menash.myself.core.designsystem.component.charts.ColumnChart
import com.menash.myself.core.designsystem.component.charts.ColumnGroup
import com.menash.myself.core.designsystem.component.charts.DataState
import com.menash.myself.core.designsystem.component.charts.LineChart
import com.menash.myself.core.designsystem.component.charts.LinePoint
import com.menash.myself.core.designsystem.component.charts.PlanActualBars
import com.menash.myself.core.designsystem.component.charts.PlanActualItem
import com.menash.myself.core.designsystem.component.charts.ShareBar
import com.menash.myself.core.designsystem.component.charts.ShareSegment
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
 * Every shared chart on one sheet: under and over plan, no plan, zero plan with spending, a complete,
 * an open ("so far") and a missing column, a line with an unknown point, and a share bar.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class ChartsGalleryScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shekels: (Long) -> String = { "₪${"%,d".format(it / 100)}" }

    @Test fun chartsGallery_light_font10() = shoot(dark = false, font = 1.0f)

    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun chartsGallery_dark_font10() = shoot(dark = true, font = 1.0f)

    @Test fun chartsGallery_light_font13() = shoot(dark = false, font = 1.3f)

    private fun shoot(dark: Boolean, font: Float) {
        val series = listOf(ChartSeries("הכנסות", ChartTone.Accent), ChartSeries("הוצאות", ChartTone.Soft))
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PlanActualBars(
                        items = listOf(
                            PlanActualItem("קבועות", actual = 820_000, plan = 900_000),
                            PlanActualItem("שוטפות", actual = 640_000, plan = 500_000),
                            PlanActualItem("תקופתיות", actual = 120_000, plan = 0),
                            PlanActualItem("בלי קטגוריה", actual = 90_000, tone = ChartTone.Neutral),
                        ),
                        format = shekels,
                    )
                    ColumnChart(
                        groups = listOf(
                            ColumnGroup("אוג׳", listOf(2_100_000, 1_800_000)),
                            ColumnGroup("ספט׳", listOf(0, 0), DataState.MISSING),
                            ColumnGroup("אוק׳", listOf(2_050_000, 2_300_000)),
                            ColumnGroup("נוב׳", listOf(900_000, 700_000), DataState.PARTIAL),
                        ),
                        series = series,
                        format = shekels,
                    )
                    ChartLegend(series.map { it.label to it.tone })
                    LineChart(
                        points = listOf(
                            LinePoint("יולי", 15_000_000), LinePoint("אוג׳", 15_400_000), LinePoint("ספט׳", null),
                            LinePoint("אוק׳", 15_900_000), LinePoint("היום", 16_200_000),
                        ),
                        format = shekels,
                    )
                    ShareBar(
                        segments = listOf(
                            ShareSegment("עו״ש", 2_000_000, ChartTone.Accent),
                            ShareSegment("חיסכון", 6_000_000, ChartTone.Strong),
                            ShareSegment("השקעות", 8_000_000, ChartTone.Soft),
                        ),
                        format = shekels,
                    )
                }
            }
        }
    }
}
