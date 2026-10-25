package com.menash.myself.ui.calculators.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.core.domain.model.AiSubscription
import com.menash.myself.core.domain.model.Money
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AiSubscriptionsSummarySection(
    totalMonthlyIls: Double,
    totalYearlyIls: Double,
    activeCount: Int,
    totalCount: Int,
    usdRate: Double
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
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
                Text(
                    "₪${fmt.format(totalMonthlyIls.toInt())}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("$activeCount מתוך $totalCount מנויים פעילים", fontSize = 11.sp, color = Color.Gray)
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("הוצאה שנתית מצטברת", fontSize = 11.sp, color = Color.DarkGray)
                Text(
                    "₪${fmt.format(totalYearlyIls.toInt())}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text("שער מחושב: ₪$usdRate ל-$", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AiCategoryFilterRow(
    categories: List<String>,
    subscriptions: List<AiSubscription>,
    activeSubs: List<AiSubscription>,
    usdRate: Double,
    selectedCategoryFilter: String?,
    onSelectCategory: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedCategoryFilter == null,
                onClick = { onSelectCategory(null) },
                label = { Text("הכל (${subscriptions.size})") }
            )
        }
        items(categories) { cat ->
            val catSum = activeSubs.filter { it.category == cat }.sumOf { it.monthlyCostIls(usdRate).shekels }
            FilterChip(
                selected = selectedCategoryFilter == cat,
                onClick = { onSelectCategory(if (selectedCategoryFilter == cat) null else cat) },
                label = { Text("$cat (₪${catSum.toInt()})") }
            )
        }
    }
}

@Composable
fun AiSubscriptionCard(
    sub: AiSubscription,
    usdRate: Double,
    onUpdate: (AiSubscription) -> Unit,
    onDelete: () -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val monthlyIls = sub.monthlyCostIls(usdRate).shekels
    val yearlyIls = sub.yearlyCostIls(usdRate).shekels

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
            AiSubscriptionCardHeader(sub = sub, onUpdate = onUpdate, onDelete = onDelete)
            AiSubscriptionSpendBanner(sub = sub, monthlyIls = monthlyIls, yearlyIls = yearlyIls, fmt = fmt)
            HorizontalDivider()
            AiSubscriptionInlineFields(sub = sub, onUpdate = onUpdate)
        }
    }
}

@Composable
private fun AiSubscriptionCardHeader(
    sub: AiSubscription,
    onUpdate: (AiSubscription) -> Unit,
    onDelete: () -> Unit
) {
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
}

@Composable
private fun AiSubscriptionSpendBanner(
    sub: AiSubscription,
    monthlyIls: Double,
    yearlyIls: Double,
    fmt: NumberFormat
) {
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
}

@Composable
private fun AiSubscriptionInlineFields(
    sub: AiSubscription,
    onUpdate: (AiSubscription) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = sub.price.shekels.toString(),
            onValueChange = {
                val v = it.toDoubleOrNull()
                if (v != null) onUpdate(sub.copy(price = Money.fromShekels(v)))
            },
            label = { Text("מחיר נקוב") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )

        OutlinedButton(
            onClick = {
                val next = if (sub.currency == "USD") "ILS" else "USD"
                onUpdate(sub.copy(currency = next))
            },
            modifier = Modifier
                .weight(0.9f)
                .padding(top = 8.dp)
        ) {
            Text(if (sub.currency == "USD") "$ דולר" else "₪ שקל", fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = {
                val next = if (sub.billingCycle == "MONTHLY") "YEARLY" else "MONTHLY"
                onUpdate(sub.copy(billingCycle = next))
            },
            modifier = Modifier
                .weight(0.9f)
                .padding(top = 8.dp)
        ) {
            Text(if (sub.billingCycle == "MONTHLY") "חודשי" else "שנתי", fontSize = 12.sp)
        }
    }

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
