package com.menash.myself.ui.calculators.pension

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.PensionFund
import java.text.NumberFormat
import java.util.Locale

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PensionFundCard(
    fund: PensionFund,
    isHighestBalance: Boolean,
    isHighestDeposit: Boolean,
    onUpdate: (PensionFund) -> Unit,
    onDelete: () -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val isProfit = !fund.profit.isNegative()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pension_card_${fund.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighestBalance || isHighestDeposit) Color(0xFFFFFDFD) else Color.White
        ),
        border = if (isHighestBalance || isHighestDeposit) {
            CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFFFCA5A5)))
        } else {
            CardDefaults.outlinedCardBorder()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PensionFundCardHeader(fund = fund, onUpdate = onUpdate, onDelete = onDelete)

            if (isHighestBalance || isHighestDeposit) {
                PensionHighFeeChips(
                    fund = fund,
                    isHighestBalance = isHighestBalance,
                    isHighestDeposit = isHighestDeposit
                )
            }

            PensionMetricsBanner(fund = fund, isProfit = isProfit, fmt = fmt)

            HorizontalDivider()

            PensionInlineInputs(fund = fund, onUpdate = onUpdate)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PensionFundCardHeader(
    fund: PensionFund,
    onUpdate: (PensionFund) -> Unit,
    onDelete: () -> Unit
) {
    val fundTypes = listOf(stringResource(R.string.ui_str_99), stringResource(R.string.ui_str_97), stringResource(R.string.ui_str_107), stringResource(R.string.ui_str_110))
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = typeDropdownExpanded,
            onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
            modifier = Modifier.weight(1.1f)
        ) {
            OutlinedTextField(
                value = fund.fundType,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.ui_str_98)) },
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

        OutlinedTextField(
            value = fund.provider,
            onValueChange = { onUpdate(fund.copy(provider = it)) },
            label = { Text(stringResource(R.string.ui_str_111)) },
            modifier = Modifier.weight(1.2f),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
        )

        IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_fund_${fund.id}")) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.ui_str_104), tint = Color.Gray)
        }
    }
}

@Composable
private fun PensionHighFeeChips(
    fund: PensionFund,
    isHighestBalance: Boolean,
    isHighestDeposit: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isHighestBalance) {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        stringResource(R.string.ui_str_108, fund.feeOnBalancePercent),
                        fontSize = 11.sp,
                        color = Color(0xFF991B1B)
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFFEE2E2))
            )
        }
        if (isHighestDeposit) {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        stringResource(R.string.ui_str_94, fund.feeOnDepositsPercent),
                        fontSize = 11.sp,
                        color = Color(0xFF991B1B)
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFFEF3C7))
            )
        }
    }
}

@Composable
private fun PensionMetricsBanner(
    fund: PensionFund,
    isProfit: Boolean,
    fmt: NumberFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(stringResource(R.string.ui_str_105), fontSize = 11.sp, color = Color.DarkGray)
            Text(
                "${if (isProfit) "+" else ""}₪${fmt.format(fund.profit.shekels.toInt())}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isProfit) Color(0xFF059669) else Color(0xFFDC2626)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.ui_str_95), fontSize = 11.sp, color = Color.DarkGray)
            Text(
                "${if (isProfit) "+" else ""}${String.format(Locale.US, "%.1f", fund.profitPercent)}%",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isProfit) Color(0xFF059669) else Color(0xFFDC2626)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(stringResource(R.string.ui_str_96), fontSize = 11.sp, color = Color.DarkGray)
            Text(
                stringResource(R.string.ui_fmtformatfundyearlybalancefeeestimateshekelstoint_109, fmt.format(fund.yearlyBalanceFeeEstimate.shekels.toInt())),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
private fun PensionInlineInputs(
    fund: PensionFund,
    onUpdate: (PensionFund) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = fund.currentValue.shekels.toInt().toString(),
            onValueChange = {
                val v = it.toDoubleOrNull()
                if (v != null) onUpdate(fund.copy(currentValue = Money.fromShekels(v)))
            },
            label = { Text(stringResource(R.string.ui_str_100)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = fund.totalDeposits.shekels.toInt().toString(),
            onValueChange = {
                val v = it.toDoubleOrNull()
                if (v != null) onUpdate(fund.copy(totalDeposits = Money.fromShekels(v)))
            },
            label = { Text(stringResource(R.string.ui_str_102)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = fund.feeOnBalancePercent.toString(),
            onValueChange = {
                val v = it.toDoubleOrNull()
                if (v != null) onUpdate(fund.copy(feeOnBalancePercent = v))
            },
            label = { Text(stringResource(R.string.ui_str_106)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = fund.feeOnDepositsPercent.toString(),
            onValueChange = {
                val v = it.toDoubleOrNull()
                if (v != null) onUpdate(fund.copy(feeOnDepositsPercent = v))
            },
            label = { Text(stringResource(R.string.ui_str_103)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
    }

    OutlinedTextField(
        value = fund.notes,
        onValueChange = { onUpdate(fund.copy(notes = it)) },
        label = { Text(stringResource(R.string.ui_str_101)) },
        modifier = Modifier.fillMaxWidth()
    )
}
