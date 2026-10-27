package com.menash.myself

import android.app.Application
import com.menash.myself.core.crash.SanitizingUncaughtExceptionHandler
import com.menash.myself.core.di.AppContainer

/**
 * Application subclass owning the singleton AppContainer and HouseholdSession
 * so they survive Activity recreation (rotation, dark mode, locale changes).
 */
class MyselfApp : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        SanitizingUncaughtExceptionHandler.install()
        com.menash.myself.core.external.boi.BoiSyncWorker.enqueue(this)
    }
}
