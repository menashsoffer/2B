package com.menash.myself.ui.money

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.EmptyState
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.CategoryTrendUi
import com.menash.myself.feature.money.CategoryTrendView
import com.menash.myself.feature.money.PlanTarget
import com.menash.myself.feature.money.TrendMonth
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.SuggestionStatus

data class CategoryTrendActions(val onAccept: () -> Unit = {}, val onBack: () -> Unit = {})

/** "מגמה" of one category: twelve months against the plan, the figures, and the suggested plan in one tap. */
@Composable
fun CategoryTrendContent(ui: CategoryTrendUi, actions: CategoryTrendActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("trend_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        val view = ui.view
        MoneySubScreen(title = view?.name.orEmpty(), subtitle = stringResource(R.string.trend_subtitle), onBack = actions.onBack)
        if (view == null) return@Column
        if (view.closedCount == 0) {
            EmptyState(title = stringResource(R.string.trend_empty), description = stringResource(R.string.period_empty_detail), testTag = "trend_empty")
            return@Column
        }
        SectionTitle(stringResource(R.string.trend_chart))
        MoneySurface(testTag = "trend_chart") {
            Column(Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                TrendBars(view.months)
                Text(stringResource(R.string.trend_legend), style = AppType.caption, color = MaterialTheme.appColors.textSecondary)
            }
        }
        Figures(view)
        Suggestion(ui, view, actions)
    }
}

@Composable
private fun Figures(view: CategoryTrendView) {
    MoneySurface(testTag = "trend_figures") {
        view.baseline?.let { row ->
            AmountRow(
                title = stringResource(R.string.trend_average),
                amount = formatMoney(row.average),
                detail = stringResource(R.string.trend_average_detail, view.closedCount),
            )
            AmountRow(title = stringResource(R.string.trend_range), amount = stringResource(R.string.trend_range_value, formatMoney(row.min), formatMoney(row.max)))
            AmountRow(title = stringResource(R.string.trend_latest), amount = formatMoney(row.latest))
        }
        AmountRow(
            title = stringResource(R.string.trend_plan_now),
            amount = view.currentPlan?.let(::formatMoney) ?: stringResource(R.string.trend_plan_none),
        )
    }
}

@Composable
private fun Suggestion(ui: CategoryTrendUi, view: CategoryTrendView, actions: CategoryTrendActions) {
    SectionTitle(stringResource(R.string.trend_suggestion))
    MoneySurface(testTag = "trend_suggestion") {
        val suggested = view.suggested
        val target = view.target
        when {
            view.klass == ExpenseClass.PERIODIC -> AmountRow(title = stringResource(R.string.trend_periodic), amount = null)
            view.status == SuggestionStatus.NOT_ENOUGH_DATA -> AmountRow(title = stringResource(R.string.trend_wait), amount = null)
            suggested == null || target == null -> AmountRow(title = stringResource(R.string.trend_no_change), amount = null)
            else -> Column(Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                Text(
                    text = view.currentPlan?.let { stringResource(R.string.trend_suggestion_detail, formatMoney(suggested), formatMoney(it)) }
                        ?: stringResource(R.string.trend_suggestion_new, formatMoney(suggested)),
                    style = AppType.body,
                    color = MaterialTheme.appColors.textPrimary,
                )
                val note = when (target) {
                    is PlanTarget.Item -> stringResource(R.string.trend_accept_item, target.name)
                    PlanTarget.NewBudget -> stringResource(R.string.trend_accept_new, view.name)
                    is PlanTarget.Manual ->
                        if (target.count == 0) stringResource(R.string.trend_manual_none) else stringResource(R.string.trend_manual, target.count)
                }
                Text(note, style = AppType.caption, color = MaterialTheme.appColors.textSecondary)
                if (target !is PlanTarget.Manual) {
                    PrimaryButton(
                        text = stringResource(R.string.trend_accept, formatMoney(suggested), hebrewMonthName(view.from.month)),
                        onClick = actions.onAccept,
                        isLoading = ui.saving,
                        modifier = Modifier.fillMaxWidth().testTag("trend_accept"),
                    )
                }
            }
        }
    }
}

/** A bar per closed month on one scale, with the plan as a line; a month that is not closed shows "–". */
@Composable
private fun TrendBars(months: List<TrendMonth>) {
    val colors = MaterialTheme.appColors
    val top = months.maxOfOrNull { maxOf(it.actual?.agorot ?: 0L, it.planned?.agorot ?: 0L) }?.takeIf { it > 0 } ?: 1L
    val description = months.filter { it.actual != null }.joinToString("; ") { "${hebrewMonthName(it.month.month)}: ${formatMoney(it.actual!!)}" }
    Row(
        modifier = Modifier.fillMaxWidth().height(150.dp).semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        months.forEach { month ->
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    val actual = month.actual
                    if (actual == null) {
                        Box(Modifier.fillMaxSize().border(1.dp, colors.cardBorder, RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.period_chart_missing), style = AppType.caption, color = colors.textSecondary)
                        }
                    } else {
                        val over = month.planned != null && actual > month.planned
                        Box(
                            Modifier.width(10.dp).fillMaxHeight((actual.agorot.toFloat() / top).coerceIn(0.02f, 1f))
                                .background(if (over) colors.danger else colors.accent, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                        )
                    }
                    month.planned?.let { plan ->
                        Column(Modifier.fillMaxWidth().fillMaxHeight((plan.agorot.toFloat() / top).coerceIn(0.01f, 1f))) {
                            Box(Modifier.fillMaxWidth().height(2.dp).background(colors.textSecondary))
                        }
                    }
                }
                Text(
                    month.month.monthValue.toString(),
                    style = AppType.caption, color = colors.textSecondary, textAlign = TextAlign.Center, maxLines = 1,
                )
            }
        }
    }
}
