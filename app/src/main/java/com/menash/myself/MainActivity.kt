package com.menash.myself

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.menash.myself.auth.SignInScreen
import com.menash.myself.core.di.AppContainer
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
import com.menash.myself.ui.quickcapture.QuickCaptureDialog
import com.menash.myself.ui.tasks.TasksScreen
import com.menash.myself.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val appContainer = remember { AppContainer(context.applicationContext) }

            MyApplicationTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalAppContainer provides appContainer
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val auth = Firebase.auth
                        var currentUser by remember { mutableStateOf(auth.currentUser) }

                        DisposableEffect(auth) {
                            val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { fbAuth ->
                                currentUser = fbAuth.currentUser
                            }
                            auth.addAuthStateListener(listener)
                            onDispose {
                                auth.removeAuthStateListener(listener)
                            }
                        }

                        var bypassSignIn by remember { mutableStateOf(false) }

                        if (currentUser == null && !bypassSignIn) {
                            SignInScreen(
                                onAuthSuccess = {
                                    currentUser = auth.currentUser
                                },
                                onContinueToCalculators = {
                                    bypassSignIn = true
                                }
                            )
                        } else {
                            // Non-destructive Room -> Firestore migration (behind flag, disabled by default)
                            LaunchedEffect(currentUser) {
                                if (currentUser != null) {
                                    MigrationManager.checkAndMigrate(context)
                                }
                            }

                            val mainViewModel: MainViewModel = viewModel(
                                factory = MainViewModel.factory(appContainer)
                            )
                            val navController = rememberNavController()
                            val activeError by mainViewModel.activeError.collectAsState()
                            var showQuickCapture by remember { mutableStateOf(false) }

                            LaunchedEffect(currentUser) {
                                if (currentUser != null) {
                                    mainViewModel.initializeHouseholdForUser()
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Surface Firestore errors in UI
                                    if (activeError != null) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = activeError!!,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                TextButton(onClick = {
                                                    mainViewModel.clearErrors()
                                                }) {
                                                    Text("סגור", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }

                                    NavHost(
                                        navController = navController,
                                        startDestination = if (currentUser == null) "calculators" else "home",
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        composable("home") {
                                            LifeHomeScreen(
                                                onNavigate = { route -> navController.navigate(route) },
                                                onOpenQuickCapture = { showQuickCapture = true }
                                            )
                                        }
                                        composable("tasks") {
                                            TasksScreen(onBack = { navController.popBackStack() })
                                        }
                                        composable("dreams") {
                                            DreamsScreen(onBack = { navController.popBackStack() })
                                        }
                                        composable("inbox") {
                                            InboxTriageScreen(onBack = { navController.popBackStack() })
                                        }
                                        composable("fitness") {
                                            FitnessScreen(onBack = { navController.popBackStack() })
                                        }
                                        composable("calendar") {
                                            CalendarScreen(onBack = { navController.popBackStack() })
                                        }
                                        composable("catalog") {
                                            FileCatalogScreen(onBack = { navController.popBackStack() })
                                        }
                                        composable("household") {
                                            HouseholdScreen(
                                                onBack = { navController.popBackStack() },
                                                onSignOut = {
                                                    appContainer.householdSession.setHouseholdId(null)
                                                    currentUser = null
                                                    bypassSignIn = false
                                                }
                                            )
                                        }
                                        composable("calculators") {
                                            CalculatorsHubScreen(
                                                navController = navController,
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                        composable("car_calculator") {
                                            CarComparisonScreen(onBack = { navController.popBackStack() })
                                        }
                                        composable("ai_calculator") {
                                            AiSubscriptionsScreen(onBack = { navController.popBackStack() })
                                        }
                                        composable("pension_calculator") {
                                            PensionSavingsScreen(onBack = { navController.popBackStack() })
                                        }
                                    }
                                }

                                // Global quick capture modal accessible from home FAB
                                if (showQuickCapture) {
                                    QuickCaptureDialog(
                                        onDismiss = { showQuickCapture = false },
                                        onSave = { text, space ->
                                            mainViewModel.addQuickCapture(text, space) {
                                                showQuickCapture = false
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
