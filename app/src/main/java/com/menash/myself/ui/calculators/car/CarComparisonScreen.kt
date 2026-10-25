package com.menash.myself.ui.calculators.car

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.data.AppDatabase
import com.menash.myself.data.entities.CarEntity
import com.menash.myself.data.io.CsvHelper
import com.menash.myself.ui.common.CsvMappingDialog
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarComparisonScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).appDao() }
    val cars by dao.getAllCarsFlow().collectAsState(initial = emptyList())

    val baselineCar = cars.firstOrNull { it.isBaseline } ?: cars.firstOrNull()

    var editingEnergyForCar by remember { mutableStateOf<CarEntity?>(null) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("השוואת רכבים — עלות 5 שנים", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("car_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                        modifier = Modifier.testTag("car_import_csv_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "ייבוא CSV")
                    }
                    IconButton(
                        onClick = {
                            val headers = listOf("דגם רכב", "מחיר קנייה", "מכירה (5 שנים)", "אנרגיה שנתית", "ביטוח שנתי", "איתוראן", "טיפולים", "טסט", "צמיגים ל-5 שנים", "עלות חודשית נטו")
                            val rows = cars.map {
                                listOf(
                                    it.name,
                                    it.purchasePrice.toInt().toString(),
                                    it.resaleValueAfter5Years.toInt().toString(),
                                    it.energyCostYearly.toInt().toString(),
                                    it.insuranceYearly.toInt().toString(),
                                    it.trackerYearly.toInt().toString(),
                                    it.maintenanceYearly.toInt().toString(),
                                    it.annualTestYearly.toInt().toString(),
                                    it.tires5YearsTotal.toInt().toString(),
                                    it.netMonthlyCost.toInt().toString()
                                )
                            }
                            val csv = CsvHelper.exportToCsv(headers, rows)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_TEXT, csv)
                                putExtra(Intent.EXTRA_SUBJECT, "השוואת רכבים.csv")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "שתף נתוני רכבים ב-CSV"))
                        },
                        modifier = Modifier.testTag("car_export_csv_button")
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
                        dao.insertCar(
                            CarEntity(
                                name = "רכב חדש ${cars.size + 1}",
                                purchasePrice = 140000.0,
                                resaleValueAfter5Years = 80000.0,
                                energyCostYearly = 7000.0,
                                insuranceYearly = 4500.0,
                                trackerYearly = 600.0,
                                maintenanceYearly = 1600.0,
                                annualTestYearly = 1800.0,
                                tires5YearsTotal = 2200.0,
                                isBaseline = cars.isEmpty()
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("add_car_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "הוסף רכב")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // Visual Bar Chart comparison
            if (cars.isNotEmpty()) {
                item {
                    CarBarChartCard(cars = cars)
                }
            }

            // Cards list with inline editing
            items(cars, key = { it.id }) { car ->
                CarCard(
                    car = car,
                    baselineCar = baselineCar,
                    onUpdate = { updated -> scope.launch { dao.updateCar(updated) } },
                    onDelete = { scope.launch { dao.deleteCar(car) } },
                    onSetBaseline = {
                        scope.launch {
                            cars.forEach { c ->
                                dao.updateCar(c.copy(isBaseline = c.id == car.id))
                            }
                        }
                    },
                    onOpenEnergyHelper = { editingEnergyForCar = car }
                )
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    // Energy Helper Dialog
    editingEnergyForCar?.let { car ->
        EnergyHelperDialog(
            initialEnergy = car.energyCostYearly,
            onDismiss = { editingEnergyForCar = null },
            onApply = { calculated ->
                scope.launch {
                    dao.updateCar(car.copy(energyCostYearly = calculated))
                }
                editingEnergyForCar = null
            }
        )
    }

    // CSV Mapping Dialog
    if (showCsvMapping && importedCsvRows.isNotEmpty()) {
        CsvMappingDialog(
            headers = importedCsvRows.first(),
            previewRows = importedCsvRows.drop(1),
            targetFields = listOf("דגם רכב", "מחיר קנייה", "מכירה (5 שנים)", "אנרגיה שנתית", "ביטוח שנתי", "טיפולים", "צמיגים ל-5 שנים"),
            onDismiss = { showCsvMapping = false },
            onConfirmMap = { mapping ->
                scope.launch {
                    val dataRows = importedCsvRows.drop(1)
                    val newCars = dataRows.mapNotNull { row ->
                        try {
                            val name = row.getOrNull(mapping["דגם רכב"] ?: -1) ?: "רכב מיובא"
                            val purchase = row.getOrNull(mapping["מחיר קנייה"] ?: -1)?.toDoubleOrNull() ?: 120000.0
                            val resale = row.getOrNull(mapping["מכירה (5 שנים)"] ?: -1)?.toDoubleOrNull() ?: 60000.0
                            val energy = row.getOrNull(mapping["אנרגיה שנתית"] ?: -1)?.toDoubleOrNull() ?: 7000.0
                            val insurance = row.getOrNull(mapping["ביטוח שנתי"] ?: -1)?.toDoubleOrNull() ?: 4500.0
                            val maintenance = row.getOrNull(mapping["טיפולים"] ?: -1)?.toDoubleOrNull() ?: 1500.0
                            val tires = row.getOrNull(mapping["צמיגים ל-5 שנים"] ?: -1)?.toDoubleOrNull() ?: 2000.0
                            CarEntity(
                                name = name,
                                purchasePrice = purchase,
                                resaleValueAfter5Years = resale,
                                energyCostYearly = energy,
                                insuranceYearly = insurance,
                                trackerYearly = 600.0,
                                maintenanceYearly = maintenance,
                                annualTestYearly = 1800.0,
                                tires5YearsTotal = tires,
                                isBaseline = false
                            )
                        } catch (e: Exception) { null }
                    }
                    dao.insertCars(newCars)
                    Toast.makeText(context, "יובאו ${newCars.size} רכבים בהצלחה!", Toast.LENGTH_SHORT).show()
                    showCsvMapping = false
                }
            }
        )
    }
}

@Composable
fun CarBarChartCard(cars: List<CarEntity>) {
    val maxCost = cars.maxOfOrNull { it.netMonthlyCost }?.coerceAtLeast(100.0) ?: 100.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("השוואת עלות חודשית נטו (₪/חודש)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("כולל ירידת ערך וכל ההוצאות", fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val count = cars.size
                val barWidth = (size.width / (count * 1.5f)).coerceIn(24f, 70f)
                val totalBarsWidth = count * barWidth
                val remainingSpace = size.width - totalBarsWidth
                val gap = (remainingSpace / (count + 1)).coerceAtLeast(10f)

                cars.forEachIndexed { i, car ->
                    val x = gap + i * (barWidth + gap)
                    val ratio = (car.netMonthlyCost / maxCost).toFloat().coerceIn(0.05f, 1f)
                    val barHeight = ratio * (size.height - 40f)
                    val y = size.height - barHeight - 20f

                    val barColor = if (car.isBaseline) Color(0xFF0F2E4A) else Color(0xFF059669)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )

                    drawContext.canvas.nativeCanvas.apply {
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 26f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        // Draw cost on top of bar
                        drawText("₪${car.netMonthlyCost.toInt()}", x + barWidth / 2, y - 6f, textPaint)

                        // Draw car label at bottom
                        val label = if (car.name.length > 8) car.name.take(7) + ".." else car.name
                        drawText(label, x + barWidth / 2, size.height - 2f, textPaint)
                    }
                }
            }
        }
    }
}

@Composable
fun CarCard(
    car: CarEntity,
    baselineCar: CarEntity?,
    onUpdate: (CarEntity) -> Unit,
    onDelete: () -> Unit,
    onSetBaseline: () -> Unit,
    onOpenEnergyHelper: () -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val diffFromBaseline = if (baselineCar != null && baselineCar.id != car.id) {
        baselineCar.net5YearCost - car.net5YearCost
    } else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("car_card_${car.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (car.isBaseline) Color(0xFFF0F7FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Car Name, Baseline chip, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = car.name,
                    onValueChange = { onUpdate(car.copy(name = it)) },
                    label = { Text("דגם רכב") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("car_name_input_${car.id}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (car.isBaseline) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("רכב בסיס", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                } else {
                    OutlinedButton(
                        onClick = onSetBaseline,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("set_baseline_button_${car.id}")
                    ) {
                        Text("הגדר כבסיס", fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_car_${car.id}")) {
                    Icon(Icons.Default.Delete, contentDescription = "מחק רכב", tint = Color.Gray)
                }
            }

            // Headline Numbers Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (car.isBaseline) Color(0xFFE2EDF8) else Color(0xFFF1F5F9),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("נטו חודשי (60 חודש)", fontSize = 11.sp, color = Color.DarkGray)
                    Text(
                        "₪${fmt.format(car.netMonthlyCost.toInt())}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("סה״כ נטו ל-5 שנים", fontSize = 11.sp, color = Color.DarkGray)
                    Text(
                        "₪${fmt.format(car.net5YearCost.toInt())}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            // Baseline comparison result
            if (!car.isBaseline && baselineCar != null) {
                val diffResult = CarCalculationVerbatimHelpers.calculateBaselineDiffVerbatim(
                    carNet5YearCost = car.net5YearCost,
                    baselineNet5YearCost = baselineCar.net5YearCost,
                    baselineCarName = baselineCar.name,
                    fmt = fmt
                )
                Text(
                    text = diffResult.text,
                    color = if (diffResult.isSaving) Color(0xFF059669) else Color(0xFFDC2626),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider()

            // Inline Inputs: Purchase & Resale
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = car.purchasePrice.toInt().toString(),
                    onValueChange = { onUpdate(car.copy(purchasePrice = it.toDoubleOrNull() ?: car.purchasePrice)) },
                    label = { Text("מחיר קנייה (₪)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = car.resaleValueAfter5Years.toInt().toString(),
                    onValueChange = { onUpdate(car.copy(resaleValueAfter5Years = it.toDoubleOrNull() ?: car.resaleValueAfter5Years)) },
                    label = { Text("מכירה בעוד 5 שנים (₪)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            // Inline Inputs: Energy (with calculator dialog trigger) & Insurance
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = car.energyCostYearly.toInt().toString(),
                    onValueChange = { onUpdate(car.copy(energyCostYearly = it.toDoubleOrNull() ?: car.energyCostYearly)) },
                    label = { Text("אנרגיה שנתית (₪)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = onOpenEnergyHelper) {
                            Icon(Icons.Default.Calculate, contentDescription = "מחשבון אנרגיה", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.weight(1.2f)
                )
                OutlinedTextField(
                    value = car.insuranceYearly.toInt().toString(),
                    onValueChange = { onUpdate(car.copy(insuranceYearly = it.toDoubleOrNull() ?: car.insuranceYearly)) },
                    label = { Text("ביטוח שנתי (₪)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            // Inline Inputs: Maintenance, Test, Tracker, Tires
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = car.maintenanceYearly.toInt().toString(),
                    onValueChange = { onUpdate(car.copy(maintenanceYearly = it.toDoubleOrNull() ?: car.maintenanceYearly)) },
                    label = { Text("טיפולים שנתי") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = car.annualTestYearly.toInt().toString(),
                    onValueChange = { onUpdate(car.copy(annualTestYearly = it.toDoubleOrNull() ?: car.annualTestYearly)) },
                    label = { Text("טסט/אגרה") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = car.tires5YearsTotal.toInt().toString(),
                    onValueChange = { onUpdate(car.copy(tires5YearsTotal = it.toDoubleOrNull() ?: car.tires5YearsTotal)) },
                    label = { Text("צמיגים (5 שנים)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun EnergyHelperDialog(
    initialEnergy: Double,
    onDismiss: () -> Unit,
    onApply: (Double) -> Unit
) {
    var isFuel by remember { mutableStateOf(true) }
    var annualKm by remember { mutableStateOf("18000") }
    // Fuel parameters
    var kmPerLiter by remember { mutableStateOf("15.0") }
    var fuelPrice by remember { mutableStateOf("7.40") }
    // EV parameters
    var kwhPer100Km by remember { mutableStateOf("17.0") }
    var homeKwhPrice by remember { mutableStateOf("0.62") }
    var publicSessionsCount by remember { mutableStateOf("12") }
    var publicSessionCost by remember { mutableStateOf("60") }

    val calculatedCost: Double = remember(isFuel, annualKm, kmPerLiter, fuelPrice, kwhPer100Km, homeKwhPrice, publicSessionsCount, publicSessionCost) {
        val km = annualKm.toDoubleOrNull() ?: 0.0
        val kml = kmPerLiter.toDoubleOrNull() ?: 1.0
        val price = fuelPrice.toDoubleOrNull() ?: 0.0
        val kwh100 = kwhPer100Km.toDoubleOrNull() ?: 0.0
        val kwhCost = homeKwhPrice.toDoubleOrNull() ?: 0.0
        val sessions = publicSessionsCount.toDoubleOrNull() ?: 0.0
        val sessionPrice = publicSessionCost.toDoubleOrNull() ?: 0.0
        CarCalculationVerbatimHelpers.calculateEnergyCostVerbatim(
            isFuel = isFuel,
            annualKm = km,
            kmPerLiter = kml,
            fuelPrice = price,
            kwhPer100Km = kwh100,
            homeKwhPrice = kwhCost,
            publicSessionsCount = sessions,
            publicSessionCost = sessionPrice
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("עוזר חישוב אנרגיה שנתית", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = isFuel, onClick = { isFuel = true })
                        Text("דלק (בנזין / דיזל)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !isFuel, onClick = { isFuel = false })
                        Text("רכב חשמלי (EV)")
                    }
                }

                OutlinedTextField(
                    value = annualKm,
                    onValueChange = { annualKm = it },
                    label = { Text("נסועה שנתית ממוצעת (ק״מ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isFuel) {
                    OutlinedTextField(
                        value = kmPerLiter,
                        onValueChange = { kmPerLiter = it },
                        label = { Text("צריכת דלק (ק״מ לליטר)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fuelPrice,
                        onValueChange = { fuelPrice = it },
                        label = { Text("מחיר לליטר (₪)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = kwhPer100Km,
                        onValueChange = { kwhPer100Km = it },
                        label = { Text("צריכת חשמל (קוט״ש ל-100 ק״מ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = homeKwhPrice,
                        onValueChange = { homeKwhPrice = it },
                        label = { Text("מחיר קוט״ש ביתי (₪)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = publicSessionsCount,
                            onValueChange = { publicSessionsCount = it },
                            label = { Text("טעינות ציבוריות/שנה") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = publicSessionCost,
                            onValueChange = { publicSessionCost = it },
                            label = { Text("מחיר טעינה (₪)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "עלות אנרגיה שנתית מחושבת: ₪${calculatedCost.toInt()}",
                        modifier = Modifier.padding(10.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApply(calculatedCost) }) {
                Text("החל על הרכב")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        }
    )
}
