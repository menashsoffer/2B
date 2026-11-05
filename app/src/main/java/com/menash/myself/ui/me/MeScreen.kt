package com.menash.myself.ui.me

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseUser
import com.menash.myself.BuildConfig
import com.menash.myself.auth.AuthManager
import com.menash.myself.auth.SignOutConfirmDialog
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.ui.account.AccountManagementRoute
import com.menash.myself.ui.settings.SettingsDialog
import com.menash.myself.ui.shell.GalleryIndex
import kotlinx.coroutines.launch

/** Wires [MeContent]: sign in and out, household, preferences, account management, debug gallery. */
@Composable
fun MeScreen(
    currentUser: FirebaseUser?,
    onRequestSignIn: () -> Unit,
    onSignedOut: () -> Unit,
    onOpenHousehold: () -> Unit,
    onOpenRoute: (String) -> Unit,
) {
    val appContainer = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPreferences by remember { mutableStateOf(false) }
    var showManagement by rememberSaveable { mutableStateOf(false) }
    var managementKey by rememberSaveable { mutableStateOf("") }
    var confirmSignOut by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }

    MeContent(
        state = MeState(
            signedIn = currentUser != null,
            displayName = currentUser?.displayName,
            email = currentUser?.email,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
        ),
        actions = MeActions(
            onSignIn = onRequestSignIn,
            onHousehold = onOpenHousehold,
            onPreferences = { showPreferences = true },
            onManageAccount = {
                managementKey = java.util.UUID.randomUUID().toString()
                showManagement = true
            },
            onSignOut = { confirmSignOut = true },
            onVersionLongPress = if (BuildConfig.DEBUG) ({ showGallery = true }) else null,
        ),
    )

    if (showPreferences) {
        val rate by appContainer.exchangeRateRepository.currentRateFlow.collectAsStateWithLifecycle()
        SettingsDialog(
            currentRate = rate?.rate ?: 0.0,
            onDismiss = { showPreferences = false },
            onSaveRate = { newRate ->
                scope.launch { appContainer.exchangeRateRepository.setManualRate(newRate) }
                showPreferences = false
            },
            onOpenLegacyScreen = { route ->
                showPreferences = false
                onOpenRoute(route)
            },
        )
    }

    if (showManagement) {
        AccountManagementRoute(
            instanceKey = managementKey,
            onClose = { showManagement = false },
            onSignIn = { showManagement = false; onSignedOut(); onRequestSignIn() },
        )
    }

    if (confirmSignOut) {
        SignOutConfirmDialog(
            onDismiss = { confirmSignOut = false },
            onConfirm = {
                confirmSignOut = false
                AuthManager.signOut(
                    context = context,
                    credentialManager = CredentialManager.create(context),
                    scope = scope,
                    onSignOutComplete = onSignedOut,
                )
            },
        )
    }

    if (BuildConfig.DEBUG && showGallery) {
        Dialog(
            onDismissRequest = { showGallery = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            GalleryIndex(onClose = { showGallery = false }, onOpenLegacy = { route ->
                showGallery = false
                onOpenRoute(route)
            })
        }
    }
}
