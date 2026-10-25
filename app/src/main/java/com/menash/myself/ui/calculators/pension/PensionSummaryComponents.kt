package com.menash.myself.ui.calculators.pension

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.core.domain.model.PensionFund
import java.text.NumberFormat
import java.util.Locale

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
@Composable
fun PensionSummaryCard(
    totalValue: Double,
    totalDeposits: Double,
    totalProfit: Double,
    totalProfitPercent: Double,
    totalEstimatedYearlyFees: Double
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
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
                    Text(stringResource(R.string.ui_str_127), fontSize = 12.sp, color = Color.DarkGray)
                    Text(
                        "₪${fmt.format(totalValue.toInt())}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.ui_str_118), fontSize = 12.sp, color = Color.DarkGray)
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
                    Text(stringResource(R.string.ui_str_126), fontSize = 11.sp, color = Color.DarkGray)
                    val isProfitable = totalProfit >= 0
                    Text(
                        "${if (isProfitable) "+" else ""}₪${fmt.format(totalProfit.toInt())} (${String.format(Locale.US, "%.1f", totalProfitPercent)}%)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isProfitable) Color(0xFF047857) else Color(0xFFDC2626)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.ui_str_128), fontSize = 11.sp, color = Color.DarkGray)
                    Text(
                        stringResource(R.string.ui_fmtformattotalestimatedyearlyfeestoint_132, fmt.format(totalEstimatedYearlyFees.toInt())),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun HighFeesWarningBanner(
    highestBalanceFund: PensionFund?,
    maxBalanceFee: Double,
    highestDepositFund: PensionFund?,
    maxDepositFee: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("high_fees_warning_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFFFECACA)))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = stringResource(R.string.ui_str_133),
                tint = Color(0xFFDC2626),
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.ui_str_134),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF991B1B)
                )
                if (highestBalanceFund != null && maxBalanceFee > 0) {
                    Text(
                        stringResource(R.string.ui_highestbalancefundfeeonbalancepercent_highestbalancefundprovider_129, highestBalanceFund.feeOnBalancePercent, highestBalanceFund.provider, highestBalanceFund.fundType),
                        fontSize = 12.sp,
                        color = Color(0xFF7F1D1D),
                        fontWeight = FontWeight.Medium
                    )
                }
                if (highestDepositFund != null && maxDepositFee > 0) {
                    Text(
                        stringResource(R.string.ui_highestdepositfundfeeondepositspercent_highestdepositfundprovider_130, highestDepositFund.feeOnDepositsPercent, highestDepositFund.provider, highestDepositFund.fundType),
                        fontSize = 12.sp,
                        color = Color(0xFF7F1D1D),
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    stringResource(R.string.ui_str_131),
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun PensionFundTypeFilterRow(
    fundTypes: List<String>,
    funds: List<PensionFund>,
    selectedTypeFilter: String?,
    onSelectType: (String?) -> Unit
) {
    val allLabel = stringResource(R.string.ui_str_123)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(fundTypes) { type ->
            val count = if (type == allLabel) funds.size else funds.count { it.fundType == type }
            FilterChip(
                selected = (selectedTypeFilter == null && type == allLabel) || selectedTypeFilter == type,
                onClick = { onSelectType(if (type == allLabel) null else type) },
                label = { Text("$type ($count)") }
            )
        }
    }
}
