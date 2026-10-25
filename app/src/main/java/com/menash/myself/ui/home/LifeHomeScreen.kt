package com.menash.myself.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.menash.myself.calendar.CalendarHelper
import com.menash.myself.data.firestore.HouseholdRepository
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.CalendarEvent
import com.menash.myself.data.models.DreamItem
import com.menash.myself.data.models.FitnessLog
import com.menash.myself.data.models.Household
import com.menash.myself.data.models.SpaceType
import com.menash.myself.data.models.TaskItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeHomeScreen(
    onNavigate: (String) -> Unit,
    onOpenQuickCapture: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = Firebase.auth.currentUser
    val lifeRepo = remember { LifeHubRepository(context) }
    val householdRepo = remember { HouseholdRepository(context) }

    var household by remember { mutableStateOf<Household?>(null) }
    val allTasks by lifeRepo.observeTasks(null, household).collectAsState(initial = emptyList())
    val allDreams by lifeRepo.observeDreams(null, null, household).collectAsState(initial = emptyList())
    val inboxItems by lifeRepo.observeInbox(household).collectAsState(initial = emptyList())
    val todayFitness by lifeRepo.observeTodayFitness().collectAsState(initial = null)
    val weeklyFitness by lifeRepo.observeWeeklyFitness().collectAsState(initial = emptyList())

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

    LaunchedEffect(Unit) {
        household = householdRepo.getOrCreateHousehold()
        householdRepo.observeHousehold().collect { h ->
            if (h != null) household = h
        }
    }

    LaunchedEffect(hasCalendarPermission) {
        if (hasCalendarPermission) {
            calendarEvents = CalendarHelper.getTodayEvents(context)
        }
    }

    // Top 3 tasks for today
    val top3Tasks = remember(allTasks) {
        allTasks.filter { it.isTop3 && !it.isDone }.take(3)
    }

    // Featured dream (prioritize status: in planning or highest estimated cost)
    val featuredDream = remember(allDreams) {
        allDreams.firstOrNull { it.status == "חוסכים/מתכננים" || it.status == "בבדיקה" }
            ?: allDreams.firstOrNull { it.status == "חלום" }
    }

    // Fitness streak calculation
    val streakDays = remember(weeklyFitness) {
        weeklyFitness.takeWhile { it.moved }.size
    }

    var roadmapExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "מה מול העיניים",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        val partnerName = if (!household?.partnerEmail.isNullOrEmpty()) "שותף פעיל" else "משק בית פרטי"
                        Text(
                            text = "${user?.displayName ?: "שלום"} • $partnerName",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    // Inbox triage badge button
                    IconButton(
                        onClick = { onNavigate("inbox") },
                        modifier = Modifier.testTag("home_inbox_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (inboxItems.isNotEmpty()) {
                                    Badge(
                                        containerColor = Color(0xFFF59E0B),
                                        contentColor = Color.White
                                    ) {
                                        Text("${inboxItems.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Inbox, contentDescription = "אינבוקס ולכידה מהירה")
                        }
                    }

                    // Household & Profile button
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            // --- SECTION 1: TOP 3 TASKS (MAX 3 ITEMS) ---
            item {
                HomeSectionHeader(
                    title = "3 המשימות של היום (Top 3)",
                    subtitle = "רק מה שקריטי היום, ללא הסחות דעת",
                    actionText = "כל המשימות (${allTasks.count { !it.isDone }})",
                    onActionClick = { onNavigate("tasks") }
                )

                if (top3Tasks.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("tasks") }
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Checklist, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "לא נבחרו משימות Top 3 להיום. לחץ לבחירת משימות ממוקדות.",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        top3Tasks.forEach { task ->
                            TopTaskItemCard(
                                task = task,
                                onToggle = { scope.launch { lifeRepo.toggleTaskDone(task) } },
                                onCardClick = { onNavigate("tasks") }
                            )
                        }
                    }
                }
            }

            // --- SECTION 2: TODAY'S CALENDAR (MAX 2 ITEMS) ---
            item {
                HomeSectionHeader(
                    title = "אירועים ביומן להיום",
                    subtitle = "מתוך יומן המכשיר",
                    actionText = "יומן מלא",
                    onActionClick = { onNavigate("calendar") }
                )

                if (!hasCalendarPermission) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "אפשר גישה ליומן כדי לראות פגישות להיום מול העיניים.",
                                fontSize = 12.sp,
                                color = Color(0xFF1E3A8A),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                            ) {
                                Text("אפשר", fontSize = 11.sp)
                            }
                        }
                    }
                } else if (calendarEvents.isEmpty()) {
                    Text(
                        "היום פנוי מאירועים ביומן",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        calendarEvents.take(2).forEach { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (event.isAllDay) "כל היום" else "${event.startTime} - ${event.endTime}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = event.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // --- SECTION 3: DAILY FITNESS CHECK ---
            item {
                HomeSectionHeader(
                    title = "כושר ותנועה יומית",
                    subtitle = if (streakDays > 0) "רצף של $streakDays ימים פעילים!" else "בדיקה יומית מהירה",
                    actionText = "מעקב שבועי",
                    onActionClick = { onNavigate("fitness") }
                )

                val didMove = todayFitness?.moved == true
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (didMove) Color(0xFFF0FDF4) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable { onNavigate("fitness") }
                        .testTag("home_fitness_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (didMove) Icons.Default.DirectionsRun else Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = if (didMove) Color(0xFF16A34A) else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (didMove) "הזזת את הגוף היום! (${todayFitness?.minutes ?: 0} דק׳ ${todayFitness?.type ?: ""})" else "האם הזזת את הגוף היום?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (didMove) Color(0xFF15803D) else Color.DarkGray
                            )
                            Text(
                                text = if (didMove) "כל הכבוד, המשך כך!" else "לחץ לסימון מהיר של הפעילות",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = didMove,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    lifeRepo.saveTodayFitness(
                                        moved = checked,
                                        minutes = if (checked && (todayFitness?.minutes ?: 0) == 0) 30 else (todayFitness?.minutes ?: 0),
                                        type = todayFitness?.type ?: "הליכה"
                                    )
                                }
                            },
                            modifier = Modifier.testTag("home_fitness_quick_switch")
                        )
                    }
                }
            }

            // --- SECTION 4: ONE FEATURED DREAM ---
            item {
                HomeSectionHeader(
                    title = "חלום ממוקד",
                    subtitle = "חלום אחד בתהליך חסכון או תכנון",
                    actionText = "כל החלומות (${allDreams.size})",
                    onActionClick = { onNavigate("dreams") }
                )

                if (featuredDream == null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("dreams") }
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.StarBorder, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("אין כרגע חלום מוגדר. לחץ להוספת חלום או יעד משפחתי.", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                } else {
                    FeaturedDreamCard(
                        dream = featuredDream,
                        onClick = { onNavigate("dreams") }
                    )
                }
            }

            // --- SECTION 5: ONE-TAP ACCESS TO DEEP SECTIONS ---
            item {
                Text(
                    text = "גישה מהירה לכל התחומים",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickNavButton(
                        icon = Icons.Default.Calculate,
                        label = "המחשבונים שלי",
                        badge = "8",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("calculators") }
                    )
                    QuickNavButton(
                        icon = Icons.Default.FolderOpen,
                        label = "קטלוג וקבצים",
                        badge = "CSV",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("catalog") }
                    )
                    QuickNavButton(
                        icon = Icons.Default.ListAlt,
                        label = "חלומות ורשימות",
                        badge = "${allDreams.size}",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("dreams") }
                    )
                }
            }

            // --- SECTION 6: COLLAPSIBLE ROADMAP (PHASES 2 & 3) ---
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .testTag("roadmap_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { roadmapExpanded = !roadmapExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Upcoming, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "מפת דרכים — בקרוב (שלבים 2 ו-3)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Icon(
                                if (roadmapExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }

                        AnimatedVisibility(visible = roadmapExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RoadmapItem(
                                    phase = "שלב 2 — כספים וחשבונות",
                                    desc = "תזרים מזומנים חודשי אוטומטי מייבוא בנקים ואשראי, חלוקה לקטגוריות, ניהול הוראות קבע, ויעד יתרה חיובית מקושר לחיסכון החלומות."
                                )
                                RoadmapItem(
                                    phase = "שלב 2 — בריאות",
                                    desc = "סנכרון נתוני הליכה ופעילות אוטומטית מחיישני הטלפון לתוך המעקב היומי."
                                )
                                RoadmapItem(
                                    phase = "שלב 3 — תפעול החיים",
                                    desc = "ארכיון מסמכים ותעודות אחריות, ניהול מלאי ביתי וציוד, ביקורת מנויים תקופתית, ותחזוקה שוטפת של הבית והרכב."
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    subtitle: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        TextButton(
            onClick = onActionClick,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(actionText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TopTaskItemCard(
    task: TaskItem,
    onToggle: () -> Unit,
    onCardClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("top_task_${task.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isDone) Color.Gray else Color.Black
                )
                if (task.nextAction.isNotEmpty()) {
                    Text(
                        text = "פעולה הבאה: ${task.nextAction}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (task.space == SpaceType.SHARED.id || task.space == SpaceType.SHARED.displayName) {
                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "משותף",
                        color = Color(0xFF166534),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedDreamCard(
    dream: DreamItem,
    onClick: () -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val hasMoneyGoal = dream.estimatedCost > 0
    val progress = if (hasMoneyGoal) (dream.savedAmount / dream.estimatedCost).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("featured_dream_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dream.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = dream.status,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (hasMoneyGoal) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "נחסך: ₪${fmt.format(dream.savedAmount.toInt())} מתוך ₪${fmt.format(dream.estimatedCost.toInt())}",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        Text("${(progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0xFFE2E8F0)
                    )
                }
            } else if (dream.nextAction.isNotEmpty()) {
                Text("צעד הבא: ${dream.nextAction}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun QuickNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    badge: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(badge, fontSize = 9.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun RoadmapItem(phase: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(phase, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(desc, fontSize = 11.sp, color = Color.DarkGray, lineHeight = 16.sp)
    }
}
