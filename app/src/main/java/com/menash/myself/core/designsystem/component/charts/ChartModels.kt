package com.menash.myself.core.designsystem.component.charts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.menash.myself.core.designsystem.theme.appColors

/**
 * Generic chart data for any screen (money today; tasks, fitness or shopping later). Values are plain
 * Longs (agorot for money) and every chart takes a `format` to print them, so the design system never
 * knows what is being counted. Colors are tones from the palette, never raw colors.
 */
enum class ChartTone { Accent, Strong, Soft, Warning, Danger, Success, Neutral }

@Composable
fun ChartTone.color(): Color {
    val colors = MaterialTheme.appColors
    return when (this) {
        ChartTone.Accent -> colors.accent
        ChartTone.Strong -> colors.accentStrong
        ChartTone.Soft -> colors.accentSoft
        ChartTone.Warning -> colors.warning
        ChartTone.Danger -> colors.danger
        ChartTone.Success -> colors.success
        ChartTone.Neutral -> colors.textSecondary
    }
}

/** One row of a plan-vs-actual chart. A null [plan] means "no plan": the bar shows the actual only. */
data class PlanActualItem(
    val label: String,
    val actual: Long,
    val plan: Long? = null,
    val tone: ChartTone = ChartTone.Accent,
    val key: String = label,
) {
    val over: Boolean get() = plan != null && actual > plan
}

/**
 * How complete a column's data is. PARTIAL is drawn lighter (an open month "so far"); MISSING is a gap
 * with a "missing" mark and never a zero bar.
 */
enum class DataState { COMPLETE, PARTIAL, MISSING }

/** One x-position of a column chart; [values] has one value per series, in series order. */
data class ColumnGroup(
    val label: String,
    val values: List<Long>,
    val state: DataState = DataState.COMPLETE,
    val key: String = label,
)

data class ChartSeries(val label: String, val tone: ChartTone)

/** One point of a line; a null [value] breaks the line there (unknown, not zero). */
data class LinePoint(val label: String, val value: Long?)

/** One part of a 100% stacked bar. */
data class ShareSegment(val label: String, val value: Long, val tone: ChartTone)
