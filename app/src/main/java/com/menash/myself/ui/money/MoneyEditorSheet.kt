package com.menash.myself.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.Chip
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.AmountScope
import com.menash.myself.feature.money.DraftPreview
import com.menash.myself.feature.money.EditKind
import com.menash.myself.feature.money.EditorState
import com.menash.myself.feature.money.ItemDraft
import com.menash.myself.feature.money.domain.Card
import com.menash.myself.feature.money.domain.Category

/** Kinds offered by "להוסיף", in the order of the month rows. */
val addableKinds = listOf(
    EditKind.INCOME, EditKind.FIXED, EditKind.SUBSCRIPTION, EditKind.VARIABLE, EditKind.PERIODIC,
    EditKind.INSTALLMENT, EditKind.DEBT, EditKind.SAVING, EditKind.ASSET, EditKind.CARD,
)

@Composable
fun kindLabel(kind: EditKind): String = stringResource(
    when (kind) {
        EditKind.INCOME -> R.string.money_kind_income
        EditKind.FIXED -> R.string.money_kind_fixed
        EditKind.SUBSCRIPTION -> R.string.money_kind_subscription
        EditKind.VARIABLE -> R.string.money_kind_variable
        EditKind.PERIODIC -> R.string.money_kind_periodic
        EditKind.INSTALLMENT -> R.string.money_kind_installment
        EditKind.DEBT -> R.string.money_kind_debt
        EditKind.SAVING -> R.string.money_kind_saving
        EditKind.ASSET -> R.string.money_kind_asset
        EditKind.CARD -> R.string.money_kind_card
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KindPickerSheet(onPick: (EditKind) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.appColors.card,
    ) {
        KindPickerContent(onPick)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KindPickerContent(onPick: (EditKind) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.s16)
            .padding(bottom = Spacing.s24)
            .testTag("money_kind_picker"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        Text(
            text = stringResource(R.string.money_add_title),
            style = AppType.title,
            color = MaterialTheme.appColors.textPrimary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s8),
        ) {
            addableKinds.forEach { kind ->
                Chip(text = kindLabel(kind), onClick = { onPick(kind) }, modifier = Modifier.testTag("money_kind_${kind.name.lowercase()}"))
            }
        }
    }
}

/** What the form needs besides the draft. */
data class EditorChoices(
    val cards: List<Card>,
    val categories: List<Category>,
    val preview: DraftPreview,
)

data class EditorActions(
    val onChange: ((ItemDraft) -> ItemDraft) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onNewCard: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyEditorSheet(
    editor: EditorState,
    choices: EditorChoices,
    actions: EditorActions,
    onScope: (AmountScope) -> Unit,
    onDismissScope: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.appColors.card,
    ) {
        MoneyEditorForm(editor, choices, actions)
    }
    if (editor.askScope) {
        AlertDialog(
            onDismissRequest = onDismissScope,
            title = { Text(stringResource(R.string.money_scope_title), style = AppType.title) },
            confirmButton = {
                TextButton(onClick = { onScope(AmountScope.FROM_NOW) }, modifier = Modifier.testTag("money_scope_from_now")) {
                    Text(stringResource(R.string.money_scope_from_now), style = AppType.body)
                }
            },
            dismissButton = {
                TextButton(onClick = { onScope(AmountScope.THIS_MONTH) }, modifier = Modifier.testTag("money_scope_this_month")) {
                    Text(stringResource(R.string.money_scope_this_month), style = AppType.body)
                }
            },
            containerColor = MaterialTheme.appColors.card,
        )
    }
    if (editor.confirmDelete) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = {
                Text(stringResource(R.string.money_delete_confirm, editor.draft.name.trim()), style = AppType.title)
            },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text(stringResource(R.string.money_delete), style = AppType.body, color = MaterialTheme.appColors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete) {
                    Text(stringResource(R.string.money_cancel), style = AppType.body)
                }
            },
            containerColor = MaterialTheme.appColors.card,
        )
    }
}

