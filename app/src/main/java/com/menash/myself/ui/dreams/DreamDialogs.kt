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

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
@Composable
fun CreateDreamDialog(
    defaultList: String,
    onDismiss: () -> Unit,
    onSave: (Dream) -> Unit
) {
    val allListsLabel = stringResource(R.string.ui_str_123)
    val defaultListLabel = stringResource(R.string.ui_str_311)
    val appIdeaListLabel = stringResource(R.string.ui_str_307)
    val wishListLabel = stringResource(R.string.ui_str_321)
    val bucketListLabel = stringResource(R.string.ui_str_331)

    var title by remember { mutableStateOf("") }
    var list by remember(defaultList, allListsLabel, defaultListLabel) {
        mutableStateOf(if (defaultList == allListsLabel) defaultListLabel else defaultList)
    }
    var estimatedCostInput by remember { mutableStateOf("") }
    var spaceSelection by remember(list, appIdeaListLabel) {
        mutableStateOf(if (list == appIdeaListLabel) SpaceSelection.MINE else SpaceSelection.SHARED)
    }

    val availableLists = listOf(defaultListLabel, wishListLabel, appIdeaListLabel, bucketListLabel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_str_332), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.ui_str_329)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.ui_str_339), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableLists.forEach { l ->
                        FilterChip(
                            selected = list == l,
                            onClick = {
                                list = l
                                spaceSelection = if (l == appIdeaListLabel) SpaceSelection.MINE else SpaceSelection.SHARED
                            },
                            label = { Text(l, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = estimatedCostInput,
                    onValueChange = { estimatedCostInput = it },
                    label = { Text(stringResource(R.string.ui_str_334)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
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
                Text(stringResource(R.string.ui_str_328))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_str_92)) }
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
        stringResource(R.string.ui_1_323),
        stringResource(R.string.ui_2_324),
        stringResource(R.string.ui_3_330),
        stringResource(R.string.ui_4_338),
        stringResource(R.string.ui_5_336),
        stringResource(R.string.ui_6_333),
        stringResource(R.string.ui_7_322)
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
        title = { Text(stringResource(R.string.ui_7_335), fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        stringResource(R.string.ui_str_340),
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
                        label = { Text(stringResource(R.string.ui_next_268), fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(answers.toMap(), nextActionInput.trim()) }) {
                Text(stringResource(R.string.ui_str_325))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_str_92)) }
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
        title = { Text(stringResource(R.string.ui_str_326), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.ui_str_337, dream.title))
                if (isShared) {
                    Text(
                        stringResource(R.string.ui_str_327),
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
