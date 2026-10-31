package com.menash.myself.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseUser
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.FloatingNavBar
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.FloatingNavItem
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.ui.MainViewModel
import com.menash.myself.core.ui.UiEvent
import com.menash.myself.ui.money.hebrewMonthYear
import com.menash.myself.ui.settings.SettingsDialog
import kotlinx.coroutines.launch

@Composable
fun AppShell(
    mainViewModel: MainViewModel,
    currentUser: FirebaseUser?,
    onRequestSignIn: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedRoute = shellTabs.firstOrNull { tab ->
        backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
    }?.route.orEmpty()
    val activeError by mainViewModel.errorFlow.collectAsStateWithLifecycle(initialValue = null)
    val vmError by mainViewModel.errorState.collectAsStateWithLifecycle()
    var showAccount by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val appContainer = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackbarHost = remember { SnackbarHostState() }
    fun openRoute(route: String) {
        val destination = navController.currentDestination
        val alreadyOpen = destination?.hierarchy?.any { it.route == route } == true
        if (alreadyOpen) return
        if (route in shellTabRoutes) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            navController.navigate(route)
        }
    }

    LaunchedEffect(appContainer, snackbarHost) {
        var previous: kotlinx.coroutines.Job? = null
        appContainer.uiEvents.events.collect { event ->
            val text = when (event) {
                UiEvent.MONEY_SAVED -> context.getString(R.string.feedback_saved)
                UiEvent.DONATION_SAVED -> context.getString(R.string.feedback_donation_saved)
                UiEvent.DELETED -> context.getString(R.string.feedback_deleted)
                is UiEvent.ImportDone -> context.getString(R.string.feedback_import_done, event.written)
                is UiEvent.CategoryAssigned -> context.getString(R.string.feedback_category_assigned, event.name)
                is UiEvent.SaveError -> event.message
                is UiEvent.MonthClosed -> context.getString(R.string.feedback_month_closed, hebrewMonthYear(event.month))
            }
            if (event == UiEvent.DELETED || event is UiEvent.MonthClosed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            previous?.cancel()
            snackbarHost.currentSnackbarData?.dismiss()
            previous = scope.launch {
                val month = (event as? UiEvent.ImportDone)?.unassignedMonth
                val action = when {
                    month != null -> context.getString(R.string.feedback_assign)
                    event is UiEvent.SaveError -> context.getString(R.string.money_retry)
                    else -> null
                }
                val result = snackbarHost.showSnackbar(text, actionLabel = action, duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed && month != null) {
                    openRoute(ShellRoute.Money)
                    appContainer.uiEvents.openUnassigned(month)
                } else if (result == SnackbarResult.ActionPerformed && event is UiEvent.SaveError) {
                    event.retry()
                }
            }
        }
    }

    // A shared bank or card file opens the money tab, which runs the import.
    val sharedFiles by appContainer.importInbox.pending.collectAsStateWithLifecycle()
    LaunchedEffect(sharedFiles.isNotEmpty()) {
        if (sharedFiles.isNotEmpty()) openRoute(ShellRoute.Money)
    }

    AppShellContent(
        state = AppShellState(
            selectedRoute = selectedRoute,
            errorMessage = activeError ?: vmError,
        ),
        onDismissError = { mainViewModel.clearErrors() },
        onSelectTab = ::openRoute,
        snackbarHostState = snackbarHost,
    ) { contentModifier ->
        AppNavHost(
            navController = navController,
            displayName = currentUser?.displayName,
            onOpenRoute = ::openRoute,
            onOpenAccount = { showAccount = true },
            onSignedOut = onSignedOut,
            modifier = contentModifier,
        )
    }

    if (showAccount) {
        AccountSheet(
            signedIn = currentUser != null,
            displayName = currentUser?.displayName?.takeIf { it.isNotBlank() },
            email = currentUser?.email?.takeIf { it.isNotBlank() },
            onDismiss = { showAccount = false },
            onSettings = {
                showAccount = false
                showSettings = true
            },
            onHousehold = {
                showAccount = false
                openRoute(ShellRoute.Household)
            },
            onSignIn = {
                showAccount = false
                onRequestSignIn()
            },
            onSignedOut = {
                showAccount = false
                onSignedOut()
            },
        )
    }

    if (showSettings) {
        val rate by appContainer.exchangeRateRepository.currentRateFlow.collectAsStateWithLifecycle()
        SettingsDialog(
            currentRate = rate?.rate ?: 0.0,
            onDismiss = { showSettings = false },
            onSaveRate = { newRate ->
                scope.launch { appContainer.exchangeRateRepository.setManualRate(newRate) }
                showSettings = false
            },
            onOpenLegacyScreen = { route ->
                showSettings = false
                openRoute(route)
            },
        )
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            mainViewModel.initializeHouseholdForUser()
        }
    }
}

/** What the shell chrome shows: the selected tab and an optional error line. */
data class AppShellState(
    val selectedRoute: String,
    val errorMessage: String? = null,
)

/**
 * Stateless shell: error banner, the tab content and the floating nav bar.
 * [AppShell] passes the nav host as [content]; screenshot tests pass a fixed screen.
 */
@Composable
fun AppShellContent(
    state: AppShellState,
    onDismissError: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable (Modifier) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            val errorMessage = state.errorMessage
            if (errorMessage != null) {
                ShellErrorBanner(message = errorMessage, onDismiss = onDismissError)
            }
            content(Modifier.weight(1f))
        }
        FloatingNavBar(
            items = shellTabs.map { tab ->
                FloatingNavItem(
                    route = tab.route,
                    label = stringResource(tab.labelRes),
                    icon = tab.icon,
                    testTag = tab.testTag,
                )
            },
            selectedRoute = state.selectedRoute,
            onSelect = onSelectTab,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(
                    start = FloatingNavBarDefaults.outerMargin,
                    end = FloatingNavBarDefaults.outerMargin,
                    bottom = FloatingNavBarDefaults.outerMargin,
                ),
        )
        if (snackbarHostState != null) {
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(FloatingNavBarDefaults.contentPadding()))
        }
    }
}

@Composable
private fun ShellErrorBanner(
    message: String,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("global_error_banner")
            .padding(horizontal = Spacing.s16, vertical = Spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = AppType.caption,
            color = colors.danger,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) {
            Text(text = stringResource(R.string.ui_str_2), style = AppType.caption)
        }
    }
}
