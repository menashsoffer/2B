package com.menash.myself.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.account.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AccountAction { LEAVE, TRANSFER, DELETE_HOUSEHOLD, DELETE_ACCOUNT }
enum class AccountPhase { LOADING, READY, CONFIRM, TYPE_CONFIRM, SECOND_CONFIRM, RUNNING, FAILED, COMPLETE, DELETED }
data class AccountManagementState(
    val uid: String,
    val household: AccountHousehold? = null,
    val phase: AccountPhase = AccountPhase.LOADING,
    val action: AccountAction? = null,
    val typed: String = "",
    val step: DeletionStep? = null,
    val pendingAccount: Boolean = false,
) {
    val busy get() = phase == AccountPhase.RUNNING || phase == AccountPhase.LOADING
    val owner get() = household?.ownerUid == uid
    val other get() = household?.otherMember(uid)
}

class AccountManagementViewModel(
    private val port: AccountDeletionPort,
    pending: Boolean = false,
    private val confirmationWord: String,
) : ViewModel() {
    var reauthenticate: suspend () -> Unit = port::reauthenticate
    private val deletion = AccountDeletion(object : AccountDeletionPort by port {
        override suspend fun reauthenticate() = this@AccountManagementViewModel.reauthenticate()
    })
    private val mutable = MutableStateFlow(AccountManagementState(port.uid, pendingAccount = pending))
    val state = mutable.asStateFlow()
    init { load() }
    fun load() {
        if (state.value.phase == AccountPhase.RUNNING) return
        mutable.value = state.value.copy(phase = AccountPhase.LOADING)
        viewModelScope.launch {
            try {
                port.requireNetwork()
                mutable.value = state.value.copy(household = port.households().firstOrNull(), phase = AccountPhase.READY, step = null)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = state.value.copy(phase = AccountPhase.FAILED, step = DeletionStep.NETWORK) }
        }
    }
    fun choose(action: AccountAction) {
        val s = state.value
        if (s.phase != AccountPhase.READY) return
        if (s.pendingAccount && action != AccountAction.DELETE_ACCOUNT) return
        if (action != AccountAction.DELETE_ACCOUNT && s.household == null) return
        if (action == AccountAction.LEAVE && s.owner) return
        if (action in listOf(AccountAction.TRANSFER, AccountAction.DELETE_HOUSEHOLD) && !s.owner) return
        if (action == AccountAction.TRANSFER && s.other == null) return
        if (s.household?.deleting == true && action !in listOf(AccountAction.DELETE_HOUSEHOLD, AccountAction.DELETE_ACCOUNT)) return
        mutable.value = s.copy(action = action, typed = "", phase = if (action == AccountAction.DELETE_HOUSEHOLD) AccountPhase.TYPE_CONFIRM else AccountPhase.CONFIRM)
    }
    fun type(value: String) { if (state.value.phase == AccountPhase.TYPE_CONFIRM) mutable.value = state.value.copy(typed = value) }
    fun cancel() {
        if (state.value.phase in listOf(AccountPhase.CONFIRM, AccountPhase.TYPE_CONFIRM, AccountPhase.SECOND_CONFIRM))
            mutable.value = state.value.copy(phase = AccountPhase.READY, action = null, typed = "")
    }
    fun confirm() {
        when (state.value.phase) {
            AccountPhase.TYPE_CONFIRM -> {
                if (state.value.typed != confirmationWord) return
                if (state.value.other != null) { mutable.value = state.value.copy(phase = AccountPhase.SECOND_CONFIRM); return }
            }
            AccountPhase.CONFIRM, AccountPhase.SECOND_CONFIRM -> Unit
            else -> return
        }
        execute()
    }
    fun retry() {
        if (state.value.phase != AccountPhase.FAILED) return
        if (state.value.action == null) load() else execute()
    }
    private fun execute() {
        val s = state.value
        val action = s.action ?: return
        mutable.value = s.copy(phase = AccountPhase.RUNNING, step = DeletionStep.NETWORK)
        viewModelScope.launch {
            try {
                val progress: (DeletionStep) -> Unit = { mutable.value = state.value.copy(step = it) }
                when (action) {
                    AccountAction.LEAVE -> deletion.leaveHousehold(requireNotNull(s.household).id, progress)
                    AccountAction.TRANSFER -> deletion.transferOwnership(requireNotNull(s.household).id, requireNotNull(s.other), progress)
                    AccountAction.DELETE_HOUSEHOLD -> deletion.deleteHousehold(requireNotNull(s.household).id, progress, s.household.memberUids)
                    AccountAction.DELETE_ACCOUNT -> deletion.deleteMyAccount(progress)
                }
                mutable.value = state.value.copy(phase = if (action == AccountAction.DELETE_ACCOUNT) AccountPhase.DELETED else AccountPhase.COMPLETE)
            } catch (e: CancellationException) { throw e }
            catch (e: DeletionFailure) {
                // A newly joined member needs a new confirmation, never a blind destructive retry.
                val changed = generateSequence(e as Throwable?) { it.cause }.any { it is HouseholdMembershipChanged }
                mutable.value = state.value.copy(phase = AccountPhase.FAILED, step = e.step,
                    action = if (changed && action == AccountAction.DELETE_HOUSEHOLD) null else action)
            }
            catch (_: Exception) { mutable.value = state.value.copy(phase = AccountPhase.FAILED) }
        }
    }
}
