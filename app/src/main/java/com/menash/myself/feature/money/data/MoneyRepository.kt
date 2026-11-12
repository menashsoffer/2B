package com.menash.myself.feature.money.data

import com.menash.myself.feature.money.domain.Category
import kotlinx.coroutines.flow.Flow

/** Household money documents. Money is fully shared: every member reads and writes all of it. */
interface MoneyRepository {
    /** Emits the whole household money state on every change; [MoneyData] () when no household is active. */
    fun observe(): Flow<MoneyData>
    fun observeForScreen(): Flow<MoneyData> = observe()
    fun observeForImport(): Flow<MoneyData> = observeForScreen()

    /** Creates or replaces the document with the entry's id. */
    suspend fun save(entry: MoneyEntry)

    /** Save related entries as one operation when the backing store supports it. */
    suspend fun saveAll(entries: List<MoneyEntry>) {
        entries.forEach { save(it) }
    }

    suspend fun delete(collection: MoneyCollection, id: String)

    /** Writes [categories] in one batch. Ids are stable, so writing them twice does not duplicate. */
    suspend fun saveCategories(categories: List<Category>)
}

/** The Paamonim defaults go in only when the household has no categories at all. */
fun categoriesToSeed(existing: List<Category>, defaults: List<Category>): List<Category> =
    if (existing.isEmpty()) defaults else defaults.filter { it.id == com.menash.myself.feature.money.domain.DONATION_CATEGORY_ID && existing.none { category -> category.id == it.id } }
