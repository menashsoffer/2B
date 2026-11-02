package com.menash.myself.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.MonthSummaryUi
import com.menash.myself.feature.money.MonthSummaryView
import com.menash.myself.feature.money.domain.ExpenseClass
import java.time.YearMonth

private fun monthLabel(month: YearMonth): String = "${hebrewMonthName(month.month)} ${month.year}"

/** "סיכום החודש" after a close: five short cards, read from the frozen month. */
@Composable
fun MonthSummaryContent(ui: MonthSummaryUi, onBack: () -> Unit, modifier: Modifier = Modifier, onCategory: (String) -> Unit = {}) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("summary_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        val month = ui.month ?: return@Column
        MoneySubScreen(
            title = stringResource(R.string.summary_title, monthLabel(month)),
            subtitle = stringResource(R.string.summary_subtitle),
            onBack = onBack,
        )
        val view = ui.view ?: return@Column
        Headline(view)
        if (view.classes.isNotEmpty()) {
            SectionTitle(stringResource(R.string.summary_by_class))
            MoneySurface(testTag = "summary_classes") {
                view.classes.forEach { row ->
                    AmountRow(
                        title = classTitle(row.klass),
                        amount = formatMoney(row.actual),
                        detail = row.planned?.let { stringResource(R.string.summary_planned, formatMoney(it)) }
                            ?: stringResource(R.string.period_no_plan),
                        amountColor = if (row.planned != null && row.actual > row.planned) MaterialTheme.appColors.danger else null,
                    )
                }
            }
        }
        SectionTitle(stringResource(R.string.summary_overruns))
        MoneySurface(testTag = "summary_overruns") {
            if (view.overruns.isEmpty()) AmountRow(title = stringResource(R.string.summary_no_overruns), amount = null)
            view.overruns.forEach { row ->
                AmountRow(
                    title = row.name ?: stringResource(R.string.period_class_other),
                    amount = formatMoney(row.actual),
                    detail = stringResource(R.string.summary_overrun_detail, formatMoney(row.planned), formatMoney(row.over)),
                    amountColor = MaterialTheme.appColors.danger,
                    onClick = row.categoryId?.let { id -> { onCategory(id) } },
                )
            }
        }
        if (view.periodic.isNotEmpty()) {
            SectionTitle(stringResource(R.string.summary_periodic))
            MoneySurface(testTag = "summary_periodic") {
                view.periodic.forEach { fund ->
                    val ahead = fund.status.advance
                    AmountRow(
                        title = fund.name,
                        amount = formatMoney(fund.status.remainingThisYear),
                        detail = if (ahead.isPositive()) stringResource(R.string.summary_periodic_ahead, formatMoney(ahead))
                            else stringResource(R.string.summary_periodic_detail, formatMoney(fund.status.accruedToDate), formatMoney(fund.status.spentToDate)),
                        amountColor = if (ahead.isPositive()) MaterialTheme.appColors.warning else null,
                    )
                }
            }
        }
        NetWorth(view)
        Suggestions(view, onCategory)
    }
}

@Composable
private fun Headline(view: MonthSummaryView) {
    val colors = MaterialTheme.appColors
    MoneySurface(testTag = "summary_headline") {
        Column(Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Caption(stringResource(R.string.summary_left))
            Text(
                formatMoney(view.left),
                style = AppType.number,
                color = if (view.left.isNegative()) colors.danger else colors.textPrimary,
                modifier = Modifier.testTag("summary_left"),
            )
            view.leftPercent?.let { Caption(stringResource(R.string.summary_left_percent, it.toPlainString())) }
            Caption(stringResource(R.string.summary_income_expenses, formatMoney(view.income), formatMoney(view.expenses)))
            val diff = view.vsAverage
            Text(
                text = when {
                    diff == null -> stringResource(R.string.summary_first)
                    diff.isPositive() -> stringResource(R.string.summary_vs_average_more, formatMoney(diff), view.previousCount)
                    diff.isNegative() -> stringResource(R.string.summary_vs_average_less, formatMoney(-diff), view.previousCount)
                    else -> stringResource(R.string.summary_vs_average_same, view.previousCount)
                },
                style = AppType.body,
                color = when {
                    diff == null -> colors.textSecondary
                    diff.isNegative() -> colors.warning
                    else -> colors.success
                },
                modifier = Modifier.testTag("summary_vs_average"),
            )
        }
    }
}

@Composable
private fun NetWorth(view: MonthSummaryView) {
    val change = view.netWorth
    val unknown = view.closed.balances.isEmpty() && view.closed.liabilities.isEmpty()
    if (change == null && !unknown) return
    MoneySurface(testTag = "summary_net_worth") {
        if (change == null) {
            AmountRow(title = stringResource(R.string.summary_net_worth), amount = null, detail = stringResource(R.string.summary_net_worth_unknown))
        } else {
            val signed = if (change.change.isPositive()) "+${formatMoney(change.change)}" else formatMoney(change.change)
            AmountRow(
                title = stringResource(R.string.summary_net_worth),
                amount = formatMoney(change.end),
                detail = stringResource(R.string.period_net_worth_detail, monthLabel(change.startFrom), signed),
            )
        }
    }
}

@Composable
private fun Suggestions(view: MonthSummaryView, onCategory: (String) -> Unit) {
    SectionTitle(stringResource(R.string.summary_plan))
    MoneySurface(testTag = "summary_plan") {
        val suggestions = view.suggestions
        when {
            suggestions == null -> AmountRow(title = stringResource(R.string.summary_plan_wait), amount = null)
            suggestions.isEmpty() -> AmountRow(title = stringResource(R.string.summary_plan_ok), amount = null)
            else -> {
                Caption(stringResource(R.string.summary_plan_detail, view.suggestionMonths), Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8))
                suggestions.forEach { row ->
                    AmountRow(
                        title = row.name,
                        amount = formatMoney(row.suggested),
                        detail = row.current?.let { stringResource(R.string.summary_plan_now, formatMoney(it)) }
                            ?: stringResource(R.string.summary_plan_none),
                        onClick = { onCategory(row.categoryId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = AppType.caption, color = MaterialTheme.appColors.textSecondary, modifier = modifier)
}

@Composable
private fun classTitle(klass: ExpenseClass?): String = stringResource(
    when (klass) {
        ExpenseClass.FIXED -> R.string.money_row_fixed
        ExpenseClass.CURRENT -> R.string.money_row_current
        ExpenseClass.PERIODIC -> R.string.money_row_periodic
        null -> R.string.period_class_other
    },
)
