package com.menash.myself.ui.calculators.car

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
import androidx.compose.material.icons.filled.DirectionsCar
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
import com.menash.myself.core.domain.model.Car
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.domain.util.CsvParser
import com.menash.myself.feature.calculators.car.CarCalculatorViewModel
import com.menash.myself.ui.common.CsvMappingDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarComparisonScreen(
    onBack: () -> Unit,
    isOffline: Boolean = false
) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val viewModel: CarCalculatorViewModel = viewModel(
        factory = CarCalculatorViewModel.factory(
            carRepository = appContainer.carRepository,
            errorReporter = appContainer.errorReporter
        )
    )
    val cars by viewModel.cars.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val carList = cars.orEmpty()
    val baselineCar = carList.firstOrNull { it.isBaseline } ?: carList.firstOrNull()
    var editingEnergyForCar by remember { mutableStateOf<Car?>(null) }
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
            CarComparisonTopBar(
                onBack = onBack,
                onAddCar = { viewModel.insertCar(createDefaultCar(carList.isEmpty())) },
                onImportCsv = { csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                onExportCsv = { shareCarsCsv(context, carList) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OfflineIndicator(isOffline = isOffline)
            CarComparisonContent(
                uiState = uiState,
                baselineCar = baselineCar,
                onRetry = { viewModel.clearError() },
                onAddFirstCar = { viewModel.insertCar(createDefaultCar(true)) },
                onUpdateCar = { viewModel.updateCar(it) },
                onDeleteCar = { viewModel.deleteCar(it) },
                onSetBaseline = { viewModel.setBaselineCar(it.id) },
                onOpenEnergyHelper = { editingEnergyForCar = it }
            )
        }
    }

    editingEnergyForCar?.let { car ->
        EnergyHelperDialog(
            initialEnergy = car.energyCostYearly.shekels,
            onDismiss = { editingEnergyForCar = null },
            onApply = { calculated ->
                viewModel.updateCar(car.copy(energyCostYearly = Money.fromShekels(calculated)))
                editingEnergyForCar = null
            }
        )
    }

    if (showCsvMapping && importedCsvRows.isNotEmpty()) {
        CsvMappingDialog(
            headers = importedCsvRows.first(),
            previewRows = importedCsvRows.drop(1),
            targetFields = listOf("דגם רכב", "מחיר קנייה", "מכירה (5 שנים)", "אנרגיה שנתית", "ביטוח שנתי", "טיפולים", "צמיגים ל-5 שנים"),
            onDismiss = { showCsvMapping = false },
            onConfirmMap = { mapping ->
                val newCars = parseMappedCars(importedCsvRows.drop(1), mapping)
                viewModel.insertCars(newCars)
                Toast.makeText(context, "יובאו ${newCars.size} רכבים בהצלחה!", Toast.LENGTH_SHORT).show()
                showCsvMapping = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarComparisonTopBar(
    onBack: () -> Unit,
    onAddCar: () -> Unit,
    onImportCsv: () -> Unit,
    onExportCsv: () -> Unit
) {
    TopAppBar(
        title = { Text("השוואת רכבים — עלות 5 שנים", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("car_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
            }
        },
        actions = {
            IconButton(onClick = onAddCar, modifier = Modifier.testTag("add_car_top_action")) {
                Icon(Icons.Default.Add, contentDescription = "הוסף רכב")
            }
            IconButton(onClick = onImportCsv, modifier = Modifier.testTag("car_import_csv_button")) {
                Icon(Icons.Default.UploadFile, contentDescription = "ייבוא CSV")
            }
            IconButton(onClick = onExportCsv, modifier = Modifier.testTag("car_export_csv_button")) {
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
private fun CarComparisonContent(
    uiState: UiState<List<Car>>,
    baselineCar: Car?,
    onRetry: () -> Unit,
    onAddFirstCar: () -> Unit,
    onUpdateCar: (Car) -> Unit,
    onDeleteCar: (Car) -> Unit,
    onSetBaseline: (Car) -> Unit,
    onOpenEnergyHelper: (Car) -> Unit
) {
    when (uiState) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(message = uiState.message, onRetry = onRetry)
        is UiState.Empty -> EmptyState(
            title = "אין רכבים להשוואה",
            description = "הוסף רכב ראשון כדי לחשב ולהשוות עלויות ל-5 שנים.",
            icon = Icons.Default.DirectionsCar,
            actionLabel = "הוסף רכב",
            onActionClick = onAddFirstCar
        )
        is UiState.Content -> {
            val cars = uiState.data
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(6.dp)) }
                item { CarBarChartCard(cars = cars) }
                items(cars, key = { it.id }) { car ->
                    CarCard(
                        car = car,
                        baselineCar = baselineCar,
                        onUpdate = onUpdateCar,
                        onDelete = { onDeleteCar(car) },
                        onSetBaseline = { onSetBaseline(car) },
                        onOpenEnergyHelper = { onOpenEnergyHelper(car) }
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

private fun createDefaultCar(isFirst: Boolean): Car = Car(
    name = "רכב חדש להשוואה",
    purchasePrice = Money.fromShekels(140000.0),
    resaleValueAfter5Years = Money.fromShekels(75000.0),
    energyCostYearly = Money.fromShekels(7200.0),
    insuranceYearly = Money.fromShekels(5000.0),
    trackerYearly = Money.fromShekels(600.0),
    maintenanceYearly = Money.fromShekels(1800.0),
    annualTestYearly = Money.fromShekels(1800.0),
    tires5YearsTotal = Money.fromShekels(2200.0),
    isBaseline = isFirst
)

private fun shareCarsCsv(context: android.content.Context, cars: List<Car>) {
    val headers = listOf("דגם רכב", "מחיר קנייה", "מכירה (5 שנים)", "אנרגיה שנתית", "ביטוח שנתי", "איתוראן", "טיפולים", "טסט", "צמיגים ל-5 שנים", "עלות חודשית נטו")
    val rows = cars.map {
        listOf(
            it.name,
            it.purchasePrice.shekels.toInt().toString(),
            it.resaleValueAfter5Years.shekels.toInt().toString(),
            it.energyCostYearly.shekels.toInt().toString(),
            it.insuranceYearly.shekels.toInt().toString(),
            it.trackerYearly.shekels.toInt().toString(),
            it.maintenanceYearly.shekels.toInt().toString(),
            it.annualTestYearly.shekels.toInt().toString(),
            it.tires5YearsTotal.shekels.toInt().toString(),
            it.netMonthlyCost.shekels.toInt().toString()
        )
    }
    val csv = CsvParser.exportToCsv(headers, rows)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_TEXT, csv)
        putExtra(Intent.EXTRA_SUBJECT, "השוואת רכבים.csv")
    }
    context.startActivity(Intent.createChooser(sendIntent, "שתף נתוני רכבים ב-CSV"))
}

private fun parseMappedCars(dataRows: List<List<String>>, mapping: Map<String, Int>): List<Car> =
    dataRows.mapNotNull { row ->
        try {
            val name = row.getOrNull(mapping["דגם רכב"] ?: -1) ?: "רכב מיובא"
            val purchase = row.getOrNull(mapping["מחיר קנייה"] ?: -1)?.toDoubleOrNull() ?: 120000.0
            val resale = row.getOrNull(mapping["מכירה (5 שנים)"] ?: -1)?.toDoubleOrNull() ?: 60000.0
            val energy = row.getOrNull(mapping["אנרגיה שנתית"] ?: -1)?.toDoubleOrNull() ?: 7000.0
            val insurance = row.getOrNull(mapping["ביטוח שנתי"] ?: -1)?.toDoubleOrNull() ?: 4500.0
            val maintenance = row.getOrNull(mapping["טיפולים"] ?: -1)?.toDoubleOrNull() ?: 1500.0
            val tires = row.getOrNull(mapping["צמיגים ל-5 שנים"] ?: -1)?.toDoubleOrNull() ?: 2000.0
            Car(
                name = name,
                purchasePrice = Money.fromShekels(purchase),
                resaleValueAfter5Years = Money.fromShekels(resale),
                energyCostYearly = Money.fromShekels(energy),
                insuranceYearly = Money.fromShekels(insurance),
                trackerYearly = Money.fromShekels(600.0),
                maintenanceYearly = Money.fromShekels(maintenance),
                annualTestYearly = Money.fromShekels(1800.0),
                tires5YearsTotal = Money.fromShekels(tires),
                isBaseline = false
            )
        } catch (e: Exception) {
            null
        }
    }
