package com.menash.myself.feature.money.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.PeriodView
import com.menash.myself.feature.money.domain.MonthlyComparison

/** A calendar year against the year before, by monthly averages, and the categories that rose the most. */
@Composable
internal fun PeriodYearCompare(view: PeriodView, onCategory: (String) -> Unit) {
    val comparison = view.comparison ?: return
    val previous = comparison.year - 1
    SectionTitle(stringResource(R.string.period_compare_title, previous))
    MoneySurface(testTag = "period_compare") {
        Text(
            stringResource(R.string.period_compare_months, comparison.monthsThis, comparison.monthsPrevious),
            style = AppType.caption,
            color = MaterialTheme.appColors.textSecondary,
            modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8),
        )
        CompareRow(stringResource(R.string.period_income), comparison.income, previous, higherIsGood = true)
        CompareRow(stringResource(R.string.period_expenses), comparison.expenses, previous, higherIsGood = false)
        CompareRow(stringResource(R.string.period_left), comparison.saving, previous, higherIsGood = true)
    }
    if (view.risers.isNotEmpty()) {
        SectionTitle(stringResource(R.string.period_risers))
        MoneySurface(testTag = "period_risers") {
            view.risers.forEach { riser ->
                AmountRow(
                    title = riser.name ?: stringResource(R.string.period_class_other),
                    amount = formatMoney(riser.thisYear),
                    detail = stringResource(R.string.period_riser_detail, formatMoney(riser.difference), previous),
                    amountColor = MaterialTheme.appColors.danger,
                    onClick = riser.categoryId?.let { id -> { onCategory(id) } },
                )
            }
        }
    }
}

@Composable
private fun CompareRow(title: String, values: MonthlyComparison, previous: Int, higherIsGood: Boolean) {
    val colors = MaterialTheme.appColors
    val diff = values.difference
    val good = if (higherIsGood) diff.isPositive() else diff.isNegative()
    AmountRow(
        title = title,
        amount = formatMoney(values.thisYear),
        detail = stringResource(R.string.period_compare_detail, previous, formatMoney(values.previousYear), signedMoney(diff)),
        amountColor = when {
            diff == Money.ZERO -> null
            good -> colors.success
            else -> colors.warning
        },
    )
}

private fun signedMoney(amount: Money): String = if (amount.isPositive()) "+${formatMoney(amount)}" else formatMoney(amount)
