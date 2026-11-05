package com.menash.myself.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
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
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.BigPicture
import com.menash.myself.feature.money.FlowState
import com.menash.myself.feature.money.MonthRowKind
import com.menash.myself.feature.money.MonthView
import com.menash.myself.feature.money.classBars
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.importer.LineFilter
import java.time.format.TextStyle
import java.util.Locale

private const val MaxAssetSegments = 5

private val formatAgorot: (Long) -> String = { formatMoney(Money(it)) }

private fun classTone(kind: MonthRowKind?): ChartTone = when (kind) {
    MonthRowKind.FIXED -> ChartTone.Strong
    MonthRowKind.CURRENT -> ChartTone.Accent
    MonthRowKind.PERIODIC -> ChartTone.Warning
    MonthRowKind.INSTALLMENTS -> ChartTone.Neutral
    else -> ChartTone.Neutral
}

/** The month's charts: plan vs actual per class, then the largest categories. Nothing before an import. */
@Composable
internal fun MonthCharts(view: MonthView.Planned, actions: MoneyActions) {
    val bars = classBars(view)
    if (bars.isEmpty()) return
    val unassignedLabel = stringResource(R.string.chart_unassigned)
    val items = bars.map { bar ->
        PlanActualItem(
            label = bar.kind?.let { rowLabel(it) } ?: unassignedLabel,
            actual = bar.actual.agorot,
            plan = bar.plan?.agorot,
            tone = classTone(bar.kind),
            key = bar.kind?.name ?: "UNASSIGNED",
        )
    }
    SectionTitle(stringResource(R.string.chart_month_classes))
    MoneySurface(testTag = "money_chart_classes") {
        PlanActualBars(
            items = items,
            format = formatAgorot,
            modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8),
            onClick = { item ->
                val kind = MonthRowKind.entries.firstOrNull { it.name == item.key }
                if (kind != null) actions.onToggleRow(kind) else actions.onTransactions(LineFilter.UNASSIGNED)
            },
            testTag = "chart_classes",
        )
    }
    TopCategoriesChart(view.top, actions)
}

@Composable
internal fun TopCategoriesChart(top: com.menash.myself.feature.money.CategoryTop, actions: MoneyActions) {
    if (top.shown.isEmpty()) return
    SectionTitle(stringResource(R.string.chart_top_categories))
    MoneySurface(testTag = "money_chart_top") {
        PlanActualBars(
            items = top.shown.map { PlanActualItem(it.name, it.actual.agorot, it.plan?.agorot, ChartTone.Accent, it.categoryId) },
            format = formatAgorot,
            modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8),
            onClick = { actions.onCategory(it.key) },
            testTag = "chart_top",
        )
        if (top.more > 0) {
            AmountRow(
                title = stringResource(R.string.chart_more_categories, top.more),
                amount = null,
                onClick = { actions.onTransactions(LineFilter.ALL) },
                testTag = "chart_top_more",
            )
        }
    }
}

/** The big picture's charts: half a year of income vs spending, net worth over time, the assets mix. */
@Composable
internal fun BigPictureCharts(picture: BigPicture) {
    val colors = MaterialTheme.appColors
    if (picture.flows.any { it.state != FlowState.MISSING }) {
        SectionTitle(stringResource(R.string.chart_flows))
        MoneySurface(testTag = "money_chart_flows") {
            Column(modifier = Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                ColumnChart(
                    groups = picture.flows.map { flow ->
                        ColumnGroup(
                            label = flow.month.month.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.forLanguageTag("he")),
                            values = listOf(flow.income.agorot, flow.expenses.agorot),
                            state = when (flow.state) {
                                FlowState.CLOSED -> DataState.COMPLETE
                                FlowState.SO_FAR -> DataState.PARTIAL
                                FlowState.MISSING -> DataState.MISSING
                            },
                            key = flow.month.toString(),
                        )
                    },
                    series = listOf(
                        ChartSeries(stringResource(R.string.chart_income), ChartTone.Accent),
                        ChartSeries(stringResource(R.string.chart_expenses), ChartTone.Warning),
                    ),
                    format = formatAgorot,
                    testTag = "chart_flows",
                )
                if (picture.flows.any { it.state == FlowState.SO_FAR }) {
                    Text(stringResource(R.string.chart_flows_note), style = AppType.caption, color = colors.textSecondary)
                }
                picture.monthlySaving?.let {
                    Text(stringResource(R.string.chart_average_saving, formatMoney(it)), style = AppType.caption, color = colors.textPrimary)
                }
            }
        }
    }
    if (picture.worth.isNotEmpty() && picture.netWorth != null) {
        SectionTitle(stringResource(R.string.chart_worth))
        MoneySurface(testTag = "money_chart_worth") {
            val points = picture.worth.map { LinePoint(hebrewMonthYear(it.month), it.worth?.agorot) }
            Column(modifier = Modifier.padding(Spacing.s16)) {
                if (points.count { it.value != null } >= 2) {
                    LineChart(points, formatAgorot, testTag = "chart_worth")
                } else {
                    Text(
                        stringResource(R.string.chart_worth_empty),
                        style = AppType.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.testTag("chart_worth_empty"),
                    )
                }
            }
        }
    }
    val assets = picture.assets.map { it.asset }.filter { it.kind == AssetKind.ASSET && it.balance.isPositive() }
    if (assets.size >= 2) {
        val tones = listOf(ChartTone.Strong, ChartTone.Accent, ChartTone.Success, ChartTone.Warning, ChartTone.Neutral, ChartTone.Soft)
        // Names, not the internal type key: what the household typed is what it recognises.
        val sorted = assets.sortedByDescending { it.balance.agorot }
        val shown = sorted.take(MaxAssetSegments).map { it.name to it.balance.agorot }
        val rest = sorted.drop(MaxAssetSegments).sumOf { it.balance.agorot }
        val otherLabel = stringResource(R.string.chart_assets_other)
        val byType = if (rest > 0) shown + (otherLabel to rest) else shown
        SectionTitle(stringResource(R.string.chart_assets))
        MoneySurface(testTag = "money_chart_assets") {
            ShareBar(
                segments = byType.mapIndexed { i, (type, sum) -> ShareSegment(type, sum, tones[i % tones.size]) },
                format = formatAgorot,
                modifier = Modifier.padding(Spacing.s16),
                testTag = "chart_assets",
            )
        }
    }
}
