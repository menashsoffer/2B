package com.menash.myself.core.data.legacy.firestore

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.menash.myself.core.data.model.ItemLocation
import com.menash.myself.core.data.legacy.models.Household
import com.menash.myself.core.data.legacy.models.SpaceType
import com.menash.myself.core.data.legacy.models.TaskItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class TaskFirestoreDataSource(
    private val requireDb: () -> FirebaseFirestore,
    private val currentUid: () -> String?,
    private val requireUid: () -> String,
    private val privateCollection: (String, String) -> CollectionReference,
    private val collectionRef: (ItemLocation, String) -> CollectionReference,
    private val reportError: (String, Throwable) -> Unit
) {

    fun observePrivateTasks(uid: String): Flow<List<TaskItem>> = callbackFlow {
        val listener = privateCollection(uid, "tasks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    reportError("observePrivateTasks", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TaskItem::class.java)?.copy(id = doc.id, householdId = "", space = "private")
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    fun observeTasks(spaceFilter: String? = null, household: Household? = null): Flow<List<TaskItem>> = callbackFlow {
        val uid = currentUid()
        val hid = household?.id?.trim()
        if (uid == null || hid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = requireDb().collection("households").document(hid).collection("tasks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    reportError("observeTasks", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TaskItem::class.java)?.copy(id = doc.id, householdId = hid)
                } ?: emptyList()

                val filtered = items.filter { task ->
                    if (spaceFilter != null && spaceFilter != "all" && spaceFilter != "הכל") {
                        task.space == spaceFilter
                    } else {
                        true
                    }
                }
                trySend(filtered)
            }

        awaitClose { listener.remove() }
    }

    fun observeMergedTasks(
        household: Household?,
        observeHouseholdTasks: (Household?) -> Flow<List<TaskItem>> = { observeTasks(household = it) },
        observeUserPrivateTasks: (String) -> Flow<List<TaskItem>> = { observePrivateTasks(it) }
    ): Flow<List<TaskItem>> {
        val uid = currentUid()
        val hid = household?.id?.trim()
        if (hid.isNullOrBlank()) return flowOf(emptyList())

        val householdFlow = observeHouseholdTasks(household)
        val privateFlow = if (uid != null) observeUserPrivateTasks(uid) else flowOf(emptyList())

        return combine(householdFlow, privateFlow) { hhTasks, privTasks ->
            (hhTasks + privTasks).sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
    }

    suspend fun createTask(
        title: String,
        nextAction: String = "",
        isTop3: Boolean = false,
        isFiveBefore11: Boolean = false,
        dueDate: String = "",
        space: String = SpaceType.SHARED.id,
        location: ItemLocation
    ) {
        val uid = when (location) {
            is ItemLocation.Private -> location.uid
            is ItemLocation.Household -> requireUid()
        }
        val effectiveSpace = if (location is ItemLocation.Private) "private" else space
        val effectiveHouseholdId = (location as? ItemLocation.Household)?.id ?: ""
        try {
            val docRef = collectionRef(location, "tasks").document()
            val payload = mapOf(
                "title" to title.trim(),
                "nextAction" to nextAction.trim(),
                "isTop3" to isTop3,
                "isFiveBefore11" to isFiveBefore11,
                "dueDate" to dueDate,
                "isDone" to false,
                "space" to effectiveSpace,
                "userId" to uid,
                "householdId" to effectiveHouseholdId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            reportError("createTask", e)
            throw e
        }
    }

    suspend fun toggleTaskDone(taskId: String, currentDone: Boolean, location: ItemLocation) {
        try {
            collectionRef(location, "tasks").document(taskId)
                .update("isDone", !currentDone)
                .await()
        } catch (e: Exception) {
            reportError("toggleTaskDone", e)
            throw e
        }
    }

    suspend fun setTaskTop3(taskId: String, isTop3: Boolean, location: ItemLocation) {
        try {
            collectionRef(location, "tasks").document(taskId)
                .update("isTop3", isTop3)
                .await()
        } catch (e: Exception) {
            reportError("setTaskTop3", e)
            throw e
        }
    }

    suspend fun setTaskFiveBefore11(taskId: String, isFive: Boolean, location: ItemLocation) {
        try {
            collectionRef(location, "tasks").document(taskId)
                .update("isFiveBefore11", isFive)
                .await()
        } catch (e: Exception) {
            reportError("setTaskFiveBefore11", e)
            throw e
        }
    }

    suspend fun updateTaskDate(taskId: String, date: String, location: ItemLocation) {
        try {
            collectionRef(location, "tasks").document(taskId)
                .update("dueDate", date)
                .await()
        } catch (e: Exception) {
            reportError("updateTaskDate", e)
            throw e
        }
    }

    suspend fun deleteTask(taskId: String, location: ItemLocation) {
        try {
            collectionRef(location, "tasks").document(taskId)
                .delete()
                .await()
        } catch (e: Exception) {
            reportError("deleteTask", e)
            throw e
        }
    }
}
