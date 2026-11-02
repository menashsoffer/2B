package com.menash.myself.feature.money.data

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.MonthSnapshot
import com.menash.myself.feature.money.domain.PeriodicStatus
import java.time.LocalDate
import java.time.YearMonth

/** Pure mapping for closedMonths/{yyyy-MM}; monetary values are Long agorot, never asset metadata. */
object ClosedMonthDocuments {
    private const val NO_CATEGORY = "_none"
    private val monthId = Regex("[0-9]{4}-(0[1-9]|1[0-2])")

    private fun month(id: String): YearMonth {
        require(monthId.matches(id)) { "Expected a canonical yyyy-MM document id" }
        return YearMonth.parse(id)
    }

    fun encode(closed: ClosedMonth): Map<String, Any> {
        month(closed.month.toString())
        require(NO_CATEGORY !in closed.byCategory) { "Category id '$NO_CATEGORY' is reserved" }
        return buildMap {
            putAll(encodeFields(closed))
            if (closed.balancesEntered) put("balancesEntered", true)
        }
    }

    private fun encodeFields(closed: ClosedMonth): Map<String, Any> {
        return mapOf(
            "actualIncomes" to closed.snapshot.actualIncomes.agorot,
            "actualExpenses" to closed.snapshot.actualExpenses.agorot,
            "byCategory" to closed.byCategory.entries.associate { (id, amount) -> (id ?: NO_CATEGORY) to amount.agorot },
            "periodic" to closed.periodic.mapValues { (_, status) ->
                mapOf(
                    "accruedToDate" to status.accruedToDate.agorot,
                    "spentToDate" to status.spentToDate.agorot,
                    "remainingThisYear" to status.remainingThisYear.agorot,
                )
            },
            "balances" to closed.balances.mapValues { it.value.agorot },
            "liabilities" to closed.liabilities.mapValues { it.value.agorot },
            "closedOn" to closed.closedOn.toString(),
        )
    }

    fun decode(id: String, data: Map<String, Any?>): ClosedMonth {
        val month = month(id)
        val closeDate = requireNotNull(data["closedOn"] as? String) { "Missing ISO close date" }
        val closedOn = LocalDate.parse(closeDate)
        require(closedOn.toString() == closeDate) { "Expected a canonical ISO close date" }
        return ClosedMonth(
            month = month,
            snapshot = MonthSnapshot(month, money(data["actualIncomes"]), money(data["actualExpenses"]), balances = emptyList()),
            byCategory = optionalMap(data, "byCategory").entries.associate { (key, value) ->
                (if (key == NO_CATEGORY) null else key) to money(value)
            },
            periodic = optionalMap(data, "periodic").mapValues { (_, value) ->
                val fields = stringMap(value)
                PeriodicStatus(money(fields["accruedToDate"]), money(fields["spentToDate"]), money(fields["remainingThisYear"]))
            },
            balances = optionalMap(data, "balances").mapValues { money(it.value) },
            liabilities = optionalMap(data, "liabilities").mapValues { money(it.value) },
            closedOn = closedOn,
            balancesEntered = data["balancesEntered"] as? Boolean ?: false,
        )
    }

    private fun money(value: Any?): Money = Money(requireNotNull(value as? Long) { "Expected Long agorot" })

    private fun optionalMap(data: Map<String, Any?>, key: String): Map<String, Any?> =
        if (key in data) stringMap(data[key]) else emptyMap()

    private fun stringMap(value: Any?): Map<String, Any?> {
        require(value is Map<*, *>) { "Expected a map" }
        return value.entries.associate { (key, entry) ->
            require(key is String) { "Expected a string map key" }
            key to entry
        }
    }
}
