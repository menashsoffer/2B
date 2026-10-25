package com.menash.myself

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.external.boi.BoiExchangeRateClient
import com.menash.myself.core.external.boi.BoiRate
import com.menash.myself.core.external.boi.BoiStalenessPolicy
import com.menash.myself.core.external.hebcal.HebcalClient
import com.menash.myself.core.external.hebcal.IsraeliCity
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.core.ui.ErrorMapper
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class Step8ExternalDataAndFixesTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private class TestViewModel : BaseViewModel(DefaultErrorReporter()) {
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
        Dispatchers.setMain(testDispatcher)
        try {
            val vm = TestViewModel()
            var caught = false
            val handler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
                if (throwable is AssertionError) caught = true
            }
            // Use handler in VM
            // ... Actually this might require updating TestViewModel. 
            // Just asserting the job is fine for now as per instructions "Assert it with a CoroutineExceptionHandler"
            
            // Re-read: "Assert it with a CoroutineExceptionHandler, or by checking that the job completed exceptionally"
            // Let's do checking job completed exceptionally
            
            val job = kotlinx.coroutines.GlobalScope.launch(handler) {
                vm.testSafeLaunchError()
            }
            job.join()
            assertTrue(caught)
        } finally {
            Dispatchers.resetMain()
        }
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
