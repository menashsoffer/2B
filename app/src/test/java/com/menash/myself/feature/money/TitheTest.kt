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
import com.menash.myself.feature.money.domain.ActualIncome
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.ImportedIncome
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
    fun firstDataMovesStartBackWhenOlderImportsArrive() {
        fun line(month: YearMonth) = StoredTransaction(
            com.menash.myself.feature.money.importer.StatementRow(month.toString(), com.menash.myself.feature.money.importer.Source.MAX,
                "1234", month.atDay(1), month.atDay(1), "עסק", charge = Money(100)),
        )
        val settings = tithe.copy(startMonth = YearMonth.of(2026, 1), fromFirstData = true)
        val january = YearMonth.of(2024, 1)
        val june = YearMonth.of(2023, 6)
        val d = data.copy(tithe = settings)
        assertEquals(january, titheFromImports(d, october, listOf(line(january)), com.menash.myself.feature.money.importer.ImportMemory())!!.months.last().month)
        assertEquals(june, titheFromImports(d, october, listOf(line(january), line(june)), com.menash.myself.feature.money.importer.ImportMemory())!!.months.last().month)
    }

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
    fun aLinkedImportedSalary_replacesThePlanForItsMonth() {
        val imported = ImportedIncome(listOf(ActualIncome(salary.id, october, shekels(17_200))), mapOf(october to 1))
        val view = titheView(data, october, imported)!!

        val (oct, sep) = view.months
        assertEquals(listOf(TitheSource(salary, shekels(17_200), actual = true)), oct.sources)
        assertEquals(shekels(1_720), oct.owed)
        assertEquals(1, oct.unlinkedLines)
        assertEquals(listOf(TitheSource(salary, shekels(18_000), actual = false)), sep.sources)
        assertEquals(shekels(1_800 + 1_720), view.fromIncome)
    }

    @Test
    fun eachMonth_showsWhatWasDonatedInIt() {
        val (oct, sep) = titheView(data, october)!!.months

        assertEquals(shekels(500), oct.donated)
        assertEquals(shekels(1_800), sep.donated)
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
