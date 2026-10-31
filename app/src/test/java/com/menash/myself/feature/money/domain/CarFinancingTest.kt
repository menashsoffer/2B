package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class CarFinancingTest {
    private val zero = BigDecimal.ZERO
    private val car = CarProfile("car", "car", Energy.Fuel(12000, BigDecimal(12), Money.ZERO))
    private fun cost(option: Option, horizon: Int = 60, profile: CarProfile = car) =
        compareOptions(profile, listOf(option), horizon).single() as OptionCost
    @Test fun zeroPercentDealerLosesToCashDiscount() {
        val cash = Option.Cash(Money(9000000), zero)
        val dealer = Option.DealerFinancing(Money(10000000), Money(1000000), zero)
        val result = compareOptions(car, listOf(dealer, cash)).filterIsInstance<OptionCost>()
        assertEquals(cash, result.first().option)
        assertEquals(Money(1000000), result.last().financeCost)
        assertEquals(Money(10000000), result.last().netTotal)
    }
    @Test fun shpitzerReferenceTableSixPercentSixtyMonths() {
        // Reference: Excel PMT(6%/12,60,-100000) = 1933.28015294279 ILS.
        // Independent 50-digit Decimal amortization table: interest at month 12 = 5519.04511084817;
        // month 60 = 15996.8091765675. Payment is not rounded before accumulating interest.
        val loan = Option.BankLoan(Money(10000000), Money.ZERO, BigDecimal(6))
        assertEquals(Money(193328), cost(loan).monthlyPayment)
        assertEquals(Money(1599681), cost(loan).financeCost)
        assertEquals(Money(551905), cost(loan, 12).financeCost)
        assertEquals(Money(10551905), cost(loan, 12).netTotal)
        assertEquals(cost(loan).financeCost, cost(loan, 72).financeCost)
    }
    @Test fun earlySettlementFeeOnlyBeforeMaturityWhenDebtRemains() {
        val loan = Option.BankLoan(Money(60000), Money.ZERO, zero, earlyRepaymentFee = Money(100))
        assertEquals(Money(60100), cost(loan, 12).netTotal)
        assertEquals(Money(60000), cost(loan).netTotal)
        assertEquals(Money.ZERO, cost(loan.copy(downPayment = loan.price), 12).financeCost)
    }
    @Test fun dealerBalloonSettledAtHorizonOrAtMaturity() {
        val dealer = Option.DealerFinancing(Money(60000), Money(1000), zero, balloon = Money(30000), earlyRepaymentFee = Money(50))
        assertEquals(Money(500), cost(dealer).monthlyPayment)
        assertEquals(Money(60050), cost(dealer, 12).netTotal)
        assertEquals(Money(60000), cost(dealer).netTotal)
        assertEquals(Money(1000), cost(dealer, 72).financeCost)
    }
    @Test fun positiveRateBalloonAmortization() {
        // Two months at 1% monthly, P=10000 agorot, balloon=5000: payment=2587.562189...
        val dealer = Option.DealerFinancing(Money(10000), Money.ZERO, BigDecimal(12), 2, Money(5000))
        assertEquals(Money(2588), cost(dealer, 2).monthlyPayment)
        assertEquals(Money(175), cost(dealer, 2).financeCost)
        assertEquals(Money(100), cost(dealer, 1).financeCost)
    }
    @Test fun cashCompoundMonthlyOpportunityCostAndNegativeNet() {
        val cash = Option.Cash(Money(10000), BigDecimal(12))
        assertEquals(Money(201), cost(cash, 2).financeCost)
        val gain = cost(cash.copy(lostAnnualReturn = zero, resale = Money(10003)), 2)
        assertEquals(Money(-3), gain.netTotal)
        assertEquals(Money(-2), gain.netMonthly)
    }
    @Test fun leaseMileageAndIncludedCostsAreProrated() {
        val profile = car.copy(insuranceYearly = Money(1200), maintenanceYearly = Money(2400),
            tiresPerYear = Money(1200), licenseAndTestYearly = Money(1200), otherYearly = Money(120))
        val lease = Option.Leasing(Money(1000), Money(500), 24, Included.entries.toSet(), 10000, Money(10))
        val result = cost(lease, 6, profile)
        assertEquals(Money(10060), result.running) // 2000 extra km * 10 + 120 yearly, half a year
        // All 24 payments are owed although the horizon is 6 months: 500 + 24 x 1000 + 10,060.
        assertEquals(Money(34560), result.netTotal)
        assertEquals(Money.ZERO, result.resale)
    }
    @Test fun shortLeaseReturnsReasonAndComparableCostsStaySorted() {
        val lease = Option.Leasing(Money(100), Money.ZERO, 12, kmLimitPerYear = 10000, pricePerExtraKm = Money.ZERO)
        val result = compareOptions(car, listOf(lease, Option.Cash(Money(200), zero), Option.Cash(Money(100), zero)), 24)
        assertEquals(listOf(Money(100), Money(200)), result.filterIsInstance<OptionCost>().map { it.netTotal })
        assertEquals(NotComparable(lease, NotComparableReason.LEASE_SHORTER_THAN_HORIZON), result.last())
        assertTrue(compareOptions(car, listOf(lease), 12).single() is OptionCost)
    }
    @Test fun tradeInCreditsEveryOptionAndReducesInterestBearingCapital() {
        val credit = Money(1000)
        val pairs = listOf(
            Option.Cash(Money(60000), zero) to Option.Cash(Money(60000), zero, credit),
            Option.BankLoan(Money(60000), Money.ZERO, zero) to Option.BankLoan(Money(60000), Money.ZERO, zero, tradeIn = credit),
            Option.DealerFinancing(Money(60000), Money.ZERO, zero) to Option.DealerFinancing(Money(60000), Money.ZERO, zero, tradeIn = credit),
            Option.Leasing(Money(1000), Money(2000), kmLimitPerYear = 12000, pricePerExtraKm = Money.ZERO) to
                Option.Leasing(Money(1000), Money(2000), kmLimitPerYear = 12000, pricePerExtraKm = Money.ZERO, tradeIn = credit),
        )
        for ((original, traded) in pairs) assertEquals(1000L, cost(original).netTotal.agorot - cost(traded).netTotal.agorot)
        val loan = Option.BankLoan(Money(60000), Money.ZERO, BigDecimal(6))
        assertTrue(cost(loan.copy(tradeIn = credit)).financeCost < cost(loan).financeCost)
    }
    @Test fun knownCostsNeedDistanceOnlyForMileagePricedLease() {
        val profile = car.copy(energy = Energy.Known(Money(12000)))
        val lease = Option.Leasing(Money(100), Money.ZERO, kmLimitPerYear = 10000, pricePerExtraKm = Money(1))
        assertEquals(NotComparable(lease, NotComparableReason.MISSING_ANNUAL_DISTANCE), compareOptions(profile, listOf(lease)).single())
        assertTrue(compareOptions(profile, listOf(lease.copy(kmPerYear = 12000))).single() is OptionCost)
    }
    @Test fun invalidAmountsTermsRatesAndHorizonFail() {
        assertThrows(IllegalArgumentException::class.java) { compareOptions(car, emptyList(), 0) }
        assertThrows(IllegalArgumentException::class.java) { Option.Cash(Money(1), zero, Money(2)) }
        assertThrows(IllegalArgumentException::class.java) { Option.BankLoan(Money(100), Money(101), zero) }
        assertThrows(IllegalArgumentException::class.java) { Option.BankLoan(Money(100), Money.ZERO, BigDecimal(-1)) }
        assertThrows(IllegalArgumentException::class.java) { Option.DealerFinancing(Money(100), Money.ZERO, zero, balloon = Money(101)) }
        assertThrows(IllegalArgumentException::class.java) { Option.Leasing(Money(1), Money.ZERO, 0, kmLimitPerYear = 1, pricePerExtraKm = Money.ZERO) }
    }
    @Test fun downPaymentAndLeaseUpfrontLoseTheSameReturnAsCash() {
        val rate = BigDecimal(12)
        val cash = cost(Option.Cash(Money(10000), rate), 2)
        val loan = cost(Option.BankLoan(Money(50000), Money(10000), zero, months = 2, lostAnnualReturn = rate), 2)
        val lease = cost(Option.Leasing(Money(0), Money(10000), 2, kmLimitPerYear = 12000, pricePerExtraKm = Money.ZERO, lostAnnualReturn = rate), 2)
        assertEquals(Money(201), cash.financeCost)
        assertEquals(Money(201), loan.financeCost)
        assertEquals(Money(201), lease.financeCost)
        val traded = cost(Option.Leasing(Money(0), Money(1000), 2, kmLimitPerYear = 12000, pricePerExtraKm = Money.ZERO,
            tradeIn = Money(5000), lostAnnualReturn = rate), 2)
        assertEquals(Money.ZERO, traded.financeCost)
    }
    @Test fun leaseLongerThanHorizonOwesEveryPayment() {
        val lease = Option.Leasing(Money(2000), Money.ZERO, 72, kmLimitPerYear = 12000, pricePerExtraKm = Money.ZERO)
        val atHorizon = cost(lease.copy(months = 60))
        val longer = cost(lease)
        assertEquals(12L * 2000, longer.netTotal.agorot - atHorizon.netTotal.agorot)
    }
}
