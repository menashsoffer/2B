package com.menash.myself.shell

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/** Sign-in route outside the tab graphs. Stage 3 reuses [requireSignIn] for a guest save. */
object RequireSignIn {
    const val Route = "auth/sign_in"
    const val ResultKey = "require_sign_in_succeeded"
}

/** The tab bar stays off the sign-in route (B13). */
fun shellShowsNavBar(route: String?): Boolean = route != RequireSignIn.Route

/** Tab roots other than בית use this to go home and keep that tab's stack (B5). */
val LocalLeaveTab = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
fun TabRootBack(enabled: Boolean = true) {
    val leave = LocalLeaveTab.current ?: return
    BackHandler(enabled = enabled, onBack = leave)
}

/** Sign-out lands on the בית root and drops every saved tab, including the money walk stack. */
fun NavHostController.signOutToHome() {
    shellTabRoutes.forEach { route -> clearBackStack(route) }
    navigate(ShellRoute.Home) {
        popUpTo(graph.id) {
            inclusive = true
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }
}

fun NavHostController.openTab(route: String, onMoneyReselect: () -> Unit = {}) {
    val already = currentDestination?.hierarchy?.any { it.route == route } == true
    openShellRoute(route)
    if (already && route == ShellRoute.Money) onMoneyReselect()
}

fun NavHostController.openShellRoute(route: String) {
    val destination = currentDestination
    val alreadyOpen = destination?.hierarchy?.any { it.route == route } == true
    if (alreadyOpen) {
        if (route in shellTabRoutes) popBackStack("$route/main", inclusive = false)
        return
    }
    if (route in shellTabRoutes) {
        navigate(route) {
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    } else {
        navigate(route)
    }
}

/**
 * Runs [onReady] when the user is already signed in.
 * Otherwise opens [RequireSignIn.Route] and, after a successful sign-in, the caller resumes
 * from [RequireSignInEffect]. Cancel pops back without setting the result.
 */
fun NavHostController.requireSignIn(signedIn: Boolean, onReady: () -> Unit = {}) {
    if (signedIn) onReady() else navigate(RequireSignIn.Route) { launchSingleTop = true }
}

/** Observes a successful sign-in that returns to this destination. Clears the result after delivery. */
@Composable
fun RequireSignInEffect(navController: NavHostController, onSignedIn: () -> Unit) {
    val entry = navController.currentBackStackEntry ?: return
    val result by entry.savedStateHandle.getStateFlow(RequireSignIn.ResultKey, false).collectAsStateWithLifecycle()
    LaunchedEffect(result) {
        if (!result) return@LaunchedEffect
        entry.savedStateHandle[RequireSignIn.ResultKey] = false
        onSignedIn()
    }
}
