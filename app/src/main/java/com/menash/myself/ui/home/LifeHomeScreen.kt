package com.menash.myself.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.menash.myself.calendar.CalendarHelper
import com.menash.myself.core.designsystem.component.ErrorState
import com.menash.myself.core.designsystem.component.LoadingState
import com.menash.myself.core.designsystem.component.OfflineIndicator
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.core.domain.model.CalendarEvent
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.FitnessEntry
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.feature.home.LifeHomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeHomeScreen(
    onNavigate: (String) -> Unit,
    onOpenQuickCapture: () -> Unit
) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val viewModel: LifeHomeViewModel = viewModel(
        factory = LifeHomeViewModel.factory(
            taskRepository = appContainer.taskRepository,
            dreamRepository = appContainer.dreamRepository,
            inboxRepository = appContainer.inboxRepository,
            fitnessRepository = appContainer.fitnessRepository,
            householdRepository = appContainer.householdRepository,
            errorReporter = appContainer.errorReporter
        )
    )

    val user = runCatching { Firebase.auth.currentUser }.getOrNull()
    val viewerUid = user?.uid ?: ""
    val household by viewModel.household.collectAsState()
    val visibilityFilter by viewModel.visibilityFilter.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()
    val allDreams by viewModel.dreams.collectAsState()
    val inboxItems by viewModel.inboxItems.collectAsState()
    val todayFitness by viewModel.todayFitness.collectAsState()
    val weeklyFitness by viewModel.weeklyFitness.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var calendarEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCalendarPermission = isGranted
        if (isGranted) {
            calendarEvents = CalendarHelper.getTodayEvents(context)
        }
    }

    LaunchedEffect(hasCalendarPermission) {
        if (hasCalendarPermission) {
            calendarEvents = CalendarHelper.getTodayEvents(context)
        }
    }

    Scaffold(
        topBar = {
            LifeHomeTopBar(
                userName = user?.displayName ?: "שלום",
                hasPartner = !household?.partnerEmail.isNullOrEmpty(),
                inboxCount = inboxItems.size,
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenQuickCapture,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("לכידה מהירה", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("global_quick_capture_fab")
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OfflineIndicator(isOffline = false)
            when (val state = uiState) {
                is UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(message = state.message, onRetry = { viewModel.clearError() })
                else -> LifeHomeDashboardContent(
                    visibilityFilter = visibilityFilter,
                    allTasks = allTasks,
                    allDreams = allDreams,
                    todayFitness = todayFitness,
                    weeklyFitness = weeklyFitness,
                    viewerUid = viewerUid,
                    hasCalendarPermission = hasCalendarPermission,
                    calendarEvents = calendarEvents,
                    onFilterChange = { viewModel.setVisibilityFilter(it) },
                    onToggleTaskDone = { viewModel.toggleTaskDone(it) },
                    onRequestCalendarPermission = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                    onSaveTodayFitness = { moved, mins, type -> viewModel.saveTodayFitness(moved, mins, type) },
                    onNavigate = onNavigate
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LifeHomeTopBar(
    userName: String,
    hasPartner: Boolean,
    inboxCount: Int,
    onNavigate: (String) -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(text = "מה מול העיניים", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                val partnerName = if (hasPartner) "שותף פעיל" else "משק בית פרטי"
                Text(
                    text = "$userName • $partnerName",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        },
        actions = {
            IconButton(
                onClick = { onNavigate("inbox") },
                modifier = Modifier.testTag("home_inbox_button")
            ) {
                BadgedBox(
                    badge = {
                        if (inboxCount > 0) {
                            Badge(containerColor = Color(0xFFF59E0B), contentColor = Color.White) {
                                Text("$inboxCount")
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Inbox, contentDescription = "אינבוקס ולכידה מהירה")
                }
            }
            IconButton(
                onClick = { onNavigate("household") },
                modifier = Modifier.testTag("home_household_button")
            ) {
                Icon(Icons.Default.Group, contentDescription = "ניהול משק בית")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
private fun LifeHomeDashboardContent(
    visibilityFilter: VisibilityFilter,
    allTasks: List<Task>,
    allDreams: List<Dream>,
    todayFitness: FitnessEntry?,
    weeklyFitness: List<FitnessEntry>,
    viewerUid: String,
    hasCalendarPermission: Boolean,
    calendarEvents: List<CalendarEvent>,
    onFilterChange: (VisibilityFilter) -> Unit,
    onToggleTaskDone: (Task) -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onSaveTodayFitness: (Boolean, Int, String) -> Unit,
    onNavigate: (String) -> Unit
) {
    val top3Tasks = remember(allTasks) { allTasks.filter { it.isTop3 && !it.isDone }.take(3) }
    val featuredDream = remember(allDreams) {
        allDreams.firstOrNull { it.status == "חוסכים/מתכננים" || it.status == "בבדיקה" }
            ?: allDreams.firstOrNull { it.status == "חלום" }
    }
    val streakDays = remember(weeklyFitness) { weeklyFitness.takeWhile { it.moved }.size }
    var roadmapExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        item {
            HomeVisibilityFilterRow(visibilityFilter = visibilityFilter, onFilterChange = onFilterChange)
        }
        item {
            HomeTop3TasksSection(
                top3Tasks = top3Tasks,
                activeTasksCount = allTasks.count { !it.isDone },
                viewerUid = viewerUid,
                onToggleTaskDone = onToggleTaskDone,
                onOpenTasks = { onNavigate("tasks") }
            )
        }
        item {
            HomeTodayCalendarSection(
                hasCalendarPermission = hasCalendarPermission,
                calendarEvents = calendarEvents,
                onRequestPermission = onRequestCalendarPermission,
                onOpenCalendar = { onNavigate("calendar") }
            )
        }
        item {
            HomeDailyFitnessSection(
                todayFitness = todayFitness,
                streakDays = streakDays,
                onSaveTodayFitness = onSaveTodayFitness,
                onOpenFitness = { onNavigate("fitness") }
            )
        }
        item {
            HomeFeaturedDreamAndQuickNavSection(
                featuredDream = featuredDream,
                dreamsCount = allDreams.size,
                roadmapExpanded = roadmapExpanded,
                onToggleRoadmap = { roadmapExpanded = !roadmapExpanded },
                onNavigate = onNavigate
            )
        }
    }
}
