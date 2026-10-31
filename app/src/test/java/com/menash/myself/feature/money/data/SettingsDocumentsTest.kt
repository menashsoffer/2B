package com.menash.myself.feature.money.data

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.CarCompareDraft
import com.menash.myself.feature.money.CarPlan
import com.menash.myself.feature.money.domain.Donation
import com.menash.myself.feature.money.domain.FundKind
import com.menash.myself.feature.money.domain.Included
import com.menash.myself.feature.money.domain.PensionFund
import com.menash.myself.feature.money.domain.TitheSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** Car, pension and maaser documents keep what the user typed through a save and a load. */
class SettingsDocumentsTest {

    @Test
    fun carPlan_roundTripsAHalfFilledComparison() {
        val plan = CarPlan(
            categoryIds = setOf("current-3", "fixed-7"),
            compare = CarCompareDraft(
                horizonYears = 7, kmPerYear = "15000", price = "160000", loanRate = "4.9",
                leaseMonthly = "2,450", leaseIncluded = setOf(Included.INSURANCE, Included.TIRES),
            ),
        )

        assertEquals(plan, CarDocuments.plan(CarPlan.ID, CarDocuments.encode(plan)))
    }

    @Test
    fun carPlan_blankFieldsAreNotStoredAndAnUnknownHorizonFallsBackToFive() {
        val doc = CarDocuments.encode(CarPlan(compare = CarCompareDraft(price = "90000")))
        val fields = doc["fields"] as Map<*, *>
        assertEquals(setOf("price"), fields.keys)

        val loaded = CarDocuments.plan(CarPlan.ID, doc + ("horizonYears" to 4L))
        assertEquals(5, loaded.compare.horizonYears)
    }

    @Test
    fun carPlan_anEmptyDocumentGivesTheDefaults() {
        assertEquals(CarPlan(), CarDocuments.plan(CarPlan.ID, emptyMap()))
    }

    @Test
    fun pensionFund_roundTripsWithAndWithoutOpeningDate() {
        val fund = PensionFund(
            id = "pension-1", kind = FundKind.HISHTALMUT, provider = "מור",
            value = Money(14_500_000), totalDeposits = Money(12_000_000), monthlyDeposit = Money(180_000),
            feeOnBalance = BigDecimal("0.55"), feeOnDeposits = BigDecimal("0.0"),
            openedOn = LocalDate.of(2019, 3, 1), lastUpdated = LocalDate.of(2026, 10, 30),
        )
        listOf(fund, fund.copy(openedOn = null)).forEach { f ->
            assertEquals(f, PensionDocuments.fund(f.id, MoneyDoc(PensionDocuments.encode(f))))
        }
        assertFalse("openedOn" in PensionDocuments.encode(fund.copy(openedOn = null)))
    }

    @Test
    fun donationAndTitheSettings_roundTrip() {
        val donation = Donation(
            id = "d-1", date = LocalDate.of(2026, 10, 12), amount = Money(36_000),
            organisation = "לתת", submittedForTaxRefund = true,
        )
        assertEquals(donation, TitheDocuments.donation(donation.id, MoneyDoc(TitheDocuments.encode(donation))))

        val settings = TitheSettings(
            startMonth = YearMonth.of(2026, 1), opening = Money(-5_000),
            portfolioAssetId = "asset-onezero", depositsBefore = Money(2_000_000),
        )
        listOf(settings, settings.copy(portfolioAssetId = null)).forEach { s ->
            assertEquals(s, TitheDocuments.settings(TitheSettings.ID, MoneyDoc(TitheDocuments.encode(s))))
        }
    }
}
