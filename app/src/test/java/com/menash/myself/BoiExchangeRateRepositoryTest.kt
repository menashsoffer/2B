package com.menash.myself

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.menash.myself.core.data.repository.impl.BoiExchangeRateRepository
import com.menash.myself.core.external.boi.BoiRate
import com.menash.myself.core.external.boi.BoiRateSource
import com.menash.myself.core.external.boi.BoiRateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class BoiExchangeRateRepositoryTest {

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private class FakeBoiRateSource : BoiRateSource {
        var latestCalls = 0
        var windowCalls = 0
        var nextLatest: Result<BoiRate> = Result.success(
            BoiRate(3.0, LocalDate.of(2026, 10, 26), Instant.parse("2026-10-26T14:30:00Z"), null, false)
        )
        var nextWindow: Result<List<Pair<LocalDate, Double>>> = Result.success(emptyList())

        override suspend fun fetchLatestRate(): Result<BoiRate> {
            latestCalls++
            return nextLatest
        }

        override suspend fun fetchRateWindow(
            startDate: LocalDate,
            endDate: LocalDate
        ): Result<List<Pair<LocalDate, Double>>> {
            windowCalls++
            return nextWindow
        }
    }

    private class Harness(
        val source: FakeBoiRateSource,
        val repo: BoiExchangeRateRepository,
        val store: BoiRateStore
    )

    private fun TestScope.harness(
        clock: Clock = Clock.fixed(Instant.parse("2026-10-25T17:00:00Z"), ZoneId.of("Asia/Jerusalem"))
    ): Harness {
        val source = FakeBoiRateSource()
        val store = BoiRateStore(InMemoryPreferencesDataStore())
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val repo = BoiExchangeRateRepository(store, source, clock, scope)
        return Harness(source, repo, store)
    }

    @Test
    fun manualOverrideOnly_noOfficialRate_fetchesOnce() = runTest {
        val h = harness()
        h.store.setManualOverride(3.5)
        val current = h.repo.currentRateFlow.value
        assertEquals(true, current?.isManualOverride)
        assertNull(current?.officialRate)
        assertNull(current?.officialRateDate)
        h.repo.refreshRateIfNeeded()
        assertEquals(1, h.source.latestCalls)
        assertEquals(0, h.source.windowCalls)
    }

    @Test
    fun officialRateStale_whileOverrideActive_fetches() = runTest {
        val h = harness()
        h.store.saveAutoRate(
            BoiRate(3.1, LocalDate.of(2026, 10, 20), Instant.parse("2026-10-20T10:00:00Z"), null, false)
        )
        h.store.setManualOverride(3.5)
        assertEquals(LocalDate.of(2026, 10, 20), h.repo.currentRateFlow.value?.officialRateDate)
        h.repo.refreshRateIfNeeded()
        assertEquals(1, h.source.latestCalls)
        assertEquals(0, h.source.windowCalls)
    }

    @Test
    fun officialRateFresh_doesNotFetch() = runTest {
        val h = harness()
        // 16:30 Asia/Jerusalem on the clock's day, after BoiStalenessPolicy's 16:00 cutoff.
        h.store.saveAutoRate(
            BoiRate(3.1, LocalDate.of(2026, 10, 25), Instant.parse("2026-10-25T14:30:00Z"), null, false)
        )
        h.store.setManualOverride(3.5)
        assertEquals(LocalDate.of(2026, 10, 25), h.repo.currentRateFlow.value?.officialRateDate)
        h.repo.refreshRateIfNeeded()
        assertEquals(0, h.source.latestCalls)
        assertEquals(0, h.source.windowCalls)
    }

    @Test
    fun historical_saturdayRequest_returnsThursdayRateAndDate() = runTest {
        val h = harness()
        val thursday = LocalDate.of(2026, 10, 22)
        val sunday = LocalDate.of(2026, 10, 25)
        h.source.nextWindow = Result.success(listOf(thursday to 3.107, sunday to 3.200))
        val result = h.repo.fetchHistoricalRate(LocalDate.of(2026, 10, 24))
        assertTrue(result.isSuccess)
        val rate = result.getOrThrow()
        assertEquals(3.107, rate.officialRate)
        assertEquals(thursday, rate.officialRateDate)
        assertEquals(1, h.source.windowCalls)
        assertEquals(0, h.source.latestCalls)
    }

    @Test
    fun historical_emptyWindow_isFailure() = runTest {
        val h = harness()
        h.source.nextWindow = Result.success(emptyList())
        val result = h.repo.fetchHistoricalRate(LocalDate.of(2026, 10, 24))
        assertTrue(result.isFailure)
        assertEquals(1, h.source.windowCalls)
        assertEquals(0, h.source.latestCalls)
    }

    @Test
    fun historical_sourceFailure_isFailure() = runTest {
        val h = harness()
        h.source.nextWindow = Result.failure(IOException("sdmx down"))
        val result = h.repo.fetchHistoricalRate(LocalDate.of(2026, 10, 24))
        assertTrue(result.isFailure)
        assertEquals(1, h.source.windowCalls)
        assertEquals(0, h.source.latestCalls)
    }
}
