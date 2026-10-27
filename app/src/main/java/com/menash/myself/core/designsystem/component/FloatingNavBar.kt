package com.menash.myself.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.StarOutline
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import androidx.compose.ui.res.stringResource

data class FloatingNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
)

object FloatingNavBarDefaults {
    val barHeight = 64.dp
    val outerMargin = Spacing.s8 + Spacing.s4

    @Composable
    fun contentPadding(): PaddingValues {
        val navigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        return PaddingValues(bottom = barHeight + outerMargin + navigationInset)
    }
}

@Composable
fun FloatingNavBar(
    items: List<FloatingNavItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FloatingNavBarDefaults.barHeight)
            .clip(AppShapes.pill)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, AppShapes.pill)
            .padding(horizontal = Spacing.s8)
            .selectableGroup()
            .testTag("floating_nav_bar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            FloatingNavItemCell(
                item = item,
                selected = item.route == selectedRoute,
                onSelect = { onSelect(item.route) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FloatingNavItemCell(
    item: FloatingNavItem,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val tint = if (selected) colors.accentStrong else colors.textSecondary
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                role = Role.Tab,
                onClick = onSelect,
            )
            .testTag(item.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(AppShapes.pill)
                .background(if (selected) colors.accentSoft else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = tint,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.s4))
        Text(
            text = item.label,
            style = if (selected) AppType.caption.copy(fontWeight = FontWeight.Medium) else AppType.caption,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "Nav light", showBackground = true, widthDp = 360, locale = "iw")
@Composable
private fun FloatingNavBarLightPreview() {
    MyApplicationTheme(darkTheme = false) {
        FloatingNavBarPreview()
    }
}

@Preview(
    name = "Nav dark",
    showBackground = true,
    widthDp = 360,
    locale = "iw",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun FloatingNavBarDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        FloatingNavBarPreview()
    }
}

@Composable
private fun FloatingNavBarPreview() {
    FloatingNavBar(
        items = listOf(
            FloatingNavItem("home", stringResource(R.string.nav_home), Icons.Outlined.Home, "nav_home"),
            FloatingNavItem("money", stringResource(R.string.nav_money), Icons.Outlined.AccountBalanceWallet, "nav_money"),
            FloatingNavItem("dreams", stringResource(R.string.nav_dreams), Icons.Outlined.StarOutline, "nav_dreams"),
            FloatingNavItem("shopping", stringResource(R.string.nav_shopping), Icons.Outlined.ShoppingBag, "nav_shopping"),
            FloatingNavItem("tracking", stringResource(R.string.nav_tracking), Icons.Outlined.BarChart, "nav_tracking"),
        ),
        selectedRoute = "home",
        onSelect = {},
    )
}
