package com.menash.myself.feature.money.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Motion
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.MonthRow

@Composable
internal fun PlanRow(row: MonthRow, isExpanded: Boolean, actions: MoneyActions) {
    val colors = MaterialTheme.appColors
    val actual = row.actual
    val duration = Motion.duration(Motion.medium)
    AmountRow(
        title = rowLabel(row.kind),
        amount = formatMoney(row.total),
        detail = if (!row.actualKnown) stringResource(R.string.status_income_not_imported) else actual?.let { stringResource(R.string.money_actual, formatMoney(it)) },
        titleStyle = AppType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
        action = RowAction.Expand(isExpanded) { actions.onToggleRow(row.kind) },
        testTag = "money_row_${row.kind.name.lowercase()}",
    )
    AnimatedVisibility(visible = isExpanded, enter = Motion.expand(duration), exit = Motion.shrink(duration)) {
        Column { row.items.forEach { item ->
            AmountRow(
                title = item.name,
                amount = item.monthly?.let(::formatMoney),
                detail = item.actual?.let { stringResource(R.string.money_actual, formatMoney(it)) }
                    ?: if (item.monthly == null) stringResource(R.string.money_usd_missing) else null,
                amountColor = colors.textSecondary,
                action = RowAction.Edit { actions.onEdit(item.entry) },
                modifier = Modifier.padding(start = Spacing.s16),
                testTag = "money_item_${item.entry.id}",
            )
        } }
    }
}

