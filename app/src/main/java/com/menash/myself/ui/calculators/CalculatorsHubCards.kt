package com.menash.myself.ui.calculators

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
data class CalculatorCardItem(
    val id: String,
    val title: String,
    val headline: String,
    val icon: ImageVector,
    val route: String?,
    val isReady: Boolean = false
)

@Composable
fun BackupWarningBanner(onBackupNow: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFFFECACA))),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag("backup_warning_banner")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.ui_str_40),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B),
                    fontSize = 13.sp
                )
                Text(
                    stringResource(R.string.ui_str_36),
                    color = Color(0xFF4B5563),
                    fontSize = 11.sp
                )
            }
            Button(
                onClick = onBackupNow,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.ui_str_37), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CalculatorsStatusBar(
    usdRate: Double,
    lastBackup: Long,
    daysSinceBackup: Long,
    onEditRate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onEditRate)
        ) {
            Icon(
                Icons.Default.CurrencyExchange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                stringResource(R.string.ui_usdrate_39, usdRate),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Default.Edit,
                contentDescription = stringResource(R.string.ui_str_38),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(12.dp)
                    .padding(start = 2.dp)
            )
        }

        Text(
            text = if (lastBackup == 0L) stringResource(R.string.ui_str_35) else stringResource(R.string.ui_dayssincebackup_41, daysSinceBackup),
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun CalculatorsGrid(
    calculators: List<CalculatorCardItem>,
    onCardClick: (CalculatorCardItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(calculators.size) { index ->
            val item = calculators[index]
            CalculatorHubCard(item = item, onClick = { onCardClick(item) })
        }
    }
}

@Composable
private fun CalculatorHubCard(
    item: CalculatorCardItem,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .testTag("calculator_card_${item.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (item.isReady) Color.White else Color(0xFFF8FAFC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isReady) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = if (item.isReady) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    item.headline,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isReady) MaterialTheme.colorScheme.secondary else Color.Gray,
                    maxLines = 2
                )
                if (!item.isReady) {
                    Text(
                        stringResource(R.string.ui_str_42),
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
