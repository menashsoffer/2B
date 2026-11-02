package com.menash.myself.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    /** Null keeps the accent; a status color (warning, danger) when the bar carries meaning. */
    color: Color? = null,
) {
    val colors = MaterialTheme.appColors
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.s8)
            .clip(AppShapes.control),
        color = color ?: colors.accent,
        trackColor = colors.accentSoft,
    )
}
