package com.example.ui.calculators.ai

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.AppDatabase
import com.example.data.entities.AiSubscriptionEntity
import com.example.data.io.CsvHelper
import com.example.ui.common.CsvMappingDialog
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSubscriptionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).appDao() }

    val settings by dao.getSettingsFlow().collectAsState(initial = null)
    val usdRate = settings?.usdToIlsRate ?: 3.11

    val subscriptions by dao.getAllAiSubscriptionsFlow().collectAsState(initial = emptyList())
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    var showCsvMapping by remember { mutableStateOf(false) }
    var importedCsvRows by remember { mutableStateOf<List<List<String>>>(emptyList()) }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } ?: ""
            importedCsvRows = CsvHelper.parseCsv(content)
            if (importedCsvRows.isNotEmpty()) showCsvMapping = true
        }
    }

    // Calculations
    val activeSubs = subscriptions.filter { it.isActive }
    val totalMonthlyIls = activeSubs.sumOf { it.monthlyCostIls(usdRate) }
    val totalYearlyIls = activeSubs.sumOf { it.yearlyCostIls(usdRate) }

    val categories = remember(subscriptions) {
        subscriptions.map { it.category.ifBlank { "כללי" } }.distinct()
    }

    val filteredSubs = remember(subscriptions, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) subscriptions
        else subscriptions.filter { it.category == selectedCategoryFilter }
    }

    val fmt = NumberFormat.getNumberInstance(Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("מנויי AI וכלים דיגיטליים", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ai_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                        modifier = Modifier.testTag("ai_import_csv_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "ייבוא CSV")
                    }
                    IconButton(
                        onClick = {
                            val headers = listOf("שם כלי", "קטגוריה", "פעיל", "תדירות", "מטבע", "מחיר", "עלות חודשית בש״ח", "הערות")
                            val rows = subscriptions.map {
                                listOf(
                                    it.name,
                                    it.category,
                                    if (it.isActive) "פעיל" else "מבוטל",
                                    it.billingCycle,
                                    it.currency,
                                    it.price.toString(),
                                    it.monthlyCostIls(usdRate).toInt().toString(),
                                    it.notes
                                )
                            }
                            val csv = CsvHelper.exportToCsv(headers, rows)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_TEXT, csv)
                                putExtra(Intent.EXTRA_SUBJECT, "מנויי_AI.csv")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "שתף נתוני מנויי AI"))
                        },
                        modifier = Modifier.testTag("ai_export_csv_button")
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
                    scope.launch {
                        dao.insertAiSubscription(
                            AiSubscriptionEntity(
                                name = "כלי AI חדש",
                                category = "פרודוקטיביות",
                                isActive = true,
                                billingCycle = "MONTHLY",
                                currency = "USD",
                                price = 20.0,
                                notes = ""
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("add_ai_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "הוסף מנוי")
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

            // Summary Cards (Monthly spend, Yearly spend, Active count, USD rate)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("הוצאה חודשית פעילה", fontSize = 11.sp, color = Color.DarkGray)
                                Text("₪${fmt.format(totalMonthlyIls.toInt())}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                Text("${activeSubs.size} מתוך ${subscriptions.size} מנויים פעילים", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("הוצאה שנתית מצטברת", fontSize = 11.sp, color = Color.DarkGray)
                                Text("₪${fmt.format(totalYearlyIls.toInt())}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
                                Text("שער מחושב: ₪$usdRate ל-$", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Categories filter chips & category spend preview
            if (categories.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("הכל (${subscriptions.size})") }
                            )
                        }
                        items(categories) { cat ->
                            val catSum = activeSubs.filter { it.category == cat }.sumOf { it.monthlyCostIls(usdRate) }
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat },
                                label = { Text("$cat (₪${catSum.toInt()})") }
                            )
                        }
                    }
                }
            }

            // Items list with inline editing
            items(filteredSubs, key = { it.id }) { sub ->
                AiSubscriptionCard(
                    sub = sub,
                    usdRate = usdRate,
                    onUpdate = { updated -> scope.launch { dao.updateAiSubscription(updated) } },
                    onDelete = { scope.launch { dao.deleteAiSubscription(sub) } }
                )
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showCsvMapping && importedCsvRows.isNotEmpty()) {
        CsvMappingDialog(
            headers = importedCsvRows.first(),
            previewRows = importedCsvRows.drop(1),
            targetFields = listOf("שם כלי", "קטגוריה", "מחיר", "מטבע (USD/ILS)", "תדירות (חודשי/שנתי)", "הערות"),
            onDismiss = { showCsvMapping = false },
            onConfirmMap = { mapping ->
                scope.launch {
                    val dataRows = importedCsvRows.drop(1)
                    val newSubs = dataRows.mapNotNull { row ->
                        try {
                            val name = row.getOrNull(mapping["שם כלי"] ?: -1) ?: "כלי מיובא"
                            val category = row.getOrNull(mapping["קטגוריה"] ?: -1) ?: "כללי"
                            val price = row.getOrNull(mapping["מחיר"] ?: -1)?.toDoubleOrNull() ?: 15.0
                            val currencyRaw = row.getOrNull(mapping["מטבע (USD/ILS)"] ?: -1)?.uppercase() ?: "USD"
                            val currency = if (currencyRaw.contains("ILS") || currencyRaw.contains("₪") || currencyRaw.contains("שח")) "ILS" else "USD"
                            val cycleRaw = row.getOrNull(mapping["תדירות (חודשי/שנתי)"] ?: -1)?.uppercase() ?: "MONTHLY"
                            val cycle = if (cycleRaw.contains("YEAR") || cycleRaw.contains("שנתי")) "YEARLY" else "MONTHLY"
                            val notes = row.getOrNull(mapping["הערות"] ?: -1) ?: ""

                            AiSubscriptionEntity(
                                name = name,
                                category = category,
                                isActive = true,
                                billingCycle = cycle,
                                currency = currency,
                                price = price,
                                notes = notes
                            )
                        } catch (e: Exception) { null }
                    }
                    dao.insertAiSubscriptions(newSubs)
                    Toast.makeText(context, "יובאו ${newSubs.size} מנויים בהצלחה!", Toast.LENGTH_SHORT).show()
                    showCsvMapping = false
                }
            }
        )
    }
}

@Composable
fun AiSubscriptionCard(
    sub: AiSubscriptionEntity,
    usdRate: Double,
    onUpdate: (AiSubscriptionEntity) -> Unit,
    onDelete: () -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val monthlyIls = sub.monthlyCostIls(usdRate)
    val yearlyIls = sub.yearlyCostIls(usdRate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_card_${sub.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (sub.isActive) Color.White else Color(0xFFF8FAFC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (sub.isActive) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Name, Active Toggle, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = sub.name,
                    onValueChange = { onUpdate(sub.copy(name = it)) },
                    label = { Text("שם הכלי") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = sub.isActive,
                    onClick = { onUpdate(sub.copy(isActive = !sub.isActive)) },
                    label = { Text(if (sub.isActive) "פעיל ✓" else "מבוטל ✕") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFD1FAE5),
                        selectedLabelColor = Color(0xFF065F46)
                    )
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "מחק מנוי", tint = Color.Gray)
                }
            }

            // Calculated Spend Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (sub.isActive) Color(0xFFE2EDF8) else Color(0xFFF1F5F9),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("עלות חודשית בש״ח", fontSize = 11.sp, color = Color.DarkGray)
                    Text(
                        "₪${fmt.format(monthlyIls.toInt())}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (sub.isActive) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("עלות שנתית", fontSize = 11.sp, color = Color.DarkGray)
                    Text(
                        "₪${fmt.format(yearlyIls.toInt())}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.DarkGray
                    )
                }
            }

            HorizontalDivider()

            // Price, Currency Switch, Billing Frequency Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = sub.price.toString(),
                    onValueChange = { onUpdate(sub.copy(price = it.toDoubleOrNull() ?: sub.price)) },
                    label = { Text("מחיר נקוב") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )

                // Currency button toggle
                OutlinedButton(
                    onClick = {
                        val next = if (sub.currency == "USD") "ILS" else "USD"
                        onUpdate(sub.copy(currency = next))
                    },
                    modifier = Modifier.weight(0.9f).padding(top = 8.dp)
                ) {
                    Text(if (sub.currency == "USD") "$ דולר" else "₪ שקל", fontSize = 12.sp)
                }

                // Billing cycle button toggle
                OutlinedButton(
                    onClick = {
                        val next = if (sub.billingCycle == "MONTHLY") "YEARLY" else "MONTHLY"
                        onUpdate(sub.copy(billingCycle = next))
                    },
                    modifier = Modifier.weight(0.9f).padding(top = 8.dp)
                ) {
                    Text(if (sub.billingCycle == "MONTHLY") "חודשי" else "שנתי", fontSize = 12.sp)
                }
            }

            // Category & Notes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = sub.category,
                    onValueChange = { onUpdate(sub.copy(category = it)) },
                    label = { Text("קטגוריה") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = sub.notes,
                    onValueChange = { onUpdate(sub.copy(notes = it)) },
                    label = { Text("הערות") },
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}
