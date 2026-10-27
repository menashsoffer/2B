package com.menash.myself.core.crash

import android.os.Process
import android.util.Log
import com.menash.myself.BuildConfig
import kotlin.system.exitProcess

/**
 * Wraps the process handler so Crashlytics records a sanitized fatal.
 *
 * Crashlytics installs its handler before [android.app.Application.onCreate].
 * [com.google.firebase.provider.FirebaseInitProvider] is a ContentProvider whose
 * `onCreate` is documented to run before `Application.onCreate`, and it calls
 * `FirebaseApp.initializeApp`. [com.google.firebase.crashlytics.CrashlyticsRegistrar]
 * is `eagerInDefaultApp`, so that init constructs `FirebaseCrashlytics`, which calls
 * `CrashlyticsCore.onPreExecute`. That method captures the current default handler and
 * replaces it with `CrashlyticsUncaughtExceptionHandler`.
 *
 * https://github.com/firebase/firebase-android-sdk/blob/master/firebase-common/src/main/java/com/google/firebase/provider/FirebaseInitProvider.java
 * https://github.com/firebase/firebase-android-sdk/blob/master/firebase-crashlytics/src/main/java/com/google/firebase/crashlytics/CrashlyticsRegistrar.java
 * https://github.com/firebase/firebase-android-sdk/blob/master/firebase-crashlytics/src/main/java/com/google/firebase/crashlytics/internal/common/CrashlyticsCore.java
 * https://developer.android.com/reference/android/app/Application#onCreate()
 */
object SanitizingUncaughtExceptionHandler {
    private const val TAG = "SanitizingUncaughtExceptionHandler"

    internal var sanitize: (Throwable) -> Throwable = { error -> CrashlyticsNonFatal.sanitized(error) }

    internal var terminate: () -> Unit = {
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    val install: () -> Unit = {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            dispatch(thread, throwable, previous)
        }
    }

    internal val dispatch: (Thread, Throwable, Thread.UncaughtExceptionHandler?) -> Unit =
        { thread, throwable, previous ->
            if (BuildConfig.DEBUG) {
                runCatching { Log.e(TAG, "Uncaught exception", throwable) }
            }
            val safe = try {
                sanitize(throwable)
            } catch (_: Throwable) {
                RuntimeException().also { fallback ->
                    runCatching { fallback.stackTrace = throwable.stackTrace }
                }
            }
            if (previous != null) {
                previous.uncaughtException(thread, safe)
            } else {
                terminate()
            }
        }
}
