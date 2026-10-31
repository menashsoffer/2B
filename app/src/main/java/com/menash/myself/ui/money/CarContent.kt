package com.menash.myself.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.CarCompareView
import com.menash.myself.feature.money.CarCurrent
import com.menash.myself.feature.money.CarOption
import com.menash.myself.feature.money.CarUi
import com.menash.myself.feature.money.kind
import com.menash.myself.feature.money.domain.NotComparable
import com.menash.myself.feature.money.domain.NotComparableReason
import com.menash.myself.feature.money.domain.OptionCost

data class CarActions(
    val onCategories: () -> Unit = {},
    val onCompare: () -> Unit = {},
    val onClose: () -> Unit = {},
)

@Composable
internal fun carOptionLabel(option: CarOption): String = stringResource(
    when (option) {
        CarOption.CASH -> R.string.car_opt_cash
        CarOption.BANK_LOAN -> R.string.car_opt_loan
        CarOption.DEALER -> R.string.car_opt_dealer
        CarOption.LEASING -> R.string.car_opt_lease
    }
)

/** "רכב" full screen: today's real cost from the files, then the new-car options cheapest first. */
@Composable
fun CarContent(ui: CarUi, actions: CarActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("car_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        ScreenHeader(title = stringResource(R.string.car_title), subtitle = stringResource(R.string.car_subtitle))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.car_now), modifier = Modifier.weight(1f))
            TextButton(onClick = actions.onCategories, modifier = Modifier.testTag("car_categories")) {
                Text(stringResource(R.string.car_categories), style = AppType.caption)
            }
        }
        ui.current?.let { Current(it) }
        SectionTitle(stringResource(R.string.car_new))
        val compare = ui.compare
        if (compare == null) {
            Text(stringResource(R.string.car_new_empty), style = AppType.body, color = MaterialTheme.appColors.textSecondary)
        } else {
            Compare(compare)
        }
        PrimaryButton(
            text = stringResource(if (compare == null) R.string.car_new_fill else R.string.car_new_edit),
            onClick = actions.onCompare,
            modifier = Modifier.fillMaxWidth().testTag("car_compare"),
        )
        TextButton(onClick = actions.onClose, modifier = Modifier.testTag("car_back")) {
            Text(stringResource(R.string.car_back), style = AppType.body)
        }
    }
}

@Composable
private fun Current(current: CarCurrent) {
    val colors = MaterialTheme.appColors
    val yearly = current.yearly
    MoneySurface(testTag = "car_now") {
        if (yearly == null) {
            AmountRow(title = stringResource(R.string.car_no_lines), amount = null)
            return@MoneySurface
        }
        Column(modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8)) {
            Text(stringResource(R.string.car_yearly, hebrewMonthYear(current.endMonth)), style = AppType.caption, color = colors.textSecondary)
            Text(formatMoney(yearly), style = AppType.number, color = colors.textPrimary)
        }
        current.monthly?.let { AmountRow(title = stringResource(R.string.car_monthly), amount = formatMoney(it)) }
        current.byCategory.forEach { AmountRow(title = it.category.name, amount = formatMoney(it.yearly)) }
        if (current.coveredMonths < 12) {
            Text(
                text = stringResource(R.string.car_partial, current.coveredMonths),
                style = AppType.caption,
                color = colors.warning,
                modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8).testTag("car_partial"),
            )
        }
    }
}

@Composable
private fun Compare(view: CarCompareView) {
    val colors = MaterialTheme.appColors
    Text(
        text = (if (view.horizonFromLease) stringResource(R.string.car_horizon_lease, view.horizonMonths) else stringResource(R.string.car_horizon, view.horizonMonths / 12)) +
            " · " + stringResource(R.string.car_running_basis),
        style = AppType.caption,
        color = colors.textSecondary,
    )
    MoneySurface(testTag = "car_options") {
        view.rows.forEach { row ->
            val label = carOptionLabel(row.option.kind())
            when (row) {
                is OptionCost -> {
                    val above = view.aboveCheapest(row)
                    AmountRow(
                        title = label,
                        detail = listOf(
                            stringResource(R.string.car_row_detail, formatMoney(row.netTotal), formatMoney(row.financeCost)),
                            stringResource(R.string.car_row_payment, formatMoney(row.upfront), formatMoney(row.monthlyPayment)),
                        ).joinToString("\n"),
                        amount = formatMoney(row.netMonthly) + " " + stringResource(R.string.car_per_month),
                        testTag = "car_option_${row.option.kind().name.lowercase()}",
                    )
                    Chip(
                        text = if (above == null) stringResource(R.string.car_cheapest) else stringResource(R.string.car_more_than, formatMoney(above)),
                        variant = if (above == null) ChipVariant.Success else ChipVariant.Default,
                        modifier = Modifier.padding(horizontal = Spacing.s16).padding(bottom = Spacing.s8),
                    )
                }
                is NotComparable -> AmountRow(
                    title = label,
                    detail = stringResource(
                        if (row.reason == NotComparableReason.LEASE_SHORTER_THAN_HORIZON) R.string.car_lease_short else R.string.car_lease_km
                    ),
                    amount = null,
                )
            }
        }
    }
    if (view.incomplete.isNotEmpty()) {
        Text(
            text = stringResource(R.string.car_incomplete, view.incomplete.map { carOptionLabel(it) }.joinToString(", ")),
            style = AppType.caption,
            color = colors.warning,
        )
    }
    if (view.invalid.isNotEmpty()) {
        Text(
            text = stringResource(R.string.car_invalid, view.invalid.map { carOptionLabel(it) }.joinToString(", ")),
            style = AppType.caption,
            color = colors.danger,
        )
    }
}
