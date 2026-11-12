package com.menash.myself.feature.money.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.TitheMonth
import java.time.YearMonth

/** One month: each source with what came in (actual or planned), what that owes, and what was donated. */
@Composable
fun TitheMonthCard(month: TitheMonth, onLink: (YearMonth) -> Unit) {
    val colors = MaterialTheme.appColors
    MoneySurface(testTag = "tithe_month_${month.month}") {
        AmountRow(title = hebrewMonthYear(month.month), amount = null, titleStyle = AppType.title)
        month.sources.forEach { source ->
            AmountRow(
                title = source.income.name,
                detail = stringResource(if (source.actual) R.string.tithe_actual else R.string.tithe_planned),
                amount = formatMoney(source.net),
                amountColor = if (source.actual) null else colors.textSecondary,
            )
        }
        AmountRow(title = stringResource(R.string.tithe_month_owed), amount = formatMoney(month.owed))
        AmountRow(title = stringResource(R.string.tithe_month_donated), amount = formatMoney(month.donated))
        if (month.unlinkedLines > 0) {
            AmountRow(
                title = stringResource(R.string.tithe_unlinked, month.unlinkedLines),
                detail = stringResource(R.string.tithe_unlinked_detail),
                amount = null,
                amountColor = colors.warning,
                action = RowAction.Open { onLink(month.month) },
                testTag = "tithe_unlinked_${month.month}",
            )
        }
    }
}
