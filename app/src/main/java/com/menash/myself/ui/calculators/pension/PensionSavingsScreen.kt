package com.menash.myself.ui.calculators.pension

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
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
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
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.PensionFund
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.domain.util.CsvParser
import com.menash.myself.feature.calculators.pension.PensionCalculatorViewModel
import com.menash.myself.ui.common.CsvMappingDialog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PensionSavingsScreen(
    onBack: () -> Unit,
    isOffline: Boolean = false
) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val viewModel: PensionCalculatorViewModel = viewModel(
        factory = PensionCalculatorViewModel.factory(
            pensionFundRepository = appContainer.pensionFundRepository,
            errorReporter = appContainer.errorReporter
        )
    )
    val funds by viewModel.funds.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedTypeFilter by remember { mutableStateOf<String?>(null) }
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
            PensionSavingsTopBar(
                onBack = onBack,
                onImportCsv = { csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                onExportCsv = { sharePensionFundsCsv(context, funds) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.insertPensionFund(createDefaultPensionFund()) },
                modifier = Modifier.testTag("add_fund_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "הוסף קרן או קופה")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OfflineIndicator(isOffline = isOffline)
            PensionSavingsContent(
                uiState = uiState,
                selectedTypeFilter = selectedTypeFilter,
                onSelectType = { selectedTypeFilter = it },
                onRetry = { viewModel.clearError() },
                onAddFirst = { viewModel.insertPensionFund(createDefaultPensionFund()) },
                onUpdate = { viewModel.updatePensionFund(it) },
                onDelete = { viewModel.deletePensionFund(it) }
            )
        }
    }

    if (showCsvMapping && importedCsvRows.isNotEmpty()) {
        CsvMappingDialog(
            headers = importedCsvRows.first(),
            previewRows = importedCsvRows.drop(1),
            targetFields = listOf("סוג חיסכון", "גוף מנהל", "שווי נוכחי", "סך הפקדות", "דמי ניהול מצבירה (%)", "דמי ניהול מהפקדה (%)", "הערות"),
            onDismiss = { showCsvMapping = false },
            onConfirmMap = { mapping ->
                val newFunds = parseMappedPensionFunds(importedCsvRows.drop(1), mapping)
                viewModel.insertPensionFunds(newFunds)
                Toast.makeText(context, "יובאו ${newFunds.size} קופות וקרנות בהצלחה!", Toast.LENGTH_SHORT).show()
                showCsvMapping = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PensionSavingsTopBar(
    onBack: () -> Unit,
    onImportCsv: () -> Unit,
    onExportCsv: () -> Unit
) {
    TopAppBar(
        title = { Text("פנסיה וחיסכון", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("pension_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
            }
        },
        actions = {
            IconButton(onClick = onImportCsv, modifier = Modifier.testTag("pension_import_csv_button")) {
                Icon(Icons.Default.UploadFile, contentDescription = "ייבוא CSV")
            }
            IconButton(onClick = onExportCsv, modifier = Modifier.testTag("pension_export_csv_button")) {
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
private fun PensionSavingsContent(
    uiState: UiState<List<PensionFund>>,
    selectedTypeFilter: String?,
    onSelectType: (String?) -> Unit,
    onRetry: () -> Unit,
    onAddFirst: () -> Unit,
    onUpdate: (PensionFund) -> Unit,
    onDelete: (PensionFund) -> Unit
) {
    when (uiState) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(message = uiState.message, onRetry = onRetry)
        is UiState.Empty -> EmptyState(
            title = "אין קופות או קרנות רשומות",
            description = "הוסף קרן פנסיה או קרן השתלמות למעקב אחר צבירה ודמי ניהול.",
            icon = Icons.Default.Savings,
            actionLabel = "הוסף קרן או קופה",
            onActionClick = onAddFirst
        )
        is UiState.Content -> {
            val funds = uiState.data
            val totalValue = funds.sumOf { it.currentValue.shekels }
            val totalDeposits = funds.sumOf { it.totalDeposits.shekels }
            val totalProfit = totalValue - totalDeposits
            val totalProfitPercent = if (totalDeposits > 0.0) (totalProfit / totalDeposits) * 100.0 else 0.0
            val totalEstimatedYearlyFees = funds.sumOf { it.yearlyBalanceFeeEstimate.shekels }
            val maxBalanceFee = funds.maxOfOrNull { it.feeOnBalancePercent } ?: 0.0
            val maxDepositFee = funds.maxOfOrNull { it.feeOnDepositsPercent } ?: 0.0
            val highestBalanceFund = funds.firstOrNull { it.feeOnBalancePercent == maxBalanceFee && maxBalanceFee > 0.0 }
            val highestDepositFund = funds.firstOrNull { it.feeOnDepositsPercent == maxDepositFee && maxDepositFee > 0.0 }
            val fundTypes = listOf("הכל", "קרן פנסיה", "קרן השתלמות", "קופת גמל", "גמל להשקעה")
            val filteredFunds = remember(funds, selectedTypeFilter) {
                if (selectedTypeFilter == null || selectedTypeFilter == "הכל") funds
                else funds.filter { it.fundType == selectedTypeFilter }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                item {
                    PensionSummaryCard(
                        totalValue = totalValue,
                        totalDeposits = totalDeposits,
                        totalProfit = totalProfit,
                        totalProfitPercent = totalProfitPercent,
                        totalEstimatedYearlyFees = totalEstimatedYearlyFees
                    )
                }
                if (funds.isNotEmpty() && (maxBalanceFee > 0.0 || maxDepositFee > 0.0)) {
                    item {
                        HighFeesWarningBanner(
                            highestBalanceFund = highestBalanceFund,
                            maxBalanceFee = maxBalanceFee,
                            highestDepositFund = highestDepositFund,
                            maxDepositFee = maxDepositFee
                        )
                    }
                }
                item {
                    PensionFundTypeFilterRow(
                        fundTypes = fundTypes,
                        funds = funds,
                        selectedTypeFilter = selectedTypeFilter,
                        onSelectType = onSelectType
                    )
                }
                items(filteredFunds, key = { it.id }) { fund ->
                    val isHighestBalance = fund.feeOnBalancePercent == maxBalanceFee && maxBalanceFee > 0.0
                    val isHighestDeposit = fund.feeOnDepositsPercent == maxDepositFee && maxDepositFee > 0.0
                    PensionFundCard(
                        fund = fund,
                        isHighestBalance = isHighestBalance,
                        isHighestDeposit = isHighestDeposit,
                        onUpdate = onUpdate,
                        onDelete = { onDelete(fund) }
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

private fun createDefaultPensionFund(): PensionFund = PensionFund(
    fundType = "קרן השתלמות",
    provider = "חברה מנהלת",
    currentValue = Money.fromShekels(50000.0),
    totalDeposits = Money.fromShekels(40000.0),
    feeOnBalancePercent = 0.25,
    feeOnDepositsPercent = 0.0,
    notes = ""
)

private fun sharePensionFundsCsv(context: android.content.Context, funds: List<PensionFund>) {
    val headers = listOf("סוג חיסכון", "גוף מנהל", "שווי נוכחי (₪)", "סך הפקדות (₪)", "רווח (₪)", "תשואה (%)", "דמי ניהול מצבירה (%)", "דמי ניהול מהפקדה (%)", "הערות")
    val rows = funds.map {
        listOf(
            it.fundType, it.provider, it.currentValue.shekels.toInt().toString(),
            it.totalDeposits.shekels.toInt().toString(), it.profit.shekels.toInt().toString(),
            String.format(Locale.US, "%.2f", it.profitPercent) + "%",
            "${it.feeOnBalancePercent}%", "${it.feeOnDepositsPercent}%", it.notes
        )
    }
    val csv = CsvParser.exportToCsv(headers, rows)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_TEXT, csv)
        putExtra(Intent.EXTRA_SUBJECT, "פנסיה_וחיסכון.csv")
    }
    context.startActivity(Intent.createChooser(sendIntent, "שתף נתוני פנסיה וחיסכון"))
}

private fun parseMappedPensionFunds(
    dataRows: List<List<String>>,
    mapping: Map<String, Int>
): List<PensionFund> = dataRows.mapNotNull { row ->
    try {
        val type = row.getOrNull(mapping["סוג חיסכון"] ?: -1) ?: "קרן פנסיה"
        val provider = row.getOrNull(mapping["גוף מנהל"] ?: -1) ?: "גוף מנהל"
        val currentVal = row.getOrNull(mapping["שווי נוכחי"] ?: -1)?.toDoubleOrNull() ?: 50000.0
        val deposits = row.getOrNull(mapping["סך הפקדות"] ?: -1)?.toDoubleOrNull() ?: 40000.0
        val feeBalance = row.getOrNull(mapping["דמי ניהול מצבירה (%)"] ?: -1)?.toDoubleOrNull() ?: 0.2
        val feeDeposit = row.getOrNull(mapping["דמי ניהול מהפקדה (%)"] ?: -1)?.toDoubleOrNull() ?: 0.0
        val notes = row.getOrNull(mapping["הערות"] ?: -1) ?: ""

        PensionFund(
            fundType = type,
            provider = provider,
            currentValue = Money.fromShekels(currentVal),
            totalDeposits = Money.fromShekels(deposits),
            feeOnBalancePercent = feeBalance,
            feeOnDepositsPercent = feeDeposit,
            notes = notes
        )
    } catch (e: Exception) {
        null
    }
}
