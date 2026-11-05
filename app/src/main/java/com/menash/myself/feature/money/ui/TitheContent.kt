package com.menash.myself.feature.money.ui

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
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.DonationMatch
import com.menash.myself.feature.money.TitheUi
import com.menash.myself.feature.money.TitheView
import com.menash.myself.feature.money.domain.Donation
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class TitheActions(
    val onStart: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onAddDonation: () -> Unit = {},
    val onDonation: (Donation) -> Unit = {},
    val onMatch: (DonationMatch) -> Unit = {},
    /** Opens the month's imported lines to link income to its source. */
    val onLink: (YearMonth) -> Unit = {},
    val onClose: () -> Unit = {},
)

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy")

/** "מעשרות" full screen: the balance and where it comes from, then each month, then the donations. */
@Composable
fun TitheContent(ui: TitheUi, actions: TitheActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("tithe_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        MoneySubScreen(title = stringResource(R.string.tithe_title), subtitle = stringResource(R.string.tithe_subtitle), onBack = actions.onClose)
        val view = ui.view
        if (view == null) {
            MoneySurface(testTag = "tithe_start") {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s8),
                ) {
                    Text(stringResource(R.string.tithe_start_title), style = AppType.title, color = MaterialTheme.appColors.textPrimary)
                    Text(stringResource(R.string.tithe_start_detail), style = AppType.body, color = MaterialTheme.appColors.textSecondary)
                }
            }
            PrimaryButton(
                text = stringResource(R.string.tithe_start),
                onClick = actions.onStart,
                modifier = Modifier.fillMaxWidth().testTag("tithe_start_button"),
            )
        } else {
            Balance(view)
            Breakdown(view)
            SectionTitle(stringResource(R.string.tithe_months))
            view.months.forEach { TitheMonthCard(it, actions.onLink) }
            view.possibleDuplicates.forEach { match ->
                MoneySurface {
                    AmountRow(title = stringResource(R.string.tithe_possible_duplicate),
                        detail = stringResource(R.string.tithe_duplicate_detail, match.donation.date.format(dateFormat), match.line.date.format(dateFormat)),
                        amount = formatMoney(match.donation.amount))
                    TextButton(onClick = { actions.onMatch(match) }) { Text(stringResource(R.string.tithe_same_donation)) }
                }
            }
            Donations(view, actions)
            TextButton(onClick = actions.onSettings, modifier = Modifier.testTag("tithe_settings")) {
                Text(stringResource(R.string.tithe_settings), style = AppType.body)
            }
        }
    }
}

@Composable
private fun Balance(view: TitheView) {
    val colors = MaterialTheme.appColors
    val balance = view.balance
    MoneySurface(testTag = "tithe_balance") {
        Column(modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8)) {
            Text(
                text = stringResource(
                    when {
                        balance > Money.ZERO -> R.string.tithe_owed
                        balance < Money.ZERO -> R.string.tithe_ahead
                        else -> R.string.tithe_settled
                    }
                ),
                style = AppType.caption,
                color = colors.textSecondary,
            )
            Text(
                text = formatMoney(Money(Math.abs(balance.agorot))),
                style = AppType.number,
                color = if (balance > Money.ZERO) colors.warning else colors.success,
            )
            Text(
                text = stringResource(R.string.tithe_since, hebrewMonthYear(view.effectiveStart)),
                style = AppType.caption,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun Breakdown(view: TitheView) {
    MoneySurface(testTag = "tithe_breakdown") {
        if (!view.settings.opening.isZero()) {
            AmountRow(title = stringResource(R.string.tithe_opening), amount = formatMoney(view.settings.opening))
        }
        AmountRow(title = stringResource(R.string.tithe_from_income), amount = formatMoney(view.fromIncome))
        view.portfolio?.let {
            AmountRow(
                title = stringResource(R.string.tithe_from_portfolio),
                detail = stringResource(R.string.tithe_portfolio_detail, formatMoney(it.value), formatMoney(it.deposits)),
                amount = formatMoney(it.owed),
            )
        }
        AmountRow(title = stringResource(R.string.tithe_donated), amount = formatMoney(Money(-view.donated.agorot)))
        if (view.unsubmitted.count > 0) {
            AmountRow(
                title = stringResource(R.string.tithe_unsubmitted),
                detail = stringResource(R.string.tithe_unsubmitted_detail, view.unsubmitted.count),
                amount = formatMoney(view.unsubmitted.total),
                testTag = "tithe_unsubmitted",
            )
        }
    }
    if (view.countedIncomes == 0) {
        Text(
            text = stringResource(R.string.tithe_no_rated),
            style = AppType.body,
            color = MaterialTheme.appColors.warning,
            modifier = Modifier.testTag("tithe_no_rated"),
        )
    }
}

@Composable
private fun Donations(view: TitheView, actions: TitheActions) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(stringResource(R.string.tithe_donations), modifier = Modifier.weight(1f))
        TextButton(onClick = actions.onAddDonation, modifier = Modifier.testTag("tithe_add_donation")) {
            Text(stringResource(R.string.tithe_add), style = AppType.caption)
        }
    }
    MoneySurface(testTag = "tithe_donations") {
        if (view.donations.isEmpty()) {
            AmountRow(title = stringResource(R.string.tithe_no_donations), amount = null)
        }
        view.donations.forEach { donation ->
            AmountRow(
                title = donation.organisation.ifEmpty { stringResource(R.string.tithe_no_organisation) },
                detail = listOfNotNull(
                    donation.date.format(dateFormat),
                    stringResource(R.string.tithe_submitted).takeIf { donation.submittedForTaxRefund },
                ).joinToString(" · "),
                amount = formatMoney(donation.amount),
                onClick = { actions.onDonation(donation) },
                testTag = "tithe_donation_${donation.id}",
            )
        }
    }
}
