package com.menash.myself.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.menash.myself.R
import com.menash.myself.core.data.account.reauthenticateGoogle
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.core.ui.UiEvent
import com.menash.myself.feature.account.*

@Composable
fun AccountManagementRoute(instanceKey: String, onClose: () -> Unit, onSignIn: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    // The opening key survives configuration changes; reopening starts a fresh state machine.
    val factory = remember { object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val port = container.accountDeletionPort()
            return AccountManagementViewModel(
                port, container.accountDeletionPending.uid.value == port.uid,
                context.getString(R.string.account_delete_word),
            ) as T
        }
    } }
    val vm: AccountManagementViewModel = viewModel(key = instanceKey, factory = factory)
    val state by vm.state.collectAsStateWithLifecycle()
    vm.reauthenticate = { reauthenticateGoogle(context, state.uid) }
    val active = remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        onDispose {
            active.value = false
            vm.reauthenticate = { error("Account screen is not attached") }
        }
    }
    LaunchedEffect(state.phase) {
        if (state.phase == AccountPhase.FAILED) container.uiEvents.send(UiEvent.SaveError(
            context.getString(R.string.account_step_failed, context.getString(stepString(state.step ?: com.menash.myself.core.data.account.DeletionStep.NETWORK))), { if (active.value) vm.retry() }))
        if (state.phase == AccountPhase.COMPLETE) container.uiEvents.send(if (state.action == AccountAction.DELETE_HOUSEHOLD) UiEvent.DELETED else UiEvent.MONEY_SAVED)
    }
    Dialog(onDismissRequest = { if (!state.busy) onClose() }, properties = DialogProperties(
        dismissOnBackPress = !state.busy, dismissOnClickOutside = false, usePlatformDefaultWidth = false,
    )) {
        BackHandler(enabled = state.busy) { }
        AccountManagementContent(state, vm::choose, vm::confirm, vm::cancel, vm::type, vm::retry, onClose, onSignIn)
    }
}
