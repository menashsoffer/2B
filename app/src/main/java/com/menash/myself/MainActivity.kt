package com.menash.myself

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.menash.myself.auth.SignInScreen
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.data.firestore.MigrationManager
import com.menash.myself.ui.MainViewModel
import com.menash.myself.ui.calculators.CalculatorsHubScreen
import com.menash.myself.ui.calculators.ai.AiSubscriptionsScreen
import com.menash.myself.ui.calculators.car.CarComparisonScreen
import com.menash.myself.ui.calculators.pension.PensionSavingsScreen
import com.menash.myself.ui.calendar.CalendarScreen
import com.menash.myself.ui.catalog.FileCatalogScreen
import com.menash.myself.ui.dreams.DreamsScreen
import com.menash.myself.ui.fitness.FitnessScreen
import com.menash.myself.ui.home.LifeHomeScreen
import com.menash.myself.ui.household.HouseholdScreen
import com.menash.myself.ui.inbox.InboxTriageScreen
import com.menash.myself.ui.more.MoreScreen
import com.menash.myself.ui.quickcapture.QuickCaptureDialog
import com.menash.myself.ui.tasks.TasksScreen
import com.menash.myself.core.designsystem.theme.MyApplicationTheme

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
data class BottomNavTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
)

private val bottomNavTabs = listOf(
    BottomNavTab("home", R.string.ui_str_6, Icons.Default.Home, "nav_tab_home"),
    BottomNavTab("dreams", R.string.ui_str_3, Icons.Default.Star, "nav_tab_dreams"),
    BottomNavTab("tasks", R.string.ui_str_4, Icons.Default.CheckCircle, "nav_tab_tasks"),
    BottomNavTab("calculators", R.string.ui_str_1, Icons.Default.Calculate, "nav_tab_calculators"),
    BottomNavTab("more", R.string.ui_str_5, Icons.Default.MoreHoriz, "nav_tab_more")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appContainer = LocalAppContainer.current
            MyApplicationTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalAppContainer provides appContainer
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MyselfRootContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun MyselfRootContent() {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val auth = remember { runCatching { Firebase.auth }.getOrNull() }
    var currentUser by remember { mutableStateOf(auth?.currentUser) }
    var bypassSignIn by remember { mutableStateOf(false) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            currentUser = fbAuth.currentUser
        }
        auth?.addAuthStateListener(listener)
        onDispose {
            auth?.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null && !bypassSignIn) {
        SignInScreen(
            onAuthSuccess = { currentUser = auth?.currentUser },
            onContinueToCalculators = { bypassSignIn = true }
        )
    } else {
        LaunchedEffect(currentUser) {
            if (currentUser != null) {
                MigrationManager.checkAndMigrate(context)
            }
        }
        val mainViewModel: MainViewModel = viewModel(
            factory = MainViewModel.factory(appContainer)
        )
        LaunchedEffect(currentUser) {
            if (currentUser != null) {
                mainViewModel.initializeHouseholdForUser()
            }
        }
        AuthenticatedShell(
            mainViewModel = mainViewModel,
            startInCalculatorsOnly = currentUser == null,
            onSignedOut = {
                appContainer.householdSession.setHouseholdId(null)
                currentUser = null
                bypassSignIn = false
            }
        )
    }
}

@Composable
private fun AuthenticatedShell(
    mainViewModel: MainViewModel,
    startInCalculatorsOnly: Boolean,
    onSignedOut: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"
    val activeError by mainViewModel.errorFlow.collectAsState(initial = null)
    val vmError by mainViewModel.errorState.collectAsState()
    var showQuickCapture by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("main_bottom_navigation")) {
                bottomNavTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes), fontSize = 11.sp) },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickCapture = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("persistent_capture_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ui_str_7))
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
            val errorMsg = activeError ?: vmError
            if (errorMsg != null) {
                GlobalErrorBanner(message = errorMsg, onDismiss = { mainViewModel.clearErrors() })
            }
            MyselfNavGraph(
                navController = navController,
                startDestination = if (startInCalculatorsOnly) "calculators" else "home",
                onOpenQuickCapture = { showQuickCapture = true },
                onSignedOut = onSignedOut,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showQuickCapture) {
        QuickCaptureDialog(
            onDismiss = { showQuickCapture = false },
            onSave = { text, forWhom, isPrivate ->
                mainViewModel.addInboxItem(text = text, forWhom = forWhom, isPrivate = isPrivate)
                showQuickCapture = false
            }
        )
    }
}

@Composable
private fun GlobalErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("global_error_banner"),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_str_2), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MyselfNavGraph(
    navController: NavHostController,
    startDestination: String,
    onOpenQuickCapture: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("home") {
            LifeHomeScreen(
                onNavigate = { route -> navController.navigate(route) },
                onOpenQuickCapture = onOpenQuickCapture
            )
        }
        composable("tasks") { TasksScreen(onBack = { navController.popBackStack() }) }
        composable("dreams") { DreamsScreen(onBack = { navController.popBackStack() }) }
        composable("calculators") {
            CalculatorsHubScreen(navController = navController, onBack = { navController.popBackStack() })
        }
        composable("more") {
            MoreScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable("inbox") { InboxTriageScreen(onBack = { navController.popBackStack() }) }
        composable("fitness") { FitnessScreen(onBack = { navController.popBackStack() }) }
        composable("calendar") { CalendarScreen(onBack = { navController.popBackStack() }) }
        composable("catalog") { FileCatalogScreen(onBack = { navController.popBackStack() }) }
        composable("household") {
            HouseholdScreen(
                onBack = { navController.popBackStack() },
                onSignOut = onSignedOut
            )
        }
        composable("car_calculator") { CarComparisonScreen(onBack = { navController.popBackStack() }) }
        composable("ai_calculator") { AiSubscriptionsScreen(onBack = { navController.popBackStack() }) }
        composable("pension_calculator") { PensionSavingsScreen(onBack = { navController.popBackStack() }) }
    }
}
