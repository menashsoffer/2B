package com.menash.myself.ui.money

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
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.GoalsUi
import com.menash.myself.feature.money.GoalsViewModel

/** "האם אפשר?" with its goal sheet and the delete question. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(ui: GoalsUi, viewModel: GoalsViewModel, modifier: Modifier = Modifier) {
    GoalsContent(
        ui = ui,
        actions = GoalsActions(onAdd = viewModel::startAdding, onGoal = viewModel::startEditing, onClose = viewModel::close),
        modifier = modifier,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GoalsOverlays(ui: GoalsUi, viewModel: GoalsViewModel) {
    val editor = ui.editor ?: return
    val month = ui.month ?: return
    ModalBottomSheet(
        onDismissRequest = viewModel::dismissEditor,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.appColors.card,
    ) {
        GoalEditorForm(
            editor = editor,
            month = month,
            actions = GoalEditorActions(
                onChange = viewModel::updateDraft,
                onSave = viewModel::save,
                onDelete = { viewModel.requestDelete(true) },
            ),
        )
    }
    if (editor.confirmDelete) {
        AlertDialog(
            onDismissRequest = { viewModel.requestDelete(false) },
            title = { Text(stringResource(R.string.goals_delete_title, editor.draft.name), style = AppType.title) },
            confirmButton = {
                TextButton(onClick = viewModel::delete, modifier = Modifier.testTag("goals_delete_confirm")) {
                    Text(stringResource(R.string.goals_delete_confirm), style = AppType.body, color = MaterialTheme.appColors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.requestDelete(false) }) {
                    Text(stringResource(R.string.goals_keep), style = AppType.body)
                }
            },
        )
    }
}
