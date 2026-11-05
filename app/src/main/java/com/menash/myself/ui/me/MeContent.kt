package com.menash.myself.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.ListRow
import com.menash.myself.core.designsystem.component.ProfileAvatar
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

/** What «אני» shows. Null name and email = guest. */
data class MeState(
    val signedIn: Boolean,
    val displayName: String? = null,
    val email: String? = null,
    val versionName: String = "",
    val versionCode: Int = 0,
)

class MeActions(
    val onSignIn: () -> Unit = {},
    val onHousehold: () -> Unit = {},
    val onPreferences: () -> Unit = {},
    val onManageAccount: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    /** Debug only: long press on the version line opens the hidden gallery. */
    val onVersionLongPress: (() -> Unit)? = null,
)

/**
 * «אני»: the one place for the account. Profile, household, preferences, account management,
 * sign out and the version. Stateless; [MeScreen] wires it.
 */
@Composable
fun MeContent(state: MeState, actions: MeActions, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("me_screen"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        ScreenHeader(title = stringResource(R.string.me_title))
        ProfileCard(state)
        if (!state.signedIn) {
            MeGroup {
                MeRow(stringResource(R.string.account_sign_in), Icons.AutoMirrored.Outlined.Login,
                    "account_sign_in", actions.onSignIn)
            }
        }
        MeSection(stringResource(R.string.me_section_home)) {
            MeRow(stringResource(R.string.account_household), Icons.Outlined.Group, "account_household",
                actions.onHousehold, subtitle = stringResource(R.string.me_household_detail))
        }
        MeSection(stringResource(R.string.me_section_app)) {
            MeRow(stringResource(R.string.me_preferences), Icons.Outlined.Tune, "account_settings",
                actions.onPreferences, subtitle = stringResource(R.string.me_preferences_detail))
        }
        if (state.signedIn) {
            MeSection(stringResource(R.string.me_section_account)) {
                MeRow(stringResource(R.string.account_management), Icons.Outlined.ManageAccounts,
                    "account_management", actions.onManageAccount,
                    subtitle = stringResource(R.string.me_management_detail))
                MeRow(stringResource(R.string.account_sign_out), Icons.AutoMirrored.Outlined.Logout,
                    "account_sign_out", actions.onSignOut, danger = true)
            }
        }
        val longPress = actions.onVersionLongPress
        Text(
            text = stringResource(R.string.me_version, state.versionName, state.versionCode),
            style = AppType.caption,
            color = colors.textSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.s8)
                .testTag("me_version_line")
                .then(if (longPress != null) Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { longPress() })
                } else Modifier),
        )
    }
}

@Composable
private fun ProfileCard(state: MeState) {
    val colors = MaterialTheme.appColors
    val name = state.displayName?.takeIf { it.isNotBlank() }
    MeGroup {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.s16)
                .testTag("me_profile"),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s16),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(displayName = name, onClick = null)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                Text(
                    text = name ?: stringResource(R.string.me_guest),
                    style = AppType.title,
                    color = colors.textPrimary,
                )
                val detail = if (state.signedIn) state.email?.takeIf { it.isNotBlank() }
                else stringResource(R.string.me_guest_detail)
                if (detail != null) {
                    Text(text = detail, style = AppType.caption, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun MeSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        Text(
            text = title,
            style = AppType.caption,
            color = MaterialTheme.appColors.textSecondary,
            modifier = Modifier
                .padding(horizontal = Spacing.s4)
                .semantics { heading() },
        )
        MeGroup(content)
    }
}

@Composable
private fun MeGroup(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.appColors.card,
        shape = AppShapes.card,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = Spacing.s4)) { content() }
    }
}

/** One tappable row: icon, title, optional subtitle and a chevron so it reads as a link. */
@Composable
internal fun MeRow(
    title: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    danger: Boolean = false,
) {
    val colors = MaterialTheme.appColors
    val tint: Color = if (danger) colors.danger else colors.accent
    ListRow(
        title = title,
        subtitle = subtitle,
        titleColor = if (danger) colors.danger else null,
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        },
        trailingContent = if (danger) null else {
            {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        onClick = onClick,
        testTag = testTag,
    )
}
