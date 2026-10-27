package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.LocalDate
import java.time.YearMonth

/** Amounts use minor units; subscription amounts are in their declared currency. */
data class AmountChange(val effectiveFrom: YearMonth, val amount: Money)

data class AmountHistory(
    val changes: List<AmountChange>,
    val overrides: Map<YearMonth, Money> = emptyMap(),
) {
    init {
        require(changes.map { it.effectiveFrom }.distinct().size == changes.size) {
            "Only one default per effective month is allowed"
        }
        require(changes.all { it.amount >= Money.ZERO } && overrides.values.all { it >= Money.ZERO })
    }
}

interface RecurringItem { val amounts: AmountHistory }
enum class Frequency { MONTHLY, YEARLY }
enum class Currency { ILS, USD }
enum class AssetKind { ASSET, LIABILITY }

data class Card(val name: String, val billingDay: Int) {
    init { require(billingDay in 1..31) }
}
data class Income(val name: String, override val amounts: AmountHistory) : RecurringItem
data class FixedExpense(
    val name: String, override val amounts: AmountHistory,
    val frequency: Frequency = Frequency.MONTHLY, val card: Card? = null,
) : RecurringItem
data class Subscription(
    val name: String, override val amounts: AmountHistory,
    val currency: Currency = Currency.ILS, val frequency: Frequency = Frequency.MONTHLY,
    val renewalDate: LocalDate, val card: Card? = null,
) : RecurringItem
data class VariableBudget(
    val name: String, override val amounts: AmountHistory, val card: Card? = null,
) : RecurringItem

/** Pass currentMonth explicitly so the default first month is deterministic. */
data class InstallmentPurchase(
    val name: String, val total: Money, val currentMonth: YearMonth,
    val numberOfPayments: Int = 1, val firstMonth: YearMonth = currentMonth.plusMonths(1),
    val card: Card? = null,
) {
    init { require(total >= Money.ZERO); require(numberOfPayments > 0) }
}
data class Asset(
    val name: String, val type: String, val balance: Money,
    val kind: AssetKind, val liquid: Boolean, val lastUpdated: LocalDate,
) {
    init { require(balance >= Money.ZERO) }
}
data class MonthSnapshot(
    val month: YearMonth, val actualIncomes: Money, val actualExpenses: Money,
    val balances: List<Asset> = emptyList(),
) {
    init { require(actualIncomes >= Money.ZERO); require(actualExpenses >= Money.ZERO) }
}
data class MoneyItems(
    val incomes: List<Income> = emptyList(),
    val fixedExpenses: List<FixedExpense> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val variableBudgets: List<VariableBudget> = emptyList(),
    val installments: List<InstallmentPurchase> = emptyList(),
)
