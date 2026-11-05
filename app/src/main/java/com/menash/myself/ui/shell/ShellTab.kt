package com.menash.myself.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.menash.myself.BuildConfig
import com.menash.myself.R

object ShellRoute {
    const val Home = "home"
    const val Money = "money"
    const val Life = "life"
    const val Me = "me"
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

/**
 * The bar: בית · כסף · (חיים) · אני. חיים holds tasks, dreams, fitness and the calendar,
 * which are not finished yet, so it is only in debug builds (v1 scope, 02/11/2026).
 */
fun shellTabsFor(debug: Boolean): List<ShellTab> = listOfNotNull(
    ShellTab(ShellRoute.Home, R.string.nav_home, Icons.Outlined.Home, "nav_home"),
    ShellTab(ShellRoute.Money, R.string.nav_money, Icons.Outlined.AccountBalanceWallet, "nav_money"),
    ShellTab(ShellRoute.Life, R.string.nav_life, Icons.Outlined.Spa, "nav_life").takeIf { debug },
    ShellTab(ShellRoute.Me, R.string.nav_me, Icons.Outlined.Person, "nav_me"),
)

val shellTabs = shellTabsFor(BuildConfig.DEBUG)

val shellTabRoutes = shellTabs.map { tab -> tab.route }.toSet()

/** The old empty tabs. Off the bar; debug builds still register them as routes. */
val placeholderTabs: List<ShellTab> = listOf(
    ShellTab(ShellRoute.Dreams, R.string.nav_dreams, Icons.Outlined.StarOutline, "nav_dreams"),
    ShellTab(ShellRoute.Shopping, R.string.nav_shopping, Icons.Outlined.ShoppingBag, "nav_shopping"),
    ShellTab(ShellRoute.Tracking, R.string.nav_tracking, Icons.Outlined.BarChart, "nav_tracking"),
)
