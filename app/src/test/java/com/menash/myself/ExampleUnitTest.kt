package com.menash.myself

import com.menash.myself.data.entities.AiSubscriptionEntity
import com.menash.myself.data.entities.CarEntity
import com.menash.myself.data.entities.PensionFundEntity
import com.menash.myself.data.io.CsvHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun pensionFundCalculations_areCorrect() {
        val fund = PensionFundEntity(
            fundType = "קרן השתלמות",
            provider = "אלטשולר שחם",
            currentValue = 145000.0,
            totalDeposits = 110000.0,
            feeOnBalancePercent = 0.25,
            feeOnDepositsPercent = 0.0,
            notes = "מסלול מחקה S&P 500"
        )

        assertEquals(35000.0, fund.profit, 0.001)
        val expectedPercent = (35000.0 / 110000.0) * 100.0
        assertEquals(expectedPercent, fund.profitPercent, 0.001)

        val expectedAnnualFee = 145000.0 * 0.0025
        assertEquals(expectedAnnualFee, fund.yearlyBalanceFeeEstimate, 0.001)
    }

    @Test
    fun carCalculations_areCorrect() {
        val car = CarEntity(
            name = "טויוטה קורולה",
            purchasePrice = 148000.0,
            resaleValueAfter5Years = 88000.0,
            energyCostYearly = 7200.0,
            insuranceYearly = 4800.0,
            trackerYearly = 650.0,
            maintenanceYearly = 1800.0,
            annualTestYearly = 1950.0,
            tires5YearsTotal = 2400.0,
            isBaseline = true
        )

        // Yearly running: 7200 + 4800 + 650 + 1800 + 1950 = 16400
        assertEquals(16400.0, car.yearlyRunningCost, 0.001)
        // 5-year running: 16400 * 5 = 82000
        assertEquals(82000.0, car.fiveYearRunningCost, 0.001)
        // Total 5-year: 148000 + 82000 + 2400 = 232400
        assertEquals(232400.0, car.total5YearCost, 0.001)
        // Net 5-year: 232400 - 88000 = 144400
        assertEquals(144400.0, car.net5YearCost, 0.001)
        // Net monthly: 144400 / 60 = 2406.666...
        assertEquals(144400.0 / 60.0, car.netMonthlyCost, 0.001)
    }

    @Test
    fun aiSubscriptionCalculations_areCorrect() {
        val subUsdMonthly = AiSubscriptionEntity(
            name = "ChatGPT",
            category = "LLM",
            price = 20.0,
            currency = "USD",
            billingCycle = "MONTHLY"
        )
        val rate = 3.5
        assertEquals(70.0, subUsdMonthly.monthlyCostIls(rate), 0.001)
        assertEquals(840.0, subUsdMonthly.yearlyCostIls(rate), 0.001)

        val subUsdYearly = AiSubscriptionEntity(
            name = "Cursor",
            category = "Code",
            price = 240.0,
            currency = "USD",
            billingCycle = "YEARLY"
        )
        // 240 * 3.5 = 840 yearly in ILS, monthly = 840 / 12 = 70 ILS
        assertEquals(70.0, subUsdYearly.monthlyCostIls(rate), 0.001)
        assertEquals(840.0, subUsdYearly.yearlyCostIls(rate), 0.001)
    }

    @Test
    fun csvHelper_parsesAndExportsCorrectly() {
        val headers = listOf("Name", "Provider", "Value")
        val rows = listOf(
            listOf("קרן השתלמות", "אלטשולר", "145000"),
            listOf("פנסיה", "מנורה, מבטחים", "320000")
        )
        val csv = CsvHelper.exportToCsv(headers, rows)
        val parsed = CsvHelper.parseCsv(csv)

        assertEquals(3, parsed.size)
        assertEquals("Name", parsed[0][0])
        assertEquals("מנורה, מבטחים", parsed[2][1])
    }
}
