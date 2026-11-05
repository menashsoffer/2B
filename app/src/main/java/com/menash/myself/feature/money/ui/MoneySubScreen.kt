package com.menash.myself.feature.money.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.appColors

/** Shared heading and system-back behavior for money destinations. */
@Composable
fun MoneySubScreen(title: String, onBack: () -> Unit, subtitle: String? = null, content: @Composable () -> Unit = {}) {
    BackHandler(enabled = LocalMoneyBackActive.current) { onBack() }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.money_back), tint = MaterialTheme.appColors.textPrimary)
            }
            ScreenHeader(title = title, subtitle = subtitle)
        }
        content()
    }
}
