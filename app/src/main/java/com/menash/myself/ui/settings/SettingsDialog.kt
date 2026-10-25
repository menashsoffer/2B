package com.menash.myself.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.core.di.LocalAppContainer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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
                            val msg = if (success) "הנתונים שוחזרו בהצלחה!" else "שגיאה בפורמט קובץ הגיבוי"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success) onDismiss()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "שגיאה בקריאת הקובץ: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("הגדרות וניהול נתונים", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            SettingsDialogBody(
                rateInput = rateInput,
                onRateInputChange = { rateInput = it },
                lastBackupTimestamp = lastBackupTimestamp,
                onBackupClick = {
                    if (onBackupNow != null) {
                        onBackupNow()
                    } else {
                        scope.launch { appContainer.settingsRepository.createAndShareBackup(context) }
                    }
                },
                onRestoreClick = {
                    fileRestoreLauncher.launch(arrayOf("application/json", "*/*"))
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = rateInput.toDoubleOrNull() ?: currentRate
                    onSaveRate(rate)
                },
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("שמור הגדרות")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_settings_button")
            ) {
                Text("סגור")
            }
        }
    )
}

@Composable
private fun SettingsDialogBody(
    rateInput: String,
    onRateInputChange: (String) -> Unit,
    lastBackupTimestamp: Long,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    val lastBackupText = if (lastBackupTimestamp == 0L) {
        "טרם בוצע גיבוי"
    } else {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastBackupTimestamp))
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = rateInput,
            onValueChange = onRateInputChange,
            label = { Text("שער דולר ידני (USD → ₪)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("usd_rate_input"),
            supportingText = { Text("משפיע מיידית על כל המחשבונים (מנויים, טיולים וכו')") }
        )

        HorizontalDivider()

        Text("גיבוי ושחזור מקומי (JSON)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(
            "גיבוי אחרון: $lastBackupText",
            fontSize = 12.sp,
            color = Color.DarkGray
        )
        Text(
            "שים לב: שחזור פריטים משותפים (של שנינו) יעדכן את הנתונים עבור שני בני הזוג.",
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
                onClick = onBackupClick,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("גבה כעת")
            }

            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag("restore_backup_button"),
                onClick = onRestoreClick,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("שחזר מקובץ")
            }
        }
    }
}
