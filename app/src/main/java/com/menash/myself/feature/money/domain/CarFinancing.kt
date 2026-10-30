package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal

enum class Included { INSURANCE, MAINTENANCE, TIRES, LICENSE }

sealed interface Option {
    val tradeIn: Money
    data class Cash(
        val price: Money, val lostAnnualReturn: BigDecimal,
        override val tradeIn: Money = Money.ZERO, val resale: Money = Money.ZERO,
    ) : Option {
        init { nonnegative(price, tradeIn, resale); require(tradeIn <= price && lostAnnualReturn.signum() >= 0) }
    }
    data class BankLoan(
        val price: Money, val downPayment: Money, val annualRate: BigDecimal, val months: Int = 60,
        override val tradeIn: Money = Money.ZERO, val resale: Money = Money.ZERO,
        val earlyRepaymentFee: Money = Money.ZERO,
    ) : Option {
        init {
            nonnegative(price, downPayment, tradeIn, resale, earlyRepaymentFee)
            require(annualRate.signum() >= 0 && months > 0)
            require(downPayment.decimal().add(tradeIn.decimal()) <= price.decimal())
        }
    }
    data class DealerFinancing(
        val listPrice: Money, val cashDiscount: Money, val annualRate: BigDecimal,
        val months: Int = 60, val balloon: Money = Money.ZERO,
        override val tradeIn: Money = Money.ZERO, val resale: Money = Money.ZERO,
        val earlyRepaymentFee: Money = Money.ZERO,
    ) : Option {
        init {
            nonnegative(listPrice, cashDiscount, balloon, tradeIn, resale, earlyRepaymentFee)
            require(annualRate.signum() >= 0 && months > 0 && cashDiscount <= listPrice)
            require(tradeIn.decimal().add(balloon.decimal()) <= listPrice.decimal())
        }
    }
    data class Leasing(
        val monthlyPayment: Money, val upfront: Money, val months: Int = 60,
        val includedYearly: Set<Included> = emptySet(), val kmLimitPerYear: Int,
        val pricePerExtraKm: Money, override val tradeIn: Money = Money.ZERO,
        /** Required for mileage-priced leases when Energy.Known supplies no annual distance. */
        val kmPerYear: Int? = null,
    ) : Option {
        init {
            nonnegative(monthlyPayment, upfront, pricePerExtraKm, tradeIn)
            require(months > 0 && kmLimitPerYear >= 0 && (kmPerYear == null || kmPerYear >= 0))
        }
    }
}

sealed interface OptionComparison { val option: Option }
data class OptionCost(
    override val option: Option, val upfront: Money, val monthlyPayment: Money, val financeCost: Money,
    val running: Money, val resale: Money, val netTotal: Money, val netMonthly: Money,
) : OptionComparison

enum class NotComparableReason { LEASE_SHORTER_THAN_HORIZON, MISSING_ANNUAL_DISTANCE }
data class NotComparable(override val option: Option, val reason: NotComparableReason) : OptionComparison
