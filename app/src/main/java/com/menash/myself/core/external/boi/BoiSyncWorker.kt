package com.menash.myself.core.external.boi

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * WorkManager worker for syncing Bank of Israel USD/ILS representative rate daily.
 */
class BoiSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME = "BoiSyncPeriodicWork"

        fun computeInitialDelay(clock: java.time.Clock = java.time.Clock.system(BoiStalenessPolicy.ISRAEL_ZONE)): Long {
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

        fun enqueuePeriodicWork(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val initialDelay = computeInitialDelay()
            val request = androidx.work.PeriodicWorkRequestBuilder<BoiSyncWorker>(1, java.util.concurrent.TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            try {
                androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            } catch (e: Exception) {
                android.util.Log.e("BoiSyncWorker", "Failed to enqueue work", e)
            }
        }
    }

    override suspend fun doWork(): Result {
        val today = java.time.ZonedDateTime.now(BoiStalenessPolicy.ISRAEL_ZONE).toLocalDate()
        if (!BoiStalenessPolicy.isBusinessDay(today)) {
            return Result.success()
        }

        val client = BoiExchangeRateClient()
        val store = BoiRateStore(applicationContext.boiDataStore)

        val result = client.fetchLatestRate()
        return result.fold(
            onSuccess = { rate ->
                store.saveAutoRate(rate)
                Result.success()
            },
            onFailure = {
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        )
    }
}
