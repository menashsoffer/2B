package com.menash.myself.feature.money.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.BigPicture
import com.menash.myself.feature.money.CategoryTop
import com.menash.myself.feature.money.FlowState
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.MonthFlowPoint
import com.menash.myself.feature.money.MonthRowKind
import com.menash.myself.feature.money.MonthView
import com.menash.myself.feature.money.TopCategory
import com.menash.myself.feature.money.WorthPoint
import com.menash.myself.feature.money.buildMoneyMonth
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.LineFilter
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.captureR2Screen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.YearMonth

/**
 * The money charts with full data: classes and top categories in an imported month, and in the big
 * picture half a year (closed, open "so far" and missing months) and net worth with an unknown month.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class MoneyChartsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val month = MoneySamples.month
    private val lines = ImportFixtures.parse("hapoalim_oct.xlsx").rows.map { StoredTransaction(it) }
    private val money = buildMoneyMonth(MoneySamples.filled, month, MoneySamples.today, MoneySamples.rate, lines)

    private val planned: MonthView.Planned = (money.view as MonthView.Planned).copy(
        top = CategoryTop(
            shown = listOf(
                TopCategory("food", "מזון", Money(250_000), Money(310_000)),
                TopCategory("fuel", "דלק", Money(80_000), Money(62_000)),
                TopCategory("gifts", "מתנות", null, Money(40_000)),
            ),
            more = 4,
        ),
    )

    private fun shekels(n: Long) = Money(n * 100)

    private val picture: BigPicture = money.bigPicture.copy(
        flows = listOf(
            MonthFlowPoint(month.minusMonths(5), shekels(15_000), shekels(12_800), FlowState.CLOSED),
            MonthFlowPoint(month.minusMonths(4), shekels(15_200), shekels(16_100), FlowState.CLOSED),
            MonthFlowPoint(month.minusMonths(3), Money.ZERO, Money.ZERO, FlowState.MISSING),
            MonthFlowPoint(month.minusMonths(2), shekels(15_100), shekels(13_900), FlowState.CLOSED),
            MonthFlowPoint(month.minusMonths(1), shekels(15_300), shekels(12_500), FlowState.CLOSED),
            MonthFlowPoint(month, shekels(9_000), shekels(7_400), FlowState.SO_FAR),
        ),
        worth = listOf(
            WorthPoint(month.minusMonths(4), shekels(98_000)),
            WorthPoint(month.minusMonths(3), null),
            WorthPoint(month.minusMonths(2), shekels(104_000)),
            WorthPoint(month.minusMonths(1), shekels(109_500)),
            WorthPoint(month, shekels(113_000)),
        ),
        monthlySaving = shekels(1_850),
    )

    @Test fun monthCharts_light_font10() = shoot(dark = false, font = 1.0f) { MonthCharts(planned, MoneyActions()) }

    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun monthCharts_dark_font13() = shoot(dark = true, font = 1.3f) { MonthCharts(planned, MoneyActions()) }

    @Test fun bigPictureCharts_light_font10() = shoot(dark = false, font = 1.0f) { BigPictureCharts(picture) }

    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun bigPictureCharts_dark_font13() = shoot(dark = true, font = 1.3f) { BigPictureCharts(picture) }

    @Test
    fun tappingBars_opensTheRightPlaces() {
        val toggled = mutableListOf<MonthRowKind>()
        val filters = mutableListOf<LineFilter>()
        val categories = mutableListOf<String>()
        val actions = MoneyActions(
            onToggleRow = { toggled += it },
            onTransactions = { filters += it },
            onCategory = { categories += it },
        )
        composeRule.setContent { Rtl { Column(Modifier.verticalScroll(rememberScrollState())) { MonthCharts(planned, actions) } } }
        val first = planned.rows.first { it.kind == MonthRowKind.CURRENT }.kind
        composeRule.onNodeWithTag("chart_classes_${first.name}").performClick()
        composeRule.onNodeWithTag("chart_top_food").performClick()
        composeRule.onNodeWithTag("chart_top_more").performClick()
        assertEquals(listOf(MonthRowKind.CURRENT), toggled)
        assertEquals(listOf("food"), categories)
        assertEquals(listOf(LineFilter.ALL), filters)
    }

    private fun shoot(dark: Boolean, font: Float, content: @Composable ColumnScope.() -> Unit) {
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl {
                Column(
                    Modifier.background(MaterialTheme.appColors.background).verticalScroll(rememberScrollState()).padding(16.dp),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
