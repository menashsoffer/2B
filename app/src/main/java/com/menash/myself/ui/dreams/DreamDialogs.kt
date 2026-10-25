package com.menash.myself.ui.dreams

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.SpaceSelection

@Composable
fun CreateDreamDialog(
    defaultList: String,
    onDismiss: () -> Unit,
    onSave: (Dream) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var list by remember { mutableStateOf(if (defaultList == "הכל") "לבית" else defaultList) }
    var estimatedCostInput by remember { mutableStateOf("") }
    var spaceSelection by remember {
        mutableStateOf(if (list == "פיתוח ואפליקציות") SpaceSelection.MINE else SpaceSelection.SHARED)
    }

    val availableLists = listOf("לבית", "טיסות משפחה", "פיתוח ואפליקציות", "אישי")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("הוספת חלום / יעד", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("שם החלום / הפריט") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("רשימה:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableLists.forEach { l ->
                        FilterChip(
                            selected = list == l,
                            onClick = {
                                list = l
                                spaceSelection = if (l == "פיתוח ואפליקציות") SpaceSelection.MINE else SpaceSelection.SHARED
                            },
                            label = { Text(l, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = estimatedCostInput,
                    onValueChange = { estimatedCostInput = it },
                    label = { Text("עלות משוערת (₪) - אופציונלי") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("מרחב:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                            Dream(
                                title = title.trim(),
                                list = list,
                                estimatedCost = Money.fromShekels(estimatedCostInput.toDoubleOrNull() ?: 0.0),
                                forWhom = spaceSelection.forWhom,
                                isPrivate = spaceSelection.isPrivate
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("שמור חלום")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        }
    )
}

@Composable
fun AppIdeaTemplateDialog(
    dream: Dream,
    onDismiss: () -> Unit,
    onSave: (questions: Map<String, String>, nextAction: String) -> Unit
) {
    val questionPrompts = listOf(
        "1. מה הבעיה הכואבת?",
        "2. למי זה מיועד בדיוק?",
        "3. מה הפתרון המינימלי (MVP)?",
        "4. מה היתרון הייחודי / הוגן?",
        "5. איך מרוויחים / מודל עסקי?",
        "6. מה הסיכון הכי גדול?",
        "7. מה המדד להצלחה ראשונית?"
    )

    val answers = remember {
        mutableStateMapOf<String, String>().apply {
            questionPrompts.forEach { q ->
                this[q] = dream.appIdeaQuestions[q] ?: ""
            }
        }
    }
    var nextActionInput by remember { mutableStateOf(dream.nextAction) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("תבנית 7 שאלות לרעיון + Next Action", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "ענה בקצרה ובדיוק — מונע התפזרות ועומס קוגניטיבי:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
                items(questionPrompts) { question ->
                    OutlinedTextField(
                        value = answers[question] ?: "",
                        onValueChange = { answers[question] = it },
                        label = { Text(question, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                item {
                    OutlinedTextField(
                        value = nextActionInput,
                        onValueChange = { nextActionInput = it },
                        label = { Text("צעד ראשון מיידי (Next Action)", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(answers.toMap(), nextActionInput.trim()) }) {
                Text("שמור תבנית")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        }
    )
}

@Composable
fun ConfirmDeleteDreamDialog(
    dream: Dream,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isShared = dream.forWhom == ForWhom.BOTH && !dream.isPrivate
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("מחיקת חלום", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("האם למחוק את החלום \"${dream.title}\"?")
                if (isShared) {
                    Text(
                        "שים לב: זהו חלום משותף לשניכם במשק הבית.",
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
                Text("מחק")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        }
    )
}
