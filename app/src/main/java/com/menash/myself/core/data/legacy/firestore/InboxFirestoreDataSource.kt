package com.menash.myself.core.data.legacy.firestore

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.menash.myself.core.data.model.ItemLocation
import com.menash.myself.core.data.legacy.models.Household
import com.menash.myself.core.data.legacy.models.InboxItem
import com.menash.myself.core.data.legacy.models.SpaceType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class InboxFirestoreDataSource(
    private val requireDb: () -> FirebaseFirestore,
    private val currentUid: () -> String?,
    private val requireUid: () -> String,
    private val privateCollection: (String, String) -> CollectionReference,
    private val collectionRef: (ItemLocation, String) -> CollectionReference,
    private val reportError: (String, Throwable) -> Unit
) {

    fun observePrivateInbox(uid: String): Flow<List<InboxItem>> = callbackFlow {
        val listener = privateCollection(uid, "inbox")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    reportError("observePrivateInbox", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(InboxItem::class.java)?.copy(id = doc.id, householdId = "", space = "private")
                }?.filter { it.status == "inbox" } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    fun observeInbox(household: Household? = null): Flow<List<InboxItem>> = callbackFlow {
        val uid = currentUid()
        val hid = household?.id?.trim()
        if (uid == null || hid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = requireDb().collection("households").document(hid).collection("inbox")
            .whereEqualTo("status", "inbox")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    reportError("observeInbox", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(InboxItem::class.java)?.copy(id = doc.id, householdId = hid)
                } ?: emptyList()

                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    fun observeMergedInbox(
        household: Household?,
        observeHouseholdInbox: (Household?) -> Flow<List<InboxItem>> = { observeInbox(household = it) },
        observeUserPrivateInbox: (String) -> Flow<List<InboxItem>> = { observePrivateInbox(it) }
    ): Flow<List<InboxItem>> {
        val uid = currentUid()
        val hid = household?.id?.trim()
        if (hid.isNullOrBlank()) return flowOf(emptyList())

        val householdFlow = observeHouseholdInbox(household)
        val privateFlow = if (uid != null) observeUserPrivateInbox(uid) else flowOf(emptyList())

        return combine(householdFlow, privateFlow) { hhInbox, privInbox ->
            (hhInbox + privInbox).sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
    }

    suspend fun addInboxItem(text: String, space: String = SpaceType.SHARED.id, location: ItemLocation) {
        val uid = when (location) {
            is ItemLocation.Private -> location.uid
            is ItemLocation.Household -> requireUid()
        }
        val effectiveSpace = if (location is ItemLocation.Private) "private" else space
        val effectiveHouseholdId = (location as? ItemLocation.Household)?.id ?: ""
        try {
            val docRef = collectionRef(location, "inbox").document()
            val payload = mapOf(
                "text" to text.trim(),
                "space" to effectiveSpace,
                "status" to "inbox",
                "triagedType" to "",
                "userId" to uid,
                "householdId" to effectiveHouseholdId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            reportError("addInboxItem", e)
            throw e
        }
    }

    suspend fun markInboxTriaged(itemId: String, triagedType: String, location: ItemLocation) {
        try {
            collectionRef(location, "inbox").document(itemId)
                .update(
                    mapOf(
                        "status" to "triaged",
                        "triagedType" to triagedType
                    )
                ).await()
        } catch (e: Exception) {
            reportError("markInboxTriaged", e)
            throw e
        }
    }

    suspend fun deleteInboxItem(itemId: String, location: ItemLocation) {
        try {
            collectionRef(location, "inbox").document(itemId)
                .delete()
                .await()
        } catch (e: Exception) {
            reportError("deleteInboxItem", e)
            throw e
        }
    }
}
