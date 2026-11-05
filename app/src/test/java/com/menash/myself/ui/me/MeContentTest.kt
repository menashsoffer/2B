package com.menash.myself.ui.me

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.SampleName
import com.menash.myself.r2.captureR2Screen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
