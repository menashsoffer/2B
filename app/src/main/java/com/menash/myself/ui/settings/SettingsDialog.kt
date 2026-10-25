package com.menash.myself.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menash.myself.R
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.core.external.hebcal.IsraeliCity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun SettingsDialog(
    currentRate: Double,
    lastBackupTimestamp: Long,
    onDismiss: () -> Unit,
    onSaveRate: (Double) -> Unit,
    onBackupNow: (() -> Unit)? = null,
    onRestoreJson: ((String) -> Unit)? = null
) {
    var rateInput by remember { mutableStateOf(currentRate.toString()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appContainer = LocalAppContainer.current

    var showCityDropdown by remember { mutableStateOf(false) }
    val boiRateState by appContainer.exchangeRateRepository.currentRateFlow.collectAsStateWithLifecycle()
    val selectedCityState by appContainer.hebcalRepository.selectedCityFlow.collectAsStateWithLifecycle()
    var useManualRate by remember(boiRateState?.isManualOverride) { mutableStateOf(boiRateState?.isManualOverride == true) }

    val fileRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val jsonStr = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                    if (jsonStr != null) {
                        if (onRestoreJson != null) {
                            onRestoreJson(jsonStr)
                        } else {
                            val success = appContainer.settingsRepository.restoreFromJson(context, jsonStr)
                            val msg = if (success) context.getString(R.string.ui_str_252) else context.getString(R.string.ui_str_253)
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success) onDismiss()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.ui_emessage_246, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.ui_str_241), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Hebcal City Picker Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCityDropdown = true }
                        .testTag("hebcal_city_picker_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("עיר לזמני שבת וחגים:", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = selectedCityState?.nameHebrew ?: "בחר עיר",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (selectedCityState == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Text("נתונים: Hebcal.com", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                DropdownMenu(
                    expanded = showCityDropdown,
                    onDismissRequest = { showCityDropdown = false }
                ) {
                    IsraeliCity.DEFAULT_CITIES.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(city.nameHebrew) },
                            onClick = {
                                showCityDropdown = false
                                scope.launch { appContainer.hebcalRepository.selectCity(city) }
                            }
                        )
                    }
                }


                // BOI USD/ILS Rate Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("בנק ישראל, שער יציג:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    boiRateState?.let { r ->
                        val dateFormatted = r.rateDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        if (r.isManualOverride) {
                            Text("שער ידני: ₪${r.rate} / שער רשמי: ₪${r.rate} ($dateFormatted)", fontSize = 12.sp)
                            Text("דריסה ידנית פעילה", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                        } else {
                            Text("שער עדכני: ₪${r.rate} ($dateFormatted)", fontSize = 12.sp)
                        }
                        if (r.isStale) {
                            Text("לא עודכן לאחרונה", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    } ?: Text("לא הוגדר שער", fontSize = 12.sp, color = Color.Gray)
                }

                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text(stringResource(R.string.ui_usd_247)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("usd_rate_input"),
                    supportingText = { Text(stringResource(R.string.ui_str_251)) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = useManualRate,
                        onCheckedChange = { useManualRate = it },
                        modifier = Modifier.testTag("use_manual_rate_checkbox")
                    )
                    Text("השתמש בדריסה ידנית לשער הדולר", fontSize = 13.sp)
                }

                if (boiRateState?.isManualOverride == true) {
                    TextButton(
                        onClick = {
                            scope.launch { appContainer.exchangeRateRepository.clearManualRate() }
                        },
                        modifier = Modifier.testTag("return_to_official_rate_button")
                    ) {
                        Text("חזור לשער הרשמי של בנק ישראל")
                    }
                }

                HorizontalDivider()

                Text(stringResource(R.string.ui_json_248), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                val lastBackupText = if (lastBackupTimestamp == 0L) {
                    stringResource(R.string.ui_str_249)
                } else {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastBackupTimestamp))
                }
                Text(
                    stringResource(R.string.ui_lastbackuptext_244, lastBackupText),
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Text(
                    stringResource(R.string.ui_str_243),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backup_now_button"),
                        onClick = {
                            if (onBackupNow != null) {
                                onBackupNow()
                            } else {
                                scope.launch { appContainer.settingsRepository.createAndShareBackup(context) }
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.ui_str_245))
                    }

                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_backup_button"),
                        onClick = {
                            fileRestoreLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.ui_str_250))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = rateInput.toDoubleOrNull() ?: currentRate
                    onSaveRate(rate)
                    scope.launch {
                        if (useManualRate) {
                            appContainer.exchangeRateRepository.setManualRate(rate)
                        } else {
                            appContainer.exchangeRateRepository.clearManualRate()
                        }
                    }
                },
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text(stringResource(R.string.ui_str_242))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_settings_button")
            ) {
                Text(stringResource(R.string.ui_str_2))
            }
        }
    )
}
