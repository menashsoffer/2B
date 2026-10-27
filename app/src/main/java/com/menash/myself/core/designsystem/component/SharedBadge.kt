package com.menash.myself.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

/**
 * One small shared icon. Private items draw nothing.
 * New screens use this instead of the three-colour ForWhom badges.
 */
@Composable
fun SharedBadge(
    isShared: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isShared) return
    Icon(
        imageVector = Icons.Outlined.Group,
        contentDescription = "משותף",
        tint = MaterialTheme.appColors.accent,
        modifier = modifier.size(Spacing.s16),
    )
}
