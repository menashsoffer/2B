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
            val now = java.time.ZonedDateTime.now(clock.zone)
            var target = now.withHour(16).withMinute(30).withSecond(0).withNano(0)
            if (now.isAfter(target) || now == target) {
                target = target.plusDays(1)
            }
            return java.time.Duration.between(now, target).toMillis()
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
        val now = java.time.ZonedDateTime.now(BoiStalenessPolicy.ISRAEL_ZONE)
        val dow = now.dayOfWeek
        if (dow == java.time.DayOfWeek.FRIDAY || dow == java.time.DayOfWeek.SATURDAY) {
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
