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

        fun enqueuePeriodicWork(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val request = androidx.work.PeriodicWorkRequestBuilder<BoiSyncWorker>(1, java.util.concurrent.TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
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
