package com.menash.myself.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.MoneyFormatter
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.designsystem.theme.tabular
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.BarSegment
import com.menash.myself.feature.money.MonthRowKind
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val Hebrew: Locale = Locale.forLanguageTag("he")

fun hebrewMonthName(month: Month): String = month.getDisplayName(TextStyle.FULL_STANDALONE, Hebrew)

fun hebrewMonthYear(month: YearMonth): String = "${hebrewMonthName(month.month)} ${month.year}"

fun formatMoney(money: Money): String = MoneyFormatter.format(money)

/** Card surface of the new screens: radius 20, card colour, one hairline border. */
@Composable
fun MoneySurface(
    modifier: Modifier = Modifier,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, AppShapes.card)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(vertical = Spacing.s8),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppType.caption,
        color = MaterialTheme.appColors.textSecondary,
        modifier = modifier.padding(start = Spacing.s8, top = Spacing.s8),
    )
}

/** Name on the start side, amount on the end side, optional detail line. 48dp minimum touch height. */
@Composable
fun AmountRow(
    title: String,
    amount: String?,
    modifier: Modifier = Modifier,
    detail: String? = null,
    titleStyle: androidx.compose.ui.text.TextStyle = AppType.body,
    amountColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    testTag: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = MaterialTheme.appColors
    val click = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            role = Role.Button,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .then(click)
            .semantics(mergeDescendants = true) {}
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = Spacing.s16, vertical = Spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = titleStyle,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail != null) {
                Text(text = detail, style = AppType.caption, color = colors.textSecondary)
            }
        }
        if (amount != null) {
            Text(
                text = amount,
                style = titleStyle.tabular(),
                color = amountColor ?: colors.textPrimary,
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun rowLabel(kind: MonthRowKind?): String = stringResource(
    when (kind) {
        MonthRowKind.INCOME -> R.string.money_row_income
        MonthRowKind.FIXED -> R.string.money_row_fixed
        MonthRowKind.CURRENT -> R.string.money_row_current
        MonthRowKind.PERIODIC -> R.string.money_row_periodic
        MonthRowKind.INSTALLMENTS -> R.string.money_row_installments
        MonthRowKind.DEBTS -> R.string.money_row_debts
        MonthRowKind.SAVINGS -> R.string.money_row_savings
        null -> R.string.money_row_free
    }
)

@Composable
private fun segmentColor(kind: MonthRowKind?): Color {
    val colors = MaterialTheme.appColors
    return when (kind) {
        MonthRowKind.FIXED -> colors.accentStrong
        MonthRowKind.CURRENT -> colors.accent
        MonthRowKind.PERIODIC -> colors.warning
        MonthRowKind.INSTALLMENTS -> colors.textSecondary
        MonthRowKind.DEBTS -> colors.danger
        MonthRowKind.SAVINGS -> colors.success
        MonthRowKind.INCOME, null -> colors.accentSoft
    }
}

/** One stacked bar. Widths are the segments' share of the bar, for drawing only. */
@Composable
fun StackedBar(segments: List<BarSegment>, modifier: Modifier = Modifier) {
    if (segments.isEmpty()) return
    val description = stringResource(R.string.money_bar_description)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.s8 + Spacing.s4)
            .clip(AppShapes.pill)
            .semantics { contentDescription = description }
            .testTag("money_bar"),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segments.forEach { segment ->
            Box(
                modifier = Modifier
                    .weight(segment.amount.agorot.toFloat())
                    .fillMaxHeight()
                    .background(segmentColor(segment.kind)),
            )
        }
    }
}

/** Colour keys under the bar, two per line. */
@Composable
fun BarLegend(segments: List<BarSegment>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        segments.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s16)) {
                pair.forEach { segment ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Spacing.s8)
                                .clip(AppShapes.pill)
                                .background(segmentColor(segment.kind)),
                        )
                        Text(
                            text = rowLabel(segment.kind),
                            style = AppType.caption,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (pair.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }
}
