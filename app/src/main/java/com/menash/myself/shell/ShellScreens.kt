package com.menash.myself.shell

import androidx.compose.runtime.Composable
import com.menash.myself.core.auth.AuthSession
import com.menash.myself.core.auth.SignInScreen
import com.menash.myself.feature.home.ui.HomeScreen
import com.menash.myself.feature.me.ui.MeScreen
import com.menash.myself.feature.money.ui.MoneyScreen

/** Tab and sign-in content. Tests pass fakes; production uses [defaultShellScreens]. */
class ShellScreens(
    val home: @Composable (
        displayName: String?,
        onOpenRoute: (String) -> Unit,
        onOpenAccount: () -> Unit,
    ) -> Unit,
    val money: @Composable () -> Unit,
    val me: @Composable (
        session: AuthSession?,
        onRequestSignIn: () -> Unit,
        onSignedOut: () -> Unit,
        onOpenHousehold: () -> Unit,
        onOpenRoute: (String) -> Unit,
    ) -> Unit,
    val signIn: @Composable (onSuccess: () -> Unit, onClose: () -> Unit) -> Unit,
)

fun defaultShellScreens(): ShellScreens = ShellScreens(
    home = { displayName, onOpenRoute, onOpenAccount ->
        HomeScreen(
            displayName = displayName,
            onOpenRoute = onOpenRoute,
            onOpenAccount = onOpenAccount,
        )
    },
    money = { MoneyScreen() },
    me = { session, onRequestSignIn, onSignedOut, onOpenHousehold, onOpenRoute ->
        MeScreen(
            session = session,
            onRequestSignIn = onRequestSignIn,
            onSignedOut = onSignedOut,
            onOpenHousehold = onOpenHousehold,
            onOpenRoute = onOpenRoute,
        )
    },
    signIn = { onSuccess, onClose ->
        SignInScreen(onAuthSuccess = onSuccess, onClose = onClose)
    },
)
