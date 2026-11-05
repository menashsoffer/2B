package com.menash.myself.core.external

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
class ExternalDataRefreshTest {

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
        val friday = LocalDate.of(2026, 10, 23)
        val fetchedFriday1240 = ZonedDateTime.of(2026, 10, 23, 12, 40, 0, 0, israelZone).toInstant()

        val saturday = ZonedDateTime.of(2026, 10, 24, 20, 0, 0, 0, israelZone).toInstant()
        assertFalse(BoiStalenessPolicy.isStale(friday, fetchedFriday1240, saturday))

        val sunday = ZonedDateTime.of(2026, 10, 25, 9, 0, 0, 0, israelZone).toInstant()
        assertFalse(BoiStalenessPolicy.isStale(friday, fetchedFriday1240, sunday))

        val thursday = LocalDate.of(2026, 10, 22)
        val fetchedThursday = ZonedDateTime.of(2026, 10, 22, 15, 40, 0, 0, israelZone).toInstant()
        val friday1300 = ZonedDateTime.of(2026, 10, 23, 13, 0, 0, 0, israelZone).toInstant()
        assertTrue(BoiStalenessPolicy.isStale(thursday, fetchedThursday, friday1300))
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
        assertDelay(
            israelZone,
            from = ZonedDateTime.of(2026, 10, 22, 17, 0, 0, 0, israelZone),
            to = ZonedDateTime.of(2026, 10, 23, 12, 45, 0, 0, israelZone)
        )
        assertDelay(
            israelZone,
            from = ZonedDateTime.of(2026, 10, 23, 13, 0, 0, 0, israelZone),
            to = ZonedDateTime.of(2026, 10, 26, 15, 45, 0, 0, israelZone)
        )
        assertDelay(
            israelZone,
            from = ZonedDateTime.of(2026, 10, 25, 9, 0, 0, 0, israelZone),
            to = ZonedDateTime.of(2026, 10, 26, 15, 45, 0, 0, israelZone)
        )
        // A run that starts at Friday 12:45 is not before that slot, so the next one is Monday 15:45.
        assertDelay(
            israelZone,
            from = ZonedDateTime.of(2026, 10, 23, 12, 45, 0, 0, israelZone),
            to = ZonedDateTime.of(2026, 10, 26, 15, 45, 0, 0, israelZone)
        )
    }

    private fun assertDelay(zone: ZoneId, from: ZonedDateTime, to: ZonedDateTime) {
        val clock = java.time.Clock.fixed(from.toInstant(), zone)
        assertEquals(java.time.Duration.between(from, to).toMillis(), BoiSyncWorker.computeInitialDelay(clock))
    }
}
