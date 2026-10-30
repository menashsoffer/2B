package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MoneySamples.donations
import com.menash.myself.feature.money.MoneySamples.shekels
import com.menash.myself.feature.money.MoneySamples.tithe
import com.menash.myself.feature.money.domain.TitheSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TitheDraftTest {
    private val october = YearMonth.of(2026, 10)

    @Test
    fun aStoredDonation_comesBackUnchanged() {
        donations.forEach { assertEquals(it, buildDonation(donationDraftOf(it)) { error("keeps its id") }) }
    }

    @Test
    fun aDonation_needsAnAmountAboveZero_theOrganisationIsOptional() {
        val draft = DonationDraft(amount = "", date = LocalDate.of(2026, 10, 30))
        assertNull(buildDonation(draft) { "d" })
        assertNull(buildDonation(draft.copy(amount = "0")) { "d" })
        val donation = buildDonation(draft.copy(amount = "180", organisation = "  ")) { "don-new" }!!
        assertEquals("don-new", donation.id)
        assertEquals(shekels(180), donation.amount)
        assertEquals("", donation.organisation)
    }

    @Test
    fun aFirstCount_startsThisMonthWithNothingOpen() {
        assertEquals(TitheSettings(october), buildSettings(settingsDraftOf(null, october)))
    }

    @Test
    fun storedSettings_comeBackUnchanged_includingAnAheadOpening() {
        assertEquals(tithe, buildSettings(settingsDraftOf(tithe, october)))
        val ahead = tithe.copy(opening = shekels(-400))
        val draft = settingsDraftOf(ahead, october)
        assertEquals("400", draft.opening)
        assertEquals(true, draft.ahead)
        assertEquals(ahead, buildSettings(draft))
    }

    @Test
    fun badNumbers_countAsEmpty() {
        val settings = buildSettings(TitheSettingsDraft(october, opening = "abc", depositsBefore = "-5"))
        assertEquals(Money.ZERO, settings.opening)
        assertEquals(Money.ZERO, settings.depositsBefore)
    }
}
