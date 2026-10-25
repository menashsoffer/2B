package com.menash.myself.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.core.designsystem.component.SpaceBadge
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.core.domain.util.DateInputParser

@Composable
fun TasksFilterBar(
    visibilityFilter: VisibilityFilter,
    onFilterChange: (VisibilityFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = visibilityFilter == VisibilityFilter.MINE_AND_BOTH,
            onClick = { onFilterChange(VisibilityFilter.MINE_AND_BOTH) },
            label = { Text("שלי + של שנינו", fontSize = 11.sp) },
            modifier = Modifier.testTag("filter_mine_and_both")
        )
        FilterChip(
            selected = visibilityFilter == VisibilityFilter.ALL,
            onClick = { onFilterChange(VisibilityFilter.ALL) },
            label = { Text("הצג הכל", fontSize = 11.sp) },
            modifier = Modifier.testTag("filter_all")
        )
    }
}

@Composable
fun Top3FocusCard(
    tasks: List<Task>,
    viewerUid: String,
    onToggleDone: (Task) -> Unit,
    onRemoveTop3: (Task) -> Unit,
    onToggleFiveBefore11: (Task) -> Unit,
    onRequestDelete: (Task) -> Unit
) {
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
                Text("${tasks.size}/3", fontSize = 12.sp, color = Color.Gray)
            }

            if (tasks.isEmpty()) {
                Text("טרם בחרת Top 3 להיום. סמן משימה כ-Top 3 כדי למנוע עומס.", fontSize = 12.sp, color = Color.DarkGray)
            } else {
                tasks.forEach { task ->
                    TaskRow(
                        task = task,
                        viewerUid = viewerUid,
                        onToggleDone = { onToggleDone(task) },
                        onToggleTop3 = { onRemoveTop3(task) },
                        onToggleFiveBefore11 = { onToggleFiveBefore11(task) },
                        onDelete = { onRequestDelete(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun FiveBefore11Card(
    tasks: List<Task>,
    viewerUid: String,
    onToggleDone: (Task) -> Unit,
    onToggleTop3: (Task) -> Unit,
    onRemoveFiveBefore11: (Task) -> Unit,
    onRequestDelete: (Task) -> Unit
) {
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
                Text("${tasks.size}/5", fontSize = 12.sp, color = Color.Gray)
            }

            if (tasks.isEmpty()) {
                Text("משימות קצרות שתלויות רק בך שמסיימים בתחילת היום לתנופת עשייה.", fontSize = 11.sp, color = Color.DarkGray)
            } else {
                tasks.forEach { task ->
                    TaskRow(
                        task = task,
                        viewerUid = viewerUid,
                        onToggleDone = { onToggleDone(task) },
                        onToggleTop3 = { onToggleTop3(task) },
                        onToggleFiveBefore11 = { onRemoveFiveBefore11(task) },
                        onDelete = { onRequestDelete(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun UndatedTasksNoticeCard(undatedCount: Int) {
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
                    "יש $undatedCount משימות ללא תאריך יעד",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF78350F)
                )
                Text("משימה בלי תאריך יוצרת עומס שקט — קבע מועד או מחק.", fontSize = 11.sp, color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun TaskRow(
    task: Task,
    viewerUid: String = "",
    onToggleDone: () -> Unit,
    onToggleTop3: () -> Unit,
    onToggleFiveBefore11: () -> Unit,
    onDelete: () -> Unit,
    onScheduleClick: (() -> Unit)? = null
) {
    val dueDateDisplay = DateInputParser.formatDate(task.dueDate, task.rawDueDate)

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
            TaskRowBody(
                task = task,
                viewerUid = viewerUid,
                dueDateDisplay = dueDateDisplay,
                onScheduleClick = onScheduleClick,
                modifier = Modifier.weight(1f)
            )
            TaskRowActions(
                task = task,
                onToggleTop3 = onToggleTop3,
                onToggleFiveBefore11 = onToggleFiveBefore11,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun TaskRowBody(
    task: Task,
    viewerUid: String,
    dueDateDisplay: String,
    onScheduleClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            SpaceBadge(
                forWhom = task.forWhom,
                ownerUid = task.ownerUid.ifBlank { viewerUid },
                viewerUid = viewerUid
            )
            if (dueDateDisplay.isNotBlank()) {
                Text(text = "• $dueDateDisplay", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
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
}

@Composable
private fun TaskRowActions(
    task: Task,
    onToggleTop3: () -> Unit,
    onToggleFiveBefore11: () -> Unit,
    onDelete: () -> Unit
) {
    if (!task.isDone) {
        IconButton(onClick = onToggleTop3, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (task.isTop3) Icons.Default.Star else Icons.Default.StarOutline,
                contentDescription = "Top 3",
                tint = if (task.isTop3) Color(0xFFF59E0B) else Color.LightGray,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onToggleFiveBefore11, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Five Before 11",
                tint = if (task.isFiveBefore11) Color(0xFF059669) else Color.LightGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
        Icon(Icons.Default.Delete, contentDescription = "מחק", tint = Color.LightGray, modifier = Modifier.size(16.dp))
    }
}
