package com.menash.myself.ui.tasks

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.menash.myself.data.firestore.HouseholdRepository
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.SpaceType
import com.menash.myself.data.models.TaskItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { LifeHubRepository(context) }
    val householdRepo = remember { HouseholdRepository(context) }

    val household by householdRepo.observeHousehold().collectAsState(initial = null)
    var selectedSpaceFilter by remember { mutableStateOf<String?>(null) }

    val tasks by repo.observeTasks(selectedSpaceFilter, household).collectAsState(initial = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var taskToSchedule by remember { mutableStateOf<TaskItem?>(null) }

    // Partition tasks
    val top3Tasks = tasks.filter { it.isTop3 && !it.isDone }
    val fiveBefore11Tasks = tasks.filter { it.isFiveBefore11 && !it.isDone }
    val undatedTasks = tasks.filter { it.dueDate.isBlank() && !it.isDone && !it.isTop3 && !it.isFiveBefore11 }
    val regularTasks = tasks.filter { !it.isDone && !it.isTop3 && !it.isFiveBefore11 }
    val completedTasks = tasks.filter { it.isDone }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("משימות יומיות", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tasks_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "הוסף משימה")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Spaces filter
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val spaces = listOf("הכל", "משותף", "שלי", "של אשתי")
                    spaces.forEach { s ->
                        val isSelected = (selectedSpaceFilter == null && s == "הכל") || selectedSpaceFilter == s
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSpaceFilter = if (s == "הכל") null else s },
                            label = { Text(s, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Top 3 Focus Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⭐ Top 3 של היום (מיקוד עליון)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${top3Tasks.size}/3", fontSize = 12.sp, color = Color.Gray)
                        }

                        if (top3Tasks.isEmpty()) {
                            Text("טרם בחרת Top 3 להיום. סמן משימה כ-Top 3 כדי למנוע עומס.", fontSize = 12.sp, color = Color.DarkGray)
                        } else {
                            top3Tasks.forEach { task ->
                                TaskRow(
                                    task = task,
                                    onToggleDone = { scope.launch { repo.toggleTaskDone(task) } },
                                    onToggleTop3 = { scope.launch { repo.setTaskTop3(task, false) } },
                                    onToggleFiveBefore11 = { scope.launch { repo.setTaskFiveBefore11(task, !task.isFiveBefore11) } },
                                    onDelete = { scope.launch { repo.deleteTask(task.id) } }
                                )
                            }
                        }
                    }
                }
            }

            // Five Before 11 (5 short tasks that depend only on me, done by 11:00)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡ Five Before 11 (5 משימות קצרות עד 11:00)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${fiveBefore11Tasks.size}/5", fontSize = 12.sp, color = Color.Gray)
                        }

                        if (fiveBefore11Tasks.isEmpty()) {
                            Text("משימות קצרות שתלויות רק בך שמסיימים בתחילת היום לתנופת עשייה.", fontSize = 11.sp, color = Color.DarkGray)
                        } else {
                            fiveBefore11Tasks.forEach { task ->
                                TaskRow(
                                    task = task,
                                    onToggleDone = { scope.launch { repo.toggleTaskDone(task) } },
                                    onToggleTop3 = { scope.launch { repo.setTaskTop3(task, !task.isTop3) } },
                                    onToggleFiveBefore11 = { scope.launch { repo.setTaskFiveBefore11(task, false) } },
                                    onDelete = { scope.launch { repo.deleteTask(task.id) } }
                                )
                            }
                        }
                    }
                }
            }

            // Weekly Prompt for Undated Tasks ("קבע מועד או מחק")
            if (undatedTasks.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFB45309))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "יש ${undatedTasks.size} משימות ללא תאריך יעד",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF78350F)
                                )
                                Text("משימה בלי תאריך יוצרת עומס שקט — קבע מועד או מחק.", fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }

            // All Tasks List
            item {
                Text("כל המשימות הפעילות", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }

            items(regularTasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggleDone = { scope.launch { repo.toggleTaskDone(task) } },
                    onToggleTop3 = { scope.launch { repo.setTaskTop3(task, true) } },
                    onToggleFiveBefore11 = { scope.launch { repo.setTaskFiveBefore11(task, true) } },
                    onDelete = { scope.launch { repo.deleteTask(task.id) } },
                    onScheduleClick = { taskToSchedule = task }
                )
            }

            // Completed Tasks (Collapsed at bottom)
            if (completedTasks.isNotEmpty()) {
                item {
                    Text("הושלמו (${completedTasks.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                }
                items(completedTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggleDone = { scope.launch { repo.toggleTaskDone(task) } },
                        onToggleTop3 = {},
                        onToggleFiveBefore11 = {},
                        onDelete = { scope.launch { repo.deleteTask(task.id) } }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    // Create Task Dialog
    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { title, nextAction, isTop3, isFive, date, space ->
                scope.launch {
                    repo.createTask(
                        title = title,
                        nextAction = nextAction,
                        isTop3 = isTop3,
                        isFiveBefore11 = isFive,
                        dueDate = date,
                        space = space,
                        household = household
                    )
                    showCreateDialog = false
                }
            }
        )
    }

    // Schedule Dialog for undated task
    taskToSchedule?.let { task ->
        ScheduleTaskDialog(
            task = task,
            onDismiss = { taskToSchedule = null },
            onSetDate = { newDate ->
                scope.launch {
                    repo.updateTaskDate(task.id, newDate)
                    taskToSchedule = null
                }
            },
            onDelete = {
                scope.launch {
                    repo.deleteTask(task.id)
                    taskToSchedule = null
                }
            }
        )
    }
}

@Composable
fun TaskRow(
    task: TaskItem,
    onToggleDone: () -> Unit,
    onToggleTop3: () -> Unit,
    onToggleFiveBefore11: () -> Unit,
    onDelete: () -> Unit,
    onScheduleClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (task.isDone) Color(0xFFF8FAFC) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isDone) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onToggleDone() },
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )
            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = if (task.isDone) Color.Gray else Color(0xFF0F172A)
                )
                if (task.nextAction.isNotBlank()) {
                    Text(
                        text = "↳ ${task.nextAction}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = SpaceType.fromId(task.space).displayName,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    if (task.dueDate.isNotBlank()) {
                        Text(
                            text = "• ${task.dueDate}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else if (!task.isDone && onScheduleClick != null) {
                        Text(
                            text = "• ללא תאריך (קבע)",
                            fontSize = 10.sp,
                            color = Color(0xFFD97706),
                            modifier = Modifier.clickable { onScheduleClick() }
                        )
                    }
                }
            }

            if (!task.isDone) {
                // Top 3 action button
                IconButton(onClick = onToggleTop3, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (task.isTop3) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = "Top 3",
                        tint = if (task.isTop3) Color(0xFFF59E0B) else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Five before 11 action button
                IconButton(onClick = onToggleFiveBefore11, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Five Before 11",
                        tint = if (task.isFiveBefore11) Color(0xFF059669) else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "מחק", tint = Color.LightGray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, nextAction: String, isTop3: Boolean, isFive: Boolean, date: String, space: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var nextAction by remember { mutableStateOf("") }
    var isTop3 by remember { mutableStateOf(false) }
    var isFive by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var space by remember { mutableStateOf(SpaceType.SHARED) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("משימה חדשה", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("משימה (מתחילה בפועל, למשל: לקנות, לבדוק, לשלוח)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nextAction,
                    onValueChange = { nextAction = it },
                    label = { Text("צעד ראשון מיידי (Next Action)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("תאריך יעד (DD/MM/YYYY)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isTop3, onCheckedChange = { isTop3 = it })
                    Text("הגדר כ-Top 3 של היום", fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isFive, onCheckedChange = { isFive = it })
                    Text("הגדר כ-Five Before 11", fontSize = 12.sp)
                }

                Text("מרחב:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpaceType.entries.forEach { s ->
                        FilterChip(
                            selected = space == s,
                            onClick = { space = s },
                            label = { Text(s.displayName, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), nextAction.trim(), isTop3, isFive, dueDate.trim(), space.id)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("שמור משימה")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        }
    )
}

@Composable
fun ScheduleTaskDialog(
    task: TaskItem,
    onDismiss: () -> Unit,
    onSetDate: (String) -> Unit,
    onDelete: () -> Unit
) {
    val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("משימה ללא תאריך: קבע מועד או מחק", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\"${task.title}\"", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("משימות בלי מועד סופן להידחות לנצח. בחר מה לעשות:", fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(onClick = { onSetDate(today) }) {
                Text("קבע להיום")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text("מחק משימה", color = Color(0xFFDC2626))
                }
                TextButton(onClick = onDismiss) {
                    Text("ביטול")
                }
            }
        }
    )
}
