package com.menash.myself.ui.calculators

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.menash.myself.core.designsystem.component.ErrorState
import com.menash.myself.core.designsystem.component.LoadingState
import com.menash.myself.core.designsystem.component.OfflineIndicator
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.core.domain.model.AiSubscription
import com.menash.myself.core.domain.model.Car
import com.menash.myself.core.domain.model.PensionFund
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.feature.calculators.CalculatorsHubViewModel
import com.menash.myself.ui.settings.SettingsDialog
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorsHubScreen(
    navController: NavController,
    onBack: () -> Unit,
    isOffline: Boolean = false
) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val viewModel: CalculatorsHubViewModel = viewModel(
        factory = CalculatorsHubViewModel.factory(
            carRepository = appContainer.carRepository,
            aiSubscriptionRepository = appContainer.aiSubscriptionRepository,
            pensionFundRepository = appContainer.pensionFundRepository,
            settingsRepository = appContainer.settingsRepository,
            errorReporter = appContainer.errorReporter
        )
    )

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cars by viewModel.cars.collectAsStateWithLifecycle()
    val aiSubs by viewModel.aiSubs.collectAsStateWithLifecycle()
    val pensionFunds by viewModel.pensionFunds.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var pendingNoticeMessage by remember { mutableStateOf<String?>(null) }

    val usdRate = settings?.usdToIlsRate ?: 3.11
    val lastBackup = settings?.lastBackupTimestamp ?: 0L
    val daysSinceBackup = if (lastBackup == 0L) Long.MAX_VALUE else TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastBackup)
    val needsBackupWarning = daysSinceBackup >= 30

    val calculators = remember(cars, aiSubs, pensionFunds, usdRate) {
        buildCalculatorItems(cars, aiSubs, pensionFunds, usdRate)
    }

    Scaffold(
        topBar = {
            CalculatorsHubTopBar(
                onBack = onBack,
                onBackup = { viewModel.createAndShareBackup(context) },
                onSettings = { showSettingsDialog = true }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            OfflineIndicator(isOffline = isOffline)

            when (val state = uiState) {
                is UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.clearError() }
                )
                is UiState.Empty, is UiState.Content -> {
                    if (needsBackupWarning) {
                        BackupWarningBanner(onBackupNow = { viewModel.createAndShareBackup(context) })
                    }
                    CalculatorsStatusBar(
                        usdRate = usdRate,
                        lastBackup = lastBackup,
                        daysSinceBackup = daysSinceBackup,
                        onEditRate = { showSettingsDialog = true }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CalculatorsGrid(
                        calculators = calculators,
                        onCardClick = { item ->
                            if (item.isReady && item.route != null) {
                                navController.navigate(item.route)
                            } else {
                                pendingNoticeMessage = "מחשבון ${item.title} יתווסף בשלב הבא לבקשתך!"
                            }
                        }
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentRate = usdRate,
            lastBackupTimestamp = lastBackup,
            onDismiss = { showSettingsDialog = false },
            onSaveRate = { newRate ->
                viewModel.saveRate(newRate)
                showSettingsDialog = false
            }
        )
    }

    pendingNoticeMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { pendingNoticeMessage = null },
            title = { Text("מחשבון בשלבי פיתוח", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { pendingNoticeMessage = null }) {
                    Text("הבנתי")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculatorsHubTopBar(
    onBack: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text("המחשבונים שלי", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("מרכז מחשבונים פיננסיים (שמור מקומית ובענן)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("calculators_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה לדף הבית")
            }
        },
        actions = {
            IconButton(
                onClick = onBackup,
                modifier = Modifier.testTag("calculators_backup_button")
            ) {
                Icon(Icons.Default.Backup, contentDescription = "גיבוי נתונים")
            }
            IconButton(
                onClick = onSettings,
                modifier = Modifier.testTag("calculators_settings_button")
            ) {
                Icon(Icons.Default.Settings, contentDescription = "הגדרות ושער דולר")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

private fun buildCalculatorItems(
    cars: List<Car>,
    aiSubs: List<AiSubscription>,
    pensionFunds: List<PensionFund>,
    usdRate: Double
): List<CalculatorCardItem> {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val carHeadline = if (cars.isEmpty()) {
        "אין רכבים עדיין (לחץ להוספה)"
    } else {
        val best = cars.minByOrNull { it.netMonthlyCost } ?: cars.first()
        "הכי משתלם: ${best.name} (₪${fmt.format(best.netMonthlyCost.shekels.toInt())}/חודש)"
    }

    val activeAi = aiSubs.filter { it.isActive }
    val sumMonthlyAi = activeAi.sumOf { it.monthlyCostIls(usdRate).shekels }
    val aiHeadline = "₪${fmt.format(sumMonthlyAi.toInt())} / חודש (${activeAi.size} פעילים)"

    val pensionHeadline = if (pensionFunds.isEmpty()) {
        "אין קופות עדיין (לחץ להוספה)"
    } else {
        val totalVal = pensionFunds.sumOf { it.currentValue.shekels }
        val totalDep = pensionFunds.sumOf { it.totalDeposits.shekels }
        val profitPercent = if (totalDep > 0) ((totalVal - totalDep) / totalDep) * 100.0 else 0.0
        "סך נכסים: ₪${fmt.format(totalVal.toInt())} (${String.format(Locale.US, "%+.1f", profitPercent)}%)"
    }

    return listOf(
        CalculatorCardItem("car", "השוואת רכבים (5 שנים)", carHeadline, Icons.Default.DirectionsCar, "car_calculator", true),
        CalculatorCardItem("ai", "מנויי AI וכלים", aiHeadline, Icons.Default.SmartToy, "ai_calculator", true),
        CalculatorCardItem("pension", "פנסיה וחיסכון", pensionHeadline, Icons.Default.Savings, "pension_calculator", true),
        CalculatorCardItem("mishloach", "משלוחי מנות", "תכנון סל: ₪680 (12 מארזים)", Icons.Default.CardGiftcard, null, false),
        CalculatorCardItem("investments", "יומן השקעות", "תשואה: +14.2% (₪42,500)", Icons.Default.TrendingUp, null, false),
        CalculatorCardItem("apartment", "נדל״ן — קנייה ומכירה", "רווח צפוי: ₪385,000", Icons.Default.Apartment, null, false),
        CalculatorCardItem("donations", "החזרי מס — תרומות", "זכאות להחזר: ₪2,150", Icons.Default.VolunteerActivism, null, false),
        CalculatorCardItem("trip", "תקציב טיול", "תקציב משוער: $4,850 (₪320/יום)", Icons.Default.FlightTakeoff, null, false)
    )
}
