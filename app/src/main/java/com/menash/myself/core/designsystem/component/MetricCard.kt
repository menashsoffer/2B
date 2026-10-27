package com.menash.myself.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.designsystem.theme.tabular

enum class MetricValueStyle {
    Large,
    Medium,
}

@Composable
fun MetricCard(
    label: String,
    value: String?,
    detail: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    onClick: (() -> Unit)? = null,
    valueStyle: MetricValueStyle = MetricValueStyle.Large,
) {
    val colors = MaterialTheme.appColors
    val valueTextStyle = when (valueStyle) {
        MetricValueStyle.Large -> AppType.number
        MetricValueStyle.Medium -> AppType.title.tabular()
    }
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, AppShapes.card)
            .then(clickModifier)
            .semantics(mergeDescendants = true) {}
            .padding(Spacing.s16),
    ) {
        Text(text = label, style = AppType.caption, color = colors.textSecondary)
        Spacer(modifier = Modifier.height(Spacing.s8))
        if (value != null) {
            Text(
                text = value,
                style = valueTextStyle,
                color = colors.textPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(Spacing.s4))
        }
        Text(text = detail, style = AppType.caption, color = colors.textSecondary)
        if (progress != null) {
            Spacer(modifier = Modifier.height(Spacing.s16))
            ProgressBar(progress = progress)
        }
    }
}
