package com.menash.myself.ui.account

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.core.data.account.AccountHousehold
import com.menash.myself.feature.account.*
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class AccountManagementScreenshotTest {
    @get:Rule val composeRule = createComposeRule()
    private fun shoot(owner: Boolean, phase: AccountPhase = AccountPhase.READY, typed: String = "") {
        val home = AccountHousehold("h", if (owner) "me" else "partner", listOf("me", "partner"), "נועה", "partner", "דניאל")
        val state = AccountManagementState("me", home, phase, AccountAction.DELETE_HOUSEHOLD, typed)
        composeRule.captureR2Screen(darkTheme = false, fontScale = 1f) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AccountManagementContent(state, {}, {}, {}, {}, {}, {}, {})
            }
        }
    }
    @Test fun managementMember_light_font10() = shoot(false)
    @Test fun managementOwner_light_font10() = shoot(true)
    // AlertDialog opens its own window, which Robolectric never reports idle; the golden shows its body.
    @Test fun typedConfirmation_light_font10() =
        composeRule.captureR2Screen(darkTheme = false, fontScale = 1f) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TypedDeletionBody("למחוק") {}
            }
        }
}
