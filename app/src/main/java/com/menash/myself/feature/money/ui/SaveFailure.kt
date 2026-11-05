package com.menash.myself.feature.money.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.appColors

@Composable
fun SaveFailure(message: String?, onRetry: () -> Unit) {
    if (message == null) return
    Text(message, color = MaterialTheme.appColors.danger)
    TextButton(onClick = onRetry) { Text(stringResource(R.string.money_retry)) }
}
