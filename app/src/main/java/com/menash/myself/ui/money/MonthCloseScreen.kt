package com.menash.myself.ui.money

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.MonthCloseUi
import com.menash.myself.feature.money.MonthCloseViewModel

/** "סגירת החודש" with its one question before a closed month is replaced. The other actions open other screens. */
@Composable
fun MonthCloseScreen(ui: MonthCloseUi, viewModel: MonthCloseViewModel, actions: MonthCloseActions, modifier: Modifier = Modifier) {
    BackHandler { viewModel.close() }
    MonthCloseContent(
        ui = ui,
        actions = actions.copy(
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
            onNoLines = viewModel::markNoLines,
            onClose = viewModel::closeMonth,
        ),
        modifier = modifier,
    )
    val month = ui.view?.month ?: return
    if (ui.confirmReclose) {
        AlertDialog(
            onDismissRequest = viewModel::cancelReclose,
            title = { Text(stringResource(R.string.close_again_title, hebrewMonthName(month.month)), style = AppType.title) },
            text = { Text(stringResource(R.string.close_again_detail), style = AppType.body) },
            confirmButton = {
                TextButton(onClick = viewModel::closeMonth, modifier = Modifier.testTag("close_again_confirm")) {
                    Text(stringResource(R.string.close_again_confirm), style = AppType.body, color = MaterialTheme.appColors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelReclose) {
                    Text(stringResource(R.string.close_keep), style = AppType.body)
                }
            },
        )
    }
}
