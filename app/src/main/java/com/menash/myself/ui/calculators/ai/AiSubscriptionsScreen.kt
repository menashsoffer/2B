package com.menash.myself.ui.calculators.ai

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.UploadFile
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
import com.menash.myself.core.designsystem.component.EmptyState
import com.menash.myself.core.designsystem.component.ErrorState
import com.menash.myself.core.designsystem.component.LoadingState
import com.menash.myself.core.designsystem.component.OfflineIndicator
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.core.domain.model.AiSubscription
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.domain.util.CsvParser
import com.menash.myself.feature.calculators.ai.AiCalculatorViewModel
import com.menash.myself.ui.common.CsvMappingDialog

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSubscriptionsScreen(
    onBack: () -> Unit,
    isOffline: Boolean = false
) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val viewModel: AiCalculatorViewModel = viewModel(
        factory = AiCalculatorViewModel.factory(
            aiSubscriptionRepository = appContainer.aiSubscriptionRepository,
            settingsRepository = appContainer.settingsRepository,
            errorReporter = appContainer.errorReporter
        )
    )

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val usdRate = settings?.usdToIlsRate ?: 3.11
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var showCsvMapping by remember { mutableStateOf(false) }
    var importedCsvRows by remember { mutableStateOf<List<List<String>>>(emptyList()) }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } ?: ""
            importedCsvRows = CsvParser.parseCsv(content)
            if (importedCsvRows.isNotEmpty()) showCsvMapping = true
        }
    }

    Scaffold(
        topBar = {
            AiSubscriptionsTopBar(
                onBack = onBack,
                onAddSubscription = { viewModel.insertAiSubscription(AiSubscriptionCsvHelpers.createDefaultAiSub(context)) },
                onImportCsv = { csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                onExportCsv = { AiSubscriptionCsvHelpers.shareAiSubscriptionsCsv(context, subscriptions.orEmpty(), usdRate) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OfflineIndicator(isOffline = isOffline)
            AiSubscriptionsContent(
                uiState = uiState,
                usdRate = usdRate,
                selectedCategoryFilter = selectedCategoryFilter,
                onSelectCategory = { selectedCategoryFilter = it },
                onRetry = { viewModel.clearError() },
                onAddFirst = { viewModel.insertAiSubscription(AiSubscriptionCsvHelpers.createDefaultAiSub(context)) },
                onUpdate = { viewModel.updateAiSubscription(it) },
                onDelete = { viewModel.deleteAiSubscription(it) }
            )
        }
    }

    if (showCsvMapping && importedCsvRows.isNotEmpty()) {
        CsvMappingDialog(
            headers = importedCsvRows.first(),
            previewRows = importedCsvRows.drop(1),
            targetFields = listOf(stringResource(R.string.ui_str_164), stringResource(R.string.ui_str_150), stringResource(R.string.ui_str_161), stringResource(R.string.ui_usdils_159), stringResource(R.string.ui_str_165), stringResource(R.string.ui_str_121)),
            onDismiss = { showCsvMapping = false },
            onConfirmMap = { mapping ->
                val newSubs = AiSubscriptionCsvHelpers.parseMappedAiSubs(context, importedCsvRows.drop(1), mapping)
                viewModel.insertAiSubscriptions(newSubs)
                Toast.makeText(context, context.getString(R.string.ui_newsubssize_168, newSubs.size), Toast.LENGTH_SHORT).show()
                showCsvMapping = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSubscriptionsTopBar(
    onBack: () -> Unit,
    onAddSubscription: () -> Unit,
    onImportCsv: () -> Unit,
    onExportCsv: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.ui_ai_169), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("ai_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ui_str_60))
            }
        },
        actions = {
            IconButton(onClick = onAddSubscription, modifier = Modifier.testTag("add_ai_top_action")) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ui_str_163))
            }
            IconButton(onClick = onImportCsv, modifier = Modifier.testTag("ai_import_csv_button")) {
                Icon(Icons.Default.UploadFile, contentDescription = stringResource(R.string.ui_csv_72))
            }
            IconButton(onClick = onExportCsv, modifier = Modifier.testTag("ai_export_csv_button")) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.ui_csv_64))
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

@Composable
private fun AiSubscriptionsContent(
    uiState: UiState<List<AiSubscription>>,
    usdRate: Double,
    selectedCategoryFilter: String?,
    onSelectCategory: (String?) -> Unit,
    onRetry: () -> Unit,
    onAddFirst: () -> Unit,
    onUpdate: (AiSubscription) -> Unit,
    onDelete: (AiSubscription) -> Unit
) {
    when (uiState) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(message = uiState.message, onRetry = onRetry)
        is UiState.Empty -> EmptyState(
            title = stringResource(R.string.ui_ai_170),
            description = stringResource(R.string.ui_str_154),
            icon = Icons.Default.SmartToy,
            actionLabel = stringResource(R.string.ui_str_163),
            onActionClick = onAddFirst
        )
        is UiState.Content -> {
            val subscriptions = uiState.data
            val activeSubs = subscriptions.filter { it.isActive }
            val totalMonthlyIls = activeSubs.sumOf { it.monthlyCostIls(usdRate).shekels }
            val totalYearlyIls = activeSubs.sumOf { it.yearlyCostIls(usdRate).shekels }
            val fallbackCat = stringResource(R.string.ui_str_162)
            val categories = remember(subscriptions) {
                subscriptions.map { it.category.ifBlank { fallbackCat } }.distinct()
            }
            val filteredSubs = remember(subscriptions, selectedCategoryFilter) {
                if (selectedCategoryFilter == null) subscriptions
                else subscriptions.filter { it.category == selectedCategoryFilter }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                item {
                    AiSubscriptionsSummarySection(
                        totalMonthlyIls = totalMonthlyIls,
                        totalYearlyIls = totalYearlyIls,
                        activeCount = activeSubs.size,
                        totalCount = subscriptions.size,
                        usdRate = usdRate
                    )
                }
                if (categories.isNotEmpty()) {
                    item {
                        AiCategoryFilterRow(
                            categories = categories,
                            subscriptions = subscriptions,
                            activeSubs = activeSubs,
                            usdRate = usdRate,
                            selectedCategoryFilter = selectedCategoryFilter,
                            onSelectCategory = onSelectCategory
                        )
                    }
                }
                items(filteredSubs, key = { it.id }) { sub ->
                    AiSubscriptionCard(
                        sub = sub,
                        usdRate = usdRate,
                        onUpdate = onUpdate,
                        onDelete = { onDelete(sub) }
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}


