package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/** Actual net income received from one Income source in one month. */
data class ActualIncome(val incomeId: String, val month: YearMonth, val net: Money) {
    init { require(net >= Money.ZERO) }
}

data class Donation(
    val id: String, val date: LocalDate, val amount: Money, val organisation: String,
    val submittedForTaxRefund: Boolean = false,
) {
    init { requireId(id); require(amount >= Money.ZERO) }
}

sealed class CharityBalance {
    data class Owed(val amount: Money) : CharityBalance()
    /** Donated more than owed; amount is positive. */
    data class Credit(val amount: Money) : CharityBalance()
    object Settled : CharityBalance()
}

private fun percentOf(amount: Long, percent: Int): Long =
    BigDecimal.valueOf(amount).multiply(BigDecimal(percent)).divide(BigDecimal(100), 0, RoundingMode.HALF_UP).longValueExact()

/**
 * 10% of MAASER income + 20% of CHOMESH income + 20% of (current portfolio value − total deposits) − donations.
 * The portfolio part uses current values, so a losing portfolio makes it negative.
 */
fun charityBalance(
    incomes: List<Income>, actualIncomes: List<ActualIncome>,
    portfolioValue: Money, portfolioDeposits: Money, donations: List<Donation>,
): CharityBalance {
    require(portfolioValue >= Money.ZERO && portfolioDeposits >= Money.ZERO)
    val rates = incomes.associate { it.id to it.titheRate }
    val byRate = actualIncomes.groupBy { requireNotNull(rates[it.incomeId]) { "Unknown income id ${it.incomeId}" } }
    val fromIncome = TitheRate.entries.sumOf { rate ->
        percentOf(sumMoney(byRate[rate].orEmpty().map { it.net }).agorot, rate.percent)
    }
    val fromPortfolio = percentOf(Math.subtractExact(portfolioValue.agorot, portfolioDeposits.agorot), TitheRate.CHOMESH.percent)
    val balance = Math.subtractExact(Math.addExact(fromIncome, fromPortfolio), sumMoney(donations.map { it.amount }).agorot)
    return when {
        balance > 0 -> CharityBalance.Owed(Money(balance))
        balance < 0 -> CharityBalance.Credit(Money(-balance))
        else -> CharityBalance.Settled
    }
}

data class UnsubmittedDonations(val count: Int, val total: Money)

/** Donations dated in [year] not yet submitted for a tax refund. */
fun unsubmittedThisYear(donations: List<Donation>, year: Int): UnsubmittedDonations {
    val open = donations.filter { it.date.year == year && !it.submittedForTaxRefund }
    return UnsubmittedDonations(open.size, sumMoney(open.map { it.amount }))
}
