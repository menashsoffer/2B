package com.menash.myself.data.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.menash.myself.data.models.FitnessLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FitnessFirestoreDataSource(
    private val requireDb: () -> FirebaseFirestore,
    private val currentUid: () -> String?,
    private val requireUid: () -> String,
    private val reportError: (String, Throwable) -> Unit
) {

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun observeTodayFitness(): Flow<FitnessLog?> = callbackFlow {
        val uid = currentUid()
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val today = getTodayDateString()
        val listener = requireDb().collection("users").document(uid).collection("fitness").document(today)
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
        val uid = currentUid()
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = requireDb().collection("users").document(uid).collection("fitness")
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
            val docRef = requireDb().collection("users").document(uid).collection("fitness").document(today)
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
