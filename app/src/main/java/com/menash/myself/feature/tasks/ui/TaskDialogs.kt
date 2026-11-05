package com.menash.myself.feature.tasks.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.SpaceSelection
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.util.DateInputParser
import java.time.LocalDate

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, nextAction: String, isTop3: Boolean, isFive: Boolean, dueDate: LocalDate?, forWhom: ForWhom, isPrivate: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var nextAction by remember { mutableStateOf("") }
    var isTop3 by remember { mutableStateOf(false) }
    var isFive by remember { mutableStateOf(false) }
    var dueDateInput by remember { mutableStateOf(DateInputParser.todayString()) }
    var spaceSelection by remember { mutableStateOf(SpaceSelection.SHARED) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_str_274), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.ui_str_265)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nextAction,
                    onValueChange = { nextAction = it },
                    label = { Text(stringResource(R.string.ui_next_268)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDateInput,
                    onValueChange = { dueDateInput = it },
                    label = { Text(stringResource(R.string.ui_ddmmyyyy_259)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isTop3, onCheckedChange = { isTop3 = it })
                    Text(stringResource(R.string.ui_top_3_261), fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isFive, onCheckedChange = { isFive = it })
                    Text(stringResource(R.string.ui_five_before_11_270), fontSize = 12.sp)
                }
                Text(stringResource(R.string.ui_str_263), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpaceSelection.entries.forEach { option ->
                        FilterChip(
                            selected = spaceSelection == option,
                            onClick = { spaceSelection = option },
                            label = { Text(option.displayName, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title.trim(),
                            nextAction.trim(),
                            isTop3,
                            isFive,
                            DateInputParser.parseDateOrNull(dueDateInput),
                            spaceSelection.forWhom,
                            spaceSelection.isPrivate
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(stringResource(R.string.ui_str_272))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_str_92)) }
        }
    )
}

@Composable
fun ScheduleTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onSetDate: (LocalDate) -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_str_271), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\"${task.title}\"", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(stringResource(R.string.ui_str_267), fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(onClick = { onSetDate(LocalDate.now()) }) {
                Text(stringResource(R.string.ui_str_264))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.ui_str_269), color = Color(0xFFDC2626))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.ui_str_92))
                }
            }
        }
    )
}

@Composable
fun ConfirmDeleteTaskDialog(
    task: Task,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isShared = task.forWhom == ForWhom.BOTH && !task.isPrivate
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_str_266), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.ui_str_273, task.title))
                if (isShared) {
                    Text(
                        stringResource(R.string.ui_str_262),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.ui_str_260))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_str_92)) }
        }
    )
}
