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
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LifeHubRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(FirestoreManager.get(context))

    private val auth = Firebase.auth
    private val TAG = "LifeHubRepository"

    private fun requireUid(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    // --- TASKS ---

    fun observeTasks(spaceFilter: String? = null, household: Household? = null): Flow<List<TaskItem>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("tasks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeTasks error", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TaskItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                // Filter in memory to handle private vs shared space seamlessly
                val filtered = items.filter { task ->
                    val isOwner = task.userId == uid
                    val isShared = task.space == SpaceType.SHARED.id || task.space == SpaceType.SHARED.displayName
                    val canSee = isOwner || (isShared && household?.memberUids?.contains(uid) == true)
                    
                    if (!canSee) return@filter false
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
        val docRef = db.collection("tasks").document()
        val members = household?.memberUids ?: listOf(uid)
        val payload = mapOf(
            "title" to title.trim(),
            "nextAction" to nextAction.trim(),
            "isTop3" to isTop3,
            "isFiveBefore11" to isFiveBefore11,
            "dueDate" to dueDate,
            "isDone" to false,
            "space" to space,
            "userId" to uid,
            "householdId" to (household?.id ?: ""),
            "householdMembers" to members,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
    }

    suspend fun toggleTaskDone(task: TaskItem) {
        db.collection("tasks").document(task.id)
            .update("isDone", !task.isDone)
            .await()
    }

    suspend fun setTaskTop3(task: TaskItem, isTop3: Boolean) {
        db.collection("tasks").document(task.id)
            .update("isTop3", isTop3)
            .await()
    }

    suspend fun setTaskFiveBefore11(task: TaskItem, isFive: Boolean) {
        db.collection("tasks").document(task.id)
            .update("isFiveBefore11", isFive)
            .await()
    }

    suspend fun updateTaskDate(taskId: String, date: String) {
        db.collection("tasks").document(taskId)
            .update("dueDate", date)
            .await()
    }

    suspend fun deleteTask(taskId: String) {
        db.collection("tasks").document(taskId).delete().await()
    }

    // --- DREAMS & LISTS ---

    fun observeDreams(listFilter: String? = null, spaceFilter: String? = null, household: Household? = null): Flow<List<DreamItem>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("dreams")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeDreams error", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DreamItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                val filtered = items.filter { dream ->
                    val isOwner = dream.userId == uid
                    val isShared = dream.space == SpaceType.SHARED.id || dream.space == SpaceType.SHARED.displayName
                    val canSee = isOwner || (isShared && household?.memberUids?.contains(uid) == true)
                    if (!canSee) return@filter false

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
        val docRef = db.collection("dreams").document()
        val members = household?.memberUids ?: listOf(uid)
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
            "householdId" to (household?.id ?: ""),
            "householdMembers" to members,
            "appIdeaQuestions" to dream.appIdeaQuestions,
            "nextAction" to dream.nextAction.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
    }

    suspend fun updateDreamStatus(dreamId: String, status: String) {
        db.collection("dreams").document(dreamId).update("status", status).await()
    }

    suspend fun updateDreamSavings(dreamId: String, savedAmount: Double) {
        db.collection("dreams").document(dreamId).update("savedAmount", savedAmount).await()
    }

    suspend fun updateDreamAppIdea(dreamId: String, questions: Map<String, String>, nextAction: String) {
        db.collection("dreams").document(dreamId)
            .update(
                mapOf(
                    "appIdeaQuestions" to questions,
                    "nextAction" to nextAction
                )
            ).await()
    }

    suspend fun deleteDream(dreamId: String) {
        db.collection("dreams").document(dreamId).delete().await()
    }

    // --- INBOX (QUICK CAPTURE) ---

    fun observeInbox(household: Household? = null): Flow<List<InboxItem>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("inbox")
            .whereEqualTo("status", "inbox")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeInbox error", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(InboxItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                val visible = items.filter { item ->
                    item.userId == uid || (item.space == SpaceType.SHARED.id && household?.memberUids?.contains(uid) == true)
                }
                trySend(visible)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addInboxItem(text: String, space: String = SpaceType.SHARED.id, household: Household? = null) {
        val uid = requireUid()
        val docRef = db.collection("inbox").document()
        val members = household?.memberUids ?: listOf(uid)
        val payload = mapOf(
            "text" to text.trim(),
            "userId" to uid,
            "space" to space,
            "status" to "inbox",
            "triagedType" to "",
            "householdMembers" to members,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
    }

    suspend fun deleteInboxItem(itemId: String) {
        db.collection("inbox").document(itemId).delete().await()
    }

    suspend fun markInboxTriaged(itemId: String, triagedType: String) {
        db.collection("inbox").document(itemId)
            .update(
                mapOf(
                    "status" to "triaged",
                    "triagedType" to triagedType
                )
            ).await()
    }

    // --- FITNESS TRACKING ---

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
        val listener = db.collection("fitness_logs")
            .whereEqualTo("userId", uid)
            .whereEqualTo("date", today)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeTodayFitness error", error)
                    return@addSnapshotListener
                }
                val doc = snapshot?.documents?.firstOrNull()
                val log = doc?.toObject(FitnessLog::class.java)?.copy(id = doc.id)
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

        val listener = db.collection("fitness_logs")
            .whereEqualTo("userId", uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(7)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeWeeklyFitness error", error)
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
        val query = db.collection("fitness_logs")
            .whereEqualTo("userId", uid)
            .whereEqualTo("date", today)
            .get()
            .await()

        if (!query.isEmpty) {
            val doc = query.documents.first()
            doc.reference.update(
                mapOf(
                    "moved" to moved,
                    "minutes" to minutes,
                    "type" to type
                )
            ).await()
        } else {
            val docRef = db.collection("fitness_logs").document()
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
        }
    }
}
