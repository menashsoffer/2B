package com.menash.myself.core.designsystem.component.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

/**
 * A line over labelled points, first point at the start side (right in Hebrew). A null value breaks the
 * line: unknown is never drawn as zero. The first and last known values are printed under the chart.
 */
@Composable
fun LineChart(
    points: List<LinePoint>,
    format: (Long) -> String,
    modifier: Modifier = Modifier,
    tone: ChartTone = ChartTone.Accent,
    height: Dp = 120.dp,
    testTag: String = "chart_line",
) {
    val known = points.mapNotNull { it.value }
    if (known.size < 2) return
    val colors = MaterialTheme.appColors
    val lineColor = tone.color()
    val gridColor = colors.cardBorder
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val low = known.min()
    val high = known.max()
    val span = (high - low).coerceAtLeast(1L).toFloat()
    val description = points.joinToString(" · ") { p -> "${p.label} ${p.value?.let(format) ?: "—"}" }
    Column(modifier = modifier.fillMaxWidth().testTag(testTag), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height).clearAndSetSemantics { contentDescription = description }) {
            val pad = 6.dp.toPx()
            val step = if (points.size > 1) (size.width - 2 * pad) / (points.size - 1) else 0f
            fun x(i: Int) = if (rtl) size.width - pad - i * step else pad + i * step
            fun y(v: Long) = pad + (size.height - 2 * pad) * (1f - (v - low) / span)
            drawLine(gridColor, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), strokeWidth = 1f)
            var previous: Offset? = null
            points.forEachIndexed { i, p ->
                val v = p.value
                if (v == null) {
                    previous = null
                    return@forEachIndexed
                }
                val here = Offset(x(i), y(v))
                previous?.let { drawLine(lineColor, it, here, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round) }
                drawCircle(lineColor, radius = 4.dp.toPx(), center = here)
                drawCircle(colors.card, radius = 2.dp.toPx(), center = here, style = Stroke(width = 1.dp.toPx()))
                previous = here
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(points.first().label, style = AppType.caption, color = colors.textSecondary, modifier = Modifier.weight(1f))
            Text(points.last().label, style = AppType.caption, color = colors.textSecondary)
        }
    }
}

/** One horizontal 100% bar split by share, with a legend that prints each part's value. No pie charts. */
@Composable
fun ShareBar(
    segments: List<ShareSegment>,
    format: (Long) -> String,
    modifier: Modifier = Modifier,
    testTag: String = "chart_share",
) {
    val shown = segments.filter { it.value > 0 }
    if (shown.isEmpty()) return
    val colors = MaterialTheme.appColors
    val description = shown.joinToString(" · ") { "${it.label} ${format(it.value)}" }
    Column(modifier = modifier.fillMaxWidth().testTag(testTag), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.s8 + Spacing.s4)
                .clip(AppShapes.pill)
                .clearAndSetSemantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            shown.forEach { Box(Modifier.weight(it.value.toFloat()).fillMaxHeight().background(it.tone.color())) }
        }
        shown.forEach { segment ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                Box(Modifier.width(Spacing.s8).height(Spacing.s8).clip(AppShapes.pill).background(segment.tone.color()))
                Text(segment.label, style = AppType.caption, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(format(segment.value), style = AppType.caption, color = colors.textPrimary)
            }
        }
    }
}
