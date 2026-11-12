package com.menash.myself.feature.money.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.Chip
import com.menash.myself.core.designsystem.component.ChipVariant
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.BigPicture
import com.menash.myself.feature.money.EditKind
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.AssetKind
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

@Composable
fun BigPictureContent(picture: BigPicture, actions: MoneyActions) {
    val colors = MaterialTheme.appColors
    val dateFormat = DateTimeFormatter.ofPattern("d.M.yyyy")
    MoneySurface(testTag = "money_net_worth") {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s8),
        ) {
            Text(text = stringResource(R.string.money_net_worth), style = AppType.caption, color = colors.textSecondary)
            val netWorth = picture.totalWorth
            if (netWorth == null) {
                Text(text = stringResource(R.string.money_net_worth_empty), style = AppType.body, color = colors.textPrimary)
            } else {
                Text(
                    text = formatMoney(netWorth),
                    style = AppType.number,
                    color = if (netWorth.isNegative()) colors.danger else colors.textPrimary,
                )
            }
        }
        val pension = picture.pension
        if (pension != null) {
            AmountRow(title = stringResource(R.string.money_net_worth_liquid), amount = formatMoney(picture.netWorth ?: Money.ZERO))
            AmountRow(
                title = stringResource(R.string.money_net_worth_pension),
                amount = formatMoney(pension.value),
                testTag = "money_net_worth_pension",
            )
        }
    }
    BigPictureCharts(picture)

    SectionHeader(stringResource(R.string.money_assets)) { actions.onAddKind(EditKind.ASSET) }
    MoneySurface(testTag = "money_assets") {
        if (picture.assets.isEmpty()) {
            AmountRow(title = stringResource(R.string.money_none_yet), amount = null)
        }
        picture.assets.forEach { row ->
            val asset = row.asset
            val liability = asset.kind == AssetKind.LIABILITY
            AmountRow(
                title = asset.name,
                detail = listOfNotNull(
                    stringResource(R.string.money_liability).takeIf { liability },
                    stringResource(R.string.money_asset_updated, asset.lastUpdated.format(dateFormat)),
                ).joinToString(" · "),
                amount = formatMoney(asset.balance),
                amountColor = if (liability) colors.danger else null,
                action = RowAction.Edit { actions.onEdit(MoneyEntry.AssetEntry(asset)) },
                testTag = "money_asset_${asset.id}",
            )
            if (row.stale) {
                Chip(
                    text = stringResource(R.string.money_stale),
                    variant = ChipVariant.Warning,
                    leadingIcon = Icons.Outlined.Schedule,
                    modifier = Modifier
                        .padding(horizontal = Spacing.s16)
                        .testTag("money_stale_${asset.id}"),
                )
            }
        }
    }

    MoneySurface(testTag = "money_cushion") {
        picture.averagesCurrentMonth?.let { current ->
            Column(modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                AveragesWindowPicker(picture.averagesFrom, current, actions.onAveragesFrom)
            }
        }
        val cushion = picture.cushionMonths
        AmountRow(
            title = stringResource(R.string.money_cushion),
            detail = when {
                cushion == null -> stringResource(R.string.money_cushion_empty)
                picture.cushionFromClosed -> averagesWindowCaption(picture.averagesFrom)
                else -> stringResource(R.string.money_cushion_basis)
            },
            amount = cushion?.let { stringResource(R.string.money_cushion_value, DecimalFormat("0.#").format(it)) },
        )
        val saving = picture.monthlySaving
        AmountRow(
            title = stringResource(R.string.money_saving_rate),
            detail = if (saving == null) stringResource(R.string.money_saving_rate_empty) else averagesWindowCaption(picture.averagesFrom),
            amount = saving?.let { formatMoney(it) },
            amountColor = if (saving != null && saving.isNegative()) colors.danger else null,
            testTag = "money_saving_rate",
        )
    }

    SectionTitle(stringResource(R.string.money_tools))
    MoneySurface(testTag = "money_goals") {
        AmountRow(
            title = stringResource(R.string.goals_entry),
            detail = if (picture.goalCount == 0) {
                stringResource(R.string.goals_entry_detail)
            } else {
                stringResource(R.string.goals_entry_count, picture.goalCount)
            },
            amount = null,
            action = RowAction.Open(actions.onGoals),
            leadingIcon = Icons.Outlined.Flag,
            testTag = "money_goals_entry",
        )
        val tithe = picture.titheBalance
        AmountRow(
            title = stringResource(R.string.tithe_entry),
            detail = when {
                tithe == null -> stringResource(R.string.tithe_entry_start)
                tithe > Money.ZERO -> stringResource(R.string.tithe_entry_owed, formatMoney(tithe))
                tithe < Money.ZERO -> stringResource(R.string.tithe_entry_ahead, formatMoney(Money(-tithe.agorot)))
                else -> stringResource(R.string.tithe_entry_settled)
            },
            amount = null,
            action = RowAction.Open(actions.onTithe),
            leadingIcon = Icons.Outlined.VolunteerActivism,
            testTag = "money_tithe_entry",
        )
        AmountRow(
            title = stringResource(R.string.car_entry),
            detail = stringResource(R.string.car_entry_detail),
            amount = null,
            action = RowAction.Open(actions.onCar),
            leadingIcon = Icons.Outlined.DirectionsCar,
            testTag = "money_car_entry",
        )
        val pension = picture.pension
        AmountRow(
            title = stringResource(R.string.pension_entry),
            detail = if (pension == null) {
                stringResource(R.string.pension_entry_start)
            } else {
                stringResource(R.string.pension_entry_detail, formatMoney(pension.value), formatMoney(pension.yearlyFee))
            },
            amount = null,
            action = RowAction.Open(actions.onPension),
            leadingIcon = Icons.Outlined.Savings,
            testTag = "money_pension_entry",
        )
        AmountRow(
            title = stringResource(R.string.subs_entry),
            detail = stringResource(R.string.subs_entry_detail),
            amount = null,
            action = RowAction.Open(actions.onSubscriptions),
            leadingIcon = Icons.Outlined.Autorenew,
            testTag = "money_subs_entry",
        )
    }

    SectionHeader(stringResource(R.string.money_debts)) { actions.onAddKind(EditKind.DEBT) }
    MoneySurface(testTag = "money_debts") {
        if (picture.debts.isEmpty()) {
            AmountRow(title = stringResource(R.string.money_none_yet), amount = null)
        }
        picture.debts.forEach { entry ->
            val debt = entry.value
            AmountRow(
                title = debt.creditor,
                detail = stringResource(
                    R.string.money_debt_detail,
                    formatMoney(debt.balance),
                    formatMoney(debt.monthlyPayment),
                    debt.remainingPayments,
                ),
                amount = null,
                action = RowAction.Edit { actions.onEdit(entry) },
                testTag = "money_debt_${debt.id}",
            )
        }
    }

    SectionHeader(stringResource(R.string.money_savings)) { actions.onAddKind(EditKind.SAVING) }
    MoneySurface(testTag = "money_savings") {
        if (picture.savings.isEmpty()) {
            AmountRow(title = stringResource(R.string.money_none_yet), amount = null)
        }
        picture.savings.forEach { entry ->
            val saving = entry.value
            val maturity = saving.maturity?.let { stringResource(R.string.money_saving_maturity, hebrewMonthYear(it)) }
            AmountRow(
                title = saving.name,
                detail = listOfNotNull(
                    stringResource(R.string.money_saving_detail, formatMoney(saving.monthlyDeposit), formatMoney(saving.accumulated)),
                    maturity,
                ).joinToString(" · "),
                amount = null,
                action = RowAction.Edit { actions.onEdit(entry) },
                testTag = "money_saving_${saving.id}",
            )
        }
    }
}

@Composable
internal fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(title, modifier = Modifier.weight(1f))
        TextButton(onClick = onAdd) {
            Text(text = stringResource(R.string.money_add), style = AppType.caption)
        }
    }
}
