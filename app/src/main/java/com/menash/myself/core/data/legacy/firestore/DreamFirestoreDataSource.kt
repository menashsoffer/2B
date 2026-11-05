package com.menash.myself.core.data.legacy.firestore

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.menash.myself.core.data.model.ItemLocation
import com.menash.myself.core.data.legacy.models.DreamItem
import com.menash.myself.core.data.legacy.models.Household
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class DreamFirestoreDataSource(
    private val requireDb: () -> FirebaseFirestore,
    private val currentUid: () -> String?,
    private val requireUid: () -> String,
    private val privateCollection: (String, String) -> CollectionReference,
    private val collectionRef: (ItemLocation, String) -> CollectionReference,
    private val reportError: (String, Throwable) -> Unit
) {

    fun observePrivateDreams(uid: String): Flow<List<DreamItem>> = callbackFlow {
        val listener = privateCollection(uid, "dreams")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    reportError("observePrivateDreams", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DreamItem::class.java)?.copy(id = doc.id, householdId = "", space = "private")
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    fun observeDreams(
        listFilter: String? = null,
        spaceFilter: String? = null,
        household: Household? = null
    ): Flow<List<DreamItem>> = callbackFlow {
        val uid = currentUid()
        val hid = household?.id?.trim()
        if (uid == null || hid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = requireDb().collection("households").document(hid).collection("dreams")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    reportError("observeDreams", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DreamItem::class.java)?.copy(id = doc.id, householdId = hid)
                } ?: emptyList()

                val filtered = items.filter { dream ->
                    val matchesList = listFilter == null || listFilter == "הכל" || dream.list == listFilter
                    val matchesSpace = spaceFilter == null || spaceFilter == "הכל" || dream.space == spaceFilter
                    matchesList && matchesSpace
                }
                trySend(filtered)
            }

        awaitClose { listener.remove() }
    }

    fun observeMergedDreams(
        listFilter: String? = null,
        household: Household? = null,
        observeHouseholdDreams: (String?, Household?) -> Flow<List<DreamItem>> = { lf, h ->
            observeDreams(listFilter = lf, household = h)
        },
        observeUserPrivateDreams: (String) -> Flow<List<DreamItem>> = { observePrivateDreams(it) }
    ): Flow<List<DreamItem>> {
        val uid = currentUid()
        val hid = household?.id?.trim()
        if (hid.isNullOrBlank()) return flowOf(emptyList())

        val householdFlow = observeHouseholdDreams(listFilter, household)
        val privateFlow = if (uid != null) observeUserPrivateDreams(uid) else flowOf(emptyList())

        return combine(householdFlow, privateFlow) { hhDreams, privDreams ->
            val all = hhDreams + privDreams.filter { dream ->
                listFilter == null || listFilter == "הכל" || dream.list == listFilter
            }
            all.sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
    }

    suspend fun createDream(dream: DreamItem, location: ItemLocation) {
        val uid = when (location) {
            is ItemLocation.Private -> location.uid
            is ItemLocation.Household -> requireUid()
        }
        val effectiveSpace = if (location is ItemLocation.Private) "private" else dream.space
        val effectiveHouseholdId = (location as? ItemLocation.Household)?.id ?: ""
        try {
            val docRef = collectionRef(location, "dreams").document()
            val payload = mapOf(
                "title" to dream.title.trim(),
                "list" to dream.list.trim(),
                "estimatedCost" to dream.estimatedCost,
                "savedAmount" to dream.savedAmount,
                "link" to dream.link.trim(),
                "priority" to dream.priority,
                "status" to dream.status,
                "targetDate" to dream.targetDate,
                "notes" to dream.notes.trim(),
                "space" to effectiveSpace,
                "userId" to uid,
                "householdId" to effectiveHouseholdId,
                "appIdeaQuestions" to dream.appIdeaQuestions,
                "nextAction" to dream.nextAction.trim(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            reportError("createDream", e)
            throw e
        }
    }

    suspend fun updateDreamStatus(dreamId: String, status: String, location: ItemLocation) {
        try {
            collectionRef(location, "dreams").document(dreamId)
                .update("status", status)
                .await()
        } catch (e: Exception) {
            reportError("updateDreamStatus", e)
            throw e
        }
    }

    suspend fun updateDreamSavings(dreamId: String, savedAmount: Double, location: ItemLocation) {
        try {
            collectionRef(location, "dreams").document(dreamId)
                .update("savedAmount", savedAmount)
                .await()
        } catch (e: Exception) {
            reportError("updateDreamSavings", e)
            throw e
        }
    }

    suspend fun updateDreamAppIdea(
        dreamId: String,
        questions: Map<String, String>,
        nextAction: String,
        location: ItemLocation
    ) {
        try {
            collectionRef(location, "dreams").document(dreamId)
                .update(
                    mapOf(
                        "appIdeaQuestions" to questions,
                        "nextAction" to nextAction
                    )
                ).await()
        } catch (e: Exception) {
            reportError("updateDreamAppIdea", e)
            throw e
        }
    }

    suspend fun deleteDream(dreamId: String, location: ItemLocation) {
        try {
            collectionRef(location, "dreams").document(dreamId)
                .delete()
                .await()
        } catch (e: Exception) {
            reportError("deleteDream", e)
            throw e
        }
    }
}
