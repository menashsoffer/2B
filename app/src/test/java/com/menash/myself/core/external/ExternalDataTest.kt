package com.menash.myself.core.external

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.external.boi.BoiExchangeRateClient
import com.menash.myself.core.external.boi.BoiRate
import com.menash.myself.core.external.boi.BoiStalenessPolicy
import com.menash.myself.core.external.hebcal.HebcalClient
import com.menash.myself.core.external.hebcal.IsraeliCity
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.core.ui.ErrorMapper
import com.menash.myself.core.data.legacy.firestore.LifeHubRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException
import java.time.Instant
import java.time.LocalDate

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class ExternalDataTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private class TestViewModel : BaseViewModel {
        constructor() : super(DefaultErrorReporter())
        constructor(scope: kotlinx.coroutines.CoroutineScope) : super(DefaultErrorReporter(), scope)
        fun testSafeLaunchException() {
            safeLaunch("testAction") {
                throw IOException("Network connection timeout")
            }
        }

        fun testSafeLaunchError() {
            safeLaunch("testError") {
                throw AssertionError("JVM AssertionError must propagate")
            }
        }

        fun testSafeLaunchCancellation() {
            safeLaunch("testCancel") {
                throw CancellationException("Cancelled")
            }
        }
    }

    @Test
    fun testBaseViewModelErrorReporterNonNullAndSafeLaunchCatchException() = runTest {
        Dispatchers.setMain(testDispatcher)
        try {
            val vm = TestViewModel()
            vm.testSafeLaunchException()
            testScheduler.advanceUntilIdle()
            assertEquals(ErrorMapper.MSG_OFFLINE, vm.errorState.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun testSafeLaunchDoesNotSwallowJvmError() = runTest {
        var caught: Throwable? = null
        val handler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
            caught = throwable
        }
        val scope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + UnconfinedTestDispatcher(testScheduler) + handler
        )
        val vm = TestViewModel(scope)
        vm.testSafeLaunchError()
        testScheduler.advanceUntilIdle()
        assertTrue(caught is AssertionError)
        assertEquals("JVM AssertionError must propagate", caught?.message)
        assertNull(vm.errorState.value)
    }

    @Test
    fun testSafeLaunchRethrowsCancellationException() = runTest {
        val vm = TestViewModel()
        vm.testSafeLaunchCancellation()
        testScheduler.advanceUntilIdle()
        assertNull(vm.errorState.value)
    }

    @Test
    fun testErrorMapperFriendlyMessages() {
        val offlineEx = UnknownHostException("Unable to resolve host edge.boi.gov.il")
        assertEquals(ErrorMapper.MSG_OFFLINE, ErrorMapper.toFriendlyMessage(offlineEx))

        val permEx = RuntimeException("PERMISSION_DENIED: Missing permissions")
        assertEquals(ErrorMapper.MSG_PERMISSION_DENIED, ErrorMapper.toFriendlyMessage(permEx))

        val hhEx = IllegalStateException("missing household session")
        assertEquals(ErrorMapper.MSG_NO_HOUSEHOLD, ErrorMapper.toFriendlyMessage(hhEx))

        val genericEx = NullPointerException("Random null pointer")
        assertEquals(ErrorMapper.MSG_UNKNOWN, ErrorMapper.toFriendlyMessage(genericEx))
    }

    @Test
    fun testBoiSdmxCsvParsing() {
        val recordedCsv = """
            SERIES_CODE,FREQ,BASE_CURRENCY,COUNTER_CURRENCY,UNIT_MEASURE,DATA_TYPE,DATA_SOURCE,TIME_COLLECT,CONF_STATUS,PUB_WEBSITE,UNIT_MULT,COMMENTS,TIME_PERIOD,OBS_VALUE,RELEASE_STATUS
            RER_USD_ILS,D,USD,ILS,ILS,OF00,BOI_MRKT,V,F,Y,0,,2026-10-23,3.112,YP
        """.trimIndent()

        val client = BoiExchangeRateClient()
        val parsed = client.parseSdmxCsv(recordedCsv, Instant.parse("2026-10-23T17:00:00Z"))

        assertEquals(3.112, parsed.rate, 0.0001)
        assertEquals(LocalDate.of(2026, 10, 23), parsed.rateDate)
        assertFalse(parsed.isManualOverride)
    }

    @Test
    fun firestoreErrorFlow_permissionDenied_isFriendlyHebrew() {
        val repo = LifeHubRepository(db = null, uidProvider = { "uid" })
        val technical = RuntimeException("PERMISSION_DENIED: Missing or insufficient permissions.")
        repo.reportError("observeTasks", technical)
        assertEquals(ErrorMapper.MSG_PERMISSION_DENIED, repo.errorFlow.value)
        assertEquals(
            "observeTasks (ERROR): PERMISSION_DENIED: Missing or insufficient permissions.",
            repo.lastTechnicalError
        )
        assertFalse(repo.errorFlow.value!!.contains("PERMISSION_DENIED"))
    }

    @Test
    fun testBoiStalenessLogic() {
        val rateDate = LocalDate.of(2026, 10, 21)
        val fetchedAtOld = Instant.parse("2026-10-21T10:00:00Z")
        val nowBusinessAfter16 = Instant.parse("2026-10-22T17:00:00Z") // Thursday 17:00 Israel time

        val isStale = BoiStalenessPolicy.isStale(rateDate, fetchedAtOld, nowBusinessAfter16)
        assertTrue(isStale)
    }

    @Test
    fun testHebcalSampleJsonParsingAndNoDefaultCity() {
        val recordedJson = """
            {
              "items": [
                { "title": "Candle lighting: 16:16", "date": "2026-11-27T16:16:00+02:00", "category": "candles", "hebrew": "הדלקת נרות" },
                { "title": "Havdalah: 17:16", "date": "2026-11-28T17:16:00+02:00", "category": "havdalah", "hebrew": "הבדלה" }
              ]
            }
        """.trimIndent()

        val client = HebcalClient()
        val parsed = client.parseHebcalJson(recordedJson, geonameid = 293397, cityName = "תל אביב")

        assertEquals("תל אביב", parsed.cityName)
        assertEquals(293397, parsed.geonameid)
        assertEquals(2, parsed.items.size)
        assertTrue(parsed.upcomingShabbatCandles!!.contains("16:16"))
    }
}
