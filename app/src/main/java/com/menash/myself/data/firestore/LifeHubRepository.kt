package com.menash.myself.data.firestore

import android.content.Context
import android.util.Log
import com.menash.myself.data.models.DreamItem
import com.menash.myself.data.models.FitnessLog
import com.menash.myself.data.models.Household
import com.menash.myself.data.models.InboxItem
import com.menash.myself.data.models.SpaceType
import com.menash.myself.data.models.TaskItem
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LifeHubRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(FirestoreManager.get(context))

    private val auth = Firebase.auth
    private val TAG = "LifeHubRepository"

    private val _errorFlow = MutableStateFlow<String?>(null)
    val errorFlow: StateFlow<String?> = _errorFlow.asStateFlow()

    private fun requireUid(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    private fun reportError(action: String, error: Throwable) {
        val code = (error as? FirebaseFirestoreException)?.code?.name ?: "ERROR"
        val message = "שגיאת Firestore ב-$action ($code): ${error.localizedMessage ?: error.message}"
        Log.e(TAG, message, error)
        _errorFlow.value = message
    }

    fun clearError() {
        _errorFlow.value = null
    }

    suspend fun resolveHouseholdId(explicitHid: String = ""): String {
        if (explicitHid.isNotBlank()) {
            return explicitHid
        }
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Active household session required: User is not authenticated")
        return try {
            val query = db.collection("households").whereArrayContains("memberUids", uid).limit(1).get().await()
            val hid = query.documents.firstOrNull()?.id
                ?: throw IllegalStateException("Active household session required: No household found for user")
            hid
        } catch (e: Exception) {
            reportError("resolveHouseholdId", e)
            throw e
        }
    }

    // --- TASKS (Scoped under households/{hid}/tasks) ---

    fun observeTasks(spaceFilter: String? = null, household: Household? = null): Flow<List<TaskItem>> = callbackFlow {
        val uid = auth.currentUser?.uid
        val hid = household?.id?.trim()
        if (uid == null || hid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("households").document(hid).collection("tasks")
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

    suspend fun createTask(
        title: String,
        nextAction: String = "",
        isTop3: Boolean = false,
        isFiveBefore11: Boolean = false,
        dueDate: String = "",
        space: String = SpaceType.SHARED.id,
        household: Household? = null
    ) {
        val uid = requireUid()
        val hid = household?.id ?: throw IllegalStateException("Household ID required to create task")
        try {
            val docRef = db.collection("households").document(hid).collection("tasks").document()
            val payload = mapOf(
                "title" to title.trim(),
                "nextAction" to nextAction.trim(),
                "isTop3" to isTop3,
                "isFiveBefore11" to isFiveBefore11,
                "dueDate" to dueDate,
                "isDone" to false,
                "space" to space,
                "userId" to uid,
                "householdId" to hid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            reportError("createTask", e)
            throw e
        }
    }

    suspend fun toggleTaskDone(task: TaskItem) {
        val hid = task.householdId.ifBlank { throw IllegalStateException("Household ID required") }
        try {
            db.collection("households").document(hid).collection("tasks").document(task.id)
                .update("isDone", !task.isDone)
                .await()
        } catch (e: Exception) {
            reportError("toggleTaskDone", e)
            throw e
        }
    }

    suspend fun setTaskTop3(task: TaskItem, isTop3: Boolean) {
        val hid = task.householdId.ifBlank { throw IllegalStateException("Household ID required") }
        try {
            db.collection("households").document(hid).collection("tasks").document(task.id)
                .update("isTop3", isTop3)
                .await()
        } catch (e: Exception) {
            reportError("setTaskTop3", e)
            throw e
        }
    }

    suspend fun setTaskFiveBefore11(task: TaskItem, isFive: Boolean) {
        val hid = task.householdId.ifBlank { throw IllegalStateException("Household ID required") }
        try {
            db.collection("households").document(hid).collection("tasks").document(task.id)
                .update("isFiveBefore11", isFive)
                .await()
        } catch (e: Exception) {
            reportError("setTaskFiveBefore11", e)
            throw e
        }
    }

    suspend fun updateTaskDate(taskId: String, date: String, householdId: String = "") {
        try {
            val hid = resolveHouseholdId(householdId)
            db.collection("households").document(hid).collection("tasks").document(taskId)
                .update("dueDate", date)
                .await()
        } catch (e: Exception) {
            reportError("updateTaskDate", e)
            throw e
        }
    }

    suspend fun deleteTask(taskId: String, householdId: String = "") {
        try {
            val hid = resolveHouseholdId(householdId)
            db.collection("households").document(hid).collection("tasks").document(taskId)
                .delete()
                .await()
        } catch (e: Exception) {
            reportError("deleteTask", e)
            throw e
        }
    }

    // --- DREAMS & LISTS (Scoped under households/{hid}/dreams) ---

    fun observeDreams(listFilter: String? = null, spaceFilter: String? = null, household: Household? = null): Flow<List<DreamItem>> = callbackFlow {
        val uid = auth.currentUser?.uid
        val hid = household?.id?.trim()
        if (uid == null || hid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("households").document(hid).collection("dreams")
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

    suspend fun createDream(dream: DreamItem, household: Household? = null) {
        val uid = requireUid()
        val hid = resolveHouseholdId(household?.id ?: dream.householdId)
        try {
            val docRef = db.collection("households").document(hid).collection("dreams").document()
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
                "space" to dream.space,
                "userId" to uid,
                "householdId" to hid,
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

    suspend fun updateDreamStatus(dreamId: String, status: String, householdId: String = "") {
        try {
            val hid = resolveHouseholdId(householdId)
            db.collection("households").document(hid).collection("dreams").document(dreamId)
                .update("status", status)
                .await()
        } catch (e: Exception) {
            reportError("updateDreamStatus", e)
            throw e
        }
    }

    suspend fun updateDreamSavings(dreamId: String, savedAmount: Double, householdId: String = "") {
        try {
            val hid = resolveHouseholdId(householdId)
            db.collection("households").document(hid).collection("dreams").document(dreamId)
                .update("savedAmount", savedAmount)
                .await()
        } catch (e: Exception) {
            reportError("updateDreamSavings", e)
            throw e
        }
    }

    suspend fun updateDreamAppIdea(dreamId: String, questions: Map<String, String>, nextAction: String, householdId: String = "") {
        try {
            val hid = resolveHouseholdId(householdId)
            db.collection("households").document(hid).collection("dreams").document(dreamId)
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

    suspend fun deleteDream(dreamId: String, householdId: String = "") {
        try {
            val hid = resolveHouseholdId(householdId)
            db.collection("households").document(hid).collection("dreams").document(dreamId)
                .delete()
                .await()
        } catch (e: Exception) {
            reportError("deleteDream", e)
            throw e
        }
    }

    // --- INBOX (QUICK CAPTURE) (Scoped under households/{hid}/inbox) ---

    fun observeInbox(household: Household? = null): Flow<List<InboxItem>> = callbackFlow {
        val uid = auth.currentUser?.uid
        val hid = household?.id?.trim()
        if (uid == null || hid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("households").document(hid).collection("inbox")
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

    suspend fun addInboxItem(text: String, space: String = SpaceType.SHARED.id, household: Household? = null) {
        val uid = requireUid()
        val hid = resolveHouseholdId(household?.id ?: "")
        try {
            val docRef = db.collection("households").document(hid).collection("inbox").document()
            val payload = mapOf(
                "text" to text.trim(),
                "space" to space,
                "status" to "inbox",
                "triagedType" to "",
                "userId" to uid,
                "householdId" to hid,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            reportError("addInboxItem", e)
            throw e
        }
    }

    suspend fun markInboxTriaged(itemId: String, triagedType: String, householdId: String = "") {
        try {
            val hid = resolveHouseholdId(householdId)
            db.collection("households").document(hid).collection("inbox").document(itemId)
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

    suspend fun deleteInboxItem(itemId: String, householdId: String = "") {
        try {
            val hid = resolveHouseholdId(householdId)
            db.collection("households").document(hid).collection("inbox").document(itemId)
                .delete()
                .await()
        } catch (e: Exception) {
            reportError("deleteInboxItem", e)
            throw e
        }
    }

    // --- FITNESS TRACKING (Scoped under users/{uid}/fitness/{logId}) ---

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun observeTodayFitness(): Flow<FitnessLog?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val today = getTodayDateString()
        val listener = db.collection("users").document(uid).collection("fitness").document(today)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    reportError("observeTodayFitness", error)
                    return@addSnapshotListener
                }
                val log = snapshot?.toObject(FitnessLog::class.java)?.copy(id = snapshot.id)
                trySend(log)
            }

        awaitClose { listener.remove() }
    }

    fun observeWeeklyFitness(): Flow<List<FitnessLog>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid).collection("fitness")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(7)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    reportError("observeWeeklyFitness", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FitnessLog::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveTodayFitness(moved: Boolean, minutes: Int, type: String) {
        val uid = requireUid()
        val today = getTodayDateString()
        try {
            val docRef = db.collection("users").document(uid).collection("fitness").document(today)
            docRef.set(
                mapOf(
                    "date" to today,
                    "moved" to moved,
                    "minutes" to minutes,
                    "type" to type,
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            reportError("saveTodayFitness", e)
            throw e
        }
    }
}
