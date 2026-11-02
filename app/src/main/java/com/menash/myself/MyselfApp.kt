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
        // The e2e build only (docs/E2E.md): before anything touches Firebase Auth.
        if (BuildConfig.FIREBASE_EMULATOR_HOST.isNotEmpty()) {
            com.google.firebase.auth.FirebaseAuth.getInstance().useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 9099)
        }
        SanitizingUncaughtExceptionHandler.install()
        com.menash.myself.core.external.boi.BoiSyncWorker.enqueue(this)
    }
}
