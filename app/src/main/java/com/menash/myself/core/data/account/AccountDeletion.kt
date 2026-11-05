package com.menash.myself.core.data.account

import kotlinx.coroutines.CancellationException

/** Explicit inventory: Firestore clients cannot discover subcollections. */
object DeletionCollections {
    val money = listOf(
        "cards", "incomes", "fixedExpenses", "subscriptions", "variableBudgets",
        "installments", "periodicBudgets", "debts", "savings", "assets", "categories",
        "transactions", "merchantMemory", "sourceClosures", "goals", "donations", "tithe", "averagesSettings", "closedMonths", "pensionFunds", "car",
    )
    val household = money + listOf("tasks", "dreams", "inbox")
    // LifeHubRepository.privateCollection + Task/Dream/InboxFirestoreDataSource:
    // users/{uid}/private/{tasks|dreams|inbox}/items/{id} (including missing parent docs).
    val privateKinds = listOf("tasks", "dreams", "inbox")
}

data class AccountHousehold(
    val id: String,
    val ownerUid: String,
    val memberUids: List<String>,
    val ownerEmail: String = "",
    val partnerUid: String = "",
    val partnerEmail: String = "",
    val deleting: Boolean = false,
) {
    fun otherMember(uid: String) = memberUids.firstOrNull { it != uid }
    fun otherName(uid: String) = if (ownerUid == uid) partnerEmail else ownerEmail

    fun leave(uid: String): AccountHousehold {
        require(ownerUid != uid) { "Owner must transfer before leaving" }
        return copy(memberUids = memberUids - uid,
            partnerUid = if (partnerUid == uid) "" else partnerUid,
            partnerEmail = if (partnerUid == uid) "" else partnerEmail)
    }

    fun transfer(uid: String, toUid: String, leave: Boolean): AccountHousehold {
        require(ownerUid == uid && toUid != uid && toUid in memberUids)
        val members = if (leave) memberUids - uid else memberUids
        val partner = members.firstOrNull { it != toUid }.orEmpty()
        return copy(ownerUid = toUid, memberUids = members,
            ownerEmail = if (toUid == partnerUid) partnerEmail else "",
            partnerUid = partner, partnerEmail = if (partner == uid) ownerEmail else "")
    }
}

enum class DeletionStep { NETWORK, REAUTHENTICATE, HOUSEHOLD, HOUSEHOLD_DATA, INVITES, HOUSEHOLD_DOCUMENT, PERSONAL_DATA, REPORTS, AUTH_ACCOUNT, SIGN_OUT }
class HouseholdMembershipChanged : Exception("Household membership changed; confirm again")
class DeletionFailure(val step: DeletionStep, cause: Throwable) : Exception(step.name, cause)

/** All reads are server-only; mutations are acknowledged, online-only transactions, never queued writes. */
interface AccountDeletionPort {
    val uid: String
    val email: String
    suspend fun requireNetwork()
    suspend fun reauthenticate()
    suspend fun households(): List<AccountHousehold>
    suspend fun updateHousehold(id: String, change: (AccountHousehold) -> AccountHousehold)
    suspend fun documents(collection: String, field: String? = null, value: String? = null): List<String>
    suspend fun deleteDocuments(paths: List<String>, ownerHousehold: String? = null)
    suspend fun deleteHouseholdDocument(id: String)
    suspend fun beginHouseholdDeletion(id: String, expectedMembers: List<String>)
    suspend fun deleteUnsentReports()
    suspend fun signOut()
    suspend fun deleteAuthAccount()
    // Persisted across process death, suppresses automatic household creation until completion.
    fun setAccountDeletionPending(pending: Boolean)
}

class AccountDeletion(private val port: AccountDeletionPort) {
    private suspend fun <T> step(step: DeletionStep, progress: (DeletionStep) -> Unit, block: suspend () -> T): T {
        progress(step)
        return try { block() } catch (e: CancellationException) { throw e }
        catch (e: DeletionFailure) { throw e }
        catch (e: Exception) { throw DeletionFailure(step, e) }
    }
    private suspend fun drain(path: String, owner: String? = null, field: String? = null, value: String? = null) {
        while (true) {
            val docs = port.documents(path, field, value)
            if (docs.isEmpty()) return
            docs.chunked(500).forEach { port.deleteDocuments(it, owner) }
        }
    }
    suspend fun leaveHousehold(id: String, progress: (DeletionStep) -> Unit = {}) {
        step(DeletionStep.NETWORK, progress) { port.requireNetwork() }
        val home = step(DeletionStep.HOUSEHOLD, progress) { port.households().firstOrNull { it.id == id } } ?: return
        step(DeletionStep.HOUSEHOLD, progress) { require(home.ownerUid != port.uid) }
        step(DeletionStep.INVITES, progress) {
            if (port.email.isNotBlank()) port.deleteDocuments(listOf("invites/${port.email.lowercase()}"))
        }
        step(DeletionStep.HOUSEHOLD, progress) { port.updateHousehold(id) { it.leave(port.uid) } }
    }
    suspend fun transferOwnership(id: String, toUid: String, progress: (DeletionStep) -> Unit = {}) {
        step(DeletionStep.NETWORK, progress) { port.requireNetwork() }
        step(DeletionStep.HOUSEHOLD, progress) { port.updateHousehold(id) { if (it.ownerUid == toUid && port.uid in it.memberUids) it else it.transfer(port.uid, toUid, false) } }
    }
    suspend fun deleteHousehold(id: String, progress: (DeletionStep) -> Unit = {}, expectedMembers: List<String>? = null) {
        step(DeletionStep.NETWORK, progress) { port.requireNetwork() }
        val household = step(DeletionStep.HOUSEHOLD, progress) { port.households().firstOrNull { it.id == id } } ?: return
        step(DeletionStep.HOUSEHOLD, progress) { require(household.ownerUid == port.uid) }
        step(DeletionStep.HOUSEHOLD_DATA, progress) {
            port.beginHouseholdDeletion(id, expectedMembers ?: household.memberUids)
            DeletionCollections.household.forEach { drain("households/$id/$it", id) }
        }
        step(DeletionStep.INVITES, progress) { drain("invites", id, "householdId", id) }
        // LAST: all subcollection authorization depends on this document existing.
        step(DeletionStep.HOUSEHOLD_DOCUMENT, progress) { port.deleteHouseholdDocument(id) }
    }
    suspend fun deleteMyAccount(progress: (DeletionStep) -> Unit = {}) {
        step(DeletionStep.NETWORK, progress) { port.requireNetwork() }
        step(DeletionStep.REAUTHENTICATE, progress) { port.reauthenticate() }
        step(DeletionStep.HOUSEHOLD, progress) { port.setAccountDeletionPending(true) }
        val households = step(DeletionStep.HOUSEHOLD, progress) { port.households() }
        for (household in households) {
            val other = household.otherMember(port.uid)
            when {
                household.deleting && household.ownerUid == port.uid -> deleteHousehold(household.id, progress, household.memberUids)
                household.ownerUid != port.uid -> leaveHousehold(household.id, progress)
                other != null -> step(DeletionStep.HOUSEHOLD, progress) {
                    port.updateHousehold(household.id) { it.transfer(port.uid, other, true) }
                }
                else -> deleteHousehold(household.id, progress, household.memberUids)
            }
        }
        step(DeletionStep.PERSONAL_DATA, progress) {
            drain("users/${port.uid}/fitness")
            DeletionCollections.privateKinds.forEach { kind -> drain("users/${port.uid}/private/$kind/items") }
            // Also remove parent documents, whether or not they existed.
            drain("users/${port.uid}/private")
            drain("calculator_sync", field = "userId", value = port.uid)
            if (port.email.isNotBlank()) port.deleteDocuments(listOf("invites/${port.email.lowercase()}"))
        }
        step(DeletionStep.REPORTS, progress) { port.deleteUnsentReports() }
        step(DeletionStep.AUTH_ACCOUNT, progress) { port.deleteAuthAccount() }
        step(DeletionStep.SIGN_OUT, progress) { port.signOut() }
        port.setAccountDeletionPending(false)
    }
}
