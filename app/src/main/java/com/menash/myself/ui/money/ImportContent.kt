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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.Chip
import com.menash.myself.core.designsystem.component.ChipVariant
import com.menash.myself.core.designsystem.component.ErrorState
import com.menash.myself.core.designsystem.component.EmptyState
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.LoadingState
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.ImportUi
import com.menash.myself.feature.money.domain.ImportCheck
import com.menash.myself.feature.money.importer.FileProblem
import com.menash.myself.feature.money.importer.FileSummary
import com.menash.myself.feature.money.importer.MerchantChoice
import com.menash.myself.feature.money.importer.MerchantDecision
import com.menash.myself.feature.money.importer.Source
import java.time.format.DateTimeFormatter

data class ImportActions(
    val onPick: (String) -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onClose: () -> Unit = {},
)

/** The import flow, full screen inside the money tab. Every count comes from the [ImportUi] state. */
@Composable
fun ImportContent(ui: ImportUi, actions: ImportActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("import_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        MoneySubScreen(title = stringResource(R.string.import_title), onBack = actions.onClose)
        when (ui) {
            ImportUi.Idle -> Unit
            ImportUi.Error -> ErrorState(message = stringResource(R.string.money_load_error), onRetry = actions.onRetry, retryLabel = stringResource(R.string.money_load_retry))
            ImportUi.Reading -> LoadingState(message = stringResource(R.string.import_reading))
            ImportUi.Saving -> LoadingState(message = stringResource(R.string.import_saving))
            is ImportUi.Done -> EmptyState(
                title = stringResource(R.string.import_done_title),
                description = stringResource(R.string.import_done_detail, ui.written),
                icon = Icons.Outlined.CheckCircle,
                actionLabel = stringResource(R.string.import_close),
                onActionClick = actions.onClose,
                testTag = "import_done",
            )
            is ImportUi.Review -> ReviewBody(ui, actions)
        }
    }
}

@Composable
private fun ReviewBody(ui: ImportUi.Review, actions: ImportActions) {
    SectionTitle(stringResource(R.string.import_files))
    Column(modifier = Modifier.testTag("import_files"), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        ui.review.files.forEach { file -> MoneySurface { FileRow(file) } }
    }
    if (ui.review.nothingToSave) {
        Text(
            text = stringResource(R.string.import_nothing_new),
            style = AppType.body,
            color = MaterialTheme.appColors.textSecondary,
            modifier = Modifier.padding(horizontal = Spacing.s8).testTag("import_nothing_new"),
        )
    }

    if (ui.review.merchants.isNotEmpty()) {
        Text(
            text = stringResource(R.string.import_merchants_remaining, ui.review.merchants.count { ui.decisionFor(it.key) == null }),
            style = AppType.caption,
            color = MaterialTheme.appColors.textSecondary,
            modifier = Modifier.padding(horizontal = Spacing.s8),
        )
        MoneySurface(testTag = "import_merchants") {
            ui.review.merchants.forEach { merchant -> MerchantRow(merchant, ui, actions) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), modifier = Modifier.fillMaxWidth()) {
        if (!ui.review.nothingToSave) {
            PrimaryButton(
                text = stringResource(R.string.import_confirm),
                onClick = actions.onConfirm,
                modifier = Modifier.weight(1f).testTag("import_confirm"),
            )
        }
        TextButton(onClick = actions.onClose, modifier = Modifier.testTag("import_cancel")) {
            Text(
                text = stringResource(if (ui.review.nothingToSave) R.string.import_close else R.string.import_cancel),
                style = AppType.body,
            )
        }
    }
}

@Composable
private fun CountRow(label: Int, count: Int) {
    if (count == 0) return
    AmountRow(title = stringResource(label), amount = count.toString(), testTag = "import_count_$label")
}

private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yy")

@Composable
fun sourceLabel(source: Source): String = stringResource(
    when (source) {
        Source.HAPOALIM -> R.string.import_source_hapoalim
        Source.ONE_ZERO -> R.string.import_source_onezero
        Source.MAX -> R.string.import_source_max
        Source.CAL -> R.string.import_source_cal
    }
)

@Composable
private fun problemText(problem: FileProblem): String = stringResource(
    when (problem) {
        FileProblem.ENCRYPTED -> R.string.import_problem_encrypted
        FileProblem.TOO_LARGE -> R.string.import_problem_too_large
        FileProblem.CORRUPT -> R.string.import_problem_corrupt
        FileProblem.UNKNOWN_FORMAT, FileProblem.NO_TRANSACTIONS -> R.string.import_problem_unknown
    }
)

@Composable
private fun FileRow(file: FileSummary) {
    val source = file.source
    Column(
        modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8),
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        Text(text = file.name, style = AppType.body, color = MaterialTheme.appColors.textPrimary, maxLines = 1)
        val problem = file.problem
        when {
            // Plain text, not a chip: the reason says what to do and must not be cut off at large fonts.
            file.readError -> Text(text = stringResource(R.string.import_read_failed), style = AppType.body, color = MaterialTheme.appColors.warning)
            problem != null -> Text(text = problemText(problem), style = AppType.body, color = MaterialTheme.appColors.warning)
            source != null && file.firstDate != null && file.lastDate != null -> Text(
                text = stringResource(
                    R.string.import_file_detail, sourceLabel(source), file.rows,
                    file.firstDate.format(dayFormat), file.lastDate.format(dayFormat),
                ),
                style = AppType.caption,
                color = MaterialTheme.appColors.textSecondary,
            )
            source != null -> Text(
                text = stringResource(R.string.import_file_empty, sourceLabel(source)),
                style = AppType.caption,
                color = MaterialTheme.appColors.textSecondary,
            )
        }
        if (problem == null && !file.readError) {
            Text(text = stringResource(R.string.import_file_counts, file.newCount, file.existingCount), style = AppType.caption, color = MaterialTheme.appColors.textSecondary)
        }
        when (val total = file.total) {
            ImportCheck.Ok -> Chip(text = stringResource(R.string.import_total_ok), variant = ChipVariant.Success)
            is ImportCheck.Mismatch -> Chip(
                text = stringResource(R.string.import_total_gap, formatMoney(total.difference)),
                variant = ChipVariant.Warning,
                leadingIcon = Icons.Outlined.Warning,
            )
            null -> Unit
        }
        file.balanceBreakRow?.let {
            Chip(text = stringResource(R.string.import_balance_break, it), variant = ChipVariant.Warning, leadingIcon = Icons.Outlined.Warning)
        }
        if (file.skippedRows.isNotEmpty()) {
            Chip(text = stringResource(R.string.import_skipped_rows, file.skippedRows.joinToString(", ")), variant = ChipVariant.Warning)
        }
    }
}

@Composable
private fun MerchantRow(merchant: MerchantChoice, ui: ImportUi.Review, actions: ImportActions) {
    val decision = ui.decisionFor(merchant.key)
    val label = when (decision) {
        is MerchantDecision.Categorise -> ui.categories.firstOrNull { it.id == decision.categoryId }?.name
        MerchantDecision.OwnTransfer -> stringResource(R.string.import_own_transfer)
        MerchantDecision.Later -> stringResource(R.string.import_later)
        null -> null
    } ?: stringResource(R.string.import_choose)
    Column(modifier = Modifier.padding(end = Spacing.s16, bottom = Spacing.s8)) {
        AmountRow(
            title = merchant.name,
            amount = formatMoney(merchant.total),
            detail = merchant.hint?.let { stringResource(R.string.import_hint, it) },
            onClick = { actions.onPick(merchant.key) },
            testTag = "import_merchant_${merchant.key.hashCode()}",
        )
        Chip(
            text = label,
            variant = if (decision == null) ChipVariant.Warning else ChipVariant.Default,
            onClick = { actions.onPick(merchant.key) },
            modifier = Modifier.padding(start = Spacing.s16),
        )
    }
}
