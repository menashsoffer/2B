package com.menash.myself.core.crash

import com.google.firebase.firestore.FirebaseFirestoreException
import com.menash.myself.BuildConfig
import com.menash.myself.core.crash.CrashlyticsNonFatal
import com.menash.myself.core.crash.SanitizingUncaughtExceptionHandler
import com.menash.myself.core.ui.ErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.io.IOException
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CrashlyticsNonFatalTest {

    @Test
    fun technicalDetail_firestoreCode_omitsUserData() {
        val householdId = "hh-9f3c1a"
        val email = "ada@example.com"
        val title = "Buy milk"
        val amount = "1250.50"
        val error = FirebaseFirestoreException(
            "No document households/$householdId user $email title $title amount $amount",
            FirebaseFirestoreException.Code.NOT_FOUND
        )

        val detail = ErrorMapper.technicalDetail(error)

        assertEquals(
            "com.google.firebase.firestore.FirebaseFirestoreException: NOT_FOUND",
            detail
        )
        assertFalse(detail.contains(householdId))
        assertFalse(detail.contains(email))
        assertFalse(detail.contains(title))
        assertFalse(detail.contains(amount))
    }

    @Test
    fun technicalDetail_categoryToken_omitsMessageText() {
        val householdId = "hh-9f3c1a"
        val email = "ada@example.com"
        val household = IllegalStateException("missing household $householdId for $email")
        assertEquals(
            "java.lang.IllegalStateException: no_household",
            ErrorMapper.technicalDetail(household)
        )
        assertFalse(ErrorMapper.technicalDetail(household).contains(householdId))
        assertFalse(ErrorMapper.technicalDetail(household).contains(email))

        val offline = UnknownHostException("Unable to resolve host edge.boi.gov.il")
        assertEquals(
            "java.net.UnknownHostException: network",
            ErrorMapper.technicalDetail(offline)
        )
        assertFalse(ErrorMapper.technicalDetail(offline).contains("edge.boi.gov.il"))

        val permission = RuntimeException("PERMISSION_DENIED: Missing or insufficient permissions.")
        assertEquals(
            "java.lang.RuntimeException: permission_denied",
            ErrorMapper.technicalDetail(permission)
        )
        assertFalse(ErrorMapper.technicalDetail(permission).contains("insufficient"))

        val generic = NullPointerException("Random null pointer")
        assertEquals(
            "java.lang.NullPointerException: unknown",
            ErrorMapper.technicalDetail(generic)
        )
        assertFalse(ErrorMapper.technicalDetail(generic).contains("Random"))
    }

    @Test
    fun record_onDebug_keepsTheOriginalMessage_andSanitizedCopyOmitsIt() {
        assertTrue(BuildConfig.DEBUG)
        val email = "ada@example.com"
        val householdId = "hh-9f3c1a"
        val error = IllegalStateException("missing household $householdId for $email amount 10")
        val frame = StackTraceElement(
            "com.menash.myself.core.data.legacy.firestore.HouseholdRepository",
            "checkInvite",
            "HouseholdRepository.kt",
            59
        )
        error.stackTrace = arrayOf(frame)

        CrashlyticsNonFatal.record(error)

        assertEquals("missing household $householdId for $email amount 10", error.message)
        assertSame(frame, error.stackTrace[0])
        val safe = CrashlyticsNonFatal.sanitized(error)
        assertEquals(IllegalStateException::class.java, safe.javaClass)
        assertTrue(safe.message.isNullOrBlank())
        assertFalse(safe.toString().contains(email))
        assertFalse(safe.toString().contains(householdId))
        assertEquals("checkInvite", safe.stackTrace[0].methodName)
        assertEquals(59, safe.stackTrace[0].lineNumber)
    }

    @Test
    fun sanitized_nestedCauseChain_stripsMessages_keepsClassAndStack_andBreaksCycles() {
        val email = "ada@example.com"
        val householdId = "hh-9f3c1a"
        val uid = "uid-abc"
        val name = "Dana"
        val root = IllegalStateException("household $householdId email $email")
        root.stackTrace = arrayOf(frame("checkInvite", 59))
        val mid = RuntimeException("uid $uid", root)
        mid.stackTrace = arrayOf(frame("joinInvitedHousehold", 153))
        val top = IOException("invites/$email", mid)
        top.stackTrace = arrayOf(frame("observeHousehold", 197))
        val suppressed = IllegalArgumentException("name $name amount 42")
        suppressed.stackTrace = arrayOf(frame("invitePartner", 208))
        top.addSuppressed(suppressed)

        val safe = CrashlyticsNonFatal.sanitized(top)

        assertEquals(IOException::class.java, safe.javaClass)
        assertEquals(RuntimeException::class.java, safe.cause!!.javaClass)
        assertEquals(IllegalStateException::class.java, safe.cause!!.cause!!.javaClass)
        assertNull(safe.cause!!.cause!!.cause)
        assertEquals(IllegalArgumentException::class.java, safe.suppressed.single().javaClass)
        assertFrame(safe, "observeHousehold", 197)
        assertFrame(safe.cause!!, "joinInvitedHousehold", 153)
        assertFrame(safe.cause!!.cause!!, "checkInvite", 59)
        assertFrame(safe.suppressed.single(), "invitePartner", 208)
        assertNoSecrets(safe, email, householdId, uid, name, "42", "invites/")

        val self = SelfCause("self $email $householdId")
        val selfSafe = CrashlyticsNonFatal.sanitized(self)
        assertEquals(self.javaClass, selfSafe.javaClass)
        assertTrue(selfSafe.message.isNullOrBlank())
        assertNoSecrets(selfSafe, email, householdId)

        val left = IllegalStateException("left $email")
        val right = RuntimeException("right $uid $householdId")
        left.initCause(right)
        right.initCause(left)
        val cycleSafe = CrashlyticsNonFatal.sanitized(left)
        assertEquals(IllegalStateException::class.java, cycleSafe.javaClass)
        assertEquals(RuntimeException::class.java, cycleSafe.cause!!.javaClass)
        var hops = 0
        var cursor: Throwable? = cycleSafe
        while (cursor != null) {
            hops += 1
            assertTrue(hops < 8)
            cursor = cursor.cause
        }
        assertTrue(hops >= 2)
        assertNoSecrets(cycleSafe, email, householdId, uid)

        val firestore = FirebaseFirestoreException(
            "invites/$email household $householdId",
            FirebaseFirestoreException.Code.NOT_FOUND
        )
        firestore.stackTrace = arrayOf(frame("checkInvite", 59))
        val safeFirestore = CrashlyticsNonFatal.sanitized(firestore)
        assertEquals(FirebaseFirestoreException::class.java, safeFirestore.javaClass)
        assertEquals(FirebaseFirestoreException.Code.NOT_FOUND.name, safeFirestore.message)
        assertEquals(
            FirebaseFirestoreException.Code.NOT_FOUND,
            (safeFirestore as FirebaseFirestoreException).code
        )
        assertFrame(safeFirestore, "checkInvite", 59)
        assertFalse(safeFirestore.toString().contains(email))
        assertFalse(safeFirestore.toString().contains(householdId))
        assertFalse(safeFirestore.toString().contains("invites/"))
    }

    @Test
    fun handler_whenSanitizerThrows_forwardsMessageLessThrowable() {
        val email = "ada@example.com"
        val original = IllegalStateException("invites/$email household hh-9f3c1a")
        original.stackTrace = arrayOf(frame("joinInvitedHousehold", 153))
        var forwarded: Throwable? = null
        val previous = Thread.UncaughtExceptionHandler { _, throwable -> forwarded = throwable }
        val originalSanitize = SanitizingUncaughtExceptionHandler.sanitize
        SanitizingUncaughtExceptionHandler.sanitize = { throw OutOfMemoryError("invites/$email") }
        try {
            SanitizingUncaughtExceptionHandler.dispatch(Thread.currentThread(), original, previous)
        } finally {
            SanitizingUncaughtExceptionHandler.sanitize = originalSanitize
        }

        val sent = forwarded!!
        assertEquals(RuntimeException::class.java, sent.javaClass)
        assertNull(sent.message)
        assertFalse(sent.toString().contains(email))
        assertFalse(sent.toString().contains("hh-9f3c1a"))
        assertEquals("joinInvitedHousehold", sent.stackTrace[0].methodName)
        assertEquals(153, sent.stackTrace[0].lineNumber)
    }

    @Test
    fun handler_withoutPrevious_terminatesInsteadOfDroppingTheCrash() {
        var terminated = false
        val originalTerminate = SanitizingUncaughtExceptionHandler.terminate
        val originalSanitize = SanitizingUncaughtExceptionHandler.sanitize
        SanitizingUncaughtExceptionHandler.terminate = { terminated = true }
        SanitizingUncaughtExceptionHandler.sanitize = { throw IllegalStateException("ada@example.com") }
        try {
            SanitizingUncaughtExceptionHandler.dispatch(
                Thread.currentThread(),
                IllegalStateException("invites/ada@example.com"),
                null
            )
        } finally {
            SanitizingUncaughtExceptionHandler.terminate = originalTerminate
            SanitizingUncaughtExceptionHandler.sanitize = originalSanitize
        }
        assertTrue(terminated)
    }

    @Test
    fun debugHandler_logsOriginal_andForwardsSanitizedCopy() {
        assertTrue(BuildConfig.DEBUG)
        val email = "ada@example.com"
        val original = IllegalStateException("invites/$email")
        original.stackTrace = arrayOf(frame("joinInvitedHousehold", 153))
        var forwarded: Throwable? = null
        val before = Thread.getDefaultUncaughtExceptionHandler()
        ShadowLog.clear()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable -> forwarded = throwable }
        try {
            SanitizingUncaughtExceptionHandler.install()
            Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), original)
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(before)
        }

        val sent = forwarded!!
        assertEquals(IllegalStateException::class.java, sent.javaClass)
        assertTrue(sent.message.isNullOrBlank())
        assertFalse(sent.toString().contains(email))
        assertEquals("joinInvitedHousehold", sent.stackTrace[0].methodName)
        assertEquals("invites/$email", original.message)
        val logged = ShadowLog.getLogs().filter { it.tag == "SanitizingUncaughtExceptionHandler" }
        assertTrue(logged.any { it.throwable === original })
    }

    private fun frame(method: String, line: Int): StackTraceElement {
        return StackTraceElement(
            "com.menash.myself.core.data.legacy.firestore.HouseholdRepository",
            method,
            "HouseholdRepository.kt",
            line
        )
    }

    private fun assertFrame(error: Throwable, method: String, line: Int) {
        assertEquals(method, error.stackTrace[0].methodName)
        assertEquals(line, error.stackTrace[0].lineNumber)
        assertEquals("HouseholdRepository.kt", error.stackTrace[0].fileName)
    }

    private fun assertNoSecrets(error: Throwable, vararg secrets: String) {
        val seen = HashSet<Throwable>()
        val pending = ArrayDeque<Throwable>()
        pending.add(error)
        var visited = 0
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (!seen.add(current)) continue
            visited += 1
            assertTrue(visited < 8)
            assertTrue(current.message.isNullOrBlank())
            val rendered = current.toString()
            for (secret in secrets) {
                assertFalse(rendered.contains(secret))
            }
            current.cause?.let { pending.add(it) }
            current.suppressed.forEach { pending.add(it) }
        }
    }

    private class SelfCause(message: String) : IllegalStateException(message) {
        override val cause: Throwable
            get() = this
    }
}
