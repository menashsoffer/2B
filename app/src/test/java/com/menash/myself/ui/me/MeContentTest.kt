package com.menash.myself.ui.me

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.menash.myself.core.data.repository.ExchangeRateRepository
import com.menash.myself.core.data.repository.HebcalRepository
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.external.boi.BoiRate
import com.menash.myself.core.external.hebcal.HebcalData
import com.menash.myself.core.external.hebcal.IsraeliCity
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.SampleName
import com.menash.myself.r2.captureR2Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate

/** «אני»: every account action has one way in, and guests see sign-in instead of sign-out. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class MeContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val signedIn = MeState(signedIn = true, displayName = SampleName, email = "dana@example.com",
        versionName = "1.0", versionCode = 1)
    private val guest = MeState(signedIn = false, versionName = "1.0", versionCode = 1)

    @Test
    fun signedIn_rowsOpenTheirActions() {
        val opened = mutableListOf<String>()
        composeRule.setContent {
            MyApplicationTheme {
                MeContent(signedIn, MeActions(
                    onSignIn = { opened += "signIn" },
                    onHousehold = { opened += "household" },
                    onPreferences = { opened += "preferences" },
                    onManageAccount = { opened += "management" },
                    onSignOut = { opened += "signOut" },
                ))
            }
        }
        composeRule.onAllNodesWithTag("account_sign_in").assertCountEquals(0)
        listOf("account_household", "account_settings", "account_management", "account_sign_out").forEach { tag ->
            composeRule.onNodeWithTag(tag).performClick()
        }
        assertEquals(listOf("household", "preferences", "management", "signOut"), opened)
    }

    @Test
    fun guest_seesSignInAndNoAccountActions() {
        val opened = mutableListOf<String>()
        composeRule.setContent {
            MyApplicationTheme { MeContent(guest, MeActions(onSignIn = { opened += "signIn" })) }
        }
        composeRule.onAllNodesWithTag("account_sign_out").assertCountEquals(0)
        composeRule.onAllNodesWithTag("account_management").assertCountEquals(0)
        composeRule.onNodeWithTag("account_sign_in").performClick()
        assertEquals(listOf("signIn"), opened)
    }

    @Test
    fun preferences_switchingManualToAutoRate_clearsOverrideWithoutCompetingManualSave() {
        val rateRepo = FakeExchangeRateRepository(
            BoiRate(
                officialRate = 3.55,
                officialRateDate = LocalDate.of(2026, 10, 26),
                officialFetchedAt = Instant.parse("2026-10-26T14:30:00Z"),
                manualRate = 3.85,
                isManualOverride = true,
            ),
        )
        val hebcalRepo = FakeHebcalRepository()

        composeRule.setContent {
            MyApplicationTheme {
                MeScreen(
                    currentUser = null,
                    onRequestSignIn = {},
                    onSignedOut = {},
                    onOpenHousehold = {},
                    onOpenRoute = {},
                    exchangeRateRepository = rateRepo,
                    hebcalRepository = hebcalRepo,
                    dialogsInWindow = false,
                )
            }
        }

        composeRule.onNodeWithTag("account_settings").performClick()
        composeRule.onNodeWithTag("use_manual_rate_checkbox").assertIsOn()
        composeRule.onNodeWithTag("use_manual_rate_checkbox").performClick()
        composeRule.onNodeWithTag("use_manual_rate_checkbox").assertIsOff()
        composeRule.onNodeWithTag("save_settings_button").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag("usd_rate_input").assertCountEquals(0)
        assertEquals(emptyList<Double?>(), rateRepo.manualRateCalls)
        assertEquals(1, rateRepo.clearManualRateCalls)
        assertFalse(rateRepo.currentRateFlow.value!!.isManualOverride)
        assertEquals(3.55, rateRepo.currentRateFlow.value!!.rate ?: 0.0, 0.0001)
    }

    @Test
    fun preferences_survivesConfigurationChange() {
        val rateRepo = FakeExchangeRateRepository(
            BoiRate(
                officialRate = 3.55,
                officialRateDate = LocalDate.of(2026, 10, 26),
                officialFetchedAt = Instant.parse("2026-10-26T14:30:00Z"),
                manualRate = 3.85,
                isManualOverride = true,
            ),
        )
        val hebcalRepo = FakeHebcalRepository()
        val restorationTester = StateRestorationTester(composeRule)

        restorationTester.setContent {
            MyApplicationTheme {
                MeScreen(
                    currentUser = null,
                    onRequestSignIn = {},
                    onSignedOut = {},
                    onOpenHousehold = {},
                    onOpenRoute = {},
                    exchangeRateRepository = rateRepo,
                    hebcalRepository = hebcalRepo,
                    dialogsInWindow = false,
                )
            }
        }

        composeRule.onNodeWithTag("account_settings").performClick()
        composeRule.onNodeWithTag("use_manual_rate_checkbox").performClick()
        composeRule.onNodeWithTag("use_manual_rate_checkbox").assertIsOff()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag("usd_rate_input").assertExists()
        composeRule.onNodeWithTag("use_manual_rate_checkbox").assertIsOff()
    }

    @Test
    fun meSignedIn_light_font10() = composeRule.captureR2Screen(darkTheme = false, fontScale = 1.0f) {
        MeContent(signedIn, MeActions())
    }

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun meSignedIn_dark_font10() = composeRule.captureR2Screen(darkTheme = true, fontScale = 1.0f) {
        MeContent(signedIn, MeActions())
    }

    @Test
    fun meGuest_light_font13() = composeRule.captureR2Screen(darkTheme = false, fontScale = 1.3f) {
        MeContent(guest, MeActions())
    }
}

private class FakeExchangeRateRepository(initial: BoiRate?) : ExchangeRateRepository {
    override val currentRateFlow = MutableStateFlow(initial)
    val manualRateCalls = mutableListOf<Double?>()
    var clearManualRateCalls = 0

    override suspend fun refreshRateIfNeeded(): Result<BoiRate?> = Result.success(currentRateFlow.value)

    override suspend fun setManualRate(rate: Double?) {
        yield()
        manualRateCalls += rate
        val current = currentRateFlow.value
        currentRateFlow.value = BoiRate(
            officialRate = current?.officialRate,
            officialRateDate = current?.officialRateDate,
            officialFetchedAt = current?.officialFetchedAt,
            manualRate = rate,
            isManualOverride = rate != null && rate > 0.0,
        )
    }

    override suspend fun clearManualRate() {
        yield()
        clearManualRateCalls++
        currentRateFlow.value = currentRateFlow.value?.copy(isManualOverride = false)
    }

    override suspend fun fetchHistoricalRate(date: LocalDate): Result<BoiRate> =
        Result.failure(UnsupportedOperationException())
}

private class FakeHebcalRepository : HebcalRepository {
    override val hebcalDataFlow: StateFlow<HebcalData?> = MutableStateFlow(null)
    override val selectedCityFlow: StateFlow<IsraeliCity?> = MutableStateFlow(null)
    override suspend fun selectCity(city: IsraeliCity) {}
    override suspend fun refreshDataIfNeeded(): Result<HebcalData?> = Result.success(null)
}
