package com.menash.myself.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.ui.calculators.CalculatorsHubScreen
import com.menash.myself.ui.calculators.ai.AiSubscriptionsScreen
import com.menash.myself.ui.calculators.car.CarComparisonScreen
import com.menash.myself.ui.calculators.pension.PensionSavingsScreen
import com.menash.myself.ui.calendar.CalendarScreen
import com.menash.myself.ui.catalog.FileCatalogScreen
import com.menash.myself.ui.dreams.DreamsScreen
import com.menash.myself.ui.fitness.FitnessScreen
import com.menash.myself.ui.home.HomeScreen
import com.menash.myself.ui.home.LifeHomeScreen
import com.menash.myself.ui.household.HouseholdScreen
import com.menash.myself.ui.inbox.InboxTriageScreen
import com.menash.myself.ui.more.MoreScreen
import com.menash.myself.ui.tasks.TasksScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    displayName: String?,
    onOpenRoute: (String) -> Unit,
    onOpenAccount: () -> Unit,
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
                    displayName = displayName,
                    onOpenRoute = onOpenRoute,
                    onOpenAccount = onOpenAccount,
                )
            }
        }
        val graph = this
        shellTabs.filter { tab -> tab.route != ShellRoute.Home }.forEach { tab ->
            graph.navigation(startDestination = "${tab.route}/main", route = tab.route) {
                composable("${tab.route}/main") {
                    PlaceholderTab(
                        title = stringResource(tab.labelRes),
                        icon = tab.icon,
                        route = tab.route,
                    )
                }
            }
        }
        composable(ShellRoute.LegacyHome) {
            LegacyPage {
                LifeHomeScreen(onNavigate = onOpenRoute, onOpenQuickCapture = {})
            }
        }
        composable(ShellRoute.Tasks) {
            LegacyPage { TasksScreen(onBack = { navController.popBackStack() }) }
        }
        composable(ShellRoute.DreamsLegacy) {
            LegacyPage { DreamsScreen(onBack = { navController.popBackStack() }) }
        }
        composable(ShellRoute.Calculators) {
            LegacyPage {
                CalculatorsHubScreen(
                    navController = navController,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable(ShellRoute.Car) {
            LegacyPage { CarComparisonScreen(onBack = { navController.popBackStack() }) }
        }
        composable(ShellRoute.Ai) {
            LegacyPage { AiSubscriptionsScreen(onBack = { navController.popBackStack() }) }
        }
        composable(ShellRoute.Pension) {
            LegacyPage { PensionSavingsScreen(onBack = { navController.popBackStack() }) }
        }
        composable(ShellRoute.More) {
            LegacyPage { MoreScreen(onNavigate = onOpenRoute) }
        }
        composable(ShellRoute.Inbox) {
            LegacyPage { InboxTriageScreen(onBack = { navController.popBackStack() }) }
        }
        composable(ShellRoute.Fitness) {
            LegacyPage { FitnessScreen(onBack = { navController.popBackStack() }) }
        }
        composable(ShellRoute.Calendar) {
            LegacyPage { CalendarScreen(onBack = { navController.popBackStack() }) }
        }
        composable(ShellRoute.Catalog) {
            LegacyPage { FileCatalogScreen(onBack = { navController.popBackStack() }) }
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
