package com.menash.myself.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.data.io.BackupRestoreManager
import com.menash.myself.feature.calculators.CalculatorsHubViewModel
import com.menash.myself.ui.settings.SettingsDialog
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class CalculatorCardItem(
    val id: String,
    val title: String,
    val headline: String,
    val icon: ImageVector,
    val route: String?,
    val isReady: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appContainer = LocalAppContainer.current
    val viewModel: CalculatorsHubViewModel = viewModel(
        factory = CalculatorsHubViewModel.factory(
            carRepository = appContainer.carRepository,
            aiSubscriptionRepository = appContainer.aiSubscriptionRepository,
            pensionFundRepository = appContainer.pensionFundRepository,
            settingsRepository = appContainer.settingsRepository
        )
    )

    val settings by viewModel.settings.collectAsState()
    val cars by viewModel.cars.collectAsState()
    val aiSubs by viewModel.aiSubs.collectAsState()
    val pensionFunds by viewModel.pensionFunds.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var pendingNoticeMessage by remember { mutableStateOf<String?>(null) }

    val usdRate = settings?.usdToIlsRate ?: 3.11
    val lastBackup = settings?.lastBackupTimestamp ?: 0L
    val daysSinceBackup = if (lastBackup == 0L) Long.MAX_VALUE else TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastBackup)
    val needsBackupWarning = daysSinceBackup >= 30

    val fmt = NumberFormat.getNumberInstance(Locale.US)

    // Dynamic headline calculations
    val carHeadline = remember(cars) {
        if (cars.isEmpty()) "אין רכבים עדיין (לחץ להוספה)"
        else {
            val best = cars.minByOrNull { it.netMonthlyCost } ?: cars.first()
            "הכי משתלם: ${best.name} (₪${fmt.format(best.netMonthlyCost.toInt())}/חודש)"
        }
    }

    val aiHeadline = remember(aiSubs, usdRate) {
        val active = aiSubs.filter { it.isActive }
        val sumMonthly = active.sumOf { it.monthlyCostIls(usdRate) }
        "₪${fmt.format(sumMonthly.toInt())} / חודש (${active.size} פעילים)"
    }

    val pensionHeadline = remember(pensionFunds) {
        if (pensionFunds.isEmpty()) "אין קופות עדיין (לחץ להוספה)"
        else {
            val totalVal = pensionFunds.sumOf { it.currentValue }
            val totalDep = pensionFunds.sumOf { it.totalDeposits }
            val profitPercent = if (totalDep > 0) ((totalVal - totalDep) / totalDep) * 100.0 else 0.0
            "סך נכסים: ₪${fmt.format(totalVal.toInt())} (${String.format(Locale.US, "%+.1f", profitPercent)}%)"
        }
    }

    val calculators = listOf(
        CalculatorCardItem("car", "השוואת רכבים (5 שנים)", carHeadline, Icons.Default.DirectionsCar, "car_calculator", true),
        CalculatorCardItem("ai", "מנויי AI וכלים", aiHeadline, Icons.Default.SmartToy, "ai_calculator", true),
        CalculatorCardItem("pension", "פנסיה וחיסכון", pensionHeadline, Icons.Default.Savings, "pension_calculator", true),
        CalculatorCardItem("mishloach", "משלוחי מנות", "תכנון סל: ₪680 (12 מארזים)", Icons.Default.CardGiftcard, null, false),
        CalculatorCardItem("investments", "יומן השקעות", "תשואה: +14.2% (₪42,500)", Icons.Default.TrendingUp, null, false),
        CalculatorCardItem("apartment", "נדל״ן — קנייה ומכירה", "רווח צפוי: ₪385,000", Icons.Default.Apartment, null, false),
        CalculatorCardItem("donations", "החזרי מס — תרומות", "זכאות להחזר: ₪2,150", Icons.Default.VolunteerActivism, null, false),
        CalculatorCardItem("trip", "תקציב טיול", "תקציב משוער: $4,850 (₪320/יום)", Icons.Default.FlightTakeoff, null, false)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("המחשבונים שלי", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("מרכז מחשבונים פיננסיים אישיים", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scope.launch { BackupRestoreManager.createAndShareBackup(context) } },
                        modifier = Modifier.testTag("home_quick_backup_button")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = "גיבוי מהיר של כל הנתונים")
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "הגדרות ושער דולר")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Backup Warning Banner (if older than 30 days)
            if (needsBackupWarning) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFECACA))),
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
                                "טרם בוצע גיבוי מעל 30 יום!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B),
                                fontSize = 13.sp
                            )
                            Text(
                                "האפליקציה מחליפה את הגיליון שלך. מומלץ לשמור גיבוי עכשיו.",
                                color = Color(0xFF4B5563),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = { scope.launch { BackupRestoreManager.createAndShareBackup(context) } },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("גבה עכשיו", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Quick Status Bar: USD Rate & Backup status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showSettingsDialog = true }
                ) {
                    Icon(
                        Icons.Default.CurrencyExchange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "שער דולר גלובלי: ₪$usdRate",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "ערוך שער",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp).padding(start = 2.dp)
                    )
                }

                Text(
                    text = if (lastBackup == 0L) "טרם גובה" else "גיבוי: לפני $daysSinceBackup ימים",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 8 Calculator Cards Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(calculators.size) { index ->
                    val item = calculators[index]
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("calculator_card_${item.id}")
                            .clickable {
                                if (item.isReady && item.route != null) {
                                    navController.navigate(item.route)
                                } else {
                                    pendingNoticeMessage = "מחשבון ${item.title} יתווסף בשלב הבא לבקשתך!"
                                }
                            },
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
                                        "בפיתוח — שלב הבא",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentRate = usdRate,
            lastBackupTimestamp = lastBackup,
            onDismiss = { showSettingsDialog = false },
            onSaveRate = { newRate ->
                viewModel.saveUsdRate(newRate)
                showSettingsDialog = false
            }
        )
    }

    // Notice dialog for pending calculators
    pendingNoticeMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { pendingNoticeMessage = null },
            title = { Text("מחשבון בשלבי פיתוח", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { pendingNoticeMessage = null }) {
                    Text("הבנתי")
                }
            }
        )
    }
}
