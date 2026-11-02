package com.menash.myself.ui.money

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.key
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.LoadingState
import com.menash.myself.core.designsystem.component.ErrorState
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Motion
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.SubscriptionRow
import com.menash.myself.feature.money.SubscriptionsUi
import com.menash.myself.feature.money.UnlistedCharge
import com.menash.myself.feature.money.domain.Currency
import com.menash.myself.feature.money.domain.Frequency
import com.menash.myself.feature.money.domain.Subscription
import java.time.format.DateTimeFormatter

data class SubscriptionsActions(
    val onEdit: (Subscription) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onAddFrom: (UnlistedCharge) -> Unit = {},
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy")

/** "מנויים": what each subscription costs a month, what the card really charged, and charges not listed yet. */
@Composable
fun SubscriptionsContent(ui: SubscriptionsUi, actions: SubscriptionsActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("subs_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        MoneySubScreen(title = stringResource(R.string.subs_title), subtitle = stringResource(R.string.subs_subtitle), onBack = actions.onBack)
        val view = ui.view
        Crossfade(targetState = when { ui.loadError -> 0; view == null -> 1; else -> 2 },
            animationSpec = tween(Motion.duration(Motion.medium), easing = Motion.easing), label = "subscriptions_load") { state ->
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s16)) {
        if (state == 0) { ErrorState(message = stringResource(R.string.money_load_error), onRetry = actions.onRetry, retryLabel = stringResource(R.string.money_load_retry)) }
        else if (state == 1 || view == null) { LoadingState(testTag = "subs_loading") }
        else {
        val colors = MaterialTheme.appColors
        MoneySurface(testTag = "subs_total") {
            AmountRow(
                title = stringResource(R.string.subs_total),
                detail = view.monthlyTotal?.let { stringResource(R.string.subs_total_year, formatMoney(Money(it.agorot * 12))) }
                    ?: stringResource(R.string.subs_rate_missing),
                amount = view.monthlyTotal?.let { formatMoney(it) },
                titleStyle = AppType.title,
            )
        }
        SectionHeader(stringResource(R.string.subs_listed), onAdd = actions.onAdd)
        MoneySurface(testTag = "subs_rows") {
            if (view.rows.isEmpty()) AmountRow(title = stringResource(R.string.money_none_yet), amount = null)
            view.rows.forEach { row -> SubscriptionLine(row, actions) }
        }
        if (view.unlisted.isNotEmpty()) {
            SectionTitle(stringResource(R.string.subs_unlisted))
            Text(stringResource(R.string.subs_unlisted_detail), style = AppType.caption, color = colors.textSecondary)
            MoneySurface(testTag = "subs_unlisted") {
                view.unlisted.forEach { charge ->
                    AmountRow(
                        title = charge.name,
                        detail = stringResource(
                            R.string.subs_unlisted_line, hebrewMonthName(charge.last.month.month), charge.months,
                        ),
                        amount = formatMoney(charge.last.amount),
                        onClick = { actions.onAddFrom(charge) },
                        onClickLabel = stringResource(R.string.subs_add_from),
                        testTag = "subs_unlisted_${charge.merchantKey}",
                    )
                }
            }
        }
        }
        }
        }
    }
}

@Composable
private fun SubscriptionLine(row: SubscriptionRow, actions: SubscriptionsActions) {
    val sub = row.subscription
    val parts = buildList {
        if (sub.currency == Currency.USD) add(stringResource(R.string.subs_usd))
        row.renewsOn?.let { add(stringResource(R.string.subs_renews, it.format(dateFormat))) }
        if (sub.frequency == Frequency.MONTHLY && row.renewsOn == null) add(stringResource(R.string.subs_monthly))
        row.lastCharge?.let { charge ->
            val planned = row.planned
            val gap = planned?.let { Money(charge.amount.agorot - it.agorot) }
            add(
                if (gap != null && gap > Money.ZERO) {
                    stringResource(R.string.subs_charged_more, formatMoney(charge.amount), formatMoney(gap))
                } else {
                    stringResource(R.string.subs_charged, formatMoney(charge.amount), hebrewMonthName(charge.month.month))
                }
            )
        }
    }
    AmountRow(
        title = sub.name,
        detail = parts.joinToString(" · "),
        amount = row.monthly?.let { formatMoney(it) },
        onClick = { actions.onEdit(sub) },
        testTag = "subs_row_${sub.id}",
    )
}
