package com.menash.myself.feature.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyHistoryTest {

    private fun frame(place: MoneyPlace, month: String? = null) = MoneyFrame(place, month = month)

    @Test
    fun backFollowsOpenOrderNotPriority() {
        val close = frame(MoneyPlace.Close, "2026-09")
        val transactions = frame(MoneyPlace.Transactions, "2026-09")
        val withTransactions = syncMoneyHistory(listOf(close), listOf(close, transactions))
        assertEquals(listOf(close, transactions), withTransactions)
        assertEquals(listOf(close), syncMoneyHistory(withTransactions, listOf(close)))
        assertEquals(emptyList<MoneyFrame>(), syncMoneyHistory(listOf(close), emptyList()))
    }

    @Test
    fun importOnTopOfGoalsReturnsToGoals() {
        val goals = frame(MoneyPlace.Goals)
        val importing = frame(MoneyPlace.Import)
        val stacked = syncMoneyHistory(listOf(goals), listOf(goals, importing))
        assertEquals(MoneyPlace.Import, stacked.last().place)
        assertEquals(listOf(goals), syncMoneyHistory(stacked, listOf(goals)))
    }

    @Test
    fun closingTheLowerScreenDropsIt() {
        val close = frame(MoneyPlace.Close)
        val transactions = frame(MoneyPlace.Transactions, "2026-10")
        val left = syncMoneyHistory(listOf(close, transactions), listOf(transactions))
        assertEquals(listOf(transactions), left)
    }

    @Test
    fun freshArgsReplaceTheStoredFrame() {
        val updated = syncMoneyHistory(
            listOf(frame(MoneyPlace.Transactions, "2026-09")),
            listOf(frame(MoneyPlace.Transactions, "2026-10")),
        )
        assertEquals("2026-10", updated.single().month)
    }

    @Test
    fun encodeRoundTrip() {
        val frames = listOf(
            MoneyFrame(MoneyPlace.Close, month = "2026-09"),
            MoneyFrame(MoneyPlace.Transactions, month = "2026-09", filter = "UNASSIGNED"),
            MoneyFrame(MoneyPlace.Trend, categoryId = "food"),
        )
        assertEquals(frames, decodeMoneyHistory(encodeMoneyHistory(frames)))
        assertEquals(emptyList<MoneyFrame>(), decodeMoneyHistory(""))
    }

    @Test
    fun replaySkipsScreensThatAreAlreadyOpen() {
        val close = frame(MoneyPlace.Close, "2026-09")
        val transactions = frame(MoneyPlace.Transactions, "2026-09")
        assertEquals(listOf(transactions), framesToReplay(listOf(close, transactions), setOf(MoneyPlace.Close)))
        assertEquals(emptyList<MoneyFrame>(), framesToReplay(listOf(close), setOf(MoneyPlace.Close)))
    }

    @Test
    fun dirtyShareAsksBeforeImport() {
        assertEquals(true, shareImportShouldAsk(dirty = true))
        assertEquals(false, shareImportShouldAsk(dirty = false))
    }

    @Test
    fun decodeRejectsGarbage() {
        assertNull(MoneyFrame.decode("nope"))
        assertNull(MoneyFrame.decode("Close|2026-09"))
        assertEquals(listOf(MoneyFrame(MoneyPlace.Goals)), decodeMoneyHistory("bad;Goals|||"))
    }
}
