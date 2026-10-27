package com.menash.myself.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

enum class ChipVariant {
    Default,
    Success,
    Warning,
}

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    variant: ChipVariant = ChipVariant.Default,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.appColors
    val background: Color
    val content: Color
    when (variant) {
        ChipVariant.Default -> {
            background = colors.accentSoft
            content = colors.accentStrong
        }
        ChipVariant.Success -> {
            background = colors.successSoft
            content = colors.success
        }
        ChipVariant.Warning -> {
            background = colors.warningSoft
            content = colors.warning
        }
    }
    val visualModifier = Modifier
        .height(32.dp)
        .clip(AppShapes.pill)
        .background(background)
        .padding(horizontal = Spacing.s8)
    if (onClick != null) {
        Box(
            modifier = modifier
                .minimumInteractiveComponentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            ChipBody(text = text, content = content, leadingIcon = leadingIcon, modifier = visualModifier)
        }
    } else {
        ChipBody(text = text, content = content, leadingIcon = leadingIcon, modifier = modifier.then(visualModifier))
    }
}

@Composable
private fun ChipBody(
    text: String,
    content: Color,
    leadingIcon: ImageVector?,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = content,
            )
        }
        Text(
            text = text,
            style = AppType.caption.copy(fontWeight = FontWeight.Medium),
            color = content,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "Chips light", showBackground = true, widthDp = 360, locale = "iw")
@Composable
private fun ChipLightPreview() {
    MyApplicationTheme(darkTheme = false) {
        ChipPreviewRow()
    }
}

@Preview(
    name = "Chips dark",
    showBackground = true,
    widthDp = 360,
    locale = "iw",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ChipDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        ChipPreviewRow()
    }
}

@Composable
private fun ChipPreviewRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        Chip(text = stringResource(R.string.pill_month))
        Chip(text = stringResource(R.string.chip_paid), variant = ChipVariant.Success)
        Chip(text = stringResource(R.string.chip_pending), variant = ChipVariant.Warning)
    }
}
