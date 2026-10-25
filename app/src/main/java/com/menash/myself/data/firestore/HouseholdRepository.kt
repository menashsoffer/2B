package com.menash.myself.data.firestore

import android.content.Context
import android.util.Log
import com.menash.myself.data.models.Household
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class HouseholdRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(FirestoreManager.get(context))

    private val auth = Firebase.auth
    private val TAG = "HouseholdRepository"

    private fun requireUid(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    private fun userEmail(): String {
        return auth.currentUser?.email ?: ""
    }

    suspend fun getOrCreateHousehold(): Household {
        val uid = requireUid()
        val email = userEmail().lowercase().trim()

        // 1. Try to find existing household where user is member
        val memberQuery = db.collection("households")
            .whereArrayContains("memberUids", uid)
            .get()
            .await()

        if (!memberQuery.isEmpty) {
            val doc = memberQuery.documents.first()
            return doc.toObject(Household::class.java)?.copy(id = doc.id) ?: Household(id = doc.id)
        }

        // 2. If not found, check if invited via partnerEmail
        if (email.isNotEmpty()) {
            val inviteQuery = db.collection("households")
                .whereEqualTo("partnerEmail", email)
                .get()
                .await()

            if (!inviteQuery.isEmpty) {
                val doc = inviteQuery.documents.first()
                val household = doc.toObject(Household::class.java)
                if (household != null) {
                    val updatedMembers = (household.memberUids + uid).distinct()
                    doc.reference.update(
                        mapOf(
                            "memberUids" to updatedMembers,
                            "partnerUid" to uid
                        )
                    ).await()
                    Log.i(TAG, "Joined invited household: ${doc.id}")
                    return household.copy(id = doc.id, memberUids = updatedMembers, partnerUid = uid)
                }
            }
        }

        // 3. Create fresh household for this user
        val newHousehold = Household(
            name = "הבית שלנו",
            ownerUid = uid,
            ownerEmail = email,
            partnerEmail = "",
            partnerUid = "",
            memberUids = listOf(uid)
        )
        val docRef = db.collection("households").document()
        val payload = mapOf(
            "name" to newHousehold.name,
            "ownerUid" to uid,
            "ownerEmail" to email,
            "partnerEmail" to "",
            "partnerUid" to "",
            "memberUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        Log.i(TAG, "Created new household: ${docRef.id}")
        return newHousehold.copy(id = docRef.id)
    }

    fun observeHousehold(): Flow<Household?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("households")
            .whereArrayContains("memberUids", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to observe household", error)
                    return@addSnapshotListener
                }
                val doc = snapshot?.documents?.firstOrNull()
                val household = doc?.toObject(Household::class.java)?.copy(id = doc.id)
                trySend(household)
            }

        awaitClose { listener.remove() }
    }

    suspend fun invitePartner(householdId: String, email: String) {
        val cleanEmail = email.lowercase().trim()
        db.collection("households").document(householdId).update("partnerEmail", cleanEmail).await()
        Log.i(TAG, "Invited partner $cleanEmail to household $householdId")
    }
}
