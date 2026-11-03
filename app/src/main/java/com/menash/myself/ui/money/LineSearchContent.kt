package com.menash.myself.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.Chip
import com.menash.myself.core.designsystem.component.ChipVariant
import com.menash.myself.core.designsystem.component.EmptyState
import com.menash.myself.core.designsystem.component.ErrorState
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.LoadingState
import com.menash.myself.core.designsystem.component.PillTabs
import com.menash.myself.core.designsystem.component.SearchField
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.TransactionsUi
import com.menash.myself.feature.money.search.LineSearchScope
import com.menash.myself.feature.money.search.SearchPeriod

/** At most this many lines are drawn; the summary still counts and sums every match. */
internal const val SearchShownLines = 200

private val quickPeriods = listOf(SearchPeriod.ALL, SearchPeriod.THIS_MONTH, SearchPeriod.LAST_3, SearchPeriod.THIS_YEAR)

/**
 * Search across every month's lines: a box, a period, "only uncategorised", then matches grouped by month.
 * A line opens the same category sheet as the month's transactions; a month header opens that month.
 */
@Composable
fun LineSearchContent(ui: TransactionsUi, actions: TransactionsActions, modifier: Modifier = Modifier) {
    val scope = ui.query.search ?: return
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("search_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        MoneySubScreen(title = stringResource(R.string.search_title), onBack = actions.onClose)
        SearchField(
            value = ui.query.text,
            onValueChange = actions.onText,
            placeholder = stringResource(R.string.search_placeholder),
            autoFocus = ui.query.text.isEmpty() && scope.categoryId == null,
        )
        SearchFilters(scope, ui, actions)
        val results = ui.results
        when {
            ui.loadError -> ErrorState(
                message = stringResource(R.string.money_load_error),
                onRetry = actions.onRetry,
                retryLabel = stringResource(R.string.money_load_retry),
            )
            results == null -> LoadingState(testTag = "search_loading")
            ui.query.text.isBlank() && scope.categoryId == null && !scope.onlyUnassigned && scope.period != SearchPeriod.CUSTOM -> EmptyState(
                title = stringResource(R.string.search_hint_title),
                description = stringResource(R.string.search_hint_detail),
                icon = Icons.Outlined.Search,
                testTag = "search_hint",
            )
            results.count == 0 -> EmptyState(
                title = stringResource(R.string.search_no_results_title),
                description = stringResource(R.string.search_no_results_detail),
                icon = Icons.Outlined.SearchOff,
                actionLabel = stringResource(R.string.search_clear_filters),
                onActionClick = {
                    actions.onText("")
                    actions.onScope(LineSearchScope())
                },
                testTag = "search_no_results",
            )
            else -> {
                Text(
                    text = stringResource(R.string.search_summary, results.count, formatMoney(results.spent)),
                    style = AppType.body,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = Spacing.s8).testTag("search_summary"),
                )
                var left = SearchShownLines
                results.months.forEach { month ->
                    if (left <= 0) return@forEach
                    val shown = month.lines.take(left)
                    left -= shown.size
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(hebrewMonthYear(month.month), modifier = Modifier.weight(1f))
                        TextButton(onClick = { actions.onOpenMonth(month.month) }, modifier = Modifier.testTag("search_month_${month.month}")) {
                            Text(stringResource(R.string.search_open_month), style = AppType.caption)
                        }
                    }
                    MoneySurface(testTag = "search_lines_${month.month}") {
                        shown.forEach { line -> LineRow(line, ui, actions) }
                    }
                }
                if (results.count > SearchShownLines) {
                    Text(
                        text = stringResource(R.string.search_more, SearchShownLines),
                        style = AppType.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = Spacing.s8).testTag("search_more"),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchFilters(scope: LineSearchScope, ui: TransactionsUi, actions: TransactionsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        if (scope.period == SearchPeriod.CUSTOM && scope.from != null && scope.to != null) {
            Chip(
                text = stringResource(R.string.search_period_custom, hebrewMonthYear(scope.from), hebrewMonthYear(scope.to)),
                onClick = { actions.onPeriod(SearchPeriod.ALL) },
                modifier = Modifier.testTag("search_custom_period"),
            )
        } else {
            PillTabs(
                options = listOf(
                    stringResource(R.string.search_period_all),
                    stringResource(R.string.search_period_this_month),
                    stringResource(R.string.search_period_last_3),
                    stringResource(R.string.search_period_this_year),
                ),
                selectedIndex = quickPeriods.indexOf(scope.period).coerceAtLeast(0),
                onSelect = { actions.onPeriod(quickPeriods[it]) },
                modifier = Modifier.testTag("search_periods"),
            )
        }
        PillTabs(
            options = listOf(stringResource(R.string.search_all_lines), stringResource(R.string.search_only_unassigned)),
            selectedIndex = if (scope.onlyUnassigned) 1 else 0,
            onSelect = { actions.onScope(scope.copy(onlyUnassigned = it == 1)) },
            modifier = Modifier.testTag("search_unassigned"),
        )
        scope.categoryId?.let { id ->
            val name = ui.categories.firstOrNull { it.id == id }?.name ?: return@let
            Chip(
                text = stringResource(R.string.search_category_chip, name),
                variant = ChipVariant.Default,
                onClick = { actions.onScope(scope.copy(categoryId = null)) },
                modifier = Modifier.testTag("search_category"),
            )
        }
    }
}
