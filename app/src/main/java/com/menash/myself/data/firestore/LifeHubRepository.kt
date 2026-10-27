package com.menash.myself.data.firestore

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.menash.myself.core.data.model.ItemLocation
import com.menash.myself.data.models.DreamItem
import com.menash.myself.data.models.FitnessLog
import com.menash.myself.data.models.Household
import com.menash.myself.data.models.InboxItem
import com.menash.myself.data.models.SpaceType
import com.menash.myself.data.models.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Facade over per-collection Firestore data sources:
 * [TaskFirestoreDataSource], [DreamFirestoreDataSource], [InboxFirestoreDataSource], [FitnessFirestoreDataSource].
 */
open class LifeHubRepository(
    private val db: FirebaseFirestore? = null,
    private val uidProvider: () -> String? = { runCatching { Firebase.auth.currentUser?.uid }.getOrNull() }
) {

    constructor(context: Context) : this(FirestoreManager.get(context))

    private val TAG = "LifeHubRepository"

    private val _errorFlow = MutableStateFlow<String?>(null)
    val errorFlow: StateFlow<String?> = _errorFlow.asStateFlow()

    var lastTechnicalError: String? = null
        private set

    fun currentUid(): String? = uidProvider()

    private fun requireDb(): FirebaseFirestore {
        return db ?: throw IllegalStateException("FirebaseFirestore instance not provided")
    }

    private fun requireUid(): String {
        return uidProvider() ?: throw IllegalStateException("User not authenticated")
    }

    fun reportError(action: String, error: Throwable) {
        FirestoreErrors.report(TAG, action, error, _errorFlow) { technical ->
            lastTechnicalError = technical
        }
    }

    fun clearError() {
        _errorFlow.value = null
        lastTechnicalError = null
    }

    open suspend fun resolveHouseholdId(explicitHid: String = ""): String {
        if (explicitHid.isNotBlank()) return explicitHid
        val uid = uidProvider()
            ?: throw IllegalStateException("Active household session required: User is not authenticated")
        return try {
            val query = requireDb().collection("households").whereArrayContains("memberUids", uid).limit(1).get().await()
            query.documents.firstOrNull()?.id
                ?: throw IllegalStateException("Active household session required: No household found for user")
        } catch (e: Exception) {
            reportError("resolveHouseholdId", e)
            throw e
        }
    }

    fun privateCollection(uid: String, collection: String) =
        requireDb().collection("users")
            .document(uid)
            .collection("private")
            .document(collection)
            .collection("items")

    fun collectionRef(location: ItemLocation, collection: String) = when (location) {
        is ItemLocation.Private -> privateCollection(location.uid, collection)
        is ItemLocation.Household -> requireDb().collection("households").document(location.id).collection(collection)
    }

    private val taskDataSource = TaskFirestoreDataSource(
        requireDb = ::requireDb,
        currentUid = ::currentUid,
        requireUid = ::requireUid,
        privateCollection = ::privateCollection,
        collectionRef = ::collectionRef,
        reportError = ::reportError
    )

    private val dreamDataSource = DreamFirestoreDataSource(
        requireDb = ::requireDb,
        currentUid = ::currentUid,
        requireUid = ::requireUid,
        privateCollection = ::privateCollection,
        collectionRef = ::collectionRef,
        reportError = ::reportError
    )

    private val inboxDataSource = InboxFirestoreDataSource(
        requireDb = ::requireDb,
        currentUid = ::currentUid,
        requireUid = ::requireUid,
        privateCollection = ::privateCollection,
        collectionRef = ::collectionRef,
        reportError = ::reportError
    )

    private val fitnessDataSource = FitnessFirestoreDataSource(
        requireDb = ::requireDb,
        currentUid = ::currentUid,
        requireUid = ::requireUid,
        reportError = ::reportError
    )

    // --- TASKS ---
    open fun observePrivateTasks(uid: String): Flow<List<TaskItem>> =
        taskDataSource.observePrivateTasks(uid)

    open fun observeTasks(spaceFilter: String? = null, household: Household? = null): Flow<List<TaskItem>> =
        taskDataSource.observeTasks(spaceFilter, household)

    open fun observeMergedTasks(household: Household?): Flow<List<TaskItem>> =
        taskDataSource.observeMergedTasks(
            household = household,
            observeHouseholdTasks = { h -> observeTasks(household = h) },
            observeUserPrivateTasks = { u -> observePrivateTasks(u) }
        )

    open suspend fun createTask(
        title: String,
        nextAction: String = "",
        isTop3: Boolean = false,
        isFiveBefore11: Boolean = false,
        dueDate: String = "",
        space: String = SpaceType.SHARED.id,
        location: ItemLocation
    ) = taskDataSource.createTask(title, nextAction, isTop3, isFiveBefore11, dueDate, space, location)

    open suspend fun toggleTaskDone(taskId: String, currentDone: Boolean, location: ItemLocation) =
        taskDataSource.toggleTaskDone(taskId, currentDone, location)

    open suspend fun setTaskTop3(taskId: String, isTop3: Boolean, location: ItemLocation) =
        taskDataSource.setTaskTop3(taskId, isTop3, location)

    open suspend fun setTaskFiveBefore11(taskId: String, isFive: Boolean, location: ItemLocation) =
        taskDataSource.setTaskFiveBefore11(taskId, isFive, location)

    open suspend fun updateTaskDate(taskId: String, date: String, location: ItemLocation) =
        taskDataSource.updateTaskDate(taskId, date, location)

    open suspend fun deleteTask(taskId: String, location: ItemLocation) =
        taskDataSource.deleteTask(taskId, location)

    // --- DREAMS ---
    open fun observePrivateDreams(uid: String): Flow<List<DreamItem>> =
        dreamDataSource.observePrivateDreams(uid)

    open fun observeDreams(
        listFilter: String? = null,
        spaceFilter: String? = null,
        household: Household? = null
    ): Flow<List<DreamItem>> = dreamDataSource.observeDreams(listFilter, spaceFilter, household)

    open fun observeMergedDreams(listFilter: String? = null, household: Household? = null): Flow<List<DreamItem>> =
        dreamDataSource.observeMergedDreams(
            listFilter = listFilter,
            household = household,
            observeHouseholdDreams = { lf, h -> observeDreams(listFilter = lf, household = h) },
            observeUserPrivateDreams = { u -> observePrivateDreams(u) }
        )

    open suspend fun createDream(dream: DreamItem, location: ItemLocation) =
        dreamDataSource.createDream(dream, location)

    open suspend fun updateDreamStatus(dreamId: String, status: String, location: ItemLocation) =
        dreamDataSource.updateDreamStatus(dreamId, status, location)

    open suspend fun updateDreamSavings(dreamId: String, savedAmount: Double, location: ItemLocation) =
        dreamDataSource.updateDreamSavings(dreamId, savedAmount, location)

    open suspend fun updateDreamAppIdea(
        dreamId: String,
        questions: Map<String, String>,
        nextAction: String,
        location: ItemLocation
    ) = dreamDataSource.updateDreamAppIdea(dreamId, questions, nextAction, location)

    open suspend fun deleteDream(dreamId: String, location: ItemLocation) =
        dreamDataSource.deleteDream(dreamId, location)

    // --- INBOX ---
    open fun observePrivateInbox(uid: String): Flow<List<InboxItem>> =
        inboxDataSource.observePrivateInbox(uid)

    open fun observeInbox(household: Household? = null): Flow<List<InboxItem>> =
        inboxDataSource.observeInbox(household)

    open fun observeMergedInbox(household: Household?): Flow<List<InboxItem>> =
        inboxDataSource.observeMergedInbox(
            household = household,
            observeHouseholdInbox = { h -> observeInbox(household = h) },
            observeUserPrivateInbox = { u -> observePrivateInbox(u) }
        )

    open suspend fun addInboxItem(text: String, space: String = SpaceType.SHARED.id, location: ItemLocation) =
        inboxDataSource.addInboxItem(text, space, location)

    open suspend fun markInboxTriaged(itemId: String, triagedType: String, location: ItemLocation) =
        inboxDataSource.markInboxTriaged(itemId, triagedType, location)

    open suspend fun deleteInboxItem(itemId: String, location: ItemLocation) =
        inboxDataSource.deleteInboxItem(itemId, location)

    // --- FITNESS ---
    fun getTodayDateString(): String = fitnessDataSource.getTodayDateString()

    open fun observeTodayFitness(): Flow<FitnessLog?> = fitnessDataSource.observeTodayFitness()

    open fun observeWeeklyFitness(): Flow<List<FitnessLog>> = fitnessDataSource.observeWeeklyFitness()

    open suspend fun saveTodayFitness(moved: Boolean, minutes: Int, type: String) =
        fitnessDataSource.saveTodayFitness(moved, minutes, type)
}
