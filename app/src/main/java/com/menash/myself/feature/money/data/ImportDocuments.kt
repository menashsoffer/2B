package com.menash.myself.feature.money.data

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.InstallmentInfo
import com.menash.myself.feature.money.domain.MerchantMemory
import com.menash.myself.feature.money.importer.ImportIds
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import java.time.LocalDate

/**
 * households/{hid}/transactions/{id} and households/{hid}/merchantMemory/{m-hash}.
 * Amounts are agorot, dates "YYYY-MM-DD". Nothing personal is stored: rows arrive already masked.
 */
object ImportDocuments {

    const val TRANSACTIONS = "transactions"
    const val MERCHANT_MEMORY = "merchantMemory"

    fun encode(stored: StoredTransaction): Map<String, Any> = stored.row.let { row ->
        buildMap {
            put(SchemaVersion.KEY, SchemaVersion.CURRENT)
            put("source", row.source.name)
            put("sourceId", row.sourceId)
            put("date", row.date.toString())
            row.chargeDate?.let { put("chargeDate", it.toString()) }
            put("description", row.description)
            row.details?.let { put("details", it) }
            row.charge?.let { put("charge", it.agorot) }
            row.originalAmount?.let { put("originalAmount", it.agorot) }
            put("originalCurrency", row.originalCurrency)
            put("status", row.status.name)
            row.installment?.let { put("installmentNumber", it.number.toLong()); put("installmentOf", it.of.toLong()) }
            row.hint?.let { put("hint", it) }
            row.operation?.let { put("operation", it) }
            stored.categoryId?.let { put("categoryId", it) }
        }
    }

    fun decode(id: String, data: Map<String, Any?>): StoredTransaction {
        SchemaVersion.check(data)
        fun string(key: String): String = requireNotNull(data[key] as? String) { "Missing text field '$key'" }
        fun stringOrNull(key: String): String? = (data[key] as? String)?.takeIf { it.isNotEmpty() }
        fun longOrNull(key: String): Long? = (data[key] as? Number)?.toLong()
        val number = longOrNull("installmentNumber")
        val of = longOrNull("installmentOf")
        val row = StatementRow(
            id = id,
            source = Source.valueOf(string("source")),
            sourceId = string("sourceId"),
            date = LocalDate.parse(string("date")),
            chargeDate = stringOrNull("chargeDate")?.let(LocalDate::parse),
            description = string("description"),
            details = stringOrNull("details"),
            charge = longOrNull("charge")?.let(::Money),
            originalAmount = longOrNull("originalAmount")?.let(::Money),
            originalCurrency = stringOrNull("originalCurrency") ?: "ILS",
            status = RowStatus.valueOf(string("status")),
            installment = if (number != null && of != null) InstallmentInfo(number.toInt(), of.toInt()) else null,
            hint = stringOrNull("hint"),
            operation = stringOrNull("operation"),
        )
        return StoredTransaction(row, stringOrNull("categoryId"))
    }

    /** One document per normalised merchant: its category and / or "this is our own account". */
    fun encodeMemory(merchantKey: String, categoryId: String?, ownTransfer: Boolean): Map<String, Any> = buildMap {
        put(SchemaVersion.KEY, SchemaVersion.CURRENT)
        put("key", merchantKey)
        categoryId?.let { put("categoryId", it) }
        put("ownTransfer", ownTransfer)
    }

    fun memoryId(merchantKey: String): String = ImportIds.memoryDocId(merchantKey)

    /** Merge fields for the merchant's display name; a blank alias is removed by the caller. */
    fun encodeAlias(merchantKey: String, alias: String): Map<String, Any> {
        val clean = alias.trim()
        require(clean.isNotEmpty() && clean.length <= MAX_ALIAS) { "Alias must be 1..$MAX_ALIAS characters" }
        return memoryKey(merchantKey) + ("alias" to clean)
    }

    /** The fields every merchant merge write carries, so a merge that only deletes still leaves a valid document. */
    fun memoryKey(merchantKey: String): Map<String, Any> = mapOf(SchemaVersion.KEY to SchemaVersion.CURRENT, "key" to merchantKey)

    /** Merge fields that make a merchant ask every time; the caller also deletes its learnt categoryId. */
    fun encodeAskEachTime(merchantKey: String): Map<String, Any> =
        memoryKey(merchantKey) + ("askEachTime" to true)

    const val MAX_ALIAS = 60

    fun decodeMemory(docs: List<Map<String, Any?>>): ImportMemory {
        val categories = mutableMapOf<String, String>()
        val own = mutableSetOf<String>()
        val aliases = mutableMapOf<String, String>()
        val ask = mutableSetOf<String>()
        docs.forEach { data ->
            // A merchant written by a newer app version is ignored here; its lines simply ask again.
            if (SchemaVersion.of(data) > SchemaVersion.CURRENT) return@forEach
            val key = (data["key"] as? String)?.takeIf { it.isNotEmpty() } ?: return@forEach
            val asks = data["askEachTime"] == true
            if (asks) ask += key
            // A merchant that asks each time has no learnt category, even if an older one is still stored.
            if (!asks) (data["categoryId"] as? String)?.takeIf { it.isNotEmpty() }?.let { categories[key] = it }
            if (data["ownTransfer"] == true) own += key
            (data["alias"] as? String)?.trim()?.takeIf { it.isNotEmpty() }?.let { aliases[key] = it }
        }
        return ImportMemory(MerchantMemory(categories), own, aliases, ask)
    }
}
