package com.menash.myself.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

@Composable
fun PillTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(AppShapes.pill)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, AppShapes.pill)
            .padding(Spacing.s4)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(AppShapes.pill)
                    .background(if (selected) colors.accentSoft else colors.card.copy(alpha = 0f))
                    .selectable(
                        selected = selected,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        role = Role.Tab,
                        onClick = { onSelect(index) },
                    )
                    .padding(horizontal = Spacing.s8),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = if (selected) {
                        AppType.caption.copy(fontWeight = FontWeight.Medium)
                    } else {
                        AppType.caption
                    },
                    color = if (selected) colors.accentStrong else colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview(name = "Tabs light", showBackground = true, widthDp = 360, locale = "iw")
@Composable
private fun PillTabsLightPreview() {
    MyApplicationTheme(darkTheme = false) {
        PillTabsPreview()
    }
}

@Preview(
    name = "Tabs dark",
    showBackground = true,
    widthDp = 360,
    locale = "iw",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PillTabsDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        PillTabsPreview()
    }
}

@Composable
private fun PillTabsPreview() {
    PillTabs(
        options = listOf(
            stringResource(R.string.pill_month),
            stringResource(R.string.pill_big_picture),
        ),
        selectedIndex = 1,
        onSelect = {},
    )
}
