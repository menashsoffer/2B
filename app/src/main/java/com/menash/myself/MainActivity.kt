package com.menash.myself

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.menash.myself.auth.SignInScreen
import com.menash.myself.data.firestore.HouseholdRepository
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.firestore.MigrationManager
import com.menash.myself.data.models.Household
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val context = LocalContext.current
                        val scope = rememberCoroutineScope()
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

                        if (currentUser == null) {
                            SignInScreen(
                                onAuthSuccess = {
                                    currentUser = auth.currentUser
                                }
                            )
                        } else {
                            // Run non-destructive Room -> Firestore migration in background on startup
                            LaunchedEffect(currentUser) {
                                MigrationManager.checkAndMigrate(context)
                            }

                            val navController = rememberNavController()
                            val lifeRepo = remember { LifeHubRepository(context) }
                            val householdRepo = remember { HouseholdRepository(context) }
                            var household by remember { mutableStateOf<Household?>(null) }
                            var showQuickCapture by remember { mutableStateOf(false) }

                            LaunchedEffect(Unit) {
                                household = householdRepo.getOrCreateHousehold()
                                householdRepo.observeHousehold().collect { h ->
                                    if (h != null) household = h
                                }
                            }

                            NavHost(navController = navController, startDestination = "home") {
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
                                            currentUser = null
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

                            // Global quick capture modal accessible from home FAB
                            if (showQuickCapture) {
                                QuickCaptureDialog(
                                    onDismiss = { showQuickCapture = false },
                                    onSave = { text, space ->
                                        scope.launch {
                                            lifeRepo.addInboxItem(text, space, household)
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
