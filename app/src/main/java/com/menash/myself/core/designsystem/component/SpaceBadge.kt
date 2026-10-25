package com.menash.myself.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.menash.myself.core.designsystem.tokens.ColorTokens
import com.menash.myself.core.designsystem.tokens.Spacing
import com.menash.myself.core.designsystem.tokens.TypographyTokens
import com.menash.myself.core.domain.model.ForWhom

/**
 * Visual badge for indicating item ownership/scope (Me, Partner, Both).
 * Applies distinct color schemes from ColorTokens for immediate visual scanning.
 */
@Composable
fun SpaceBadge(
    forWhom: ForWhom,
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: (() -> Unit)? = null,
    testTag: String = "space_badge"
) {
    val colorScheme = ColorTokens.forWhomColors(forWhom)
    val displayText = label ?: when (forWhom) {
        ForWhom.ME -> "שלי"
        ForWhom.PARTNER -> "בן/בת זוג"
        ForWhom.BOTH -> "של שנינו"
    }

    val shape = RoundedCornerShape(8.dp)
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colorScheme.background)
            .border(BorderStroke(1.dp, colorScheme.border), shape)
            .then(clickableModifier)
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            style = TypographyTokens.LabelMedium,
            color = colorScheme.content
        )
    }
}
