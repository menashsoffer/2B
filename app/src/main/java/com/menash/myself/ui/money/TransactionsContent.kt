package com.menash.myself.ui.money

import com.menash.myself.core.designsystem.component.SearchField
import com.menash.myself.feature.money.search.LineSearchScope
import com.menash.myself.feature.money.search.SearchPeriod

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.key
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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.EmptyState
import com.menash.myself.core.designsystem.component.LoadingState
import com.menash.myself.core.designsystem.component.ErrorState
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.PillTabs
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Motion
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.TransactionsUi
import com.menash.myself.feature.money.amountText
import com.menash.myself.feature.money.importer.LineFilter
import com.menash.myself.feature.money.importer.MonthLine
import com.menash.myself.feature.money.importer.RowStatus
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class TransactionsActions(
    val onMonth: (YearMonth) -> Unit = {},
    val onFilter: (LineFilter) -> Unit = {},
    val onLine: (MonthLine) -> Unit = {},
    val onClose: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onText: (String) -> Unit = {},
    val onPeriod: (SearchPeriod) -> Unit = {},
    val onScope: (LineSearchScope) -> Unit = {},
    val onOpenMonth: (YearMonth) -> Unit = {},
)

private val filters = listOf(LineFilter.ALL, LineFilter.UNASSIGNED, LineFilter.PENDING)

/** One month's imported lines, full screen inside the money tab. Every number comes from [TransactionsUi]. */
@Composable
fun TransactionsContent(ui: TransactionsUi, actions: TransactionsActions, modifier: Modifier = Modifier) {
    if (ui.searching) {
        LineSearchContent(ui, actions, modifier)
        return
    }
    val month = ui.query.month ?: return
    val lines = ui.lines
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("tx_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        MoneySubScreen(title = stringResource(R.string.tx_title), subtitle = hebrewMonthYear(month), onBack = actions.onClose)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { actions.onMonth(month.minusMonths(1)) }, modifier = Modifier.testTag("tx_previous")) {
                Text(stringResource(R.string.tx_previous_month), style = AppType.body)
            }
            TextButton(onClick = { actions.onMonth(month.plusMonths(1)) }, modifier = Modifier.testTag("tx_next")) {
                Text(stringResource(R.string.tx_next_month), style = AppType.body)
            }
        }
        key(month) {
            Crossfade(targetState = when { ui.loadError -> 0; lines == null -> 1; lines.lines.isEmpty() -> 2; else -> 3 },
                animationSpec = tween(Motion.duration(Motion.medium), easing = Motion.easing), label = "transactions_load") { state ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s16)) {
                    if (state == 0) { ErrorState(message = stringResource(R.string.money_load_error), onRetry = actions.onRetry, retryLabel = stringResource(R.string.money_load_retry)) }
                    else if (state == 1 || lines == null) { LoadingState(testTag = "tx_loading") }
                    else if (state == 2) {
                        EmptyState(
                            title = stringResource(R.string.tx_empty_title),
                            description = stringResource(R.string.tx_empty_detail),
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            testTag = "tx_empty",
                        )
                    } else {
                        MoneySurface(testTag = "tx_summary") {
                            AmountRow(title = stringResource(R.string.tx_spent), amount = formatMoney(lines.spent))
                            if (!lines.income.isZero()) AmountRow(title = stringResource(R.string.tx_income), amount = formatMoney(lines.income))
                            if (lines.unassignedCount > 0) {
                                AmountRow(
                                    title = stringResource(R.string.tx_unassigned_count),
                                    amount = lines.unassignedCount.toString(),
                                    amountColor = MaterialTheme.appColors.warning,
                                    onClick = { actions.onFilter(LineFilter.UNASSIGNED) },
                                    testTag = "tx_unassigned_count",
                                )
                            }
                            if (lines.pendingCount > 0) {
                                AmountRow(
                                    title = stringResource(R.string.tx_pending_count),
                                    amount = lines.pendingCount.toString(),
                                    onClick = { actions.onFilter(LineFilter.PENDING) },
                                    testTag = "tx_pending_count",
                                )
                            }
                        }
                        PillTabs(
                            options = listOf(
                                stringResource(R.string.tx_filter_all),
                                stringResource(R.string.tx_filter_unassigned),
                                stringResource(R.string.tx_filter_pending),
                            ),
                            selectedIndex = filters.indexOf(ui.query.filter),
                            onSelect = { actions.onFilter(filters[it]) },
                            modifier = Modifier.testTag("tx_filters"),
                        )
                        SearchField(
                            value = ui.query.text,
                            onValueChange = actions.onText,
                            placeholder = stringResource(R.string.search_in_month_placeholder),
                            testTag = "tx_search",
                        )
                        val shown = ui.shown.orEmpty()
                        if (shown.isEmpty()) {
                            Text(
                                text = stringResource(R.string.tx_empty_filter),
                                style = AppType.body,
                                color = MaterialTheme.appColors.textSecondary,
                                modifier = Modifier.padding(horizontal = Spacing.s8).testTag("tx_empty_filter"),
                            )
                        } else {
                            MoneySurface(testTag = "tx_lines") {
                                shown.forEach { line -> LineRow(line, ui, actions) }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M")

@Composable
internal fun LineRow(line: MonthLine, ui: TransactionsUi, actions: TransactionsActions) {
    val colors = MaterialTheme.appColors
    val row = line.row
    val state = when {
        line.pending -> stringResource(R.string.tx_pending)
        row.status == RowStatus.WAITING_FX -> stringResource(R.string.tx_waiting_fx)
        line.income -> ui.incomes.firstOrNull { it.id == line.incomeId }?.name ?: stringResource(R.string.tx_income_line)
        line.unassigned -> stringResource(R.string.tx_unassigned)
        line.counted -> ui.categories.firstOrNull { it.id == line.categoryId }?.name ?: stringResource(R.string.tx_unassigned)
        else -> stringResource(R.string.tx_not_counted)
    }
    val charge = row.charge
    val original = row.originalAmount
    val amount = when {
        charge != null && line.income -> formatMoney(Money(-charge.agorot))
        charge != null -> formatMoney(charge)
        original != null -> "${amountText(original)} ${row.originalCurrency}"
        else -> null
    }
    AmountRow(
        title = ui.memory.displayName(row.description),
        amount = amount,
        detail = stringResource(R.string.tx_line_detail, row.date.format(dayFormat), sourceLabel(row.source), state),
        amountColor = when {
            line.unassigned -> colors.warning
            line.counted -> null
            else -> colors.textSecondary
        },
        onClick = when {
            line.canCategorise || line.removable || line.income -> ({ actions.onLine(line) })
            else -> null
        },
        testTag = "tx_line_${line.id}",
    )
}
