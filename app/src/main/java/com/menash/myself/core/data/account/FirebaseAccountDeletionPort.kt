package com.menash.myself.core.data.account

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.menash.myself.R
import com.menash.myself.core.data.session.HouseholdSession
import kotlinx.coroutines.tasks.await

class FirebaseAccountDeletionPort(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val session: HouseholdSession,
    private val pending: AccountDeletionPending,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AccountDeletionPort {
    // Capture identity once: reauthenticating another Google account must never switch the deletion target.
    private val user = requireNotNull(auth.currentUser)
    override val uid = user.uid
    override val email = user.email.orEmpty()
    private val credentials = CredentialManager.create(context)

    override suspend fun requireNetwork() {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        check(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) { "Offline" }
        // A server read verifies actual Firestore reachability, not merely network availability.
        db.collection("users").document(uid).collection("private").document("_connection").get(Source.SERVER).await()
    }
    override suspend fun reauthenticate() = reauthenticateGoogle(context, uid)
    override suspend fun households(): List<AccountHousehold> = db.collection("households")
        .whereArrayContains("memberUids", uid).get(Source.SERVER).await().documents.map { it.accountHousehold() }

    override suspend fun updateHousehold(id: String, change: (AccountHousehold) -> AccountHousehold) {
        // A member-query handles a retry after an acknowledged leave, without trying to read a forbidden doc.
        if (households().none { it.id == id }) return
        val ref = db.collection("households").document(id)
        db.runTransaction { tx ->
            val doc = tx.get(ref)
            check(doc.getBoolean("deleting") != true)
            val old = doc.accountHousehold()
            val next = change(old)
            val fields = mapOf("ownerUid" to next.ownerUid, "ownerEmail" to next.ownerEmail,
                "memberUids" to next.memberUids, "partnerUid" to next.partnerUid, "partnerEmail" to next.partnerEmail)
            val changed = fields.filter { (key, value) -> doc.get(key) != value && !(value == "" && doc.get(key) == null) }
            if (changed.isNotEmpty()) tx.update(ref, changed)
        }.await()
    }
    override suspend fun beginHouseholdDeletion(id: String, expectedMembers: List<String>) {
        val ref = db.collection("households").document(id)
        db.runTransaction { tx ->
            val doc = tx.get(ref)
            check(doc.getString("ownerUid") == uid)
            if (doc.accountHousehold().memberUids.toSet() != expectedMembers.toSet()) throw HouseholdMembershipChanged()
            if (doc.getBoolean("deleting") != true) tx.update(ref, "deleting", true)
        }.await()
    }
    override suspend fun documents(collection: String, field: String?, value: String?): List<String> {
        val ref = db.collection(collection)
        val query = if (field == null) ref else ref.whereEqualTo(field, value)
        return query.limit(500).get(Source.SERVER).await().documents.map { it.reference.path }
    }
    override suspend fun deleteDocuments(paths: List<String>, ownerHousehold: String?) {
        require(paths.size <= 500)
        if (paths.isEmpty()) return
        // Unlike WriteBatch.commit(), transactions fail offline instead of entering persistence's write queue.
        db.runTransaction { tx ->
            if (ownerHousehold != null) {
                val h = tx.get(db.collection("households").document(ownerHousehold))
                check(h.getString("ownerUid") == uid && h.getBoolean("deleting") == true)
            } else {
                tx.get(db.collection("users").document(uid).collection("private").document("_connection"))
            }
            paths.forEach { tx.delete(db.document(it)) }
        }.await()
    }
    override suspend fun deleteHouseholdDocument(id: String) {
        val ref = db.collection("households").document(id)
        db.runTransaction { tx ->
            val doc = tx.get(ref)
            check(doc.getString("ownerUid") == uid && doc.getBoolean("deleting") == true)
            tx.delete(ref)
        }.await()
    }
    override suspend fun deleteUnsentReports() { FirebaseCrashlytics.getInstance().deleteUnsentReports() }
    override suspend fun deleteAuthAccount() { user.delete().await() }
    override suspend fun signOut() {
        com.menash.myself.auth.AuthManager.signOutAndClear(credentials) { auth.signOut() }
        session.setHouseholdId(null)
    }
    override fun setAccountDeletionPending(pending: Boolean) { this.pending.set(uid, pending) }
}

internal fun DocumentSnapshot.accountHousehold() = AccountHousehold(
    id, getString("ownerUid").orEmpty(), (get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty(),
    getString("ownerEmail").orEmpty(), getString("partnerUid").orEmpty(), getString("partnerEmail").orEmpty(), getBoolean("deleting") == true,
)

/** Activity context is supplied at the time of the click, never retained by the deletion repository. */
suspend fun reauthenticateGoogle(context: Context, uid: String) {
    val auth = FirebaseAuth.getInstance()
    val user = requireNotNull(auth.currentUser)
    check(user.uid == uid)
    val credentials = CredentialManager.create(context)
    val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
    val result = credentials.getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build())
    val credential = result.credential
    check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
    user.reauthenticate(GoogleAuthProvider.getCredential(token, null)).await()
    check(auth.currentUser?.uid == uid)

}
