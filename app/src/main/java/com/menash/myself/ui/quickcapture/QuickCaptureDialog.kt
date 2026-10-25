package com.menash.myself.ui.quickcapture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.SpaceSelection

@Composable
fun QuickCaptureDialog(
    onDismiss: () -> Unit,
    onSave: (text: String, forWhom: ForWhom, isPrivate: Boolean) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedSpace by remember { mutableStateOf(SpaceSelection.SHARED) }

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
            QuickCaptureFormBody(
                text = text,
                onTextChange = { text = it },
                selectedSpace = selectedSpace,
                onSelectSpace = { selectedSpace = it }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSave(text.trim(), selectedSpace.forWhom, selectedSpace.isPrivate)
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

@Composable
private fun QuickCaptureFormBody(
    text: String,
    onTextChange: (String) -> Unit,
    selectedSpace: SpaceSelection,
    onSelectSpace: (SpaceSelection) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "זרוק לפה כל מחשבה, משימה או רעיון בלי לחשוב. נמיין מאוחר יותר בלחיצה אחת.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("מה עולה לך בראש עכשיו?") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
                .testTag("quick_capture_input"),
            shape = RoundedCornerShape(12.dp),
            maxLines = 4
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("שיוך מרחב:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpaceSelection.entries.forEach { space ->
                    FilterChip(
                        selected = selectedSpace == space,
                        onClick = { onSelectSpace(space) },
                        label = { Text(space.displayName, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
