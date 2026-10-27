package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
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
    ) : MonthPlan()
}

/** Annual conversion and currency conversion round once, after both operations. */
private fun monthlyAmount(amount: Money, frequency: Frequency, rate: BigDecimal): Money = Money(
    BigDecimal.valueOf(amount.agorot).multiply(rate)
        .divide(if (frequency == Frequency.YEARLY) BigDecimal(12) else BigDecimal.ONE, 0, RoundingMode.HALF_UP)
        .longValueExact()
)

internal fun sumMoney(amounts: Iterable<Money>): Money =
    Money(amounts.fold(0L) { sum, amount -> Math.addExact(sum, amount.agorot) })

fun monthPlan(month: YearMonth, items: MoneyItems, usdToIls: BigDecimal): MonthPlan {
    require(usdToIls > BigDecimal.ZERO) { "USD rate must be positive" }
    if (items.incomes.isEmpty() && items.fixedExpenses.isEmpty() && items.subscriptions.isEmpty() &&
        items.variableBudgets.isEmpty() && items.installments.isEmpty()) return MonthPlan.Empty
    val cards = linkedMapOf<Card, Money>()
    fun charge(card: Card?, amount: Money): Money {
        if (card != null) cards[card] = sumMoney(listOf(cards[card] ?: Money.ZERO, amount))
        return amount
    }
    val income = sumMoney(items.incomes.mapNotNull { amountFor(it, month) })
    val fixed = sumMoney(items.fixedExpenses.mapNotNull { item -> amountFor(item, month)?.let {
        charge(item.card, monthlyAmount(it, item.frequency, BigDecimal.ONE))
    } })
    val subscriptions = sumMoney(items.subscriptions.mapNotNull { item -> amountFor(item, month)?.let {
        charge(item.card, monthlyAmount(it, item.frequency, if (item.currency == Currency.USD) usdToIls else BigDecimal.ONE))
    } })
    val variable = sumMoney(items.variableBudgets.mapNotNull { item -> amountFor(item, month)?.let { charge(item.card, it) } })
    val installments = sumMoney(items.installments.mapNotNull { item ->
        val index = ChronoUnit.MONTHS.between(item.firstMonth, month)
        if (index < 0 || index >= item.numberOfPayments) null else {
            charge(item.card, installmentAmount(item, index))
        }
    })
    val expenses = sumMoney(listOf(fixed, subscriptions, variable, installments))
    return MonthPlan.Planned(month, income, fixed, subscriptions, variable, installments,
        Money(Math.subtractExact(income.agorot, expenses.agorot)),
        cards.map { (card, amount) -> CardCharge(card, chargeDate(card, month), amount) })
}
