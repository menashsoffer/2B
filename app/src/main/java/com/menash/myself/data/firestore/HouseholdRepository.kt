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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.withLock
import com.google.firebase.firestore.Source

class HouseholdRepository(private val db: FirebaseFirestore, private val pendingDeletionUid: () -> String? = { null }) {

    constructor(context: Context) : this(FirestoreManager.get(context))

    private val auth = Firebase.auth
    private val TAG = "HouseholdRepository"

    private val _errorFlow = MutableStateFlow<String?>(null)
    val errorFlow: StateFlow<String?> = _errorFlow.asStateFlow()

    var lastTechnicalError: String? = null
        private set

    private fun requireUid(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    private fun userEmail(): String {
        return auth.currentUser?.email?.lowercase()?.trim() ?: ""
    }

    private fun reportError(action: String, error: Throwable) {
        FirestoreErrors.report(TAG, action, error, _errorFlow) { technical ->
            lastTechnicalError = technical
        }
    }

    fun clearError() {
        _errorFlow.value = null
        lastTechnicalError = null
    }

    private val creationMutex = kotlinx.coroutines.sync.Mutex()

    suspend fun getOrCreateHousehold(): Household = creationMutex.withLock {
        check(pendingDeletionUid() != requireUid()) { "Account deletion pending" }
        getOrCreateUnlocked()
    }

    private suspend fun getOrCreateUnlocked(): Household {
        val uid = requireUid()
        val email = userEmail()

        try {
            // 1. Check if user already has an invite waiting
            var inviteHouseholdId: String? = null
            if (email.isNotEmpty()) {
                try {
                    val inviteDoc = db.collection("invites").document(email).get(Source.SERVER).await()
                    if (inviteDoc.exists()) {
                        inviteHouseholdId = inviteDoc.getString("householdId")
                    }
                } catch (e: Exception) {
                    reportError("checkInvite", e)
                }
            }

            // 2. Query households where user is already a member
            val memberQuery = db.collection("households")
                .whereArrayContains("memberUids", uid)
                .get(Source.SERVER)
                .await()

            if (!memberQuery.isEmpty) {
                val currentDoc = memberQuery.documents.first()
                val currentHousehold = currentDoc.toObject(Household::class.java)?.copy(id = currentDoc.id)
                    ?: Household(id = currentDoc.id)

                // If user has an invite to another household, and their current household is empty (sole member)
                if (inviteHouseholdId != null && inviteHouseholdId != currentHousehold.id &&
                    currentHousehold.memberUids.size == 1 && currentHousehold.ownerUid == uid
                ) {
                    val joined = joinInvitedHousehold(inviteHouseholdId, uid)
                    if (joined != null) {
                        try {
                            db.collection("households").document(currentHousehold.id).delete().await()
                            Log.i(TAG, "Removed empty previous household ${currentHousehold.id} after joining $inviteHouseholdId")
                        } catch (e: Exception) {
                            reportError("deleteEmptyHousehold", e)
                        }
                        return joined
                    }
                }
                return currentHousehold
            }

            // 3. User is not in any household yet: join via invite if available
            if (inviteHouseholdId != null) {
                val joined = joinInvitedHousehold(inviteHouseholdId, uid)
                if (joined != null) {
                    return joined
                }
            }

            // 4. Create fresh household for this user
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
        } catch (e: Exception) {
            reportError("getOrCreateHousehold", e)
            throw e
        }
    }

    private suspend fun joinInvitedHousehold(householdId: String, uid: String): Household? {
        try {
            val docRef = db.collection("households").document(householdId)
            // Do not read before joining (non-members cannot read).
            // Update using arrayUnion and set partnerUid:
            docRef.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "partnerUid" to uid
                )
            ).await()

            // After join succeeds, caller is in memberUids and can now read the document
            val doc = docRef.get(Source.SERVER).await()
            val household = doc.toObject(Household::class.java)?.copy(id = doc.id)

            // Clean up the invite doc
            val email = userEmail()
            if (email.isNotEmpty()) {
                try {
                    db.collection("invites").document(email).delete().await()
                    Log.i(TAG, "Deleted invite document for $email after successful join")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete invite document after join: ${e.message}")
                }
            }

            Log.i(TAG, "Successfully joined invited household $householdId")
            return household
        } catch (e: Exception) {
            reportError("joinInvitedHousehold", e)
            return null
        }
    }

    fun observeHousehold(): Flow<Household?> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        var observedUid: String? = null
        val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                listener?.remove(); listener = null; observedUid = null
                trySend(null)
            } else if (observedUid != uid) {
                listener?.remove()
                observedUid = uid
                listener = db.collection("households").whereArrayContains("memberUids", uid)
                    .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                        if (error != null) {
                            reportError("observeHousehold", error)
                            return@addSnapshotListener
                        }
                        // Empty cache is not proof of deletion. INCLUDE delivers the server's empty result too.
                        if (snapshot == null || (snapshot.isEmpty && snapshot.metadata.isFromCache)) return@addSnapshotListener
                        val doc = snapshot.documents.firstOrNull()
                        trySend(doc?.toObject(Household::class.java)?.copy(id = doc.id))
                    }
            }
        }
        auth.addAuthStateListener(authListener)
        awaitClose { auth.removeAuthStateListener(authListener); listener?.remove() }
    }

    suspend fun invitePartner(householdId: String, email: String) {
        val cleanEmail = email.lowercase().trim()
        if (cleanEmail.isEmpty()) return

        try {
            // 1. Create or update invite document for the invitee's email
            db.collection("invites").document(cleanEmail).set(
                mapOf(
                    "householdId" to householdId,
                    "ownerEmail" to userEmail()
                )
            ).await()

            // 2. Update partnerEmail on household (owner only)
            db.collection("households").document(householdId).update("partnerEmail", cleanEmail).await()
            Log.i(TAG, "Invited partner $cleanEmail to household $householdId")
        } catch (e: Exception) {
            reportError("invitePartner", e)
            throw e
        }
    }
}
