package com.menash.myself.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = MaterialTheme.appColors
    Column(modifier = modifier) {
        Text(text = title, style = AppType.title, color = colors.textPrimary)
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(Spacing.s4))
            Text(text = subtitle, style = AppType.caption, color = colors.textSecondary)
        }
    }
}
