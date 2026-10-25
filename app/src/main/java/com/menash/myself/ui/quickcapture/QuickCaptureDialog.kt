package com.menash.myself.ui.quickcapture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.data.models.SpaceType

@Composable
fun QuickCaptureDialog(
    onDismiss: () -> Unit,
    onSave: (text: String, space: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedSpace by remember { mutableStateOf(SpaceType.SHARED) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "לכידה מהירה (Inbox)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "זרוק לפה כל מחשבה, משימה או רעיון בלי לחשוב. נמיין מאוחר יותר בלחיצה אחת.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("מה עולה לך בראש עכשיו?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                        .testTag("quick_capture_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                // Space Toggle: שלי / משותף / של אשתי
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("שיוך מרחב:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SpaceType.entries.forEach { space ->
                            FilterChip(
                                selected = selectedSpace == space,
                                onClick = { selectedSpace = space },
                                label = { Text(space.displayName, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSave(text.trim(), selectedSpace.id)
                        onDismiss()
                    }
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("quick_capture_submit_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("שמור ל-Inbox")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("סגור")
            }
        }
    )
}
