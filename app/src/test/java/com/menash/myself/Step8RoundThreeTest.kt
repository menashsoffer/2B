package com.menash.myself

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.menash.myself.core.external.boi.BoiExchangeRateClient
import com.menash.myself.core.external.boi.BoiRate
import com.menash.myself.core.external.boi.BoiRateStore
import com.menash.myself.core.external.boi.BoiStalenessPolicy
import com.menash.myself.core.external.boi.BoiSyncWorker
import com.menash.myself.core.external.boi.boiDataStore
import com.menash.myself.core.external.hebcal.HebcalCalendarItem
import com.menash.myself.core.external.hebcal.HebcalData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class Step8RoundThreeTest {

    @Test
    fun testOverridePrecedence() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = BoiRateStore(context.boiDataStore)

        store.setManualOverride(null)

        val autoRate = BoiRate(
            officialRate = 3.11,
            officialRateDate = LocalDate.of(2026, 10, 23),
            officialFetchedAt = Instant.now(),
            manualRate = null,
            isManualOverride = false
        )
        store.saveAutoRate(autoRate)

        var current = store.rateFlow.first()
        assertNotNull(current)
        assertEquals(3.11, current!!.rate, 0.0001)
        assertEquals(3.11, current.officialRate!!, 0.0001)
        assertFalse(current.isManualOverride)

        store.setManualOverride(3.50)
        current = store.rateFlow.first()
        assertEquals(3.50, current!!.rate, 0.0001)
        assertEquals(3.11, current.officialRate!!, 0.0001)
        assertTrue(current.isManualOverride)

        store.saveAutoRate(autoRate.copy(officialRate = 3.15))
        current = store.rateFlow.first()
        assertEquals(3.50, current!!.rate, 0.0001)
        assertEquals(3.15, current.officialRate!!, 0.0001)
        assertTrue(current.isManualOverride)

        store.clearManualOverride()
        current = store.rateFlow.first()
        assertEquals(3.15, current!!.rate, 0.0001)
        assertFalse(current.isManualOverride)
    }

    @Test
    fun testStalenessScenarios() {
        val rateDate = LocalDate.of(2026, 10, 22)
        val fetchedAt = Instant.parse("2026-10-22T15:00:00Z")

        // Friday 12:00
        val fridayNow = Instant.parse("2026-10-23T10:00:00Z")
        // Sunday 15:59
        val sundayBefore16 = Instant.parse("2026-10-25T12:59:00Z")
        // Sunday 16:01
        val sundayAfter16 = Instant.parse("2026-10-25T13:01:00Z")

        assertFalse(BoiStalenessPolicy.isStale(rateDate, fetchedAt, fridayNow))
        assertFalse(BoiStalenessPolicy.isStale(rateDate, fetchedAt, sundayBefore16))
        assertTrue(BoiStalenessPolicy.isStale(rateDate, fetchedAt, sundayAfter16))
    }

    @Test
    fun testHebcalCurrentHebrewDateMatching() {
        val today = LocalDate.now(ZoneId.of("Asia/Jerusalem"))
        val todayStr = today.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)

        val items = listOf(
            HebcalCalendarItem(title = "Day 1", date = "${todayStr}T00:00:00+02:00", category = "day", hebrew = "י".repeat(3) + " תשרי")
        )

        val hebcalData = HebcalData(cityName = "Tel Aviv", geonameid = 293397, items = items, fetchedAt = Instant.now())
        assertEquals("י".repeat(3) + " תשרי", hebcalData.currentHebrewDate)

        val emptyHebcalData = HebcalData(cityName = "Tel Aviv", geonameid = 293397, items = emptyList(), fetchedAt = Instant.now())
        assertNull(emptyHebcalData.currentHebrewDate)
    }
}
