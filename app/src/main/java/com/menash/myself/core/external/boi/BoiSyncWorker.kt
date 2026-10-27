package com.menash.myself.core.external.boi

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import java.io.FileNotFoundException
import java.io.IOException
import java.time.Clock
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for syncing the Bank of Israel USD/ILS representative rate.
 * Each run schedules the next one-time run at [computeInitialDelay].
 *
 * The follow-up from [doWork] uses [ExistingWorkPolicy.APPEND_OR_REPLACE].
 * [ExistingWorkPolicy.REPLACE] cancels unfinished work with the same name, including
 * a RUNNING worker, so only [com.menash.myself.MyselfApp] uses REPLACE.
 *
 * [rateSource] stays null in production, and [doWork] then builds a [BoiExchangeRateClient].
 * Tests pass a fake so the sync never opens a socket.
 */
class BoiSyncWorker @JvmOverloads constructor(
    appContext: Context,
    workerParams: WorkerParameters,
    private val clock: Clock = Clock.system(BoiStalenessPolicy.ISRAEL_ZONE),
    private val rateSource: BoiRateSource? = null,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "BoiSyncWork"
        private const val LEGACY_PERIODIC_NAME = "BoiSyncPeriodicWork"

        fun computeInitialDelay(clock: Clock = Clock.system(BoiStalenessPolicy.ISRAEL_ZONE)): Long {
            val now = java.time.ZonedDateTime.now(clock)
            val target = nextSyncAt(now)
            return java.time.Duration.between(now, target).toMillis()
        }

        private fun nextSyncAt(now: java.time.ZonedDateTime): java.time.ZonedDateTime {
            val zone = BoiStalenessPolicy.ISRAEL_ZONE
            for (offset in 0..7) {
                val date = now.toLocalDate().plusDays(offset.toLong())
                val time = syncTime(date) ?: continue
                val target = java.time.ZonedDateTime.of(date, time, zone)
                if (now.isBefore(target)) {
                    return target
                }
            }
            error("No BOI sync slot within a week of $now")
        }

        private fun syncTime(date: java.time.LocalDate): java.time.LocalTime? = when (date.dayOfWeek) {
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY -> java.time.LocalTime.of(15, 45)
            java.time.DayOfWeek.FRIDAY -> java.time.LocalTime.of(12, 45)
            else -> null
        }

        /**
         * Schedules [WORK_NAME].
         *
         * Default [ExistingWorkPolicy.REPLACE] is for process start ([com.menash.myself.MyselfApp]).
         * REPLACE cancels the existing unique work, and a RUNNING worker is stopped immediately:
         * https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work
         *
         * [doWork] passes [ExistingWorkPolicy.APPEND_OR_REPLACE], which chains the next run and
         * does not cancel the current one. A prerequisite that is already FAILED or CANCELLED is
         * dropped so the new run still starts:
         * https://developer.android.com/reference/androidx/work/ExistingWorkPolicy#APPEND_OR_REPLACE
         */
        fun enqueue(
            context: Context,
            clock: Clock = Clock.system(BoiStalenessPolicy.ISRAEL_ZONE),
            existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<BoiSyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(computeInitialDelay(clock), TimeUnit.MILLISECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            try {
                val workManager = WorkManager.getInstance(context)
                workManager.cancelUniqueWork(LEGACY_PERIODIC_NAME)
                workManager.enqueueUniqueWork(WORK_NAME, existingWorkPolicy, request)
            } catch (e: Exception) {
                android.util.Log.e("BoiSyncWorker", "Failed to enqueue work", e)
            }
        }
    }

    override suspend fun doWork(): Result {
        val today = java.time.ZonedDateTime.now(clock).toLocalDate()
        if (!BoiStalenessPolicy.isBusinessDay(today)) {
            enqueue(applicationContext, clock, ExistingWorkPolicy.APPEND_OR_REPLACE)
            return Result.success()
        }

        val client = rateSource ?: BoiExchangeRateClient()
        val store = BoiRateStore(applicationContext.boiDataStore)
        val fetched = client.fetchLatestRate()
        val retry = fetched.exceptionOrNull()?.let { isNetworkFailure(it) && runAttemptCount < 3 } == true
        if (!retry) {
            fetched.getOrNull()?.let { store.saveAutoRate(it) }
            // Chain the next slot. Do not REPLACE: that cancels this RUNNING worker.
            enqueue(applicationContext, clock, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
        // A dependent of a FAILED or CANCELLED request is itself FAILED or CANCELLED, so the
        // appended next run would never execute
        // (https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/chain-work).
        // Success keeps that next run eligible. Transient network errors still return
        // Result.retry() without enqueueing, so exponential backoff on this request is unchanged.
        return if (retry) Result.retry() else Result.success()
    }

    private fun isNetworkFailure(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is IOException && current !is FileNotFoundException) return true
            current = current.cause
        }
        return false
    }
}
