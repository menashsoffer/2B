package com.menash.myself.ui.money

import androidx.activity.compose.BackHandler
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
import com.menash.myself.feature.money.TitheUi
import com.menash.myself.feature.money.TitheViewModel
import java.time.YearMonth

/** "מעשרות" with its donation and settings sheets. [onLink] opens a month's lines to link income. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitheScreen(ui: TitheUi, viewModel: TitheViewModel, onLink: (YearMonth) -> Unit, modifier: Modifier = Modifier) {
    BackHandler { viewModel.close() }
    TitheContent(
        ui = ui,
        actions = TitheActions(
            onStart = viewModel::startSettings,
            onSettings = viewModel::startSettings,
            onAddDonation = viewModel::startDonation,
            onDonation = viewModel::editDonation,
            onLink = onLink,
            onClose = viewModel::close,
        ),
        modifier = modifier,
    )
    val month = ui.month ?: return
    val today = ui.today ?: return
    val sheetColor = MaterialTheme.appColors.card
    ui.sheets.donation?.let { editor ->
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSheets,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = sheetColor,
        ) {
            DonationForm(editor, today, viewModel::updateDonation, viewModel::saveDonation) { viewModel.requestDeleteDonation(true) }
        }
        if (editor.confirmDelete) {
            AlertDialog(
                onDismissRequest = { viewModel.requestDeleteDonation(false) },
                title = {
                    val name = editor.draft.organisation.ifBlank { editor.draft.amount }
                    Text(stringResource(R.string.tithe_delete_title, name), style = AppType.title)
                },
                confirmButton = {
                    TextButton(onClick = viewModel::deleteDonation, modifier = Modifier.testTag("tithe_delete_confirm")) {
                        Text(stringResource(R.string.tithe_delete_confirm), style = AppType.body, color = MaterialTheme.appColors.danger)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.requestDeleteDonation(false) }) {
                        Text(stringResource(R.string.tithe_keep), style = AppType.body)
                    }
                },
            )
        }
    }
    ui.sheets.settings?.let { draft ->
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSheets,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = sheetColor,
        ) {
            TitheSettingsForm(draft, month, ui.assets, viewModel::updateSettings, viewModel::saveSettings)
        }
    }
}
