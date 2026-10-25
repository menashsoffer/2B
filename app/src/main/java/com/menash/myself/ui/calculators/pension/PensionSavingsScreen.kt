package com.menash.myself.ui.calculators.pension

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.data.entities.PensionFundEntity
import com.menash.myself.data.io.CsvHelper
import com.menash.myself.feature.calculators.pension.PensionCalculatorViewModel
import com.menash.myself.ui.common.CsvMappingDialog
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PensionSavingsScreen(
    onBack: () -> Unit,
    appContainer: com.menash.myself.core.di.AppContainer = LocalAppContainer.current,
    viewModel: PensionCalculatorViewModel = viewModel(
        factory = PensionCalculatorViewModel.factory(appContainer.pensionFundRepository)
    )
) {
    val context = LocalContext.current
    val funds by viewModel.funds.collectAsStateWithLifecycle()

    var selectedTypeFilter by remember { mutableStateOf<String?>(null) }
    var showCsvMapping by remember { mutableStateOf(false) }
    var importedCsvRows by remember { mutableStateOf<List<List<String>>>(emptyList()) }

    // CSV File Picker
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } ?: ""
            importedCsvRows = CsvHelper.parseCsv(content)
            if (importedCsvRows.isNotEmpty()) showCsvMapping = true
        }
    }

    // Totals calculations
    val totalValue = funds.sumOf { it.currentValue }
    val totalDeposits = funds.sumOf { it.totalDeposits }
    val totalProfit = totalValue - totalDeposits
    val totalProfitPercent = if (totalDeposits > 0.0) (totalProfit / totalDeposits) * 100.0 else 0.0
    val totalEstimatedYearlyFees = funds.sumOf { it.yearlyBalanceFeeEstimate }

    // Highest fees tracking
    val maxBalanceFee = funds.maxOfOrNull { it.feeOnBalancePercent } ?: 0.0
    val maxDepositFee = funds.maxOfOrNull { it.feeOnDepositsPercent } ?: 0.0
    val highestBalanceFund = funds.firstOrNull { it.feeOnBalancePercent == maxBalanceFee && maxBalanceFee > 0.0 }
    val highestDepositFund = funds.firstOrNull { it.feeOnDepositsPercent == maxDepositFee && maxDepositFee > 0.0 }

    val fundTypes = listOf("הכל", "קרן פנסיה", "קרן השתלמות", "קופת גמל", "גמל להשקעה")

    val filteredFunds = remember(funds, selectedTypeFilter) {
        if (selectedTypeFilter == null || selectedTypeFilter == "הכל") funds
        else funds.filter { it.fundType == selectedTypeFilter }
    }

    val fmt = NumberFormat.getNumberInstance(Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("פנסיה וחיסכון", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("pension_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                        modifier = Modifier.testTag("pension_import_csv_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "ייבוא CSV")
                    }
                    IconButton(
                        onClick = {
                            val headers = listOf("סוג חיסכון", "גוף מנהל", "שווי נוכחי (₪)", "סך הפקדות (₪)", "רווח (₪)", "תשואה (%)", "דמי ניהול מצבירה (%)", "דמי ניהול מהפקדה (%)", "הערות")
                            val rows = funds.map {
                                listOf(
                                    it.fundType,
                                    it.provider,
                                    it.currentValue.toInt().toString(),
                                    it.totalDeposits.toInt().toString(),
                                    it.profit.toInt().toString(),
                                    String.format(Locale.US, "%.2f", it.profitPercent) + "%",
                                    it.feeOnBalancePercent.toString() + "%",
                                    it.feeOnDepositsPercent.toString() + "%",
                                    it.notes
                                )
                            }
                            val csv = CsvHelper.exportToCsv(headers, rows)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_TEXT, csv)
                                putExtra(Intent.EXTRA_SUBJECT, "פנסיה_וחיסכון.csv")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "שתף נתוני פנסיה וחיסכון"))
                        },
                        modifier = Modifier.testTag("pension_export_csv_button")
                    ) {
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.insertPensionFund(
                        PensionFundEntity(
                            fundType = "קרן השתלמות",
                            provider = "חברה מנהלת",
                            currentValue = 50000.0,
                            totalDeposits = 40000.0,
                            feeOnBalancePercent = 0.25,
                            feeOnDepositsPercent = 0.0,
                            notes = ""
                        )
                    )
                },
                modifier = Modifier.testTag("add_fund_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "הוסף קרן או קופה")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Summary Totals Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("סך שווי נכסים כולל", fontSize = 12.sp, color = Color.DarkGray)
                                Text(
                                    "₪${fmt.format(totalValue.toInt())}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("סך הפקדות", fontSize = 12.sp, color = Color.DarkGray)
                                Text(
                                    "₪${fmt.format(totalDeposits.toInt())}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("סך רווח / תשואה מצטברת", fontSize = 11.sp, color = Color.DarkGray)
                                val isProfitable = totalProfit >= 0
                                Text(
                                    "${if (isProfitable) "+" else ""}₪${fmt.format(totalProfit.toInt())} (${String.format(Locale.US, "%.1f", totalProfitPercent)}%)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isProfitable) Color(0xFF047857) else Color(0xFFDC2626)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("דמי ניהול שנתיים משוערים", fontSize = 11.sp, color = Color.DarkGray)
                                Text(
                                    "~₪${fmt.format(totalEstimatedYearlyFees.toInt())} / שנה",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }

            // High Fees Highlight Banner
            if (funds.isNotEmpty() && (maxBalanceFee > 0.0 || maxDepositFee > 0.0)) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("high_fees_warning_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFECACA)))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "התראת דמי ניהול",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(24.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "מעקב ומיקוד: דמי הניהול הגבוהים ביותר בתיק",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF991B1B)
                                )
                                if (highestBalanceFund != null && maxBalanceFee > 0) {
                                    Text(
                                        "• שיא צבירה: ${highestBalanceFund.feeOnBalancePercent}% ב-${highestBalanceFund.provider} (${highestBalanceFund.fundType})",
                                        fontSize = 12.sp,
                                        color = Color(0xFF7F1D1D),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (highestDepositFund != null && maxDepositFee > 0) {
                                    Text(
                                        "• שיא הפקדה: ${highestDepositFund.feeOnDepositsPercent}% ב-${highestDepositFund.provider} (${highestDepositFund.fundType})",
                                        fontSize = 12.sp,
                                        color = Color(0xFF7F1D1D),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    "טיפ: פנייה לגוף המנהל או לסוכן יכולה לחסוך אלפי שקלים לאורך השנים!",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }

            // Fund Type Filters
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(fundTypes) { type ->
                        val count = if (type == "הכל") funds.size else funds.count { it.fundType == type }
                        FilterChip(
                            selected = (selectedTypeFilter == null && type == "הכל") || selectedTypeFilter == type,
                            onClick = { selectedTypeFilter = if (type == "הכל") null else type },
                            label = { Text("$type ($count)") }
                        )
                    }
                }
            }

            // Funds List with Inline Editing
            items(filteredFunds, key = { it.id }) { fund ->
                val isHighestBalance = fund.feeOnBalancePercent == maxBalanceFee && maxBalanceFee > 0.0
                val isHighestDeposit = fund.feeOnDepositsPercent == maxDepositFee && maxDepositFee > 0.0

                PensionFundCard(
                    fund = fund,
                    isHighestBalance = isHighestBalance,
                    isHighestDeposit = isHighestDeposit,
                    onUpdate = { updated -> viewModel.updatePensionFund(updated) },
                    onDelete = { viewModel.deletePensionFund(fund) }
                )
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    // CSV Mapping Dialog
    if (showCsvMapping && importedCsvRows.isNotEmpty()) {
        CsvMappingDialog(
            headers = importedCsvRows.first(),
            previewRows = importedCsvRows.drop(1),
            targetFields = listOf("סוג חיסכון", "גוף מנהל", "שווי נוכחי", "סך הפקדות", "דמי ניהול מצבירה (%)", "דמי ניהול מהפקדה (%)", "הערות"),
            onDismiss = { showCsvMapping = false },
            onConfirmMap = { mapping ->
                val dataRows = importedCsvRows.drop(1)
                val newFunds = dataRows.mapNotNull { row ->
                    try {
                        val type = row.getOrNull(mapping["סוג חיסכון"] ?: -1) ?: "קרן פנסיה"
                        val provider = row.getOrNull(mapping["גוף מנהל"] ?: -1) ?: "גוף מנהל"
                        val currentVal = row.getOrNull(mapping["שווי נוכחי"] ?: -1)?.toDoubleOrNull() ?: 50000.0
                        val deposits = row.getOrNull(mapping["סך הפקדות"] ?: -1)?.toDoubleOrNull() ?: 40000.0
                        val feeBalance = row.getOrNull(mapping["דמי ניהול מצבירה (%)"] ?: -1)?.toDoubleOrNull() ?: 0.2
                        val feeDeposit = row.getOrNull(mapping["דמי ניהול מהפקדה (%)"] ?: -1)?.toDoubleOrNull() ?: 0.0
                        val notes = row.getOrNull(mapping["הערות"] ?: -1) ?: ""

                        PensionFundEntity(
                            fundType = type,
                            provider = provider,
                            currentValue = currentVal,
                            totalDeposits = deposits,
                            feeOnBalancePercent = feeBalance,
                            feeOnDepositsPercent = feeDeposit,
                            notes = notes
                        )
                    } catch (e: Exception) { null }
                }
                viewModel.insertPensionFunds(newFunds)
                Toast.makeText(context, "יובאו ${newFunds.size} קופות וקרנות בהצלחה!", Toast.LENGTH_SHORT).show()
                showCsvMapping = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PensionFundCard(
    fund: PensionFundEntity,
    isHighestBalance: Boolean,
    isHighestDeposit: Boolean,
    onUpdate: (PensionFundEntity) -> Unit,
    onDelete: () -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val isProfit = fund.profit >= 0

    val fundTypes = listOf("קרן פנסיה", "קרן השתלמות", "קופת גמל", "גמל להשקעה")
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pension_card_${fund.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighestBalance || isHighestDeposit) Color(0xFFFFFDFD) else Color.White
        ),
        border = if (isHighestBalance || isHighestDeposit) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFCA5A5))) else CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Fund Type dropdown + Provider Name + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type selector
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                    modifier = Modifier.weight(1.1f)
                ) {
                    OutlinedTextField(
                        value = fund.fundType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("סוג קרן") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier.menuAnchor(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        fundTypes.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    onUpdate(fund.copy(fundType = t))
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Provider Name
                OutlinedTextField(
                    value = fund.provider,
                    onValueChange = { onUpdate(fund.copy(provider = it)) },
                    label = { Text("גוף מנהל") },
                    modifier = Modifier.weight(1.2f),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                )

                IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_fund_${fund.id}")) {
                    Icon(Icons.Default.Delete, contentDescription = "מחק קרן", tint = Color.Gray)
                }
            }

            // High Fee Badges (if applicable)
            if (isHighestBalance || isHighestDeposit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isHighestBalance) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("⚠️ דמי ניהול מצבירה הגבוהים ביותר (${fund.feeOnBalancePercent}%)", fontSize = 11.sp, color = Color(0xFF991B1B)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFFEE2E2))
                        )
                    }
                    if (isHighestDeposit) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("⚠️ דמי ניהול מהפקדה הגבוהים ביותר (${fund.feeOnDepositsPercent}%)", fontSize = 11.sp, color = Color(0xFF991B1B)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFFEF3C7))
                        )
                    }
                }
            }

            // Computed Profit & Metrics Display Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("רווח נקי מצטבר", fontSize = 11.sp, color = Color.DarkGray)
                    Text(
                        "${if (isProfit) "+" else ""}₪${fmt.format(fund.profit.toInt())}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isProfit) Color(0xFF059669) else Color(0xFFDC2626)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("תשואה כוללת", fontSize = 11.sp, color = Color.DarkGray)
                    Text(
                        "${if (isProfit) "+" else ""}${String.format(Locale.US, "%.1f", fund.profitPercent)}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isProfit) Color(0xFF059669) else Color(0xFFDC2626)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("עלות דמי ניהול צבירה", fontSize = 11.sp, color = Color.DarkGray)
                    Text(
                        "~₪${fmt.format(fund.yearlyBalanceFeeEstimate.toInt())}/שנה",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
            }

            HorizontalDivider()

            // Inline Input Fields: Current Value & Total Deposits
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fund.currentValue.toInt().toString(),
                    onValueChange = { onUpdate(fund.copy(currentValue = it.toDoubleOrNull() ?: fund.currentValue)) },
                    label = { Text("שווי נוכחי (₪)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fund.totalDeposits.toInt().toString(),
                    onValueChange = { onUpdate(fund.copy(totalDeposits = it.toDoubleOrNull() ?: fund.totalDeposits)) },
                    label = { Text("סך הפקדות (₪)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            // Inline Input Fields: Fee on Balance % & Fee on Deposits %
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fund.feeOnBalancePercent.toString(),
                    onValueChange = { onUpdate(fund.copy(feeOnBalancePercent = it.toDoubleOrNull() ?: fund.feeOnBalancePercent)) },
                    label = { Text("דמי ניהול מצבירה (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fund.feeOnDepositsPercent.toString(),
                    onValueChange = { onUpdate(fund.copy(feeOnDepositsPercent = it.toDoubleOrNull() ?: fund.feeOnDepositsPercent)) },
                    label = { Text("דמי ניהול מהפקדה (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            // Notes / Investment Track
            OutlinedTextField(
                value = fund.notes,
                onValueChange = { onUpdate(fund.copy(notes = it)) },
                label = { Text("הערות ומסלול השקעה (לדוגמה: מחקה S&P 500, כללי)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
