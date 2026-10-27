package com.menash.myself.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

@Composable
fun MetricCard(
    label: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, AppShapes.card)
            .padding(Spacing.s16),
    ) {
        Text(text = label, style = AppType.caption, color = colors.textSecondary)
        Spacer(modifier = Modifier.height(Spacing.s8))
        Text(text = value, style = AppType.number, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(Spacing.s4))
        Text(text = detail, style = AppType.caption, color = colors.textSecondary)
        if (progress != null) {
            Spacer(modifier = Modifier.height(Spacing.s16))
            ProgressBar(progress = progress)
        }
    }
}
