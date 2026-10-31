package com.menash.myself.core.data.repository.impl

import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AwaitFirestoreWriteTest {
    @Test fun rejectionAfterTimeoutIsReported() = runTest {
        val source = TaskCompletionSource<Void>()
        val failures = mutableListOf<Exception>()
        val waiting = async { withTimeoutOrNull(3_000) { awaitFirestoreWrite(source.task) { failures += it }; true } }
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()
        assertNull(waiting.await())
        val rejection = IllegalStateException("rejected")
        source.setException(rejection)
        assertEquals(1, failures.size)
        assertSame(rejection, failures.single())
    }

    @Test fun successfulWriteAfterTimeoutIsNotReportedAsFailure() = runTest {
        val source = TaskCompletionSource<Void>()
        val failures = mutableListOf<Exception>()
        val waiting = async { withTimeoutOrNull(3_000) { awaitFirestoreWrite(source.task) { failures += it }; true } }
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()
        assertNull(waiting.await())
        source.setResult(null)
        assertEquals(emptyList<Exception>(), failures)
    }
}
