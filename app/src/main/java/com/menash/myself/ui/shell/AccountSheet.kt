package com.menash.myself.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.menash.myself.R
import com.menash.myself.auth.AuthManager
import com.menash.myself.auth.SignOutConfirmDialog
import com.menash.myself.core.designsystem.component.ListRow
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(
    signedIn: Boolean,
    displayName: String?,
    email: String?,
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
    onHousehold: () -> Unit,
    onSignIn: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    var confirmSignOut by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.s24),
        ) {
            if (!displayName.isNullOrBlank()) {
                Text(
                    text = displayName,
                    style = AppType.title,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = Spacing.s16),
                )
            }
            if (!email.isNullOrBlank()) {
                Text(
                    text = email,
                    style = AppType.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s4),
                )
            }
            ListRow(
                title = stringResource(R.string.account_settings),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                },
                onClick = onSettings,
                testTag = "account_settings",
            )
            ListRow(
                title = stringResource(R.string.account_household),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Group,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                },
                onClick = onHousehold,
                testTag = "account_household",
            )
            if (signedIn) {
                ListRow(
                    title = stringResource(R.string.account_sign_out),
                    titleColor = colors.danger,
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = colors.danger,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    onClick = { confirmSignOut = true },
                    testTag = "account_sign_out",
                )
            } else {
                ListRow(
                    title = stringResource(R.string.account_sign_in),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Login,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    onClick = onSignIn,
                    testTag = "account_sign_in",
                )
            }
        }
    }
    if (confirmSignOut) {
        SignOutConfirmDialog(
            onDismiss = { confirmSignOut = false },
            onConfirm = {
                confirmSignOut = false
                val credentialManager = CredentialManager.create(context)
                AuthManager.signOut(
                    context = context,
                    credentialManager = credentialManager,
                    scope = scope,
                    onSignOutComplete = onSignedOut,
                )
            },
        )
    }
}
