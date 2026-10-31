package com.menash.myself.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.Chip
import com.menash.myself.core.designsystem.component.ChipVariant
import com.menash.myself.core.designsystem.component.PillTabs
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.component.TextInput
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.FundDraft
import com.menash.myself.feature.money.FundEditor
import com.menash.myself.feature.money.FundField
import com.menash.myself.feature.money.PensionUi
import com.menash.myself.feature.money.PensionViewModel
import com.menash.myself.feature.money.domain.FundKind
import java.time.LocalDate
import java.time.YearMonth

/** "פנסיה וחיסכון" with its fund sheet and the delete confirmation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PensionScreen(ui: PensionUi, viewModel: PensionViewModel, today: LocalDate, modifier: Modifier = Modifier) {
    PensionContent(
        ui = ui,
        actions = PensionActions(onAdd = viewModel::startAdding, onFund = viewModel::edit, onClose = viewModel::close),
        modifier = modifier,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PensionOverlays(ui: PensionUi, viewModel: PensionViewModel, today: LocalDate) {
    val editor = ui.editor ?: return
    ModalBottomSheet(
        onDismissRequest = viewModel::dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.appColors.card,
    ) {
        FundForm(editor, today, viewModel::update, viewModel::save) { viewModel.requestDelete(true) }
    }
    if (editor.confirmDelete) {
        AlertDialog(
            onDismissRequest = { viewModel.requestDelete(false) },
            title = { Text(stringResource(R.string.pension_delete_title, editor.draft.provider), style = AppType.title) },
            confirmButton = {
                TextButton(onClick = viewModel::delete, modifier = Modifier.testTag("pension_delete_confirm")) {
                    Text(stringResource(R.string.pension_delete_confirm), style = AppType.body, color = MaterialTheme.appColors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.requestDelete(false) }) {
                    Text(stringResource(R.string.pension_keep), style = AppType.body)
                }
            },
        )
    }
}

private val kinds = listOf(FundKind.PENSION, FundKind.HISHTALMUT, FundKind.GEMEL)

/** The fund sheet body; stateless so goldens render it without a window. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FundForm(
    editor: FundEditor,
    today: LocalDate,
    onChange: ((FundDraft) -> FundDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val draft = editor.draft
    val number = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    @Composable
    fun Field(field: FundField, value: String, label: Int, error: Int, change: (FundDraft, String) -> FundDraft, keyboard: Boolean = true) {
        TextInput(
            value = value,
            onValueChange = { text -> onChange { change(it, text) } },
            label = stringResource(label),
            keyboardOptions = if (keyboard) number else KeyboardOptions.Default,
            error = stringResource(error).takeIf { editor.invalid == field },
            modifier = Modifier.testTag("pension_field_${field.name.lowercase()}"),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = Spacing.s16)
            .padding(bottom = Spacing.s24)
            .testTag("pension_form"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        Text(
            text = stringResource(if (editor.isNew) R.string.pension_form_new else R.string.pension_form_edit),
            style = AppType.title,
            color = colors.textPrimary,
        )
        PillTabs(
            options = kinds.map { fundKindLabel(it) },
            selectedIndex = kinds.indexOf(draft.kind),
            onSelect = { index -> onChange { it.copy(kind = kinds[index]) } },
        )
        Field(FundField.PROVIDER, draft.provider, R.string.pension_field_provider, R.string.pension_error_provider, { d, t -> d.copy(provider = t) }, keyboard = false)
        Field(FundField.VALUE, draft.value, R.string.pension_field_value, R.string.pension_error_amount, { d, t -> d.copy(value = t) })
        Field(FundField.DEPOSITS, draft.totalDeposits, R.string.pension_field_deposits, R.string.pension_error_amount, { d, t -> d.copy(totalDeposits = t) })
        Field(FundField.MONTHLY, draft.monthlyDeposit, R.string.pension_field_monthly, R.string.pension_error_amount, { d, t -> d.copy(monthlyDeposit = t) })
        Field(FundField.FEE_BALANCE, draft.feeOnBalance, R.string.pension_field_fee_balance, R.string.pension_error_fee, { d, t -> d.copy(feeOnBalance = t) })
        Field(FundField.FEE_DEPOSITS, draft.feeOnDeposits, R.string.pension_field_fee_deposits, R.string.pension_error_fee, { d, t -> d.copy(feeOnDeposits = t) })
        OpenedOn(draft.openedOn, today, onChange)
        PrimaryButton(text = stringResource(R.string.pension_save), onClick = onSave, isLoading = editor.saving, modifier = Modifier.fillMaxWidth().testTag("pension_save"))
        SaveFailure(editor.saveError, onSave)
        if (!editor.isNew) {
            TextButton(onClick = onDelete, modifier = Modifier.testTag("pension_delete")) {
                Text(stringResource(R.string.pension_delete), style = AppType.body, color = colors.danger)
            }
        }
    }
}

/** Opening month, moved by year and month chips; only hishtalmut uses it (the 6-year liquidity date). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpenedOn(openedOn: LocalDate?, today: LocalDate, onChange: ((FundDraft) -> FundDraft) -> Unit) {
    Text(stringResource(R.string.pension_opened_hint), style = AppType.caption, color = MaterialTheme.appColors.textSecondary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        if (openedOn == null) {
            Chip(text = stringResource(R.string.pension_opened_none))
            Chip(
                text = stringResource(R.string.pension_opened_set),
                onClick = { onChange { it.copy(openedOn = today.withDayOfMonth(1)) } },
                modifier = Modifier.testTag("pension_opened_set"),
            )
        } else {
            Chip(text = stringResource(R.string.pension_opened, hebrewMonthYear(YearMonth.from(openedOn))), variant = ChipVariant.Success)
            Chip(text = stringResource(R.string.pension_year_back), onClick = { onChange { it.copy(openedOn = openedOn.minusYears(1)) } })
            Chip(text = stringResource(R.string.pension_month_back), onClick = { onChange { it.copy(openedOn = openedOn.minusMonths(1)) } })
            Chip(text = stringResource(R.string.pension_opened_clear), onClick = { onChange { it.copy(openedOn = null) } })
        }
    }
}
