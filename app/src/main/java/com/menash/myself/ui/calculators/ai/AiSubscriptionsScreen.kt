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
                onImportCsv = { csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                onExportCsv = { shareAiSubscriptionsCsv(context, subscriptions, usdRate) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.insertAiSubscription(createDefaultAiSub()) },
                modifier = Modifier.testTag("add_ai_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "הוסף מנוי")
            }
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
                onAddFirst = { viewModel.insertAiSubscription(createDefaultAiSub()) },
                onUpdate = { viewModel.updateAiSubscription(it) },
                onDelete = { viewModel.deleteAiSubscription(it) }
            )
        }
    }

    if (showCsvMapping && importedCsvRows.isNotEmpty()) {
        CsvMappingDialog(
            headers = importedCsvRows.first(),
            previewRows = importedCsvRows.drop(1),
            targetFields = listOf("שם כלי", "קטגוריה", "מחיר", "מטבע (USD/ILS)", "תדירות (חודשי/שנתי)", "הערות"),
            onDismiss = { showCsvMapping = false },
            onConfirmMap = { mapping ->
                val newSubs = parseMappedAiSubs(importedCsvRows.drop(1), mapping)
                viewModel.insertAiSubscriptions(newSubs)
                Toast.makeText(context, "יובאו ${newSubs.size} מנויים בהצלחה!", Toast.LENGTH_SHORT).show()
                showCsvMapping = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSubscriptionsTopBar(
    onBack: () -> Unit,
    onImportCsv: () -> Unit,
    onExportCsv: () -> Unit
) {
    TopAppBar(
        title = { Text("מנויי AI וכלים דיגיטליים", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("ai_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
            }
        },
        actions = {
            IconButton(onClick = onImportCsv, modifier = Modifier.testTag("ai_import_csv_button")) {
                Icon(Icons.Default.UploadFile, contentDescription = "ייבוא CSV")
            }
            IconButton(onClick = onExportCsv, modifier = Modifier.testTag("ai_export_csv_button")) {
                Icon(Icons.Default.Share, contentDescription = "ייצוא CSV")
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
            title = "אין מנויי AI רשומים",
            description = "הוסף מנוי כדי לעקוב אחר ההוצאה החודשית והשנתית בשקלים.",
            icon = Icons.Default.SmartToy,
            actionLabel = "הוסף מנוי",
            onActionClick = onAddFirst
        )
        is UiState.Content -> {
            val subscriptions = uiState.data
            val activeSubs = subscriptions.filter { it.isActive }
            val totalMonthlyIls = activeSubs.sumOf { it.monthlyCostIls(usdRate).shekels }
            val totalYearlyIls = activeSubs.sumOf { it.yearlyCostIls(usdRate).shekels }
            val categories = remember(subscriptions) {
                subscriptions.map { it.category.ifBlank { "כללי" } }.distinct()
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

private fun createDefaultAiSub(): AiSubscription = AiSubscription(
    name = "כלי AI חדש",
    category = "פרודוקטיביות",
    isActive = true,
    billingCycle = "MONTHLY",
    currency = "USD",
    price = Money.fromShekels(20.0),
    notes = ""
)

private fun shareAiSubscriptionsCsv(
    context: android.content.Context,
    subscriptions: List<AiSubscription>,
    usdRate: Double
) {
    val headers = listOf("שם כלי", "קטגוריה", "פעיל", "תדירות", "מטבע", "מחיר", "עלות חודשית בש״ח", "הערות")
    val rows = subscriptions.map {
        listOf(
            it.name,
            it.category,
            if (it.isActive) "פעיל" else "מבוטל",
            it.billingCycle,
            it.currency,
            it.price.shekels.toString(),
            it.monthlyCostIls(usdRate).shekels.toInt().toString(),
            it.notes
        )
    }
    val csv = CsvParser.exportToCsv(headers, rows)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_TEXT, csv)
        putExtra(Intent.EXTRA_SUBJECT, "מנויי_AI.csv")
    }
    context.startActivity(Intent.createChooser(sendIntent, "שתף נתוני מנויי AI"))
}

private fun parseMappedAiSubs(
    dataRows: List<List<String>>,
    mapping: Map<String, Int>
): List<AiSubscription> = dataRows.mapNotNull { row ->
    try {
        val name = row.getOrNull(mapping["שם כלי"] ?: -1) ?: "כלי מיובא"
        val category = row.getOrNull(mapping["קטגוריה"] ?: -1) ?: "כללי"
        val price = row.getOrNull(mapping["מחיר"] ?: -1)?.toDoubleOrNull() ?: 15.0
        val currencyRaw = row.getOrNull(mapping["מטבע (USD/ILS)"] ?: -1)?.uppercase() ?: "USD"
        val currency = if (currencyRaw.contains("ILS") || currencyRaw.contains("₪") || currencyRaw.contains("שח")) "ILS" else "USD"
        val cycleRaw = row.getOrNull(mapping["תדירות (חודשי/שנתי)"] ?: -1)?.uppercase() ?: "MONTHLY"
        val cycle = if (cycleRaw.contains("YEAR") || cycleRaw.contains("שנתי")) "YEARLY" else "MONTHLY"
        val notes = row.getOrNull(mapping["הערות"] ?: -1) ?: ""

        AiSubscription(
            name = name,
            category = category,
            isActive = true,
            billingCycle = cycle,
            currency = currency,
            price = Money.fromShekels(price),
            notes = notes
        )
    } catch (e: Exception) {
        null
    }
}
