package com.menash.myself.ui.calculators.car

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.core.domain.model.Car
import com.menash.myself.core.domain.model.Money
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CarBarChartCard(cars: List<Car>) {
    val maxCost = (cars.maxOfOrNull { it.netMonthlyCost.shekels } ?: 1.0).coerceAtLeast(1.0)
    Card(
        modifier = Modifier.fillMaxWidth().testTag("car_chart_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "השוואה ויזואלית — עלות חודשית נטו (₪)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val count = cars.size
                if (count == 0) return@Canvas
                val barWidth = (size.width / (count * 1.5f)).coerceIn(24f, 70f)
                val gap = ((size.width - count * barWidth) / (count + 1)).coerceAtLeast(10f)
                cars.forEachIndexed { i, car ->
                    val x = gap + i * (barWidth + gap)
                    val ratio = (car.netMonthlyCost.shekels / maxCost).toFloat().coerceIn(0.05f, 1f)
                    val barHeight = ratio * (size.height - 40f)
                    val y = size.height - barHeight - 20f
                    drawRoundRect(
                        color = if (car.isBaseline) Color(0xFF0F2E4A) else Color(0xFF059669),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 26f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText("₪${car.netMonthlyCost.shekels.toInt()}", x + barWidth / 2, y - 6f, textPaint)
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
    car: Car,
    baselineCar: Car?,
    onUpdate: (Car) -> Unit,
    onDelete: () -> Unit,
    onSetBaseline: () -> Unit,
    onOpenEnergyHelper: () -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    Card(
        modifier = Modifier.fillMaxWidth().testTag("car_card_${car.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (car.isBaseline) Color(0xFFF0F7FF) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CarCardHeader(
                car = car,
                onNameChange = { onUpdate(car.copy(name = it)) },
                onSetBaseline = onSetBaseline,
                onDelete = onDelete
            )
            CarHeadlineBanner(car = car, baselineCar = baselineCar, fmt = fmt)
            HorizontalDivider()
            CarInlineInputs(car = car, onUpdate = onUpdate, onOpenEnergyHelper = onOpenEnergyHelper)
        }
    }
}

@Composable
private fun CarCardHeader(
    car: Car,
    onNameChange: (String) -> Unit,
    onSetBaseline: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = car.name,
            onValueChange = onNameChange,
            label = { Text("דגם רכב") },
            modifier = Modifier.weight(1f).testTag("car_name_input_${car.id}")
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (car.isBaseline) {
            SuggestionChip(onClick = {}, label = { Text("רכב בסיס", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
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
}

@Composable
private fun CarHeadlineBanner(car: Car, baselineCar: Car?, fmt: NumberFormat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (car.isBaseline) Color(0xFFE2EDF8) else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("נטו חודשי (60 חודש)", fontSize = 11.sp, color = Color.DarkGray)
            Text("₪${fmt.format(car.netMonthlyCost.shekels.toInt())}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("סה״כ נטו ל-5 שנים", fontSize = 11.sp, color = Color.DarkGray)
            Text("₪${fmt.format(car.net5YearCost.shekels.toInt())}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF0F172A))
        }
    }
    if (!car.isBaseline && baselineCar != null) {
        val diffResult = CarCalculationVerbatimHelpers.calculateBaselineDiffVerbatim(
            carNet5YearCost = car.net5YearCost.shekels,
            baselineNet5YearCost = baselineCar.net5YearCost.shekels,
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
}

@Composable
private fun CarInlineInputs(car: Car, onUpdate: (Car) -> Unit, onOpenEnergyHelper: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CarMoneyField("מחיר קנייה (₪)", car.purchasePrice, Modifier.weight(1f)) { onUpdate(car.copy(purchasePrice = it)) }
        CarMoneyField("מכירה בעוד 5 שנים (₪)", car.resaleValueAfter5Years, Modifier.weight(1f)) { onUpdate(car.copy(resaleValueAfter5Years = it)) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = car.energyCostYearly.shekels.toInt().toString(),
            onValueChange = { it.toDoubleOrNull()?.let { v -> onUpdate(car.copy(energyCostYearly = Money.fromShekels(v))) } },
            label = { Text("אנרגיה שנתית (₪)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                IconButton(onClick = onOpenEnergyHelper) {
                    Icon(Icons.Default.Calculate, contentDescription = "מחשבון אנרגיה", tint = MaterialTheme.colorScheme.primary)
                }
            },
            modifier = Modifier.weight(1.2f)
        )
        CarMoneyField("ביטוח שנתי (₪)", car.insuranceYearly, Modifier.weight(1f)) { onUpdate(car.copy(insuranceYearly = it)) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CarMoneyField("טיפולים שנתי", car.maintenanceYearly, Modifier.weight(1f)) { onUpdate(car.copy(maintenanceYearly = it)) }
        CarMoneyField("טסט/אגרה", car.annualTestYearly, Modifier.weight(1f)) { onUpdate(car.copy(annualTestYearly = it)) }
        CarMoneyField("צמיגים (5 שנים)", car.tires5YearsTotal, Modifier.weight(1f)) { onUpdate(car.copy(tires5YearsTotal = it)) }
    }
}

@Composable
private fun CarMoneyField(
    label: String,
    value: Money,
    modifier: Modifier = Modifier,
    onMoneyChange: (Money) -> Unit
) {
    OutlinedTextField(
        value = value.shekels.toInt().toString(),
        onValueChange = { it.toDoubleOrNull()?.let { v -> onMoneyChange(Money.fromShekels(v)) } },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
