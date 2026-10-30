package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MoneySamples.donations
import com.menash.myself.feature.money.MoneySamples.filled
import com.menash.myself.feature.money.MoneySamples.salary
import com.menash.myself.feature.money.MoneySamples.shekels
import com.menash.myself.feature.money.MoneySamples.tithe
import com.menash.myself.feature.money.domain.AmountChange
import com.menash.myself.feature.money.domain.AmountHistory
import com.menash.myself.feature.money.domain.TitheRate
import com.menash.myself.feature.money.domain.UnsubmittedDonations
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.portfolioDeposits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class TitheTest {
    private val october = YearMonth.of(2026, 10)
    private val data = filled.copy(donations = donations, tithe = tithe)

    @Test
    fun noStartMonth_noBalance() {
        assertNull(titheView(filled, october))
    }

    @Test
    fun balance_isOpeningPlusIncomeAndPortfolioMinusDonations() {
        val view = titheView(data, october)!!

        // Salary 18,000 maaser in September and October.
        assertEquals(shekels(3_600), view.fromIncome)
        // Fund 80,000 against 70,000 deposited: 20% of 10,000.
        assertEquals(PortfolioPart(shekels(80_000), shekels(70_000), shekels(2_000)), view.portfolio)
        // The June donation is before the start month.
        assertEquals(shekels(2_300), view.donated)
        assertEquals(listOf("don-2", "don-1"), view.donations.map { it.id })
        assertEquals(shekels(250 + 3_600 + 2_000 - 2_300), view.balance)
        assertEquals(1, view.countedIncomes)
    }

    @Test
    fun importedDeposits_addToTheDepositsBefore() {
        val view = titheView(data, october, importedDeposits = shekels(5_000))!!

        assertEquals(shekels(75_000), view.portfolio!!.deposits)
        assertEquals(shekels(1_000), view.portfolio!!.owed)
    }

    @Test
    fun aLosingPortfolio_reducesWhatIsOwed_andCanLeaveACredit() {
        val view = titheView(data.copy(tithe = tithe.copy(depositsBefore = shekels(90_000))), october)!!

        assertEquals(shekels(-2_000), view.portfolio!!.owed)
        assertEquals(shekels(-450), view.balance)
    }

    @Test
    fun noPortfolioAsset_noPortfolioPart() {
        val view = titheView(data.copy(tithe = tithe.copy(portfolioAssetId = null)), october)!!

        assertNull(view.portfolio)
        assertEquals(shekels(250 + 3_600 - 2_300), view.balance)
    }

    @Test
    fun chomeshIncome_countsTwenty_andARaiseCountsFromItsMonth() {
        val raised = salary.copy(
            titheRate = TitheRate.CHOMESH,
            amounts = AmountHistory(listOf(AmountChange(YearMonth.of(2026, 1), shekels(18_000)), AmountChange(october, shekels(20_000)))),
        )
        val view = titheView(data.copy(items = data.items.copy(incomes = listOf(raised))), october)!!

        assertEquals(shekels(3_600 + 4_000), view.fromIncome)
    }

    @Test
    fun incomeWithoutTithe_isNotCounted() {
        val none = data.copy(items = data.items.copy(incomes = listOf(salary.copy(titheRate = TitheRate.NONE))))
        val view = titheView(none, october)!!

        assertEquals(Money.ZERO, view.fromIncome)
        assertEquals(0, view.countedIncomes)
    }

    @Test
    fun unsubmittedForTaxRefund_coversTheWholeYear() {
        assertEquals(UnsubmittedDonations(2, shekels(800)), titheView(data, october)!!.unsubmitted)
    }

    @Test
    fun portfolioDeposits_areTheOneZeroIncomingTransfers() {
        val rows = ImportFixtures.parse("onezero_ils.xls").rows.map { StoredTransaction(it) }
        val deposit = rows.first()
        val expected = Money(-deposit.row.charge!!.agorot)

        assertEquals(expected, portfolioDeposits(rows, deposit.row.billingMonth))
        assertEquals(Money.ZERO, portfolioDeposits(rows, deposit.row.billingMonth.plusMonths(1)))
    }
}
