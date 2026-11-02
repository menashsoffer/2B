package com.menash.myself.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import com.menash.myself.R
import com.menash.myself.core.data.account.DeletionStep
import com.menash.myself.core.designsystem.component.ListRow
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.account.*

@Composable
fun AccountManagementContent(
    state: AccountManagementState,
    onAction: (AccountAction) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onType: (String) -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    onSignIn: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val partner = state.household?.otherName(state.uid)?.takeIf { it.isNotBlank() } ?: stringResource(R.string.account_other_member)
    Surface(color = colors.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(Spacing.s16),
            verticalArrangement = Arrangement.spacedBy(Spacing.s16)) {
            Text(stringResource(R.string.account_management), style = AppType.title, color = colors.textPrimary)
            when (state.phase) {
                AccountPhase.LOADING, AccountPhase.RUNNING -> {
                    Text(stringResource(stepString(state.step ?: DeletionStep.NETWORK)), style = AppType.body)
                    Text(stringResource(R.string.account_keep_open), style = AppType.caption)
                    PrimaryButton(stringResource(R.string.account_in_progress), {}, isLoading = true)
                }
                AccountPhase.FAILED -> {
                    Text(stringResource(R.string.account_step_failed, stringResource(stepString(state.step ?: DeletionStep.NETWORK))), color = colors.danger)
                    if (state.step == DeletionStep.NETWORK) Text(stringResource(R.string.account_offline))
                    PrimaryButton(stringResource(R.string.account_retry), onRetry)
                }
                AccountPhase.DELETED -> {
                    Text(stringResource(R.string.account_deleted), style = AppType.title)
                    PrimaryButton(stringResource(R.string.account_back_signin), onSignIn)
                }
                AccountPhase.COMPLETE -> {
                    Text(stringResource(R.string.account_operation_complete), style = AppType.body)
                    PrimaryButton(stringResource(R.string.account_close), onClose)
                }
                else -> {
                    if (state.pendingAccount) Text(stringResource(R.string.account_resume))
                    if (state.household?.deleting == true) Text(stringResource(R.string.account_home_resume))
                    if (!state.pendingAccount && state.household != null) {
                        if (!state.owner && !state.household.deleting) ActionRow(R.string.account_leave, onAction, AccountAction.LEAVE)
                        if (state.owner) {
                            if (state.other != null && !state.household.deleting) ListRow(title = stringResource(R.string.account_transfer, partner), onClick = { onAction(AccountAction.TRANSFER) })
                            ActionRow(R.string.account_delete_household, onAction, AccountAction.DELETE_HOUSEHOLD)
                        }
                    }
                    ActionRow(R.string.account_delete, onAction, AccountAction.DELETE_ACCOUNT)
                }
            }
            // A failed operation remains resumable; dismissing must not restore household creation mid-account-delete.
            if (!state.busy && state.phase !in listOf(AccountPhase.DELETED, AccountPhase.COMPLETE))
                TextButton(onClick = onClose) { Text(stringResource(R.string.account_close)) }
        }
    }
    AccountConfirmationDialogs(state, partner, onType, onConfirm, onCancel)
}

@Composable
private fun AccountConfirmationDialogs(
    state: AccountManagementState, partner: String, onType: (String) -> Unit,
    onConfirm: () -> Unit, onCancel: () -> Unit,
) {
    when (state.phase) {
        AccountPhase.TYPE_CONFIRM -> TypedDeletionDialog(state.typed, onType, onConfirm, onCancel)
        AccountPhase.CONFIRM, AccountPhase.SECOND_CONFIRM -> {
            val explanation = when {
                state.phase == AccountPhase.SECOND_CONFIRM -> stringResource(R.string.account_delete_household_partner, partner)
                state.action == AccountAction.LEAVE -> stringResource(R.string.account_leave_explanation, partner)
                state.action == AccountAction.TRANSFER -> stringResource(R.string.account_transfer_explanation, partner)
                state.household == null -> stringResource(R.string.account_delete_personal)
                !state.owner -> stringResource(R.string.account_delete_member, partner)
                state.other != null -> stringResource(R.string.account_delete_owner_partner, partner)
                else -> stringResource(R.string.account_delete_owner_alone)
            }
            AlertDialog(onDismissRequest = onCancel,
                title = { Text(stringResource(R.string.account_confirm_title)) }, text = { Text(explanation) },
                confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.account_confirm)) } },
                dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.account_cancel)) } })
        }
        else -> Unit
    }
}

@Composable private fun ActionRow(label: Int, onAction: (AccountAction) -> Unit, action: AccountAction) {
    ListRow(title = stringResource(label), titleColor = MaterialTheme.appColors.danger, onClick = { onAction(action) })
}

@Composable
fun TypedDeletionDialog(typed: String, onType: (String) -> Unit, onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(onDismissRequest = onCancel, modifier = Modifier.testTag("account_typed_confirmation"),
        title = { Text(stringResource(R.string.account_delete_household)) },
        text = { TypedDeletionBody(typed, onType) },
        confirmButton = { PrimaryButton(stringResource(R.string.account_delete_household), onConfirm, enabled = typed == stringResource(R.string.account_delete_word)) },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.account_cancel)) } })
}

/** The dialog's body on its own, so a screenshot test can render it without a window. */
@Composable
fun TypedDeletionBody(typed: String, onType: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s16)) {
        Text(stringResource(R.string.account_type_explanation))
        OutlinedTextField(value = typed, onValueChange = onType, singleLine = true,
            label = { Text(stringResource(R.string.account_delete_word)) })
    }
}

internal fun stepString(step: DeletionStep): Int = when (step) {
    DeletionStep.NETWORK -> R.string.account_step_network
    DeletionStep.REAUTHENTICATE -> R.string.account_step_reauth
    DeletionStep.HOUSEHOLD -> R.string.account_step_household
    DeletionStep.HOUSEHOLD_DATA -> R.string.account_step_household_data
    DeletionStep.INVITES -> R.string.account_step_invites
    DeletionStep.HOUSEHOLD_DOCUMENT -> R.string.account_step_household_document
    DeletionStep.PERSONAL_DATA -> R.string.account_step_personal
    DeletionStep.REPORTS -> R.string.account_step_reports
    DeletionStep.AUTH_ACCOUNT -> R.string.account_step_auth
    DeletionStep.SIGN_OUT -> R.string.account_step_signout
}
