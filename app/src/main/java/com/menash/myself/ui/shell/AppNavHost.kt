package com.menash.myself.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseUser
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.menash.myself.BuildConfig
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.ui.calendar.CalendarScreen
import com.menash.myself.ui.catalog.FileCatalogScreen
import com.menash.myself.ui.dreams.DreamsScreen
import com.menash.myself.ui.fitness.FitnessScreen
import com.menash.myself.ui.home.HomeScreen
import com.menash.myself.ui.home.LifeHomeScreen
import com.menash.myself.ui.household.HouseholdScreen
import com.menash.myself.ui.inbox.InboxTriageScreen
import com.menash.myself.ui.life.LifeContent
import com.menash.myself.ui.me.MeScreen
import com.menash.myself.ui.money.MoneyScreen
import com.menash.myself.ui.more.MoreScreen
import com.menash.myself.ui.tasks.TasksScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    currentUser: FirebaseUser?,
    onOpenRoute: (String) -> Unit,
    onRequestSignIn: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ShellRoute.Home,
        modifier = modifier,
    ) {
        navigation(startDestination = "${ShellRoute.Home}/main", route = ShellRoute.Home) {
            composable("${ShellRoute.Home}/main") {
                HomeScreen(
                    displayName = currentUser?.displayName,
                    onOpenRoute = onOpenRoute,
                    onOpenAccount = { onOpenRoute(ShellRoute.Me) },
                )
            }
        }
        navigation(startDestination = "${ShellRoute.Money}/main", route = ShellRoute.Money) {
            composable("${ShellRoute.Money}/main") {
                MoneyScreen()
            }
        }
        navigation(startDestination = "${ShellRoute.Me}/main", route = ShellRoute.Me) {
            composable("${ShellRoute.Me}/main") {
                MeScreen(
                    currentUser = currentUser,
                    onRequestSignIn = onRequestSignIn,
                    onSignedOut = onSignedOut,
                    onOpenHousehold = { onOpenRoute(ShellRoute.Household) },
                    onOpenRoute = onOpenRoute,
                )
            }
            composable(ShellRoute.Household) {
                LegacyPage {
                    HouseholdScreen(
                        onBack = { navController.popBackStack() },
                        onSignOut = onSignedOut,
                    )
                }
            }
        }
        if (BuildConfig.DEBUG) {
            navigation(startDestination = "${ShellRoute.Life}/main", route = ShellRoute.Life) {
                composable("${ShellRoute.Life}/main") {
                    LifeContent(onOpenRoute = onOpenRoute)
                }
                composable(ShellRoute.Calendar) {
                    LegacyPage { CalendarScreen(onBack = { navController.popBackStack() }) }
                }
                composable(ShellRoute.Fitness) {
                    LegacyPage { FitnessScreen(onBack = { navController.popBackStack() }) }
                }
                composable(ShellRoute.Inbox) {
                    LegacyPage { InboxTriageScreen(onBack = { navController.popBackStack() }) }
                }
                composable(ShellRoute.DreamsLegacy) {
                    LegacyPage { DreamsScreen(onBack = { navController.popBackStack() }) }
                }
                composable(ShellRoute.Tasks) {
                    LegacyPage { TasksScreen(onBack = { navController.popBackStack() }) }
                }
            }
            val graph = this
            placeholderTabs.forEach { tab ->
                graph.composable(tab.route) {
                    PlaceholderTab(
                        title = stringResource(tab.labelRes),
                        icon = tab.icon,
                        route = tab.route,
                    )
                }
            }
        }
        if (BuildConfig.DEBUG) {
        composable(ShellRoute.LegacyHome) {
            LegacyPage {
                LifeHomeScreen(onNavigate = onOpenRoute, onOpenQuickCapture = {})
            }
        }
        composable(ShellRoute.More) {
            LegacyPage { MoreScreen(onNavigate = onOpenRoute) }
        }
        composable(ShellRoute.Catalog) {
            LegacyPage { FileCatalogScreen(onBack = { navController.popBackStack() }) }
        }
        }
    }
}

@Composable
private fun LegacyPage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(FloatingNavBarDefaults.contentPadding()),
    ) {
        content()
    }
}
