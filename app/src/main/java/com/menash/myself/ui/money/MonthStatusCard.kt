package com.menash.myself.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.Chip
import com.menash.myself.core.designsystem.component.ChipVariant
import com.menash.myself.core.designsystem.component.ProgressBar
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MoneyMonth
import com.menash.myself.feature.money.MonthStatus
import com.menash.myself.feature.money.UsageLevel
import java.time.format.DateTimeFormatter

private val untilFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M")
private val closedFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yy")

/** Previous / next month, with whether the shown month is still open or already closed. Start = older months. */
@Composable
internal fun MonthSwitcher(money: MoneyMonth, closed: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier.fillMaxWidth().testTag("money_month_switcher"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious, enabled = money.canGoBack, modifier = Modifier.testTag("money_month_previous")) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.status_previous_month), tint = arrowTint(money.canGoBack))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Text(
                text = stringResource(R.string.status_month_title, hebrewMonthName(money.month.month), money.month.year),
                style = AppType.body.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary,
                modifier = Modifier.testTag("money_month_title"),
            )
            Chip(
                text = stringResource(if (closed) R.string.status_closed else R.string.status_open),
                variant = if (closed) ChipVariant.Default else ChipVariant.Success,
            )
        }
        IconButton(onClick = onNext, enabled = money.canGoNext, modifier = Modifier.testTag("money_month_next")) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.status_next_month), tint = arrowTint(money.canGoNext))
        }
    }
}

/** The headline: "נשאר לשוטפות" for an open month, the plan's surplus before any import, the frozen result once closed. */
@Composable
internal fun MonthStatusHeadline(status: MonthStatus) {
    val colors = MaterialTheme.appColors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8), modifier = Modifier.testTag("money_status")) {
        when (status) {
            is MonthStatus.Open -> {
                Caption(stringResource(R.string.status_left_for_current))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                    BigAmount(status.leftForCurrent)
                    Caption(stringResource(R.string.status_of_budget, formatMoney(status.currentBudget)))
                }
                val budget = status.currentBudget.agorot
                val progress = if (budget > 0) status.currentSpent.agorot.toFloat() / budget else 1f
                ProgressBar(
                    progress = progress,
                    color = when (status.usage) {
                        UsageLevel.NORMAL -> colors.accent
                        UsageLevel.NEAR -> colors.warning
                        UsageLevel.OVER -> colors.danger
                    },
                    modifier = Modifier.testTag("money_status_progress"),
                )
                status.dataUntil?.let { Caption(stringResource(R.string.status_data_until, it.format(untilFormat))) }
                SmallFigure(stringResource(R.string.status_expected_surplus), status.expectedSurplus)
                SmallFigure(stringResource(R.string.status_spent_so_far), status.spentSoFar)
            }
            is MonthStatus.PlanOnly -> {
                Caption(stringResource(R.string.status_expected_surplus))
                BigAmount(status.expectedSurplus)
                Caption(stringResource(R.string.status_no_import))
            }
            is MonthStatus.Closed -> {
                Caption(stringResource(R.string.status_left_actual))
                BigAmount(status.left)
                Caption(
                    stringResource(
                        R.string.status_closed_on,
                        status.closedOn.format(closedFormat), formatMoney(status.income), formatMoney(status.expenses),
                    ),
                )
            }
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(text = text, style = AppType.caption, color = MaterialTheme.appColors.textSecondary)
}

@Composable
private fun BigAmount(amount: Money) {
    val colors = MaterialTheme.appColors
    Text(
        text = formatMoney(amount),
        style = AppType.number,
        color = if (amount.isNegative()) colors.danger else colors.textPrimary,
        modifier = Modifier.testTag("money_status_value"),
    )
}

@Composable
private fun SmallFigure(label: String, amount: Money) {
    val colors = MaterialTheme.appColors
    Row(modifier = Modifier.fillMaxWidth().padding(top = Spacing.s4), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = AppType.caption, color = colors.textSecondary)
        Text(
            text = formatMoney(amount),
            style = AppType.caption,
            color = if (amount.isNegative()) colors.danger else colors.textPrimary,
        )
    }
}

@Composable
private fun arrowTint(enabled: Boolean) = MaterialTheme.appColors.textSecondary.copy(alpha = if (enabled) 1f else 0.3f)
