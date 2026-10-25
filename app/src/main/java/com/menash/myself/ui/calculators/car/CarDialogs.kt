package com.menash.myself.ui.calculators.car

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
@Composable
fun EnergyHelperDialog(
    initialEnergy: Double,
    onDismiss: () -> Unit,
    onApply: (Double) -> Unit
) {
    var isFuel by remember { mutableStateOf(true) }
    var annualKm by remember { mutableStateOf("18000") }
    var kmPerLiter by remember { mutableStateOf("15.0") }
    var fuelPrice by remember { mutableStateOf("7.40") }
    var kwhPer100Km by remember { mutableStateOf("17.0") }
    var homeKwhPrice by remember { mutableStateOf("0.62") }
    var publicSessionsCount by remember { mutableStateOf("12") }
    var publicSessionCost by remember { mutableStateOf("60") }

    val calculatedCost: Double = remember(
        isFuel, annualKm, kmPerLiter, fuelPrice,
        kwhPer100Km, homeKwhPrice, publicSessionsCount, publicSessionCost
    ) {
        val km = annualKm.toDoubleOrNull() ?: 0.0
        val kml = CarCalculationVerbatimHelpers.parseKmPerLiterVerbatim(kmPerLiter)
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
        title = { Text(stringResource(R.string.ui_str_89), fontWeight = FontWeight.Bold) },
        text = {
            EnergyHelperDialogBody(
                isFuel = isFuel,
                onFuelChange = { isFuel = it },
                annualKm = annualKm,
                onAnnualKmChange = { annualKm = it },
                kmPerLiter = kmPerLiter,
                onKmPerLiterChange = { kmPerLiter = it },
                fuelPrice = fuelPrice,
                onFuelPriceChange = { fuelPrice = it },
                kwhPer100Km = kwhPer100Km,
                onKwhPer100KmChange = { kwhPer100Km = it },
                homeKwhPrice = homeKwhPrice,
                onHomeKwhPriceChange = { homeKwhPrice = it },
                publicSessionsCount = publicSessionsCount,
                onPublicSessionsCountChange = { publicSessionsCount = it },
                publicSessionCost = publicSessionCost,
                onPublicSessionCostChange = { publicSessionCost = it },
                calculatedCost = calculatedCost
            )
        },
        confirmButton = {
            Button(onClick = { onApply(calculatedCost) }) {
                Text(stringResource(R.string.ui_str_88))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_str_92)) }
        }
    )
}

@Composable
private fun EnergyHelperDialogBody(
    isFuel: Boolean,
    onFuelChange: (Boolean) -> Unit,
    annualKm: String,
    onAnnualKmChange: (String) -> Unit,
    kmPerLiter: String,
    onKmPerLiterChange: (String) -> Unit,
    fuelPrice: String,
    onFuelPriceChange: (String) -> Unit,
    kwhPer100Km: String,
    onKwhPer100KmChange: (String) -> Unit,
    homeKwhPrice: String,
    onHomeKwhPriceChange: (String) -> Unit,
    publicSessionsCount: String,
    onPublicSessionsCountChange: (String) -> Unit,
    publicSessionCost: String,
    onPublicSessionCostChange: (String) -> Unit,
    calculatedCost: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = isFuel, onClick = { onFuelChange(true) })
                Text(stringResource(R.string.ui_str_93))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !isFuel, onClick = { onFuelChange(false) })
                Text(stringResource(R.string.ui_ev_85))
            }
        }

        OutlinedTextField(
            value = annualKm,
            onValueChange = onAnnualKmChange,
            label = { Text(stringResource(R.string.ui_str_87)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        if (isFuel) {
            OutlinedTextField(
                value = kmPerLiter,
                onValueChange = onKmPerLiterChange,
                label = { Text(stringResource(R.string.ui_str_81)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = fuelPrice,
                onValueChange = onFuelPriceChange,
                label = { Text(stringResource(R.string.ui_str_91)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = kwhPer100Km,
                onValueChange = onKwhPer100KmChange,
                label = { Text(stringResource(R.string.ui_100_83)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = homeKwhPrice,
                onValueChange = onHomeKwhPriceChange,
                label = { Text(stringResource(R.string.ui_str_84)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = publicSessionsCount,
                    onValueChange = onPublicSessionsCountChange,
                    label = { Text(stringResource(R.string.ui_str_90)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = publicSessionCost,
                    onValueChange = onPublicSessionCostChange,
                    label = { Text(stringResource(R.string.ui_str_82)) },
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
                stringResource(R.string.ui_str_86, calculatedCost.toInt()),
                modifier = Modifier.padding(10.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
