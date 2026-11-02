package com.menash.myself.core.data.account

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FakeDeletionPort(var home: AccountHousehold? = AccountHousehold("h", "me", listOf("me"))) : AccountDeletionPort {
    override val uid = "me"
    override val email = "me@example.com"
    var online = true
    var reauthFails = false
    var pending = false
    var failBatch: Int? = null
    var batches = 0
    var locked = false
    val events = mutableListOf<String>()
    val docs = mutableSetOf<String>()
    override suspend fun requireNetwork() { events += "network"; check(online) }
    override suspend fun reauthenticate() { events += "reauth"; check(!reauthFails) }
    override suspend fun households() = listOfNotNull(home?.takeIf { uid in it.memberUids })
    override suspend fun updateHousehold(id: String, change: (AccountHousehold) -> AccountHousehold) {
        home?.takeIf { uid in it.memberUids }?.let { home = change(it); events += "update" }
    }
    override suspend fun documents(collection: String, field: String?, value: String?) = docs.filter {
        it.substringBeforeLast('/') == collection
    }.take(500)
    override suspend fun beginHouseholdDeletion(id: String, expectedMembers: List<String>) {
        if (home!!.memberUids.toSet() != expectedMembers.toSet()) throw HouseholdMembershipChanged()
        locked = true
    }
    override suspend fun deleteDocuments(paths: List<String>, ownerHousehold: String?) {
        assertTrue(paths.size <= 500)
        if (ownerHousehold != null) assertTrue(home != null && locked)
        batches++
        if (failBatch == batches) error("Interrupted")
        events += "batch:${paths.first()}"
        docs.removeAll(paths.toSet())
    }
    override suspend fun deleteHouseholdDocument(id: String) {
        assertTrue(docs.none { it.startsWith("households/$id/") || it.startsWith("invites/") })
        events += "household-last"; home = null
    }
    override suspend fun deleteUnsentReports() { events += "reports" }
    override suspend fun signOut() { events += "signout" }
    override suspend fun deleteAuthAccount() { events += "auth-delete" }
    override fun setAccountDeletionPending(pending: Boolean) { this.pending = pending }
}

class AccountDeletionTest {
    private val pair = AccountHousehold("h", "me", listOf("me", "other"), "me@example.com", "other", "other@example.com")
    @Test fun memberLeavesAndKeepsSharedData() = runTest {
        val p = FakeDeletionPort(pair.copy(ownerUid = "other", partnerUid = "me"))
        p.docs += "households/h/tasks/shared"
        AccountDeletion(p).deleteMyAccount()
        assertEquals(listOf("other"), p.home!!.memberUids)
        assertEquals("", p.home!!.partnerUid)
        assertTrue("households/h/tasks/shared" in p.docs)
        assertEquals(listOf("reports", "auth-delete", "signout"), p.events.takeLast(3))
        assertFalse(p.pending)
    }
    @Test fun ownerTransfersAndLeavesAtomically() = runTest {
        val p = FakeDeletionPort(pair)
        AccountDeletion(p).deleteMyAccount()
        assertEquals("other", p.home!!.ownerUid)
        assertEquals("other@example.com", p.home!!.ownerEmail)
        assertEquals(listOf("other"), p.home!!.memberUids)
        assertEquals("", p.home!!.partnerUid)
        assertEquals(1, p.events.count { it == "update" })
    }
    @Test fun soleOwnerDeletesEveryCollectionThenInvitesThenHomeThenPersonalData() = runTest {
        val p = FakeDeletionPort()
        DeletionCollections.household.forEach { p.docs += "households/h/$it/data" }
        p.docs += "invites/other@example.com"
        p.docs += "users/me/fitness/today"
        DeletionCollections.privateKinds.forEach {
            p.docs += "users/me/private/$it/items/item"
            p.docs += "users/me/private/$it"
        }
        p.docs += "calculator_sync/legacy"
        AccountDeletion(p).deleteMyAccount()
        assertNull(p.home)
        assertTrue(p.docs.isEmpty())
        assertTrue(p.events.indexOf("household-last") > p.events.indexOf("batch:invites/other@example.com"))
        assertTrue(p.events.indexOf("household-last") < p.events.indexOf("batch:users/me/fitness/today"))
    }
    @Test fun interruptionIsResumableAndBatchesNeverExceed500() = runTest {
        val p = FakeDeletionPort()
        repeat(1201) { p.docs += "households/h/cards/$it" }
        p.failBatch = 2
        try { AccountDeletion(p).deleteHousehold("h"); fail() }
        catch (e: DeletionFailure) { assertEquals(DeletionStep.HOUSEHOLD_DATA, e.step) }
        assertEquals(701, p.docs.size)
        assertNotNull(p.home)
        p.failBatch = null
        AccountDeletion(p).deleteHousehold("h")
        AccountDeletion(p).deleteHousehold("h")
        assertTrue(p.docs.isEmpty())
        assertNull(p.home)
    }
    @Test fun offlineAndReauthFailureDeleteNothing() = runTest {
        for (offline in listOf(true, false)) {
            val p = FakeDeletionPort(pair)
            p.online = !offline; p.reauthFails = !offline
            try { AccountDeletion(p).deleteMyAccount(); fail() }
            catch (e: DeletionFailure) { assertEquals(if (offline) DeletionStep.NETWORK else DeletionStep.REAUTHENTICATE, e.step) }
            assertEquals(pair, p.home)
            assertEquals(0, p.batches)
            assertFalse(p.pending)
            assertFalse("auth-delete" in p.events)
        }
    }
    @Test fun transferKeepsBothMembersAndSwapsMetadata() = runTest {
        val p = FakeDeletionPort(pair)
        AccountDeletion(p).transferOwnership("h", "other")
        assertEquals(pair.memberUids, p.home!!.memberUids)
        assertEquals("me", p.home!!.partnerUid)
        assertEquals("me@example.com", p.home!!.partnerEmail)
    }
    @Test fun memberCannotDeleteHomeAndOwnerCannotLeaveWithoutTransfer() = runTest {
        val p = FakeDeletionPort(pair)
        try { AccountDeletion(p).leaveHousehold("h"); fail() } catch (_: DeletionFailure) { }
        p.home = pair.copy(ownerUid = "other")
        try { AccountDeletion(p).deleteHousehold("h"); fail() } catch (_: DeletionFailure) { }
        assertEquals(0, p.batches)
    }
}
