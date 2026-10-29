package com.menash.myself.core.data.repository.impl

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.menash.myself.core.data.session.HouseholdSession
import com.menash.myself.data.firestore.FirestoreErrors
import com.menash.myself.feature.money.data.ImportDocuments
import com.menash.myself.feature.money.data.TransactionRepository
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.ImportPlan
import com.menash.myself.feature.money.importer.StoredTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/** households/{hid}/transactions and households/{hid}/merchantMemory. */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreTransactionRepository(
    private val db: () -> FirebaseFirestore,
    private val householdSession: HouseholdSession,
) : TransactionRepository {

    private val _errorFlow = MutableStateFlow<String?>(null)
    val errorFlow: StateFlow<String?> = _errorFlow.asStateFlow()

    fun clearError() {
        _errorFlow.value = null
    }

    private fun reportError(action: String, error: Throwable) {
        FirestoreErrors.report("FirestoreTransactionRepository", action, error, _errorFlow) {}
    }

    private fun collection(hid: String, name: String): CollectionReference =
        db().collection("households").document(hid).collection(name)

    private fun <T> observe(name: String, empty: T, map: (List<Pair<String, Map<String, Any?>>>) -> T): Flow<T> =
        householdSession.currentHouseholdId.flatMapLatest { hid ->
            if (hid.isNullOrBlank()) flowOf(empty)
            else callbackFlow {
                val listener = collection(hid, name).addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        reportError("observe:$name", error)
                        return@addSnapshotListener
                    }
                    trySend(map(snapshot?.documents?.map { it.id to it.data.orEmpty() }.orEmpty()))
                }
                awaitClose { listener.remove() }
            }
        }

    override fun observeTransactions(): Flow<List<StoredTransaction>> =
        observe(ImportDocuments.TRANSACTIONS, emptyList()) { docs ->
            docs.mapNotNull { (id, data) ->
                try {
                    ImportDocuments.decode(id, data)
                } catch (e: IllegalArgumentException) {
                    reportError("decodeTransaction:$id", e)
                    null
                } catch (e: java.time.format.DateTimeParseException) {
                    reportError("decodeTransaction:$id", e)
                    null
                }
            }
        }

    override fun observeMemory(): Flow<ImportMemory> =
        observe(ImportDocuments.MERCHANT_MEMORY, ImportMemory()) { docs -> ImportDocuments.decodeMemory(docs.map { it.second }) }

    override suspend fun apply(plan: ImportPlan) {
        try {
            val target = collection(householdSession.requireHouseholdId(), ImportDocuments.TRANSACTIONS)
            val writes: List<(com.google.firebase.firestore.WriteBatch) -> Unit> =
                plan.toWrite.map { stored -> { batch -> batch.set(target.document(stored.id), ImportDocuments.encode(stored)) } } +
                    plan.toDelete.map { id -> { batch -> batch.delete(target.document(id)) } }
            // Firestore allows 500 writes per batch.
            writes.chunked(450).forEach { chunk ->
                val batch = db().batch()
                chunk.forEach { it(batch) }
                batch.commit().await()
            }
        } catch (e: Exception) {
            reportError("applyImport", e)
            throw e
        }
    }

    override suspend fun setCategory(transactionId: String, categoryId: String?) {
        try {
            collection(householdSession.requireHouseholdId(), ImportDocuments.TRANSACTIONS).document(transactionId)
                .update("categoryId", categoryId ?: FieldValue.delete()).await()
        } catch (e: Exception) {
            reportError("setCategory", e)
            throw e
        }
    }

    override suspend fun remember(merchantKey: String, categoryId: String?, ownTransfer: Boolean) {
        try {
            collection(householdSession.requireHouseholdId(), ImportDocuments.MERCHANT_MEMORY)
                .document(ImportDocuments.memoryId(merchantKey))
                .set(ImportDocuments.encodeMemory(merchantKey, categoryId, ownTransfer), SetOptions.merge()).await()
        } catch (e: Exception) {
            reportError("rememberMerchant", e)
            throw e
        }
    }
}
