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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.menash.myself.auth.AuthManager
import com.menash.myself.auth.SignInScreen
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.ui.MainViewModel
import com.menash.myself.ui.money.sharedFileUris
import com.menash.myself.ui.shell.AppShell

class MainActivity : ComponentActivity() {
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
                        modifier = Modifier.fillMaxSize(),
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
    var currentUser by remember { mutableStateOf(auth?.currentUser) }
    var showSignIn by remember { mutableStateOf(false) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth?.addAuthStateListener(listener)
        onDispose { auth?.removeAuthStateListener(listener) }
    }

    LaunchedEffect(auth) {
        if (auth == null || auth.currentUser != null) return@LaunchedEffect
        AuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = CredentialManager.create(context),
            onAuthSuccess = { currentUser = auth.currentUser },
            onUnauthenticated = {},
            scope = scope,
        )
    }


    val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.factory(appContainer))
    if (showSignIn) {
        SignInScreen(
            onAuthSuccess = {
                currentUser = auth?.currentUser
                showSignIn = false
            },
        )
    } else {
        AppShell(
            mainViewModel = mainViewModel,
            currentUser = currentUser,
            onRequestSignIn = { showSignIn = true },
            onSignedOut = {
                appContainer.householdSession.setHouseholdId(null)
                currentUser = null
                showSignIn = false
            },
        )
    }
}
