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
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class Step8RoundThreeTest {

    @org.junit.Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

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
        val israelZone = ZoneId.of("Asia/Jerusalem")
        
        // Rate fetched Thursday 15:00
        val rateDate = LocalDate.of(2026, 10, 22)
        val fetchedAt = ZonedDateTime.of(2026, 10, 22, 15, 0, 0, 0, israelZone).toInstant()

        // Friday 12:00 -> not stale
        val fridayNow = ZonedDateTime.of(2026, 10, 23, 12, 0, 0, 0, israelZone).toInstant()
        assertFalse(BoiStalenessPolicy.isStale(rateDate, fetchedAt, fridayNow))

        // Saturday 20:00 -> not stale
        val saturdayNow = ZonedDateTime.of(2026, 10, 24, 20, 0, 0, 0, israelZone).toInstant()
        assertFalse(BoiStalenessPolicy.isStale(rateDate, fetchedAt, saturdayNow))

        // Sunday 15:59 -> not stale
        val sundayBefore16 = ZonedDateTime.of(2026, 10, 25, 15, 59, 0, 0, israelZone).toInstant()
        assertFalse(BoiStalenessPolicy.isStale(rateDate, fetchedAt, sundayBefore16))

        // Sunday 16:01 -> stale (needs refresh for Sunday's rate)
        val sundayAfter16 = ZonedDateTime.of(2026, 10, 25, 16, 1, 0, 0, israelZone).toInstant()
        assertTrue(BoiStalenessPolicy.isStale(rateDate, fetchedAt, sundayAfter16))

        // Monday 16:01 with a rate fetched on Sunday 16:30 -> stale (needs refresh for Monday's rate)
        val sundayRateDate = LocalDate.of(2026, 10, 25)
        val sundayFetchedAt = ZonedDateTime.of(2026, 10, 25, 16, 30, 0, 0, israelZone).toInstant()
        val mondayAfter16 = ZonedDateTime.of(2026, 10, 26, 16, 1, 0, 0, israelZone).toInstant()
        assertTrue(BoiStalenessPolicy.isStale(sundayRateDate, sundayFetchedAt, mondayAfter16))
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
    @Test
    fun testWorkerDelay() {
        val israelZone = ZoneId.of("Asia/Jerusalem")
        
        // Sunday 09:00 -> to 16:30 same day (7.5h)
        val sunday09 = java.time.Clock.fixed(ZonedDateTime.of(2026, 10, 25, 9, 0, 0, 0, israelZone).toInstant(), israelZone)
        assertEquals(7.5 * 60 * 60 * 1000, BoiSyncWorker.computeInitialDelay(sunday09))

        // Thursday 17:00 -> to Friday 16:30 (approx 23.5h)
        val thursday17 = java.time.Clock.fixed(ZonedDateTime.of(2026, 10, 22, 17, 0, 0, 0, israelZone).toInstant(), israelZone)
        val expectedThursdayDelay = (23 * 60 * 60 * 1000) + (30 * 60 * 1000)
        assertEquals(expectedThursdayDelay.toLong(), BoiSyncWorker.computeInitialDelay(thursday17))

        // Exactly 16:30 -> next day 16:30
        val exactly1630 = java.time.Clock.fixed(ZonedDateTime.of(2026, 10, 25, 16, 30, 0, 0, israelZone).toInstant(), israelZone)
        assertEquals(24 * 60 * 60 * 1000, BoiSyncWorker.computeInitialDelay(exactly1630))
    }
}
