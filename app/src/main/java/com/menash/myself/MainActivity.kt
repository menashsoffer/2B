package com.menash.myself

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.LayoutDirection
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.menash.myself.core.auth.AuthManager
import com.menash.myself.core.auth.toAuthSession
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.shell.MainViewModel
import com.menash.myself.feature.money.ui.sharedFileUris
import com.menash.myself.shell.AppShell

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) offerSharedFiles(intent)
        setContent {
            val appContainer = LocalAppContainer.current
            MyApplicationTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalAppContainer provides appContainer,
                ) {
                    Surface(
                        // Debug builds expose testTags as resource ids, so Maestro (docs/E2E.md) can find them.
                        modifier = Modifier.fillMaxSize().then(
                            if (BuildConfig.DEBUG) Modifier.semantics { testTagsAsResourceId = true } else Modifier
                        ),
                        color = MaterialTheme.appColors.background,
                    ) {
                        MyselfRoot()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        offerSharedFiles(intent)
    }

    /** A bank or card file shared into the app waits in the inbox until the money tab reads it. */
    private fun offerSharedFiles(intent: Intent?) {
        val uris = sharedFileUris(intent)
        if (uris.isNotEmpty()) (application as MyselfApp).appContainer.importInbox.offer(uris.map { it.toString() })
    }
}

@Composable
private fun MyselfRoot() {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val auth = remember { runCatching { Firebase.auth }.getOrNull() }
    var session by remember { mutableStateOf(auth?.currentUser?.toAuthSession()) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            session = firebaseAuth.currentUser?.toAuthSession()
        }
        auth?.addAuthStateListener(listener)
        onDispose { auth?.removeAuthStateListener(listener) }
    }

    // Returning users are signed in quietly. A guest stays on בית; sign-in is a route from «אני».
    LaunchedEffect(auth) {
        if (auth == null || auth.currentUser != null) return@LaunchedEffect
        AuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = CredentialManager.create(context),
            onAuthSuccess = { session = auth.currentUser?.toAuthSession() },
            onUnauthenticated = {},
            scope = scope,
        )
    }

    val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.factory(appContainer))
    AppShell(
        mainViewModel = mainViewModel,
        session = session,
        onSignedOut = {
            appContainer.householdSession.setHouseholdId(null)
            session = null
        },
    )
}
