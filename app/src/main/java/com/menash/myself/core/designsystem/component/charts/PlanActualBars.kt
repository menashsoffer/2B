package com.menash.myself.core.designsystem.component.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

/**
 * Horizontal plan-vs-actual bars, one per item, sharing one scale so lengths compare. The fill is the
 * actual; the part above plan is drawn in danger; a thin mark shows where the plan ends. Each row is at
 * least 48dp tall and, with [onClick], one tap target.
 */
@Composable
fun PlanActualBars(
    items: List<PlanActualItem>,
    format: (Long) -> String,
    modifier: Modifier = Modifier,
    onClick: ((PlanActualItem) -> Unit)? = null,
    testTag: String = "chart_plan_actual",
) {
    if (items.isEmpty()) return
    val scale = items.maxOf { maxOf(it.actual, it.plan ?: 0L) }.coerceAtLeast(1L)
    Column(modifier = modifier.fillMaxWidth().testTag(testTag), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        items.forEach { item -> PlanActualRow(item, scale, format, onClick, "${testTag}_${item.key}") }
    }
}

@Composable
private fun PlanActualRow(item: PlanActualItem, scale: Long, format: (Long) -> String, onClick: ((PlanActualItem) -> Unit)?, tag: String) {
    val colors = MaterialTheme.appColors
    val plan = item.plan
    val figure = when {
        plan == null -> stringResource(R.string.chart_no_plan, format(item.actual))
        item.over -> stringResource(R.string.chart_over_plan, stringResource(R.string.chart_of_plan, format(item.actual), format(plan)), format(item.actual - plan))
        else -> stringResource(R.string.chart_of_plan, format(item.actual), format(plan))
    }
    val description = stringResource(R.string.chart_item_description, item.label, figure)
    val click = onClick?.let { { it(item) } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (click != null) Modifier.clickable(onClick = click) else Modifier)
            .clearAndSetSemantics {
                contentDescription = description
                if (click != null) role = Role.Button
            }
            .padding(vertical = Spacing.s4)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(item.label, style = AppType.caption, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(figure, style = AppType.caption, color = if (item.over) colors.danger else colors.textSecondary, maxLines = 1)
        }
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().height(Spacing.s8 + Spacing.s4).clip(AppShapes.pill).background(colors.accentSoft.copy(alpha = 0.45f)),
        ) {
            val full = maxWidth
            val within = minOf(item.actual, plan ?: item.actual)
            val withinWidth = full * (within.toFloat() / scale)
            Box(Modifier.width(withinWidth).fillMaxHeight().background(item.tone.color()))
            if (item.over && plan != null) {
                Box(
                    Modifier.offset(x = withinWidth).width(full * ((item.actual - plan).toFloat() / scale)).fillMaxHeight().background(colors.danger),
                )
            }
            if (plan != null && plan > 0) {
                Box(Modifier.offset(x = (full * (plan.toFloat() / scale)) - 1.dp).width(2.dp).fillMaxHeight().background(colors.textPrimary))
            }
        }
    }
}
