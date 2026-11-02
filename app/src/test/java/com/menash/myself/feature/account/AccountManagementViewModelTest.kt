package com.menash.myself.feature.account

import com.menash.myself.core.data.account.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AccountManagementViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    @Test fun allThreeAccountPathsConfirmProgressAndFinish() = runTest(dispatcher) {
        val pair = AccountHousehold("h", "me", listOf("me", "partner"))
        for (home in listOf(pair.copy(ownerUid = "partner"), pair, pair.copy(memberUids = listOf("me")))) {
            val port = FakeDeletionPort(home)
            val vm = AccountManagementViewModel(port, confirmationWord = "למחוק")
            advanceUntilIdle()
            assertEquals(AccountPhase.READY, vm.state.value.phase)
            vm.choose(AccountAction.DELETE_ACCOUNT)
            assertEquals(AccountPhase.CONFIRM, vm.state.value.phase)
            assertEquals(0, port.batches)
            vm.confirm()
            assertEquals(AccountPhase.RUNNING, vm.state.value.phase)
            vm.cancel()
            assertEquals(AccountPhase.RUNNING, vm.state.value.phase)
            advanceUntilIdle()
            assertEquals(AccountPhase.DELETED, vm.state.value.phase)
            assertEquals(DeletionStep.SIGN_OUT, vm.state.value.step)
        }
    }
    @Test fun typedConfirmationAndPartnerSecondConfirmationAreBothRequired() = runTest(dispatcher) {
        val port = FakeDeletionPort(AccountHousehold("h", "me", listOf("me", "partner")))
        val vm = AccountManagementViewModel(port, confirmationWord = "למחוק")
        advanceUntilIdle()
        vm.choose(AccountAction.DELETE_HOUSEHOLD)
        vm.type("למח")
        vm.confirm()
        assertEquals(AccountPhase.TYPE_CONFIRM, vm.state.value.phase)
        vm.type("למחוק")
        vm.confirm()
        assertEquals(AccountPhase.SECOND_CONFIRM, vm.state.value.phase)
        assertNotNull(port.home)
        vm.confirm()
        advanceUntilIdle()
        assertEquals(AccountPhase.COMPLETE, vm.state.value.phase)
        assertNull(port.home)
    }
    @Test fun reauthFailureAndRetryKeepTheActionAndFailedStep() = runTest(dispatcher) {
        val port = FakeDeletionPort()
        port.reauthFails = true
        val vm = AccountManagementViewModel(port, confirmationWord = "למחוק")
        advanceUntilIdle()
        vm.choose(AccountAction.DELETE_ACCOUNT); vm.confirm(); advanceUntilIdle()
        assertEquals(AccountPhase.FAILED, vm.state.value.phase)
        assertEquals(DeletionStep.REAUTHENTICATE, vm.state.value.step)
        assertNotNull(port.home)
        port.reauthFails = false
        vm.retry(); advanceUntilIdle()
        assertEquals(AccountPhase.DELETED, vm.state.value.phase)
    }
    @Test fun leaveAndTransferFinishAndDoNotDeleteSharedDocuments() = runTest(dispatcher) {
        for (action in listOf(AccountAction.LEAVE, AccountAction.TRANSFER)) {
            val port = FakeDeletionPort(AccountHousehold("h", if (action == AccountAction.LEAVE) "partner" else "me", listOf("me", "partner")))
            port.docs += "households/h/tasks/shared"
            val vm = AccountManagementViewModel(port, confirmationWord = "למחוק")
            advanceUntilIdle()
            vm.choose(action); vm.confirm(); advanceUntilIdle()
            assertEquals(AccountPhase.COMPLETE, vm.state.value.phase)
            assertTrue("households/h/tasks/shared" in port.docs)
        }
    }
    @Test fun deletionInterruptionCanRetryWithoutAnotherDestructiveConfirmation() = runTest(dispatcher) {
        val port = FakeDeletionPort()
        port.docs += "households/h/tasks/shared"; port.failBatch = 1
        val vm = AccountManagementViewModel(port, confirmationWord = "למחוק")
        advanceUntilIdle()
        vm.choose(AccountAction.DELETE_HOUSEHOLD); vm.type("למחוק"); vm.confirm(); advanceUntilIdle()
        assertEquals(AccountPhase.FAILED, vm.state.value.phase)
        assertEquals(DeletionStep.HOUSEHOLD_DATA, vm.state.value.step)
        port.failBatch = null
        vm.retry(); advanceUntilIdle()
        assertEquals(AccountPhase.COMPLETE, vm.state.value.phase)
    }
    @Test fun pendingAccountDeletionOnlyAllowsResume() = runTest(dispatcher) {
        val vm = AccountManagementViewModel(FakeDeletionPort(), pending = true, confirmationWord = "למחוק")
        advanceUntilIdle()
        vm.choose(AccountAction.DELETE_HOUSEHOLD)
        assertEquals(AccountPhase.READY, vm.state.value.phase)
        vm.choose(AccountAction.DELETE_ACCOUNT)
        assertEquals(AccountPhase.CONFIRM, vm.state.value.phase)
    }
    @Test fun joiningMemberRequiresNewHouseholdConfirmation() = runTest(dispatcher) {
        val port = FakeDeletionPort()
        val vm = AccountManagementViewModel(port, confirmationWord = "למחוק")
        advanceUntilIdle()
        vm.choose(AccountAction.DELETE_HOUSEHOLD)
        vm.type("למחוק")
        port.home = port.home!!.copy(memberUids = listOf("me", "partner"))
        vm.confirm(); advanceUntilIdle()
        assertEquals(AccountPhase.FAILED, vm.state.value.phase)
        assertNull(vm.state.value.action)
        assertFalse(port.locked)
        assertNotNull(port.home)
        vm.retry(); advanceUntilIdle()
        vm.choose(AccountAction.DELETE_HOUSEHOLD); vm.type("למחוק"); vm.confirm()
        assertEquals(AccountPhase.SECOND_CONFIRM, vm.state.value.phase)
    }
    @Test fun missingHouseholdRecoveryOnlyRunsForAuthenticatedNonDeletingUser() {
        assertTrue(shouldRecoverHousehold(null, null, "partner"))
        assertTrue(shouldRecoverHousehold(null, "owner", "partner"))
        assertFalse(shouldRecoverHousehold("h", null, "partner"))
        assertFalse(shouldRecoverHousehold(null, "partner", "partner"))
        assertFalse(shouldRecoverHousehold(null, null, null))
    }
}
