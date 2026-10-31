package com.menash.myself.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.menash.myself.R

object ShellRoute {
    const val Home = "home"
    const val Money = "money"
    const val Dreams = "dreams"
    const val Shopping = "shopping"
    const val Tracking = "tracking"
    const val LegacyHome = "legacy_home"
    const val Tasks = "tasks"
    const val DreamsLegacy = "dreams_legacy"
    const val More = "more"
    const val Inbox = "inbox"
    const val Fitness = "fitness"
    const val Calendar = "calendar"
    const val Catalog = "catalog"
    const val Household = "household"
}

data class ShellTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String,
)

val shellTabs = listOf(
    ShellTab(ShellRoute.Home, R.string.nav_home, Icons.Outlined.Home, "nav_home"),
    ShellTab(ShellRoute.Money, R.string.nav_money, Icons.Outlined.AccountBalanceWallet, "nav_money"),
    ShellTab(ShellRoute.Dreams, R.string.nav_dreams, Icons.Outlined.StarOutline, "nav_dreams"),
    ShellTab(ShellRoute.Shopping, R.string.nav_shopping, Icons.Outlined.ShoppingBag, "nav_shopping"),
    ShellTab(ShellRoute.Tracking, R.string.nav_tracking, Icons.Outlined.BarChart, "nav_tracking"),
)

val shellTabRoutes = shellTabs.map { tab -> tab.route }.toSet()
