package com.menash.myself.ui.money

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
import com.menash.myself.feature.money.TransactionsUi
import com.menash.myself.feature.money.TransactionsViewModel
import com.menash.myself.feature.money.importer.choiceFor

/** The transactions screen with its category sheet and the "remove a cancelled pending line" question. */
@Composable
fun TransactionsScreen(ui: TransactionsUi, viewModel: TransactionsViewModel, modifier: Modifier = Modifier) {
    TransactionsContent(
        ui = ui,
        actions = TransactionsActions(
            onMonth = viewModel::showMonth,
            onFilter = viewModel::setFilter,
            onLine = { line -> if (line.removable) viewModel.askRemove(line.id) else viewModel.pick(line.id) },
            onClose = viewModel::close,
            onRetry = viewModel::retry,
        ),
        modifier = modifier,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TransactionsOverlays(ui: TransactionsUi, viewModel: TransactionsViewModel) {
    val lines = ui.lines?.lines.orEmpty()
    lines.firstOrNull { it.id == ui.query.picking && it.income }?.let { line ->
        IncomeSourceSheet(
            line = line,
            incomes = ui.incomes,
            onPick = { viewModel.linkIncome(line.id, it) },
            onDismiss = { viewModel.pick(null) },
        )
    }
    lines.firstOrNull { it.id == ui.query.picking && !it.income }?.let { line ->
        ImportCategorySheet(
            merchant = choiceFor(line, ui.categories),
            categories = ui.categories,
            onDecide = { viewModel.decide(line.id, it) },
            onDismiss = { viewModel.pick(null) },
        )
    }
    lines.firstOrNull { it.id == ui.query.removing }?.let { line ->
        AlertDialog(
            onDismissRequest = { viewModel.askRemove(null) },
            title = { Text(stringResource(R.string.tx_remove_title, line.row.description), style = AppType.title) },
            text = { Text(stringResource(R.string.tx_remove_detail), style = AppType.body) },
            confirmButton = {
                TextButton(onClick = { viewModel.remove(line.id) }, modifier = Modifier.testTag("tx_remove_confirm")) {
                    Text(stringResource(R.string.tx_remove), style = AppType.body, color = MaterialTheme.appColors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.askRemove(null) }) {
                    Text(stringResource(R.string.tx_keep), style = AppType.body)
                }
            },
        )
    }
}
