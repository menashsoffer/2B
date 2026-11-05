package com.menash.myself.feature.importcatalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvMappingDialog(
    headers: List<String>,
    previewRows: List<List<String>>,
    targetFields: List<String>,
    onDismiss: () -> Unit,
    onConfirmMap: (Map<String, Int>) -> Unit
) {
    // Auto map fields matching headers
    val mapping = remember {
        mutableStateMapOf<String, Int>().apply {
            targetFields.forEach { target ->
                val idx = headers.indexOfFirst { h ->
                    h.contains(target, ignoreCase = true) || target.contains(h, ignoreCase = true)
                }
                if (idx != -1) {
                    this[target] = idx
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.ui_csv_414), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    stringResource(R.string.ui_headerssize_412, headers.size),
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mapping selectors
                items(targetFields) { field ->
                    var expanded by remember { mutableStateOf(false) }
                    val selectedIdx = mapping[field]
                    val selectedLabel = if (selectedIdx != null && selectedIdx in headers.indices) {
                        headers[selectedIdx]
                    } else {
                        stringResource(R.string.ui_str_409)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(field, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier.weight(1.3f)
                            ) {
                                OutlinedTextField(
                                    value = selectedLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .testTag("mapping_dropdown_$field"),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.ui_str_413), color = Color.Gray) },
                                        onClick = {
                                            mapping.remove(field)
                                            expanded = false
                                        }
                                    )
                                    headers.forEachIndexed { i, h ->
                                        DropdownMenuItem(
                                            text = { Text(h) },
                                            onClick = {
                                                mapping[field] = i
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Table preview of first rows
                if (previewRows.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.ui_str_410), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        val scrollState = rememberScrollState()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                // Header row
                                Row(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
                                    headers.forEach { h ->
                                        Text(
                                            h,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.widthIn(min = 90.dp).padding(4.dp)
                                        )
                                    }
                                }
                                // Data rows
                                previewRows.take(3).forEach { row ->
                                    Row {
                                        headers.indices.forEach { idx ->
                                            val cell = row.getOrNull(idx) ?: ""
                                            Text(
                                                cell,
                                                fontSize = 11.sp,
                                                modifier = Modifier.widthIn(min = 90.dp).padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmMap(mapping.toMap()) },
                modifier = Modifier.testTag("confirm_import_button")
            ) {
                Text(stringResource(R.string.ui_str_411))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_import_button")
            ) {
                Text(stringResource(R.string.ui_str_92))
            }
        }
    )
}
