package com.menash.myself.core.designsystem.component

import android.content.Context
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.testing.LightQualifiers
import com.menash.myself.testing.SampleName
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Spec: ProfileAvatar contentDescription is "תפריט חשבון".
 *
 * pre-fix1: the composable sets that string as onClickLabel and clearAndSetSemantics
 * on the letter, so ContentDescription may be absent. This test stays as written.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class ProfileAvatarSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileAvatar_exposesAccountMenuContentDescription() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val description = context.getString(R.string.account_menu)
        assertEquals("תפריט חשבון", description)
        composeRule.setContent {
            MyApplicationTheme {
                ProfileAvatar(displayName = SampleName, onClick = {})
            }
        }
        composeRule.onNodeWithContentDescription(description).assertContentDescriptionEquals(description)
    }
}
