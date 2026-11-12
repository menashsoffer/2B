package com.menash.myself.feature.money.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import com.menash.myself.core.designsystem.component.LoadingState
import com.menash.myself.core.designsystem.component.ErrorState
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Motion
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.CloseView
import com.menash.myself.feature.money.MonthCloseUi
import com.menash.myself.feature.money.StatementSource
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.CloseCheckKind
import com.menash.myself.feature.money.importer.LineFilter
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class MonthCloseActions(
    val onPrevious: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onNextPending: () -> Unit = {},
    /** Opens the file picker to import the missing statement. */
    val onImport: () -> Unit = {},
    val onNoLines: (StatementSource) -> Unit = {},
    val onSourceClosed: (StatementSource, Boolean) -> Unit = { _, _ -> },
    val onLines: (YearMonth, LineFilter) -> Unit = { _, _ -> },
    val onAsset: (Asset) -> Unit = {},
    val onClose: () -> Unit = {},
    val onClassifyRest: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onEnterBalances: (Boolean) -> Unit = {},
    val onBalance: (String, String) -> Unit = { _, _ -> },
    val onMarkUnknown: () -> Unit = {},
    /** Opens "סיכום החודש" for a month that is closed. */
    val onSummary: (YearMonth) -> Unit = {},
    val onRetry: () -> Unit = {},
)

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy")

private fun monthTitle(month: YearMonth) = "${hebrewMonthName(month.month)} ${month.year}"

/** "סגירת החודש": what still blocks the close, what closing would store, and the button. */
@Composable
fun MonthCloseContent(ui: MonthCloseUi, actions: MonthCloseActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("close_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        val view = ui.view
        MoneySubScreen(
            title = view?.month?.let { stringResource(R.string.close_title, monthTitle(it)) } ?: stringResource(R.string.close_loading_title),
            subtitle = stringResource(R.string.close_subtitle),
            onBack = actions.onBack,
        )
        Crossfade(targetState = when { ui.noHousehold -> 3; ui.loadError -> 0; view == null -> 1; else -> 2 },
            animationSpec = tween(Motion.duration(Motion.medium), easing = Motion.easing), label = "close_load") { state ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s16)) {
                if (state == 3) { }
                else if (state == 0) { ErrorState(message = stringResource(R.string.money_load_error), onRetry = actions.onRetry, retryLabel = stringResource(R.string.money_load_retry)) }
                else if (state == 1 || view == null) { LoadingState(testTag = "close_loading") }
                else {
                    val canGoBack = (ui.earliestMonth ?: ui.currentMonth?.minusMonths(12))?.let { view.month > it } != false
                    val canGoNext = ui.currentMonth?.let { view.month < it.minusMonths(1) } == true
                    MonthArrows(view.month, canGoBack = canGoBack, canGoNext = canGoNext, actions)
                    view.existing?.let {
                        Chip(
                            text = stringResource(R.string.close_done_on, it.closedOn.format(dateFormat)),
                            variant = ChipVariant.Success,
                            modifier = Modifier.testTag("close_done"),
                        )
                        MoneySurface(testTag = "close_summary_open") {
                            AmountRow(
                                title = stringResource(R.string.summary_open, hebrewMonthName(view.month.month)),
                                amount = null,
                                detail = stringResource(R.string.summary_entry_detail),
                                action = RowAction.Open { actions.onSummary(view.month) },
                            )
                        }
                    }
                    ui.nextPending?.let { next ->
                        MoneySurface {
                            AmountRow(title = stringResource(R.string.close_next_pending, monthTitle(next)), amount = null,
                                action = RowAction.Open(actions.onNextPending), testTag = "close_next_pending")
                        }
                    }
                    SectionTitle(stringResource(R.string.close_checks_title))
                    MoneySurface(testTag = "close_checks") { Checks(view, actions) }
                    CloseBalancesContent(ui, actions)
                    SectionTitle(stringResource(R.string.close_summary_title))
                    MoneySurface(testTag = "close_summary") {
                        val colors = MaterialTheme.appColors
                        AmountRow(stringResource(R.string.close_income), formatMoney(view.preview.snapshot.actualIncomes))
                        AmountRow(stringResource(R.string.close_expenses), formatMoney(view.preview.snapshot.actualExpenses))
                        AmountRow(
                            title = stringResource(R.string.close_saving),
                            amount = formatMoney(view.saving),
                            titleStyle = AppType.title,
                            amountColor = if (view.saving.isNegative()) colors.danger else null,
                            testTag = "close_saving",
                        )
                    }
                    PrimaryButton(
                        text = stringResource(
                            if (view.existing == null) R.string.close_button else R.string.close_again,
                            hebrewMonthName(view.month.month),
                        ),
                        onClick = actions.onClose,
                        enabled = view.canClose && ui.balancesValid,
                        isLoading = ui.saving,
                        modifier = Modifier.fillMaxWidth().testTag("close_button"),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthArrows(month: YearMonth, canGoBack: Boolean, canGoNext: Boolean, actions: MonthCloseActions) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = actions.onPrevious, enabled = canGoBack, modifier = Modifier.testTag("close_previous")) {
            Text(stringResource(R.string.close_previous, hebrewMonthName(month.minusMonths(1).month)), style = AppType.body)
        }
        if (canGoNext) {
            TextButton(onClick = actions.onNext, modifier = Modifier.testTag("close_next")) {
                Text(stringResource(R.string.close_next, hebrewMonthName(month.plusMonths(1).month)), style = AppType.body)
            }
        }
    }
}

@Composable
private fun Checks(view: CloseView, actions: MonthCloseActions) {
    val colors = MaterialTheme.appColors
    if (view.checks.none { it.blocking }) {
        AmountRow(title = stringResource(R.string.close_ready), amount = null, testTag = "close_ready")
    }
    view.checks.forEach { check ->
        when (check.kind) {
            CloseCheckKind.MONTH_NOT_OVER -> AmountRow(
                title = stringResource(R.string.close_not_over),
                detail = stringResource(R.string.close_not_over_detail, hebrewMonthName(view.month.plusMonths(1).month)),
                amount = null,
                testTag = "close_not_over",
            )
            CloseCheckKind.UNASSIGNED -> {
                AmountRow(
                title = stringResource(R.string.close_unassigned, check.detail.toInt()),
                detail = stringResource(R.string.close_unassigned_detail),
                amount = null,
                action = RowAction.Open { actions.onLines(view.month, LineFilter.UNASSIGNED) },
                testTag = "close_unassigned",
                )
                TextButton(onClick = actions.onClassifyRest, modifier = Modifier.testTag("close_classify_rest")) {
                    Text(stringResource(R.string.close_classify_rest), style = AppType.caption)
                }
            }
            CloseCheckKind.PENDING -> AmountRow(
                title = stringResource(R.string.close_pending, check.detail.toInt()),
                detail = stringResource(R.string.close_pending_detail),
                amount = null,
                amountColor = colors.textSecondary,
                action = RowAction.Open { actions.onLines(view.month, LineFilter.PENDING) },
                testTag = "close_pending",
            )
            CloseCheckKind.MISSING_SOURCE, CloseCheckKind.STALE_BALANCE -> Unit
        }
    }
    view.missingSources.forEach { source ->
        AmountRow(
            title = stringResource(R.string.close_missing, sourceLabel(source.source), source.sourceId),
            detail = stringResource(R.string.close_missing_detail),
            amount = null,
            action = RowAction.Open(actions.onImport),
            testTag = "close_missing_${source.key}",
        )
        TextButton(
            onClick = { actions.onNoLines(source) },
            modifier = Modifier.padding(horizontal = Spacing.s8).testTag("close_no_lines_${source.key}"),
        ) {
            Text(stringResource(R.string.close_no_lines), style = AppType.caption, color = colors.accent)
        }
        TextButton(onClick = { actions.onSourceClosed(source, true) },
            modifier = Modifier.padding(horizontal = Spacing.s8).testTag("close_source_closed_${source.key}")) {
            Text(stringResource(R.string.close_source_closed), style = AppType.caption, color = colors.accent)
        }
    }
    view.closedSources.forEach { (source, from) ->
        AmountRow(title = stringResource(R.string.close_source_closed_from, sourceLabel(source.source), source.sourceId, monthTitle(from)),
            amount = null)
        TextButton(onClick = { actions.onSourceClosed(source, false) },
            modifier = Modifier.padding(horizontal = Spacing.s8).testTag("close_source_undo_${source.key}")) {
            Text(stringResource(R.string.close_source_reopen), style = AppType.caption, color = colors.accent)
        }
    }
    view.staleAssets.forEach { asset ->
        AmountRow(
            title = stringResource(R.string.close_stale, asset.name),
            detail = stringResource(R.string.close_stale_detail, asset.lastUpdated.format(dateFormat)),
            amount = formatMoney(asset.balance),
            action = RowAction.Edit { actions.onAsset(asset) },
            testTag = "close_stale_${asset.id}",
        )
    }
}
