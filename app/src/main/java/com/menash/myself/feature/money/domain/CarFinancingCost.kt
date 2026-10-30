package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal

private val one = BigDecimal.ONE
private val zero = BigDecimal.ZERO
private fun BigDecimal.dividedBy(other: BigDecimal) = divide(other, carPrecision)
private fun monthlyRate(annualPercent: BigDecimal) = annualPercent.dividedBy(BigDecimal(1200))
private data class LoanCost(val payment: BigDecimal, val interestAndFee: BigDecimal)

/** Unrounded annuity and settlement after k payments. A balloon is paid at maturity. */
private fun loanCost(
    principal: BigDecimal, annualRate: BigDecimal, months: Int, horizon: Int,
    balloon: BigDecimal = zero, earlyFee: Money = Money.ZERO,
): LoanCost {
    val rate = monthlyRate(annualRate)
    val factor = one.add(rate).pow(months, carPrecision)
    val payment = if (rate.signum() == 0) principal.subtract(balloon).dividedBy(months.toBigDecimal())
        else principal.subtract(balloon.dividedBy(factor)).multiply(rate).dividedBy(one.subtract(one.dividedBy(factor)))
    val paidMonths = minOf(months, horizon)
    val paid = payment.multiply(paidMonths.toBigDecimal())
    val balance = if (paidMonths == months) balloon else if (rate.signum() == 0) principal.subtract(paid)
        else one.add(rate).pow(paidMonths, carPrecision).let { q ->
            principal.multiply(q).subtract(payment.multiply(q.subtract(one)).dividedBy(rate))
        }
    val fee = if (horizon < months && balance.signum() > 0) earlyFee.decimal() else zero
    return LoanCost(payment, paid.add(balance).subtract(principal).add(fee))
}

private fun leaseRunning(car: CarProfile, lease: Option.Leasing, distance: Int): BigDecimal {
    var yearly = runningExact(car)
    for (included in lease.includedYearly) yearly = yearly.subtract(when (included) {
        Included.INSURANCE -> car.insuranceYearly.decimal()
        Included.MAINTENANCE -> car.maintenanceYearly.decimal()
        Included.TIRES -> car.tiresPerYear.decimal()
        Included.LICENSE -> car.licenseAndTestYearly.decimal()
    })
    val extra = maxOf(0, distance - lease.kmLimitPerYear).toBigDecimal().multiply(lease.pricePerExtraKm.decimal())
    return yearly.add(extra)
}

/** Costs ascending, followed by non-comparable options in input order. Rates are annual percentages. */
fun compareOptions(car: CarProfile, options: List<Option>, horizonMonths: Int = 60): List<OptionComparison> {
    require(horizonMonths > 0)
    val months = horizonMonths.toBigDecimal()
    val results = options.map { option ->
        var running = runningExact(car).multiply(months).dividedBy(BigDecimal(12))
        var upfront = zero
        var payment = zero
        var finance = zero
        var resale = zero
        val capital = when (option) {
            is Option.Cash -> {
                upfront = option.price.decimal().subtract(option.tradeIn.decimal())
                finance = upfront.multiply(one.add(monthlyRate(option.lostAnnualReturn)).pow(horizonMonths, carPrecision).subtract(one))
                resale = option.resale.decimal()
                upfront
            }
            is Option.BankLoan -> {
                upfront = option.downPayment.decimal()
                val principal = option.price.decimal().subtract(option.tradeIn.decimal()).subtract(upfront)
                val loan = loanCost(principal, option.annualRate, option.months, horizonMonths, earlyFee = option.earlyRepaymentFee)
                payment = loan.payment
                finance = loan.interestAndFee
                resale = option.resale.decimal()
                upfront.add(principal)
            }
            is Option.DealerFinancing -> {
                val principal = option.listPrice.decimal().subtract(option.tradeIn.decimal())
                val loan = loanCost(principal, option.annualRate, option.months, horizonMonths, option.balloon.decimal(), option.earlyRepaymentFee)
                payment = loan.payment
                finance = loan.interestAndFee.add(option.cashDiscount.decimal())
                resale = option.resale.decimal()
                // Cash-equivalent capital + lost discount + interest equals actual financed cost.
                principal.subtract(option.cashDiscount.decimal())
            }
            is Option.Leasing -> {
                if (option.months < horizonMonths) return@map NotComparable(option, NotComparableReason.LEASE_SHORTER_THAN_HORIZON)
                val distance = option.kmPerYear ?: when (val energy = car.energy) {
                    is Energy.Fuel -> energy.kmPerYear
                    is Energy.Electric -> energy.kmPerYear
                    is Energy.Known -> if (option.pricePerExtraKm == Money.ZERO) 0 else null
                }
                if (distance == null) return@map NotComparable(option, NotComparableReason.MISSING_ANNUAL_DISTANCE)
                upfront = option.upfront.decimal().subtract(option.tradeIn.decimal())
                payment = option.monthlyPayment.decimal()
                running = leaseRunning(car, option, distance).multiply(months).dividedBy(BigDecimal(12))
                upfront.add(payment.multiply(months))
            }
        }
        val total = capital.add(finance).add(running).subtract(resale)
        OptionCost(option, upfront.carMoney(), payment.carMoney(), finance.carMoney(), running.carMoney(),
            resale.carMoney(), total.carMoney(), total.dividedBy(months).carMoney())
    }
    return results.filterIsInstance<OptionCost>().sortedBy { it.netTotal } + results.filterIsInstance<NotComparable>()
}
