package com.menash.myself

import com.menash.myself.core.data.repository.impl.BoiExchangeRateRepository
import com.menash.myself.core.external.boi.BoiRate
import com.menash.myself.core.external.boi.BoiRateSource
import com.menash.myself.core.external.boi.BoiRateStore
import com.menash.myself.core.external.boi.boiDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.test.core.app.ApplicationProvider
import android.content.Context

@OptIn(ExperimentalCoroutinesApi::class)
class BoiExchangeRateRepositoryTest {

    private class FakeBoiRateSource : BoiRateSource {
        var fetchCount = 0
        var nextResult: Result<BoiRate> = Result.success(BoiRate(3.0, LocalDate.now(), Instant.now(), null, false))

        override suspend fun fetchLatestRate(): Result<BoiRate> {
            fetchCount++
            return nextResult
        }

        override suspend fun fetchRateWindow(start: LocalDate, end: LocalDate): Result<List<Pair<LocalDate, Double>>> {
            return Result.success(emptyList())
        }
    }

    @Test
    fun testRefreshRateIfNeeded() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = BoiRateStore(context.boiDataStore)
        val source = FakeBoiRateSource()
        val clock = Clock.fixed(Instant.parse("2026-10-25T17:00:00Z"), ZoneId.of("Asia/Jerusalem"))
        val repo = BoiExchangeRateRepository(store, source, clock, TestScope())

        // a. manual override only, no official rate -> fetches once
        store.setManualOverride(3.5)
        repo.refreshRateIfNeeded()
        assertEquals(1, source.fetchCount)

        // b. official rate stale while an override is active -> fetches
        val oldRate = BoiRate(3.1, LocalDate.of(2026, 10, 20), Instant.parse("2026-10-20T10:00:00Z"), 3.5, true)
        store.saveAutoRate(oldRate)
        repo.refreshRateIfNeeded()
        assertEquals(2, source.fetchCount)

        // c. official rate fresh -> does not fetch
        val freshRate = BoiRate(3.1, LocalDate.of(2026, 10, 25), Instant.parse("2026-10-25T10:00:00Z"), 3.5, true)
        store.saveAutoRate(freshRate)
        repo.refreshRateIfNeeded()
        assertEquals(2, source.fetchCount)
    }
}
