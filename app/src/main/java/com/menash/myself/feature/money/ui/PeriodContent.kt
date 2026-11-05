package com.menash.myself.feature.money.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.EmptyState
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.PillTabs
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.PeriodCategoryRow
import com.menash.myself.feature.money.PeriodPreset
import com.menash.myself.feature.money.PeriodUi
import com.menash.myself.feature.money.PeriodView
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.MonthFlow
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class PeriodActions(
    val onSelect: (PeriodPreset) -> Unit = {},
    val onBack: () -> Unit = {},
    /** Opens the category's trend. */
    val onCategory: (String) -> Unit = {},
    /** Opens search on the range's lines. */
    val onLines: (YearMonth, YearMonth) -> Unit = { _, _ -> },
)

private val presets = PeriodPreset.entries
private val shortMonth = Locale.forLanguageTag("he")

private fun monthLabel(month: YearMonth): String = "${hebrewMonthName(month.month)} ${month.year}"

/** "סיכום תקופה": totals, a bar per month, classes and the largest categories, over closed months only. */
@Composable
fun PeriodContent(ui: PeriodUi, actions: PeriodActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("period_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        MoneySubScreen(title = stringResource(R.string.period_title), subtitle = stringResource(R.string.period_subtitle), onBack = actions.onBack)
        val view = ui.view ?: return@Column
        PillTabs(
            options = listOf(
                stringResource(R.string.period_last_3), stringResource(R.string.period_last_6),
                stringResource(R.string.period_this_year), view.lastYear.toString(),
            ),
            selectedIndex = presets.indexOf(view.preset),
            onSelect = { actions.onSelect(presets[it]) },
        )
        Text(
            text = stringResource(R.string.period_range, monthLabel(view.range.from), monthLabel(view.range.to)),
            style = AppType.caption,
            color = MaterialTheme.appColors.textSecondary,
            modifier = Modifier.testTag("period_range"),
        )
        TextButton(onClick = { actions.onLines(view.range.from, view.range.to) }, modifier = Modifier.testTag("period_lines")) {
            Text(stringResource(R.string.period_lines), style = AppType.body, color = MaterialTheme.appColors.accent)
        }
        val summary = view.summary
        if (summary == null) {
            EmptyState(
                title = stringResource(R.string.period_empty),
                description = stringResource(R.string.period_empty_detail),
                testTag = "period_empty",
            )
            return@Column
        }
        PeriodTotals(view)
        SectionTitle(stringResource(R.string.period_chart))
        MoneySurface(testTag = "period_chart") {
            MonthBars(view.range.months, summary.months, Modifier.padding(Spacing.s16))
        }
        if (view.classes.isNotEmpty()) {
            SectionTitle(stringResource(R.string.period_by_class))
            MoneySurface(testTag = "period_classes") {
                view.classes.forEach { (klass, amount) -> AmountRow(title = classLabel(klass), amount = formatMoney(amount)) }
            }
        }
        if (view.categories.isNotEmpty()) {
            SectionTitle(stringResource(R.string.period_top_categories))
            MoneySurface(testTag = "period_categories") { view.categories.forEach { CategoryLine(it, actions.onCategory) } }
        }
        PeriodYearCompare(view, actions.onCategory)
        view.netWorth?.let { change ->
            MoneySurface(testTag = "period_net_worth") {
                AmountRow(
                    title = stringResource(R.string.period_net_worth),
                    amount = formatMoney(change.end),
                    detail = stringResource(R.string.period_net_worth_detail, monthLabel(change.startFrom), signed(change.change)),
                )
            }
        }
    }
}

@Composable
private fun PeriodTotals(view: PeriodView) {
    val summary = view.summary ?: return
    val colors = MaterialTheme.appColors
    MoneySurface(testTag = "period_totals") {
        Column(Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Figure(stringResource(R.string.period_income), summary.income)
            Figure(stringResource(R.string.period_expenses), summary.expenses)
            Figure(stringResource(R.string.period_left), summary.saving, highlight = true)
            summary.savingPercent?.let {
                Text(
                    stringResource(R.string.period_left_percent, formatMoney(summary.saving), it.toPlainString()),
                    style = AppType.caption, color = colors.textSecondary,
                )
            }
            Text(
                stringResource(R.string.period_closed_count, summary.closedMonths.size, formatMoney(summary.averageMonthlyExpenses)),
                style = AppType.caption, color = colors.textSecondary,
            )
            if (summary.missingMonths.isNotEmpty()) {
                Text(
                    stringResource(R.string.period_missing, summary.missingMonths.joinToString(", ") { hebrewMonthName(it.month) }),
                    style = AppType.caption, color = colors.warning, modifier = Modifier.testTag("period_missing"),
                )
            }
        }
    }
}

@Composable
private fun Figure(label: String, amount: Money, highlight: Boolean = false) {
    val colors = MaterialTheme.appColors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = AppType.body, color = colors.textSecondary)
        Text(
            formatMoney(amount),
            style = AppType.numberCompact,
            color = when {
                amount.isNegative() -> colors.danger
                highlight -> colors.success
                else -> colors.textPrimary
            },
        )
    }
}

/** Two bars per month (income, expenses) on one scale; a month without a close shows "חסר", never a zero bar. */
@Composable
private fun MonthBars(months: List<YearMonth>, flows: List<MonthFlow>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val byMonth = flows.associateBy { it.month }
    val top = flows.maxOfOrNull { maxOf(it.income.agorot, it.expenses.agorot) }?.takeIf { it > 0 } ?: 1L
    val description = flows.joinToString("; ") {
        "${hebrewMonthName(it.month.month)}: ${formatMoney(it.income)} / ${formatMoney(it.expenses)}"
    }
    Row(
        modifier = modifier.fillMaxWidth().height(140.dp).semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
        verticalAlignment = Alignment.Bottom,
    ) {
        months.forEach { month ->
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                val flow = byMonth[month]
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    if (flow == null) {
                        Box(
                            Modifier.fillMaxSize().border(1.dp, colors.cardBorder, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text(stringResource(R.string.period_chart_missing), style = AppType.caption, color = colors.textSecondary) }
                    } else {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.Bottom) {
                            Bar(flow.income.agorot.toFloat() / top, colors.accent)
                            Bar(flow.expenses.agorot.toFloat() / top, if (flow.expenses > flow.income) colors.danger else colors.accentSoft)
                        }
                    }
                }
                Text(
                    month.month.getDisplayName(TextStyle.SHORT_STANDALONE, shortMonth),
                    style = AppType.caption, color = colors.textSecondary, textAlign = TextAlign.Center, maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(
        Modifier.width(8.dp).fillMaxHeight(fraction.coerceIn(0.02f, 1f))
            .background(color, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
    )
}

@Composable
private fun CategoryLine(row: PeriodCategoryRow, onCategory: (String) -> Unit) {
    val colors = MaterialTheme.appColors
    val diff = row.diff
    AmountRow(
        title = row.name ?: stringResource(R.string.period_class_other),
        amount = formatMoney(row.actual),
        detail = when {
            diff == null -> stringResource(R.string.period_no_plan)
            diff.isPositive() -> stringResource(R.string.period_plan_diff_over, formatMoney(diff))
            else -> stringResource(R.string.period_plan_diff_under, formatMoney(-diff))
        },
        amountColor = if (diff != null && diff.isPositive()) colors.danger else null,
        onClick = row.categoryId?.let { id -> { onCategory(id) } },
    )
}

@Composable
private fun classLabel(klass: ExpenseClass?): String = stringResource(
    when (klass) {
        ExpenseClass.FIXED -> R.string.money_row_fixed
        ExpenseClass.CURRENT -> R.string.money_row_current
        ExpenseClass.PERIODIC -> R.string.money_row_periodic
        null -> R.string.period_class_other
    },
)

private fun signed(amount: Money): String = if (amount.isPositive()) "+${formatMoney(amount)}" else formatMoney(amount)
