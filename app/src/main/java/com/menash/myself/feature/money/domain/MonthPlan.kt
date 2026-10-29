package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit

fun amountFor(item: RecurringItem, month: YearMonth): Money? =
    item.amounts.overrides[month] ?: item.amounts.changes
        .filter { it.effectiveFrom <= month }.maxByOrNull { it.effectiveFrom }?.amount

data class InstallmentPayment(val month: YearMonth, val amount: Money)

fun installmentSchedule(purchase: InstallmentPurchase): List<InstallmentPayment> {
    return List(purchase.numberOfPayments) { index ->
        InstallmentPayment(purchase.firstMonth.plusMonths(index.toLong()), installmentAmount(purchase, index.toLong()))
    }
}

private fun installmentAmount(purchase: InstallmentPurchase, index: Long): Money {
    val base = purchase.total.agorot / purchase.numberOfPayments
    val remainder = if (index == 0L) purchase.total.agorot % purchase.numberOfPayments else 0L
    return Money(base + remainder)
}

fun chargeDate(card: Card, month: YearMonth): LocalDate =
    month.atDay(minOf(card.billingDay, month.lengthOfMonth()))

data class CardCharge(val card: Card, val date: LocalDate, val amount: Money)
sealed class MonthPlan {
    object Empty : MonthPlan()
    data class Planned(
        val month: YearMonth, val income: Money, val fixed: Money,
        val subscriptions: Money, val variable: Money, val installments: Money,
        val expectedSurplus: Money, val cardCharges: List<CardCharge>,
    ) : MonthPlan() {
        /** fixed + subscriptions + variable + installments, as monthly equivalents. */
        val plannedExpenses: Money get() = sumMoney(listOf(fixed, subscriptions, variable, installments))
    }
}

/** Annual conversion and currency conversion round once, after both operations. */
private fun convert(amount: Money, rate: BigDecimal, divisor: Int): Money = Money(
    BigDecimal.valueOf(amount.agorot).multiply(rate)
        .divide(BigDecimal(divisor), 0, RoundingMode.HALF_UP)
        .longValueExact()
)

private fun monthlyAmount(amount: Money, frequency: Frequency, rate: BigDecimal): Money =
    convert(amount, rate, if (frequency == Frequency.YEARLY) 12 else 1)

/** What the card is really billed: a yearly item in full in its billing month, nothing in other months. */
private fun billedAmount(amount: Money, frequency: Frequency, rate: BigDecimal, billingMonth: Month?, month: YearMonth): Money =
    when {
        frequency == Frequency.MONTHLY -> convert(amount, rate, 1)
        month.month == billingMonth -> convert(amount, rate, 1)
        else -> Money.ZERO
    }

internal fun sumMoney(amounts: Iterable<Money>): Money =
    Money(amounts.fold(0L) { sum, amount -> Math.addExact(sum, amount.agorot) })

/** usdToIls may be null only when no USD subscription is active in [month]. */
fun monthPlan(month: YearMonth, items: MoneyItems, usdToIls: BigDecimal?): MonthPlan {
    require(usdToIls == null || usdToIls > BigDecimal.ZERO) { "USD rate must be positive" }
    if (items.incomes.isEmpty() && items.fixedExpenses.isEmpty() && items.subscriptions.isEmpty() &&
        items.variableBudgets.isEmpty() && items.installments.isEmpty()) return MonthPlan.Empty
    val cards = linkedMapOf<String, Pair<Card, Money>>()
    fun charge(card: Card?, amount: Money) {
        if (card != null) cards[card.id] = card to sumMoney(listOf(cards[card.id]?.second ?: Money.ZERO, amount))
    }
    fun rateFor(item: Subscription): BigDecimal = when (item.currency) {
        Currency.ILS -> BigDecimal.ONE
        Currency.USD -> requireNotNull(usdToIls) { "USD subscription '${item.id}' is active in $month but no USD rate was given" }
    }
    val income = sumMoney(items.incomes.mapNotNull { amountFor(it, month) })
    val fixed = sumMoney(items.fixedExpenses.mapNotNull { item -> amountFor(item, month)?.let {
        charge(item.card, billedAmount(it, item.frequency, BigDecimal.ONE, item.billingMonth, month))
        monthlyAmount(it, item.frequency, BigDecimal.ONE)
    } })
    val subscriptions = sumMoney(items.subscriptions.mapNotNull { item -> amountFor(item, month)?.let {
        val rate = rateFor(item)
        charge(item.card, billedAmount(it, item.frequency, rate, item.renewalDate.month, month))
        monthlyAmount(it, item.frequency, rate)
    } })
    val variable = sumMoney(items.variableBudgets.mapNotNull { item -> amountFor(item, month)?.also { charge(item.card, it) } })
    val installments = sumMoney(items.installments.mapNotNull { item ->
        val index = ChronoUnit.MONTHS.between(item.firstMonth, month)
        if (index < 0 || index >= item.numberOfPayments) null else {
            installmentAmount(item, index).also { charge(item.card, it) }
        }
    })
    val expenses = sumMoney(listOf(fixed, subscriptions, variable, installments))
    return MonthPlan.Planned(month, income, fixed, subscriptions, variable, installments,
        Money(Math.subtractExact(income.agorot, expenses.agorot)),
        cards.values.map { (card, amount) -> CardCharge(card, chargeDate(card, month), amount) })
}
