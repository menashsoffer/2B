package com.menash.myself.ui.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.menash.myself.core.designsystem.component.ErrorState
import com.menash.myself.core.designsystem.component.LoadingState
import com.menash.myself.core.designsystem.component.OfflineIndicator
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.core.domain.model.FitnessEntry
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.feature.fitness.FitnessViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen(onBack: () -> Unit) {
    val appContainer = LocalAppContainer.current
    val viewModel: FitnessViewModel = viewModel(
        factory = FitnessViewModel.factory(
            fitnessRepository = appContainer.fitnessRepository,
            errorReporter = appContainer.errorReporter
        )
    )

    val todayLog by viewModel.todayFitness.collectAsState()
    val weeklyLogs by viewModel.weeklyLogs.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val didMove = todayLog?.moved == true
    val todayMinutes = todayLog?.minutes ?: 30
    val todayType = todayLog?.type ?: "הליכה"

    var selectedMinutes by remember(todayMinutes) { mutableIntStateOf(todayMinutes) }
    var selectedType by remember(todayType) { mutableStateOf(todayType) }

    val streakCount = remember(weeklyLogs) {
        var count = 0
        for (log in weeklyLogs.sortedByDescending { it.date }) {
            if (log.moved) count++ else break
        }
        count
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("מעקב תנועה יומי", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("fitness_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OfflineIndicator(isOffline = false)
            when (val state = uiState) {
                is UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(message = state.message, onRetry = { viewModel.clearError() })
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FitnessStreakBanner(streakCount = streakCount)
                    FitnessTodayLogCard(
                        didMove = didMove,
                        selectedMinutes = selectedMinutes,
                        selectedType = selectedType,
                        onMinutesChange = { selectedMinutes = it },
                        onTypeChange = { selectedType = it },
                        onSave = { moved, mins, type -> viewModel.saveTodayFitness(moved, mins, type) }
                    )
                    FitnessWeeklyHistoryCard(weeklyLogs = weeklyLogs)
                }
            }
        }
    }
}

@Composable
private fun FitnessStreakBanner(streakCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = Color(0xFFD97706),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (streakCount > 0) "רצף תנועה: $streakCount ימים ברציפות!" else "בוא נתחיל רצף תנועה חדש היום!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "העיקר לזוז 20–30 דקות בכל יום. עקביות מנצחת עצימות.",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
private fun FitnessTodayLogCard(
    didMove: Boolean,
    selectedMinutes: Int,
    selectedType: String,
    onMinutesChange: (Int) -> Unit,
    onTypeChange: (String) -> Unit,
    onSave: (Boolean, Int, String) -> Unit
) {
    val movementTypes = listOf("הליכה", "ריצה", "אימון כוח", "יוגה")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("בדיקת תנועה להיום", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onSave(true, selectedMinutes, selectedType) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (didMove) Color(0xFF059669) else Color(0xFFE2E8F0)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = if (didMove) Color.White else Color.DarkGray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("כן, זזתי היום!", color = if (didMove) Color.White else Color.DarkGray, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onSave(false, 0, "מנוחה") },
                    modifier = Modifier.weight(0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("יום מנוחה", fontSize = 12.sp)
                }
            }
            if (didMove) {
                HorizontalDivider()
                Text("כמה דקות זזת? ($selectedMinutes דקות)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Slider(
                    value = selectedMinutes.toFloat(),
                    onValueChange = { onMinutesChange(it.toInt()) },
                    onValueChangeFinished = { onSave(true, selectedMinutes, selectedType) },
                    valueRange = 10f..120f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("סוג הפעילות:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    movementTypes.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = {
                                onTypeChange(t)
                                onSave(true, selectedMinutes, t)
                            },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FitnessWeeklyHistoryCard(weeklyLogs: List<FitnessEntry>) {
    val hebrewLocale = Locale.forLanguageTag("he")
    val last7Days = remember(weeklyLogs) {
        (6 downTo 0).map { daysAgo ->
            val date = LocalDate.now().minusDays(daysAgo.toLong())
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, hebrewLocale)
            val moved = weeklyLogs.firstOrNull { it.date == date }?.moved == true
            Pair(dayName, moved)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("שבוע אחרון מול העיניים", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                last7Days.forEach { (dayName, moved) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (moved) Color(0xFF059669) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (moved) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (moved) Color.White else Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(dayName, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
