package com.menash.myself.core.external.boi

import android.app.Application
import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Proves a finished [BoiSyncWorker] run stays [WorkInfo.State.SUCCEEDED] and leaves
 * exactly one [WorkInfo.State.ENQUEUED] "BoiSyncWork" for the next slot.
 *
 * [BoiSyncWorker] already accepts a [Clock]. The fetch has no production setter, so this
 * test installs a [WorkerFactory] that passes a [FakeBoiRateSource]. Production still
 * constructs [BoiExchangeRateClient] when that argument is null.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class BoiSyncWorkerTest {

    private val clock: Clock = Clock.fixed(MONDAY_MORNING, BoiStalenessPolicy.ISRAEL_ZONE)
    private val attempts = mutableListOf<Int>()
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        attempts.clear()
    }

    @Test
    fun completedRunLeavesOneEnqueuedNextSlotAndSucceeds() {
        val source = FakeBoiRateSource { Result.success(sampleRate()) }
        val workManager = startWorkManager(source)
        val requestId = enqueueAndRun(workManager)

        val snapshot = awaitSnapshot(workManager, requestId) { finished, infos ->
            val state = finished?.state
            if (state == WorkInfo.State.CANCELLED || state == WorkInfo.State.FAILED) return@awaitSnapshot true
            if (finished == null && source.fetchCount > 0) return@awaitSnapshot true
            state == WorkInfo.State.SUCCEEDED && infos.count { it.state == WorkInfo.State.ENQUEUED } == 1
        }

        assertEquals("BoiSyncWork", BoiSyncWorker.WORK_NAME)
        assertEquals(
            "finished BoiSyncWork run should be SUCCEEDED, not CANCELLED",
            WorkInfo.State.SUCCEEDED,
            snapshot.finished?.state,
        )
        val enqueued = snapshot.infos.filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(snapshot.describe(), 1, enqueued.size)
        assertTrue(enqueued.single().id != requestId)
        assertEquals(1, source.fetchCount)
    }

    @Test
    fun networkFailureBeforeThirdAttemptRetriesWithoutExtraWork() {
        val source = FakeBoiRateSource { Result.failure(IOException("offline")) }
        val workManager = startWorkManager(source)
        val requestId = enqueueAndRun(workManager)

        val snapshot = awaitSnapshot(workManager, requestId) { finished, infos ->
            if (finished?.state?.isFinished == true) return@awaitSnapshot true
            source.fetchCount >= 1 &&
                finished?.state == WorkInfo.State.ENQUEUED &&
                finished.runAttemptCount >= 1 &&
                infos.size == 1 &&
                infos.single().id == requestId
        }

        assertTrue(attempts.isNotEmpty())
        assertTrue(attempts.all { it < 3 })
        assertEquals(WorkInfo.State.ENQUEUED, snapshot.finished?.state)
        assertEquals(snapshot.describe(), 1, snapshot.infos.size)
        assertEquals(requestId, snapshot.infos.single().id)
        assertEquals(1, source.fetchCount)
    }

    private fun startWorkManager(source: FakeBoiRateSource): WorkManager {
        val configuration = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(BoiSyncWorkerFactory(clock, source, attempts))
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        return WorkManager.getInstance(context)
    }

    private fun enqueueAndRun(workManager: WorkManager): UUID {
        val request = OneTimeWorkRequestBuilder<BoiSyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        workManager.enqueueUniqueWork(
            BoiSyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        ).result.get()
        val driver = WorkManagerTestInitHelper.getTestDriver(context)
            ?: error("WorkManager test driver was not installed")
        driver.setAllConstraintsMet(request.id)
        return request.id
    }

    private fun awaitSnapshot(
        workManager: WorkManager,
        requestId: UUID,
        ready: (finished: WorkInfo?, infos: List<WorkInfo>) -> Boolean,
    ): WorkSnapshot {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        var snapshot = readSnapshot(workManager, requestId)
        while (System.nanoTime() < deadline) {
            if (ready(snapshot.finished, snapshot.infos)) return snapshot
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
            snapshot = readSnapshot(workManager, requestId)
        }
        return snapshot
    }

    private fun readSnapshot(workManager: WorkManager, requestId: UUID): WorkSnapshot {
        val finished = workManager.getWorkInfoById(requestId).get()
        val infos = workManager.getWorkInfosForUniqueWork(BoiSyncWorker.WORK_NAME).get()
        return WorkSnapshot(finished, infos)
    }

    private class WorkSnapshot(
        val finished: WorkInfo?,
        val infos: List<WorkInfo>,
    ) {
        fun describe(): String = infos.joinToString { "${it.id} ${it.state}" }
    }

    private class BoiSyncWorkerFactory(
        private val clock: Clock,
        private val rateSource: BoiRateSource,
        private val attempts: MutableList<Int>,
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker? {
            if (workerClassName != BoiSyncWorker::class.java.name) return null
            attempts.add(workerParameters.runAttemptCount)
            return BoiSyncWorker(appContext, workerParameters, clock, rateSource)
        }
    }

    private class FakeBoiRateSource(
        private val fetchLatest: () -> Result<BoiRate>,
    ) : BoiRateSource {
        var fetchCount: Int = 0

        override suspend fun fetchLatestRate(): Result<BoiRate> {
            fetchCount += 1
            return fetchLatest()
        }

        override suspend fun fetchRateWindow(
            startDate: LocalDate,
            endDate: LocalDate,
        ): Result<List<Pair<LocalDate, Double>>> = Result.success(emptyList())
    }

    private companion object {
        val MONDAY_MORNING: Instant = ZonedDateTime.of(
            2026,
            10,
            26,
            10,
            0,
            0,
            0,
            BoiStalenessPolicy.ISRAEL_ZONE,
        ).toInstant()

        fun sampleRate(): BoiRate = BoiRate(
            officialRate = 3.078,
            officialRateDate = LocalDate.of(2026, 10, 26),
            officialFetchedAt = Instant.parse("2026-10-26T07:00:00Z"),
            manualRate = null,
            isManualOverride = false,
        )
    }
}
