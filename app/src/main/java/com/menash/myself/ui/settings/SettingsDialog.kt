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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.data.io.BackupRestoreManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsDialog(
    currentRate: Double,
    lastBackupTimestamp: Long,
    onDismiss: () -> Unit,
    onSaveRate: (Double) -> Unit
) {
    var rateInput by remember { mutableStateOf(currentRate.toString()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val fileRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val jsonStr = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                    if (jsonStr != null) {
                        val success = BackupRestoreManager.restoreFromJson(context, jsonStr)
                        if (success) {
                            Toast.makeText(context, "הנתונים שוחזרו בהצלחה!", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "שגיאה בפורמט קובץ הגיבוי", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "שגיאה בקריאת הקובץ: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val lastBackupText = if (lastBackupTimestamp == 0L) {
        "טרם בוצע גיבוי"
    } else {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastBackupTimestamp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("הגדרות וניהול נתונים", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // USD to ILS rate
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("שער דולר ידני (USD → ₪)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("usd_rate_input"),
                    supportingText = { Text("משפיע מיידית על כל המחשבונים (מנויים, טיולים וכו')") }
                )

                HorizontalDivider()

                // Backup and Restore Section
                Text("גיבוי ושחזור מקומי (JSON)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    "גיבוי אחרון: $lastBackupText",
                    fontSize = 12.sp,
                    color = Color.DarkGray
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
                            scope.launch {
                                BackupRestoreManager.createAndShareBackup(context)
                            }
                        },
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
                        onClick = {
                            fileRestoreLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("שחזר מקובץ")
                    }
                }
            }
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
