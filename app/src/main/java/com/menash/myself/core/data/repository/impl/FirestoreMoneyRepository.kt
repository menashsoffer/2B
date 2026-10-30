package com.menash.myself.core.data.repository.impl

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.menash.myself.core.data.session.HouseholdSession
import com.menash.myself.feature.money.data.MoneyCollection
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyDocuments
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.data.MoneyRepository
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.Card
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.MoneyItems
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import com.menash.myself.data.firestore.FirestoreErrors

/** households/{hid}/{collection}/{id}, one listener per collection. */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreMoneyRepository(
    private val db: () -> FirebaseFirestore,
    private val householdSession: HouseholdSession,
) : MoneyRepository {

    private val _errorFlow = MutableStateFlow<String?>(null)
    val errorFlow: StateFlow<String?> = _errorFlow.asStateFlow()

    fun clearError() {
        _errorFlow.value = null
    }

    private fun reportError(action: String, error: Throwable) {
        FirestoreErrors.report("FirestoreMoneyRepository", action, error, _errorFlow) {}
    }

    private data class RawDoc(val id: String, val data: Map<String, Any?>)

    override fun observe(): Flow<MoneyData> =
        householdSession.currentHouseholdId.flatMapLatest { hid ->
            if (hid.isNullOrBlank()) {
                flowOf(MoneyData())
            } else {
                val flows = MoneyCollection.entries.map { collection -> observeRaw(hid, collection) }
                combine(flows) { snapshots ->
                    decodeAll(MoneyCollection.entries.zip(snapshots.toList()).toMap())
                }
            }
        }

    override suspend fun save(entry: MoneyEntry) {
        try {
            collection(householdSession.requireHouseholdId(), entry.collection)
                .document(entry.id)
                .set(MoneyDocuments.encode(entry))
                .await()
        } catch (e: Exception) {
            reportError("saveMoney:${entry.collection.path}", e)
            throw e
        }
    }

    override suspend fun delete(collection: MoneyCollection, id: String) {
        try {
            collection(householdSession.requireHouseholdId(), collection).document(id).delete().await()
        } catch (e: Exception) {
            reportError("deleteMoney:${collection.path}", e)
            throw e
        }
    }

    override suspend fun saveCategories(categories: List<Category>) {
        if (categories.isEmpty()) return
        try {
            val target = collection(householdSession.requireHouseholdId(), MoneyCollection.CATEGORIES)
            val batch = db().batch()
            categories.forEach { category ->
                batch.set(target.document(category.id), MoneyDocuments.encode(MoneyEntry.CategoryEntry(category)))
            }
            batch.commit().await()
        } catch (e: Exception) {
            reportError("saveCategories", e)
            throw e
        }
    }

    private fun collection(hid: String, collection: MoneyCollection): CollectionReference =
        db().collection("households").document(hid).collection(collection.path)

    private fun observeRaw(hid: String, collection: MoneyCollection): Flow<List<RawDoc>> = callbackFlow {
        val listener = collection(hid, collection).addSnapshotListener { snapshot, error ->
            if (error != null) {
                reportError("observeMoney:${collection.path}", error)
                return@addSnapshotListener
            }
            val docs = snapshot?.documents?.map { doc -> RawDoc(doc.id, doc.data.orEmpty()) }.orEmpty()
            trySend(docs)
        }
        awaitClose { listener.remove() }
    }

    private fun decodeAll(raw: Map<MoneyCollection, List<RawDoc>>): MoneyData {
        fun decode(collection: MoneyCollection, cards: Map<String, Card>): List<MoneyEntry> =
            raw[collection].orEmpty().mapNotNull { doc ->
                try {
                    MoneyDocuments.decode(collection, doc.id, doc.data, cards)
                } catch (e: IllegalArgumentException) {
                    reportError("decodeMoney:${collection.path}/${doc.id}", e)
                    null
                } catch (e: java.time.format.DateTimeParseException) {
                    reportError("decodeMoney:${collection.path}/${doc.id}", e)
                    null
                }
            }
        val cards = decode(MoneyCollection.CARDS, emptyMap())
            .map { (it as MoneyEntry.CardEntry).value }
        val byId = cards.associateBy { it.id }
        val entries = MoneyCollection.entries
            .filter { it != MoneyCollection.CARDS }
            .flatMap { decode(it, byId) }
        return MoneyData(
            householdReady = true,
            cards = cards,
            items = MoneyItems(
                incomes = entries.filterIsInstance<MoneyEntry.IncomeEntry>().map { it.value },
                fixedExpenses = entries.filterIsInstance<MoneyEntry.FixedExpenseEntry>().map { it.value },
                subscriptions = entries.filterIsInstance<MoneyEntry.SubscriptionEntry>().map { it.value },
                variableBudgets = entries.filterIsInstance<MoneyEntry.VariableBudgetEntry>().map { it.value },
                installments = entries.filterIsInstance<MoneyEntry.InstallmentEntry>().map { it.value },
                periodicBudgets = entries.filterIsInstance<MoneyEntry.PeriodicBudgetEntry>().map { it.value },
                debts = entries.filterIsInstance<MoneyEntry.DebtEntry>().map { it.value },
                savings = entries.filterIsInstance<MoneyEntry.SavingEntry>().map { it.value },
            ),
            assets = entries.filterIsInstance<MoneyEntry.AssetEntry>().map<MoneyEntry.AssetEntry, Asset> { it.value },
            categories = entries.filterIsInstance<MoneyEntry.CategoryEntry>().map { it.value },
            goals = entries.filterIsInstance<MoneyEntry.GoalEntry>().map { it.value },
            donations = entries.filterIsInstance<MoneyEntry.DonationEntry>().map { it.value },
            tithe = entries.filterIsInstance<MoneyEntry.TitheEntry>().firstOrNull()?.value,
            closedMonths = entries.filterIsInstance<MoneyEntry.ClosedMonthEntry>().map { it.value }.sortedBy { it.month },
            car = entries.filterIsInstance<MoneyEntry.CarEntry>().firstOrNull()?.value,
        )
    }
}
