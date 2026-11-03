package com.menash.myself.ui.money

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.feature.money.importer.LineFilter
import com.menash.myself.core.designsystem.component.Chip
import com.menash.myself.core.designsystem.component.ChipVariant
import com.menash.myself.core.designsystem.component.EmptyState
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.LoadingState
import com.menash.myself.core.designsystem.component.PillTabs
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Motion
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.MonthRow
import com.menash.myself.feature.money.MonthRowKind
import com.menash.myself.feature.money.MonthView
import com.menash.myself.feature.money.MoneyMonth
import com.menash.myself.feature.money.MonthStatus
import com.menash.myself.feature.money.MoneyScreenState
import com.menash.myself.feature.money.data.MoneyEntry
import java.time.YearMonth
import java.time.format.DateTimeFormatter

const val MoneyTabMonth = 0
const val MoneyTabBigPicture = 1

/** Callbacks of the money tab. Everything shown comes from [MoneyScreenState]; nothing is computed here. */
data class MoneyActions(
    val onSelectTab: (Int) -> Unit = {},
    val onPreviousMonth: () -> Unit = {},
    val onNextMonth: () -> Unit = {},
    val onToggleRow: (MonthRowKind) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onStart: () -> Unit = {},
    val onEdit: (MoneyEntry) -> Unit = {},
    val onAddKind: (com.menash.myself.feature.money.EditKind) -> Unit = {},
    val onTransactions: (LineFilter) -> Unit = {},
    val onGoals: () -> Unit = {},
    val onTithe: () -> Unit = {},
    val onCloseMonth: () -> Unit = {},
    val onCar: () -> Unit = {},
    val onPension: () -> Unit = {},
    val onSubscriptions: () -> Unit = {},
    val onPeriod: () -> Unit = {},
    val onMonthSummary: (YearMonth) -> Unit = {},
    val onImport: () -> Unit = {},
)

@Composable
fun MoneyContent(
    state: MoneyScreenState,
    selectedTab: Int,
    expanded: Set<MonthRowKind>,
    actions: MoneyActions,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("money_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        ScreenHeader(title = stringResource(R.string.nav_money))
        PillTabs(
            options = listOf(stringResource(R.string.money_tab_month), stringResource(R.string.money_tab_big_picture)),
            selectedIndex = selectedTab,
            onSelect = actions.onSelectTab,
        )
        val money = state.money
        when {
            money == null -> LoadingState(modifier = Modifier.fillMaxWidth())
            !state.data.householdReady -> EmptyState(
                title = stringResource(R.string.money_no_household),
                icon = Icons.Outlined.AccountBalanceWallet,
                testTag = "money_no_household",
            )
            else -> {
                val duration = Motion.duration(Motion.medium)
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { Motion.fadeIn(duration) togetherWith Motion.fadeOut(duration) },
                    label = "money_tabs",
                ) { tab ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s16)) {
                        if (tab == MoneyTabBigPicture) BigPictureContent(money.bigPicture, actions)
                        else MonthContent(money, expanded, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthContent(
    money: MoneyMonth,
    expanded: Set<MonthRowKind>,
    actions: MoneyActions,
) {
    val view = money.view
    val colors = MaterialTheme.appColors
    val status = (view as? MonthView.Planned)?.status
    MonthSwitcher(money, closed = status is MonthStatus.Closed, onPrevious = actions.onPreviousMonth, onNext = actions.onNextMonth)
    MoneySurface(testTag = "money_import_row") {
        AmountRow(
            title = stringResource(R.string.import_entry),
            amount = null,
            detail = stringResource(R.string.import_entry_detail),
            onClick = actions.onImport,
            testTag = "money_import_top",
        )
    }
    when (view) {
        is MonthView.Empty -> {
            if (view.usdRateMissing) UsdMissingChip()
            EmptyState(
                title = stringResource(R.string.money_empty_title),
                description = stringResource(R.string.money_empty_description),
                icon = Icons.Outlined.AccountBalanceWallet,
                actionLabel = stringResource(R.string.money_empty_action),
                onActionClick = actions.onStart,
                testTag = "money_empty",
            )
        }
        is MonthView.Planned -> {
            val surplus = view.plan.expectedSurplus
            MoneySurface(testTag = "money_surplus") {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s8),
                ) {
                    if (status != null) MonthStatusHeadline(status, onAddCurrent = { actions.onAddKind(com.menash.myself.feature.money.EditKind.VARIABLE) }, onTransactions = actions.onTransactions) else Text(
                        text = formatMoney(surplus),
                        style = AppType.number,
                        color = if (surplus.isNegative()) colors.danger else colors.textPrimary,
                        modifier = Modifier.testTag("money_surplus_value"),
                    )
                    StackedBar(view.bar)
                    BarLegend(view.bar)
                    if (view.usdRateMissing) UsdMissingChip()
                }
            }
            MoneySurface(testTag = "money_rows") {
                view.rows.forEach { row ->
                    PlanRow(row, row.kind in expanded, actions)
                }
                view.unassigned?.let { unassigned ->
                    AmountRow(
                        title = stringResource(R.string.import_unassigned),
                        amount = formatMoney(unassigned),
                        amountColor = colors.textSecondary,
                        onClick = { actions.onTransactions(LineFilter.UNASSIGNED) },
                        testTag = "money_unassigned",
                    )
                }
                if (view.importedLines > 0) {
                    AmountRow(
                        title = stringResource(R.string.tx_entry),
                        amount = null,
                        detail = stringResource(R.string.tx_entry_detail, view.importedLines),
                        onClick = { actions.onTransactions(LineFilter.ALL) },
                        testTag = "money_transactions",
                    )
                }
                if (status is MonthStatus.Closed) {
                    AmountRow(
                        title = stringResource(R.string.summary_entry),
                        amount = null,
                        detail = stringResource(R.string.summary_entry_detail),
                        onClick = { actions.onMonthSummary(money.month) },
                        testTag = "money_month_summary",
                    )
                }
                AmountRow(
                    title = stringResource(R.string.close_entry),
                    amount = null,
                    detail = stringResource(R.string.close_entry_detail),
                    onClick = actions.onCloseMonth,
                    testTag = "money_close_month",
                )
                AmountRow(
                    title = if (money.current.monthValue == 1) stringResource(R.string.period_year_entry, money.current.year - 1)
                        else stringResource(R.string.period_entry),
                    amount = null,
                    detail = stringResource(R.string.period_entry_detail),
                    onClick = actions.onPeriod,
                    testTag = "money_period",
                )
            }
            if (view.cardCharges.isNotEmpty()) {
                SectionTitle(stringResource(R.string.money_card_charges))
                MoneySurface(testTag = "money_card_charges") {
                    val dayFormat = DateTimeFormatter.ofPattern("d.M")
                    view.cardCharges.forEach { charge ->
                        AmountRow(
                            title = charge.card.name,
                            detail = stringResource(R.string.money_card_charge_date, charge.date.format(dayFormat)),
                            amount = formatMoney(charge.amount),
                            onClick = { actions.onEdit(MoneyEntry.CardEntry(charge.card)) },
                        )
                    }
                }
            }
            PrimaryButton(
                text = stringResource(R.string.money_add),
                onClick = actions.onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("money_add"),
            )
        }
    }
}

@Composable
private fun PlanRow(row: MonthRow, isExpanded: Boolean, actions: MoneyActions) {
    val colors = MaterialTheme.appColors
    val actual = row.actual
    val duration = Motion.duration(Motion.medium)
    val rotation = animateFloatAsState(if (isExpanded) 180f else 0f, tween(duration, easing = Motion.easing), label = "expand")
    val state = stringResource(if (isExpanded) R.string.money_expanded else R.string.money_collapsed)
    AmountRow(
        title = rowLabel(row.kind),
        modifier = Modifier.semantics { stateDescription = state },
        trailing = { Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.rotate(rotation.value), tint = colors.textSecondary) },
        amount = formatMoney(row.total),
        detail = actual?.let { stringResource(R.string.money_actual, formatMoney(it)) },
        titleStyle = AppType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
        onClick = { actions.onToggleRow(row.kind) },
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
                onClick = { actions.onEdit(item.entry) },
                modifier = Modifier.padding(start = Spacing.s16),
                testTag = "money_item_${item.entry.id}",
            )
        } }
    }
}

@Composable
fun UsdMissingChip() {
    Chip(
        text = stringResource(R.string.money_usd_missing),
        variant = ChipVariant.Warning,
        leadingIcon = Icons.Outlined.Warning,
        modifier = Modifier.testTag("money_usd_missing"),
    )
}
