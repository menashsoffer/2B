package com.menash.myself.feature.money.data

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.AmountChange
import com.menash.myself.feature.money.domain.AmountHistory
import java.time.YearMonth

/** Typed reads of one Firestore money document; a missing required field throws [IllegalArgumentException]. */
internal class MoneyDoc(private val data: Map<String, Any?>) {
    fun string(key: String): String =
        requireNotNull(data[key] as? String) { "Missing text field '$key'" }

    fun stringOrNull(key: String): String? = (data[key] as? String)?.takeIf { it.isNotBlank() }

    fun longOrNull(key: String): Long? = (data[key] as? Number)?.toLong()

    fun long(key: String): Long = requireNotNull(longOrNull(key)) { "Missing number field '$key'" }

    fun int(key: String): Int = Math.toIntExact(long(key))

    fun money(key: String): Money = Money(long(key))

    fun boolean(key: String): Boolean = data[key] as? Boolean ?: false

    fun <E : Enum<E>> enumOrNull(key: String, values: List<E>): E? =
        stringOrNull(key)?.let { name -> values.firstOrNull { it.name == name } }

    fun history(key: String): AmountHistory {
        val raw = data[key] as? Map<*, *> ?: return AmountHistory(emptyList())
        val changes = (raw["changes"] as? List<*>).orEmpty().map { change ->
            val map = requireNotNull(change as? Map<*, *>) { "Malformed amount change" }
            AmountChange(
                effectiveFrom = YearMonth.parse(requireNotNull(map["from"] as? String) { "Missing change month" }),
                amount = Money(requireNotNull(map["amount"] as? Number) { "Missing change amount" }.toLong()),
            )
        }
        val overrides = (raw["overrides"] as? Map<*, *>).orEmpty().entries.associate { (month, amount) ->
            YearMonth.parse(requireNotNull(month as? String) { "Malformed override month" }) to
                Money(requireNotNull(amount as? Number) { "Malformed override amount" }.toLong())
        }
        return AmountHistory(changes, overrides)
    }
}
