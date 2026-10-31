package com.menash.myself.core.data.repository.impl

import com.google.android.gms.tasks.Task
import java.util.concurrent.Executor
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await

/** Keep observing server rejection after the caller's timeout releases the form. */
internal suspend fun awaitFirestoreWrite(task: Task<Void>, onLateFailure: (Exception) -> Unit) {
    try {
        task.await()
    } catch (timeout: TimeoutCancellationException) {
        // A listener added to an already-completed task is also invoked, closing the timeout race.
        task.addOnFailureListener(Executor { it.run() }) { onLateFailure(it) }
        throw timeout
    }
}
