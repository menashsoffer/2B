package com.menash.myself.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.core.designsystem.tokens.ColorTokens
import com.menash.myself.core.designsystem.tokens.Spacing
import com.menash.myself.core.designsystem.tokens.TypographyTokens
import com.menash.myself.core.domain.model.ForWhom

/**
 * Visual badge for indicating item ownership/scope (Me, Partner, Both).
 * Applies distinct, contrast-verified color schemes in both light and dark themes.
 * Computes the label for the viewer using ForWhom.labelForViewer with localized strings.
 */
@Composable
fun SpaceBadge(
    forWhom: ForWhom,
    modifier: Modifier = Modifier,
    ownerUid: String = "",
    viewerUid: String = "",
    isOwner: Boolean = ownerUid.isEmpty() || viewerUid.isEmpty() || ownerUid == viewerUid,
    partnerName: String? = null,
    label: String? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onClick: (() -> Unit)? = null,
    testTag: String = "space_badge"
) {
    val colorScheme = ColorTokens.forWhomColors(forWhom, isDarkTheme)
    val mineLabel = stringResource(R.string.for_whom_mine)
    val bothLabel = stringResource(R.string.for_whom_both)
    val defaultPartnerText = stringResource(R.string.for_whom_partner_default)
    val effectivePartnerName = partnerName?.trim()?.ifEmpty { defaultPartnerText } ?: defaultPartnerText

    val displayText = label ?: forWhom.labelForViewer(
        isOwner = isOwner,
        partnerName = effectivePartnerName,
        mineLabel = mineLabel,
        bothLabel = bothLabel
    )

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
