package com.menash.myself.core.designsystem.component.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

/**
 * Vertical grouped columns (for example income vs expense per month). Every group is one tap target;
 * tapping shows its values above the chart instead of a popup that would cover it. A PARTIAL group is
 * drawn lighter with a dashed outline, a MISSING group shows the "missing" mark, never a zero bar.
 * Values below zero are drawn as zero (columns show sizes, not signs).
 */
private val BarShape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)

@Composable
fun ColumnChart(
    groups: List<ColumnGroup>,
    series: List<ChartSeries>,
    format: (Long) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    testTag: String = "chart_columns",
) {
    if (groups.isEmpty()) return
    val colors = MaterialTheme.appColors
    val top = groups.filter { it.state != DataState.MISSING }.flatMap { it.values }.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    Column(modifier = modifier.fillMaxWidth().testTag(testTag), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        val chosen = groups.firstOrNull { it.key == selected }
        Text(
            text = chosen?.let { group -> groupText(group, series, format) } ?: "",
            style = AppType.caption,
            color = colors.textPrimary,
            modifier = Modifier.fillMaxWidth().heightIn(min = 16.dp).testTag("${testTag}_selected"),
        )
        Row(modifier = Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            groups.forEach { group ->
                val description = groupText(group, series, format)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("${testTag}_${group.key}")
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (group.key == selected) colors.accentSoft.copy(alpha = 0.35f) else Color.Transparent)
                        .clickable { selected = if (selected == group.key) null else group.key }
                        .clearAndSetSemantics {
                            contentDescription = description
                            role = Role.Button
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (group.state == DataState.MISSING) {
                        Text(stringResource(R.string.chart_missing), style = AppType.caption, color = colors.textSecondary, textAlign = TextAlign.Center)
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.s4),
                            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            group.values.forEachIndexed { index, value ->
                                val tone = series.getOrNull(index)?.tone ?: ChartTone.Accent
                                val fraction = (value.coerceAtLeast(0L).toFloat() / top).coerceIn(0f, 1f)
                                val partial = group.state == DataState.PARTIAL
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .fillMaxHeight(fraction.coerceAtLeast(0.01f))
                                        .clip(BarShape)
                                        .background(tone.color().copy(alpha = if (partial) 0.45f else 1f))
                                        .then(if (partial) Modifier.border(1.dp, tone.color(), BarShape) else Modifier),
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            groups.forEach { group ->
                Text(
                    text = group.label,
                    style = AppType.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        ChartLegend(series.map { it.label to it.tone })
    }
}

@Composable
private fun groupText(group: ColumnGroup, series: List<ChartSeries>, format: (Long) -> String): String = when (group.state) {
    DataState.MISSING -> stringResource(R.string.chart_missing_description, group.label)
    else -> {
        val values = group.values.mapIndexed { i, v -> "${series.getOrNull(i)?.label.orEmpty()} ${format(v)}" }.joinToString(" · ")
        if (group.state == DataState.PARTIAL) stringResource(R.string.chart_partial_description, group.label, values)
        else stringResource(R.string.chart_item_description, group.label, values)
    }
}

/** Colour keys, wrapped two per line. */
@Composable
fun ChartLegend(entries: List<Pair<String, ChartTone>>, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) return
    val colors = MaterialTheme.appColors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s16)) {
                pair.forEach { (label, tone) ->
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                        Box(Modifier.width(Spacing.s8).height(Spacing.s8).clip(AppShapes.pill).background(tone.color()))
                        Text(label, style = AppType.caption, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}
