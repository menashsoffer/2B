package com.menash.myself.ui.money

import androidx.activity.compose.BackHandler
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
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.AppType
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
    /** Opens the file picker to import the missing statement. */
    val onImport: () -> Unit = {},
    val onNoLines: (StatementSource) -> Unit = {},
    val onLines: (YearMonth, LineFilter) -> Unit = { _, _ -> },
    val onAsset: (Asset) -> Unit = {},
    val onClose: () -> Unit = {},
    val onBack: () -> Unit = {},
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
        BackHandler(enabled = ui.view == null && LocalMoneyBackActive.current) { actions.onBack() }
        val view = ui.view ?: return@Column
        MoneySubScreen(
            title = stringResource(R.string.close_title, monthTitle(view.month)),
            subtitle = stringResource(R.string.close_subtitle),
            onBack = actions.onBack,
        )
        MonthArrows(view.month, canGoNext = ui.currentMonth?.let { view.month < it } == true, actions)
        view.existing?.let {
            Chip(
                text = stringResource(R.string.close_done_on, it.closedOn.format(dateFormat)),
                variant = ChipVariant.Success,
                modifier = Modifier.testTag("close_done"),
            )
        }
        SectionTitle(stringResource(R.string.close_checks_title))
        MoneySurface(testTag = "close_checks") { Checks(view, actions) }
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
            enabled = view.canClose,
            isLoading = ui.saving,
            modifier = Modifier.fillMaxWidth().testTag("close_button"),
        )
    }
}

@Composable
private fun MonthArrows(month: YearMonth, canGoNext: Boolean, actions: MonthCloseActions) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = actions.onPrevious, modifier = Modifier.testTag("close_previous")) {
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
            CloseCheckKind.UNASSIGNED -> AmountRow(
                title = stringResource(R.string.close_unassigned, check.detail.toInt()),
                detail = stringResource(R.string.close_unassigned_detail),
                amount = null,
                onClick = { actions.onLines(view.month, LineFilter.UNASSIGNED) },
                testTag = "close_unassigned",
            )
            CloseCheckKind.PENDING -> AmountRow(
                title = stringResource(R.string.close_pending, check.detail.toInt()),
                detail = stringResource(R.string.close_pending_detail),
                amount = null,
                amountColor = colors.textSecondary,
                onClick = { actions.onLines(view.month, LineFilter.PENDING) },
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
            onClick = actions.onImport,
            testTag = "close_missing_${source.key}",
        )
        TextButton(
            onClick = { actions.onNoLines(source) },
            modifier = Modifier.padding(horizontal = Spacing.s8).testTag("close_no_lines_${source.key}"),
        ) {
            Text(stringResource(R.string.close_no_lines), style = AppType.caption, color = colors.textSecondary)
        }
    }
    view.staleAssets.forEach { asset ->
        AmountRow(
            title = stringResource(R.string.close_stale, asset.name),
            detail = stringResource(R.string.close_stale_detail, asset.lastUpdated.format(dateFormat)),
            amount = formatMoney(asset.balance),
            onClick = { actions.onAsset(asset) },
            testTag = "close_stale_${asset.id}",
        )
    }
}
