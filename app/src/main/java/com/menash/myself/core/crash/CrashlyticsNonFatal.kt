package com.menash.myself.core.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestoreException
import com.menash.myself.BuildConfig
import java.lang.reflect.Constructor

/**
 * Records a non-fatal and builds the throwable passed to Crashlytics.
 * [sanitized] keeps the exception class and stack frames, and strips messages
 * on the cause chain and suppressed throwables. A Firestore exception keeps
 * its status code name. No custom keys.
 * Debug builds do not record, so a debug non-fatal is not buffered for a later upload.
 */
object CrashlyticsNonFatal {
    val record: (Throwable) -> Unit = { error ->
        if (!BuildConfig.DEBUG) {
            runCatching {
                FirebaseCrashlytics.getInstance().recordException(sanitized(error))
            }
        }
    }

    internal fun sanitized(error: Throwable): Throwable = copyStripped(error, mutableSetOf())

    private fun copyStripped(error: Throwable, seen: MutableSet<Throwable>): Throwable {
        if (!seen.add(error)) {
            return blankCopy(error).also { copyStack(it, error) }
        }
        val copy = blankCopy(error)
        val cause = error.cause
        if (cause != null) {
            runCatching { copy.initCause(copyStripped(cause, seen)) }
        }
        for (suppressed in error.suppressed) {
            runCatching { copy.addSuppressed(copyStripped(suppressed, seen)) }
        }
        copyStack(copy, error)
        return copy
    }

    private val copyStack: (Throwable, Throwable) -> Unit = { target, source ->
        target.stackTrace = source.stackTrace.clone()
    }

    private fun blankCopy(error: Throwable): Throwable {
        val created = error.javaClass.declaredConstructors
            .asSequence()
            .filter { ctor -> ctor.parameterTypes.none { it.name.endsWith("DefaultConstructorMarker") } }
            .filter { ctor -> ctor.parameterTypes.none { Throwable::class.java.isAssignableFrom(it) } }
            .sortedBy { it.parameterTypes.size }
            .firstNotNullOfOrNull { ctor -> newBlankInstance(ctor, error) }
        return created ?: Exception()
    }

    private fun newBlankInstance(ctor: Constructor<*>, error: Throwable): Throwable? {
        val args = arrayOfNulls<Any>(ctor.parameterTypes.size)
        ctor.parameterTypes.forEachIndexed { index, parameter ->
            args[index] = argumentFor(parameter, error) ?: return null
        }
        return runCatching {
            ctor.isAccessible = true
            ctor.newInstance(*args) as Throwable
        }.getOrNull()
    }

    private fun argumentFor(parameter: Class<*>, error: Throwable): Any? {
        return when {
            parameter == String::class.java -> stringArgument(error)
            parameter == Int::class.javaPrimitiveType || parameter == Int::class.java -> 0
            parameter == Long::class.javaPrimitiveType || parameter == Long::class.java -> 0L
            parameter == Boolean::class.javaPrimitiveType || parameter == Boolean::class.java -> false
            parameter == Float::class.javaPrimitiveType || parameter == Float::class.java -> 0f
            parameter == Double::class.javaPrimitiveType || parameter == Double::class.java -> 0.0
            parameter.isEnum -> enumArgument(parameter, error)
            else -> null
        }
    }

    private fun stringArgument(error: Throwable): String {
        if (error is FirebaseFirestoreException) return error.code.name
        // A space, not "". FirebaseException rejects an empty detail message.
        return " "
    }

    private fun enumArgument(parameter: Class<*>, error: Throwable): Any? {
        var type: Class<*>? = error.javaClass
        while (type != null) {
            for (field in type.declaredFields) {
                if (!parameter.isAssignableFrom(field.type)) continue
                field.isAccessible = true
                val value = runCatching { field.get(error) }.getOrNull()
                if (value != null) return value
            }
            type = type.superclass
        }
        return parameter.enumConstants?.firstOrNull()
    }
}
