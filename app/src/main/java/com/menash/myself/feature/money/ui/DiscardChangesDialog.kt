package com.menash.myself.feature.money.ui

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.menash.myself.R

/** «לצאת בלי לשמור?». Back and «המשך לערוך» keep the form. «צא בלי לשמור» drops it. */
@Composable
fun DiscardChangesDialog(onKeep: () -> Unit, onLeave: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeep,
        modifier = Modifier.testTag("discard_confirm"),
        title = { Text(stringResource(R.string.discard_title)) },
        confirmButton = {
            TextButton(
                onClick = onLeave,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).testTag("discard_leave"),
            ) { Text(stringResource(R.string.discard_leave)) }
        },
        dismissButton = {
            TextButton(
                onClick = onKeep,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).testTag("discard_keep"),
            ) { Text(stringResource(R.string.discard_keep)) }
        },
    )
}
