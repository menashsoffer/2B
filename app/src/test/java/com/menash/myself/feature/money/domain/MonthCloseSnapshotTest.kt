package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class MonthCloseSnapshotTest {
    private val month = YearMonth.of(2026, 10)
    private val today = LocalDate.of(2026, 11, 2)
    private fun tx(id: String, amount: Long, billing: YearMonth = month, category: String? = "food") =
        Transaction(id, month.atDay(1), billing, "merchant", Money(amount), PaymentMethod.CREDIT_CARD, "card", category)
    private fun asset(id: String, kind: AssetKind) = Asset(id, "private name", "bank", Money(500), kind, true, today)
    private fun close(txs: List<Transaction> = emptyList(), items: MoneyItems = MoneyItems(), assets: List<Asset> = emptyList()) =
        closeMonth(month, items, txs, Money(1000), BalancesAtClose.Current(assets), null, today)

    @Test fun suppliedHistoricalBalancesAndUnknownNetWorth() {
        val entered = closeMonth(month, MoneyItems(), emptyList(), Money.ZERO,
            BalancesAtClose.Entered(mapOf("old" to Money(10)), mapOf("loan" to Money(3))), null, today)
        assertEquals(mapOf("old" to Money(10)), entered.balances)
        assertEquals(mapOf("loan" to Money(3)), entered.liabilities)
        val unknown = closeMonth(month.plusMonths(1), MoneyItems(), emptyList(), Money.ZERO,
            BalancesAtClose.Unknown, null, today.plusMonths(1))
        assertTrue(unknown.balances.isEmpty())
        assertTrue(unknown.liabilities.isEmpty())
        assertEquals(netWorthChange(listOf(entered), 2026), netWorthChange(listOf(entered, unknown), 2026))
        assertNull(netWorthChange(listOf(unknown), 2026))
    }

    @Test fun suspectBoundaryAndEmptyBalances() {
        val row = close(assets = listOf(asset("bank", AssetKind.ASSET)))
        val boundary = month.plusMonths(1).atEndOfMonth()
        assertFalse(row.copy(closedOn = boundary).hasSuspectBalances())
        assertTrue(row.copy(closedOn = boundary.plusDays(1)).hasSuspectBalances())
        assertFalse(row.copy(closedOn = boundary.plusDays(1), balances = emptyMap()).hasSuspectBalances())
    }

    @Test fun refundsReduceExpensesAndCategories() {
        val closed = close(listOf(tx("purchase", 300), tx("refund", -100)))
        assertEquals(Money(200), closed.snapshot.actualExpenses)
        assertEquals(mapOf("food" to Money(200)), closed.byCategory)
        assertEquals(Money(1000), closed.snapshot.actualIncomes)
        assertEquals(month, closed.month)
        assertEquals(month, closed.snapshot.month)
        assertEquals(today, closed.closedOn)
    }

    @Test fun excessRefundClampsOnlyFinalExpenseTotal() {
        val closed = close(listOf(tx("refund", -500), tx("purchase", 200)))
        assertEquals(Money.ZERO, closed.snapshot.actualExpenses)
        assertEquals(Money(-300), closed.byCategory["food"])
        assertEquals(closed, close(listOf(tx("purchase", 200), tx("refund", -500))))
    }

    @Test fun billingMonthWinsOverPurchaseDate() {
        val closed = close(listOf(
            tx("previous", 700, month.minusMonths(1)), tx("next", 900, month.plusMonths(1)),
            tx("current", 200).copy(date = month.minusMonths(2).atDay(10)),
        ))
        assertEquals(Money(200), closed.snapshot.actualExpenses)
        assertEquals(Money(200), closed.byCategory["food"])
    }

    @Test fun installmentsCountOnceAndCategoryActualsComeStraightFromEngine() {
        val purchase = InstallmentPurchase("p", "purchase", Money(600), month.minusMonths(1), 3, month, Card("card", "card", 5))
        val items = MoneyItems(installments = listOf(purchase))
        val txs = listOf(tx("payment", 200).copy(installment = InstallmentInfo(1, 3)), tx("food", 50), tx("none", 20, category = null))
        val closed = close(txs, items)
        assertEquals(Money(270), closed.snapshot.actualExpenses)
        assertEquals(mapOf("food" to Money(50), null to Money(20)), closed.byCategory)
        assertEquals(planVsActual(month, items, txs, null).categories.associate { it.categoryId to it.actual }, closed.byCategory)
    }

    @Test fun emptyTransactionsHaveZeroExpensesAndNoInventedIncome() {
        val closed = close()
        assertEquals(Money.ZERO, closed.snapshot.actualExpenses)
        assertTrue(closed.byCategory.isEmpty())
        assertTrue(closed.periodic.isEmpty())
    }

    @Test fun separatesBalancesWithoutRetainingAssetsOrLists() {
        val assets = mutableListOf(asset("bank", AssetKind.ASSET), asset("loan", AssetKind.LIABILITY))
        val txs = mutableListOf(tx("expense", 100))
        val closed = close(txs, assets = assets)
        assets.clear()
        txs.clear()
        assertEquals(mapOf("bank" to Money(500)), closed.balances)
        assertEquals(mapOf("loan" to Money(500)), closed.liabilities)
        assertTrue(closed.snapshot.balances.isEmpty())
        assertEquals(Money(100), closed.snapshot.actualExpenses)
        assertEquals(Money(100), closed.byCategory["food"])
    }

    @Test fun zeroAssetBalanceIsValid() {
        assertEquals(Money.ZERO, close(assets = listOf(asset("bank", AssetKind.ASSET).copy(balance = Money.ZERO))).balances["bank"])
    }

    @Test fun duplicateAssetIdsRejectedWithinAndAcrossKinds() {
        for (kind in AssetKind.entries) {
            assertThrows(IllegalArgumentException::class.java) {
                close(assets = listOf(asset("same", AssetKind.ASSET), asset("same", kind)))
            }
        }
    }

    @Test fun checkedExpenseArithmeticRejectsOverflowAndUnderflow() {
        for (values in listOf(listOf(Long.MAX_VALUE, 1), listOf(Long.MIN_VALUE, -1))) {
            assertThrows(ArithmeticException::class.java) { close(values.mapIndexed { i, amount -> tx("$i", amount) }) }
        }
    }

    @Test fun callerEnforcesChecklistAndSuppliesExchangeRate() {
        val early = month.atDay(1)
        val subscription = Subscription("usd", "usd", AmountHistory(listOf(AmountChange(month, Money(100)))), Currency.USD, renewalDate = early)
        val items = MoneyItems(subscriptions = listOf(subscription))
        assertThrows(IllegalArgumentException::class.java) { close(items = items) }
        val closed = closeMonth(month, items, emptyList(), Money.ZERO, BalancesAtClose.Unknown, BigDecimal("3.5"), early)
        assertEquals(early, closed.closedOn)
        assertEquals(mapOf<String?, Money>(null to Money.ZERO), closed.byCategory)
    }
}
