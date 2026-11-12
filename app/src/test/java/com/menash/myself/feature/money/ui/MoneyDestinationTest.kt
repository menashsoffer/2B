package com.menash.myself.feature.money.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyDestinationTest {
    @Test fun transactionsReturnToTithe() {
        val flags = MoneyOpenFlags(tithe = true, transactions = true)
        assertEquals(MoneyDestination.Transactions, moneyDestination(flags))
        assertEquals(MoneyDestination.Tithe, moneyDestination(flags.copy(transactions = false)))
    }

    @Test fun importReturnsToClose() {
        val flags = MoneyOpenFlags(close = true, importing = true)
        assertEquals(MoneyDestination.Import, moneyDestination(flags))
        assertEquals(MoneyDestination.Close, moneyDestination(flags.copy(importing = false)))
        assertEquals(MoneyDestination.Root, moneyDestination(MoneyOpenFlags()))
    }
}
