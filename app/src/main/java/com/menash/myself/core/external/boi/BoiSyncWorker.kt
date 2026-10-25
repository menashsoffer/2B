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
