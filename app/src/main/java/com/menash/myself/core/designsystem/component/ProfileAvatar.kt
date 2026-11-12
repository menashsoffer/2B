package com.menash.myself.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

@Composable
fun ProfileAvatar(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    displayName: String? = null,
) {
    val colors = MaterialTheme.appColors
    val letter = displayName?.trim()?.firstOrNull()?.takeUnless { it.isWhitespace() }?.toString()
    val description = stringResource(R.string.account_menu)
    // Null onClick draws the same circle as a picture only (the profile card in «אני»).
    val interactive = if (onClick == null) modifier.clearAndSetSemantics {} else modifier
        .minimumInteractiveComponentSize()
        .size(48.dp)
        .semantics { contentDescription = description }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = false),
            role = Role.Button,
            onClick = onClick,
        )
    Box(
        modifier = interactive,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(Spacing.s32)
                .clip(AppShapes.pill)
                .background(colors.accentSoft)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            if (letter == null) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = colors.accentStrong,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(
                    text = letter,
                    style = AppType.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accentStrong,
                )
            }
        }
    }
}

@Preview(name = "Avatar light", showBackground = true, locale = "iw")
@Composable
private fun ProfileAvatarLightPreview() {
    MyApplicationTheme(darkTheme = false) {
        ProfileAvatar(displayName = stringResource(R.string.sample_name), onClick = {})
    }
}

@Preview(
    name = "Avatar dark",
    showBackground = true,
    locale = "iw",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ProfileAvatarDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        ProfileAvatar(displayName = stringResource(R.string.sample_name), onClick = {})
    }
}
