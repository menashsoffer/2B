package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MoneySamples.shekels
import com.menash.myself.feature.money.domain.Frequency
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** "מנויים": the stored subscriptions with what really left the card, and subscription charges not listed yet. */
class SubscriptionsTest {

    private val today = LocalDate.of(2026, 10, 30)
    private val gpt = MoneySamples.chatGpt.copy(categoryId = "sub-7")
    private val netflix = MoneySamples.netflix.copy(categoryId = "sub-2")
    private val data = MoneySamples.empty.copy(items = MoneySamples.empty.items.copy(subscriptions = listOf(netflix, gpt)))

    private var n = 0
    private fun line(month: YearMonth, agorot: Long, name: String, category: String?) = StoredTransaction(
        StatementRow("r${n++}", Source.MAX, "4521", month.atDay(12), month.atDay(12), name, charge = Money(agorot)),
        categoryId = category,
    )

    private fun view(lines: List<StoredTransaction> = emptyList(), d: com.menash.myself.feature.money.data.MoneyData = data) =
        subscriptionsView(d, today, MoneySamples.rate, lines, ImportMemory())

    @Test
    fun monthlySharesInShekels_usdAtTheStoredRate() {
        val v = view()
        assertEquals(listOf("ChatGPT", "נטפליקס"), v.rows.map { it.subscription.name })
        assertEquals(Money(7_440), v.rows.first().monthly)
        assertEquals(shekels(55), v.rows.last().monthly)
        assertEquals(Money(7_440 + 5_500), v.monthlyTotal)
    }

    @Test
    fun noRate_usdRowHasNoAmountAndTheTotalWaits() {
        val v = subscriptionsView(data, today, null, emptyList(), ImportMemory())
        assertTrue(v.usdRateMissing)
        assertNull(v.monthlyTotal)
        assertNull(v.rows.first().monthly)
        assertEquals(shekels(55), v.rows.last().monthly)
    }

    @Test
    fun lastChargeFromItsCategory_showsTheCardsConversionOnTop() {
        val october = YearMonth.of(2026, 10)
        val v = view(listOf(
            line(october.minusMonths(1), 7_610, "OPENAI *CHATGPT", "sub-7"),
            line(october, 7_702, "OPENAI *CHATGPT", "sub-7"),
        ))
        val row = v.rows.first()
        assertEquals(LastCharge(october, Money(7_702)), row.lastCharge)
        assertEquals(Money(7_440), row.planned)
    }

    @Test
    fun sharedCategory_noLastCharge() {
        val both = data.copy(items = data.items.copy(subscriptions = listOf(netflix.copy(categoryId = "sub-7"), gpt)))
        val v = view(listOf(line(YearMonth.of(2026, 10), 7_702, "OPENAI", "sub-7")), both)
        v.rows.forEach { assertNull(it.lastCharge) }
    }

    @Test
    fun yearlyRenewal_isTheNextOneFromToday() {
        val yearly = netflix.copy(frequency = Frequency.YEARLY, renewalDate = LocalDate.of(2025, 3, 1))
        val v = view(d = data.copy(items = data.items.copy(subscriptions = listOf(yearly))))
        assertEquals(LocalDate.of(2027, 3, 1), v.rows.single().renewsOn)
        assertNull(view().rows.last().renewsOn)
    }

    @Test
    fun unlistedCharges_inSubscriptionCategoriesOnly_lastTwelveMonths() {
        val october = YearMonth.of(2026, 10)
        val v = view(listOf(
            line(october.minusMonths(2), 2_990, "SPOTIFY", "sub-3"),
            line(october.minusMonths(1), 2_990, "SPOTIFY", "sub-3"),
            line(october, 1_990, "חדר כושר", "sub-6"),
            line(october, 4_000, "שופרסל", "current-1"),
            line(october.minusMonths(13), 2_990, "OLD", "sub-4"),
            line(october, 7_702, "OPENAI", "sub-7"),
        ))
        assertEquals(listOf("חדר כושר", "SPOTIFY"), v.unlisted.map { it.name })
        val spotify = v.unlisted.last()
        assertEquals(2, spotify.months)
        assertEquals(LastCharge(october.minusMonths(1), Money(2_990)), spotify.last)
        assertEquals("sub-3", spotify.categoryId)
    }
}
