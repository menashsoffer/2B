package com.menash.myself.core.designsystem.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.Chip
import com.menash.myself.core.designsystem.component.ChipVariant
import com.menash.myself.core.designsystem.component.FloatingNavBar
import com.menash.myself.core.designsystem.component.FloatingNavItem
import com.menash.myself.core.designsystem.component.HeroCard
import com.menash.myself.core.designsystem.component.PillTabs
import com.menash.myself.core.designsystem.component.ProfileAvatar
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

@Composable
fun DesignSystemGallerySamples() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s16)) {
        Text(
            text = stringResource(R.string.gallery_light),
            style = AppType.caption,
            color = MaterialTheme.appColors.textSecondary,
        )
        MyApplicationTheme(darkTheme = false) {
            GallerySampleSet()
        }
        Text(
            text = stringResource(R.string.gallery_dark),
            style = AppType.caption,
            color = MaterialTheme.appColors.textSecondary,
        )
        MyApplicationTheme(darkTheme = true) {
            GallerySampleSet()
        }
    }
}

@Composable
private fun GallerySampleSet() {
    val colors = MaterialTheme.appColors
    var selectedTab by rememberSaveable { mutableIntStateOf(1) }
    var selectedRoute by rememberSaveable { mutableStateOf("home") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        HeroCard(
            label = stringResource(R.string.nav_money),
            message = stringResource(R.string.home_money_message),
            onClick = {},
        )
        PillTabs(
            options = listOf(
                stringResource(R.string.pill_month),
                stringResource(R.string.pill_big_picture),
            ),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Chip(text = stringResource(R.string.pill_month))
            Chip(text = stringResource(R.string.chip_paid), variant = ChipVariant.Success)
            Chip(text = stringResource(R.string.chip_pending), variant = ChipVariant.Warning)
        }
        ProfileAvatar(
            displayName = stringResource(R.string.sample_name),
            onClick = {},
        )
        FloatingNavBar(
            items = galleryNavItems(),
            selectedRoute = selectedRoute,
            onSelect = { selectedRoute = it },
        )
    }
}

@Composable
private fun galleryNavItems(): List<FloatingNavItem> = listOf(
    FloatingNavItem("home", stringResource(R.string.nav_home), Icons.Outlined.Home, "gallery_nav_home"),
    FloatingNavItem("money", stringResource(R.string.nav_money), Icons.Outlined.AccountBalanceWallet, "gallery_nav_money"),
    FloatingNavItem("dreams", stringResource(R.string.nav_dreams), Icons.Outlined.StarOutline, "gallery_nav_dreams"),
    FloatingNavItem("shopping", stringResource(R.string.nav_shopping), Icons.Outlined.ShoppingBag, "gallery_nav_shopping"),
    FloatingNavItem("tracking", stringResource(R.string.nav_tracking), Icons.Outlined.BarChart, "gallery_nav_tracking"),
)
