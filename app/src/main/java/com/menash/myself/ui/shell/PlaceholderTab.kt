package com.menash.myself.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.EmptyState
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.theme.appColors

@Composable
fun PlaceholderTab(
    title: String,
    icon: ImageVector,
    route: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(FloatingNavBarDefaults.contentPadding()),
    ) {
        EmptyState(
            title = title,
            description = stringResource(R.string.placeholder_building),
            icon = icon,
            modifier = Modifier.fillMaxSize(),
            testTag = "placeholder_$route",
        )
    }
}
