package com.menash.myself.feature.money.data

import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.ImportPlan
import com.menash.myself.feature.money.importer.StoredTransaction
import kotlinx.coroutines.flow.Flow

/** Imported bank and card lines plus what the household taught the importer. Shared by every member. */
interface TransactionRepository {
    /** Every stored line of the household; empty when no household is active. */
    fun observeTransactions(): Flow<List<StoredTransaction>>

    fun observeMemory(): Flow<ImportMemory>

    /** Writes a confirmed [ImportPlan]: upserts by id, so applying the same plan twice changes nothing. */
    suspend fun apply(plan: ImportPlan)

    /** The user's category for one line; kept by later re-imports. */
    suspend fun setCategory(transactionId: String, categoryId: String?)

    /** Removes one stored line (a cancelled pending card line). */
    suspend fun delete(transactionId: String)

    /** Remembers a merchant's category and / or marks it as one of the household's own accounts. */
    suspend fun remember(merchantKey: String, categoryId: String?, ownTransfer: Boolean)
}
