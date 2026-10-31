package com.menash.myself.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
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
import com.menash.myself.feature.money.FundRow
import com.menash.myself.feature.money.PensionUi
import com.menash.myself.feature.money.domain.FundKind
import com.menash.myself.feature.money.domain.PensionFund
import com.menash.myself.feature.money.domain.PensionSnapshot
import java.time.format.DateTimeFormatter

data class PensionActions(
    val onAdd: () -> Unit = {},
    val onFund: (PensionFund) -> Unit = {},
    val onClose: () -> Unit = {},
)

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy")

@Composable
internal fun fundKindLabel(kind: FundKind): String = stringResource(
    when (kind) {
        FundKind.PENSION -> R.string.pension_kind_pension
        FundKind.HISHTALMUT -> R.string.pension_kind_hishtalmut
        FundKind.GEMEL -> R.string.pension_kind_gemel
    }
)

/** "פנסיה וחיסכון" full screen: totals, then each fund with its fee in shekels and its flags. */
@Composable
fun PensionContent(ui: PensionUi, actions: PensionActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("pension_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        ScreenHeader(title = stringResource(R.string.pension_title), subtitle = stringResource(R.string.pension_subtitle))
        val total = ui.view.total
        if (total == null) {
            MoneySurface(testTag = "pension_empty") {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s8),
                ) {
                    Text(stringResource(R.string.pension_empty_title), style = AppType.title, color = MaterialTheme.appColors.textPrimary)
                    Text(stringResource(R.string.pension_empty_detail), style = AppType.body, color = MaterialTheme.appColors.textSecondary)
                }
            }
            PrimaryButton(
                text = stringResource(R.string.pension_add),
                onClick = actions.onAdd,
                modifier = Modifier.fillMaxWidth().testTag("pension_add_first"),
            )
        } else {
            Totals(total)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(stringResource(R.string.pension_funds), modifier = Modifier.weight(1f))
                TextButton(onClick = actions.onAdd, modifier = Modifier.testTag("pension_add")) {
                    Text(stringResource(R.string.pension_add), style = AppType.caption)
                }
            }
            MoneySurface(testTag = "pension_funds") {
                ui.view.rows.forEach { FundItem(it, actions) }
            }
        }
        TextButton(onClick = actions.onClose, modifier = Modifier.testTag("pension_back")) {
            Text(stringResource(R.string.pension_back), style = AppType.body)
        }
    }
}

@Composable
private fun Totals(total: PensionSnapshot) {
    val colors = MaterialTheme.appColors
    MoneySurface(testTag = "pension_totals") {
        Column(modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8)) {
            Text(stringResource(R.string.pension_total_value), style = AppType.caption, color = colors.textSecondary)
            Text(formatMoney(total.value), style = AppType.number, color = colors.textPrimary)
        }
        AmountRow(title = stringResource(R.string.pension_total_deposits), amount = formatMoney(total.deposits))
        AmountRow(
            title = stringResource(R.string.pension_total_profit),
            detail = stringResource(R.string.pension_total_profit_detail),
            amount = formatMoney(total.profit),
            amountColor = if (total.profit.isNegative()) colors.danger else null,
        )
        AmountRow(
            title = stringResource(R.string.pension_total_fee),
            detail = stringResource(R.string.pension_total_fee_detail),
            amount = formatMoney(total.yearlyFee),
            testTag = "pension_total_fee",
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FundItem(row: FundRow, actions: PensionActions) {
    val fund = row.fund
    val snapshot = row.snapshot
    AmountRow(
        title = "${fundKindLabel(fund.kind)} · ${fund.provider}",
        detail = listOf(
            stringResource(R.string.pension_fund_detail, formatMoney(snapshot.profit), formatMoney(snapshot.yearlyFee)),
            stringResource(R.string.pension_fund_updated, fund.lastUpdated.format(dateFormat)),
        ).joinToString(" · "),
        amount = formatMoney(fund.value),
        onClick = { actions.onFund(fund) },
        testTag = "pension_fund_${fund.id}",
    )
    val liquidOn = snapshot.liquidOn
    if (!row.priciest && !row.stale && liquidOn == null) return
    FlowRow(
        modifier = Modifier.padding(horizontal = Spacing.s16).padding(bottom = Spacing.s8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
        verticalArrangement = Arrangement.spacedBy(Spacing.s8),
    ) {
        if (row.priciest) {
            Chip(text = stringResource(R.string.pension_priciest), variant = ChipVariant.Warning, modifier = Modifier.testTag("pension_priciest"))
        }
        if (row.stale) {
            Chip(
                text = stringResource(R.string.pension_stale),
                variant = ChipVariant.Warning,
                leadingIcon = Icons.Outlined.Schedule,
                modifier = Modifier.testTag("pension_stale_${fund.id}"),
            )
        }
        if (liquidOn != null) {
            Chip(
                text = if (row.liquid) stringResource(R.string.pension_liquid) else stringResource(R.string.pension_liquid_on, liquidOn.format(dateFormat)),
                variant = if (row.liquid) ChipVariant.Success else ChipVariant.Default,
            )
        }
    }
}
