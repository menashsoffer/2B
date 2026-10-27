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

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
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

    val calculators = remember(cars, aiSubs, pensionFunds, usdRate, context) {
        buildCalculatorItems(context, cars.orEmpty(), aiSubs.orEmpty(), pensionFunds.orEmpty(), usdRate)
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
                                pendingNoticeMessage = context.getString(R.string.ui_itemtitle_15, item.title)
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
            },
            onOpenLegacyScreen = { route ->
                showSettingsDialog = false
                navController.navigate(route)
            },
        )
    }

    pendingNoticeMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { pendingNoticeMessage = null },
            title = { Text(stringResource(R.string.ui_str_31), fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { pendingNoticeMessage = null }) {
                    Text(stringResource(R.string.ui_str_19))
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
                Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(stringResource(R.string.ui_str_30), fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("calculators_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ui_str_28))
            }
        },
        actions = {
            IconButton(
                onClick = onBackup,
                modifier = Modifier.testTag("calculators_backup_button")
            ) {
                Icon(Icons.Default.Backup, contentDescription = stringResource(R.string.ui_str_20))
            }
            IconButton(
                onClick = onSettings,
                modifier = Modifier.testTag("calculators_settings_button")
            ) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.ui_str_23))
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
    context: android.content.Context,
    cars: List<Car>,
    aiSubs: List<AiSubscription>,
    pensionFunds: List<PensionFund>,
    usdRate: Double
): List<CalculatorCardItem> {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val carHeadline = if (cars.isEmpty()) {
        context.getString(R.string.ui_str_29)
    } else {
        val best = cars.minByOrNull { it.netMonthlyCost } ?: cars.first()
        context.getString(R.string.ui_bestname_fmtformatbestnetmonthlycostshekelstoint_17, best.name, fmt.format(best.netMonthlyCost.shekels.toInt()))
    }

    val activeAi = aiSubs.filter { it.isActive }
    val sumMonthlyAi = activeAi.sumOf { it.monthlyCostIls(usdRate).shekels }
    val aiHeadline = context.getString(R.string.ui_fmtformatsummonthlyaitoint_activeaisize_26, fmt.format(sumMonthlyAi.toInt()), activeAi.size)

    val pensionHeadline = if (pensionFunds.isEmpty()) {
        context.getString(R.string.ui_str_12)
    } else {
        val totalVal = pensionFunds.sumOf { it.currentValue.shekels }
        val totalDep = pensionFunds.sumOf { it.totalDeposits.shekels }
        val profitPercent = if (totalDep > 0) ((totalVal - totalDep) / totalDep) * 100.0 else 0.0
        context.getString(R.string.ui_fmtformattotalvaltoint_stringformatlocaleus_18, fmt.format(totalVal.toInt()), String.format(Locale.US, "%+.1f%%", profitPercent))
    }

    return listOf(
        CalculatorCardItem("car", context.getString(R.string.ui_5_13), carHeadline, Icons.Default.DirectionsCar, "car_calculator", true),
        CalculatorCardItem("ai", context.getString(R.string.ui_ai_27), aiHeadline, Icons.Default.SmartToy, "ai_calculator", true),
        CalculatorCardItem("pension", context.getString(R.string.ui_str_22), pensionHeadline, Icons.Default.Savings, "pension_calculator", true),
        CalculatorCardItem("mishloach", context.getString(R.string.ui_str_25), context.getString(R.string.ui_680_12_10), Icons.Default.CardGiftcard, null, false),
        CalculatorCardItem("investments", context.getString(R.string.ui_str_14), context.getString(R.string.ui_142_42500_16), Icons.Default.TrendingUp, null, false),
        CalculatorCardItem("apartment", context.getString(R.string.ui_str_11), context.getString(R.string.ui_385000_34), Icons.Default.Apartment, null, false),
        CalculatorCardItem("donations", context.getString(R.string.ui_str_32), context.getString(R.string.ui_2150_33), Icons.Default.VolunteerActivism, null, false),
        CalculatorCardItem("trip", context.getString(R.string.ui_str_24), context.getString(R.string.ui_4850_320_21, 4), Icons.Default.FlightTakeoff, null, false)
    )
}
