package com.menash.myself.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.Income
import com.menash.myself.feature.money.domain.TitheRate
import com.menash.myself.feature.money.importer.MonthLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeSourceSheet(
    line: MonthLine,
    incomes: List<Income>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    name: String = line.row.description,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.appColors.card,
    ) {
        IncomeSourceList(line, incomes, onPick, name = name)
    }
}

/** Which income source an imported income line is; the payer is remembered for next month. */
@Composable
fun IncomeSourceList(
    line: MonthLine,
    incomes: List<Income>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    name: String = line.row.description,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.s16)
            .padding(bottom = Spacing.s24)
            .testTag("income_source_list"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s8),
    ) {
        Text(text = stringResource(R.string.tx_income_pick_title, name), style = AppType.title, color = colors.textPrimary)
        line.row.charge?.let {
            Text(text = formatMoney(Money(-it.agorot)), style = AppType.body, color = colors.textSecondary)
        }
        Text(text = stringResource(R.string.tx_income_pick_detail), style = AppType.caption, color = colors.textSecondary)
        if (incomes.isEmpty()) {
            Text(text = stringResource(R.string.tx_income_none), style = AppType.body, color = colors.textSecondary)
        }
        MoneySurface {
            incomes.forEach { income ->
                AmountRow(
                    title = income.name,
                    detail = when (income.titheRate) {
                        TitheRate.NONE -> null
                        TitheRate.MAASER -> stringResource(R.string.money_tithe_maaser)
                        TitheRate.CHOMESH -> stringResource(R.string.money_tithe_chomesh)
                    },
                    amount = null,
                    amountColor = null,
                    onClick = { onPick(income.id) },
                    testTag = "income_source_${income.id}",
                )
            }
        }
    }
}
