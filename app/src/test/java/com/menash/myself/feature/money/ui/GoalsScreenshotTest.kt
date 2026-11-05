package com.menash.myself.feature.money.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.GoalEditor
import com.menash.myself.feature.money.GoalsUi
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.MoneySamples.shekels
import com.menash.myself.feature.money.canWe
import com.menash.myself.feature.money.goalDraftOf
import com.menash.myself.feature.money.previewAnswer
import com.menash.myself.feature.money.domain.Goal
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.YearMonth

/**
 * "האם אפשר?" goldens, light/dark at font 1.0 and 1.3: four goals on the planned surplus, where a big renovation
 * fits only with levers and leaves the rest short; no goals yet; and the goal sheet with its live answer.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class GoalsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val october = YearMonth.of(2026, 10)
    private val laptop = Goal("goal-laptop", "מחשב נייד", shekels(4_000), YearMonth.of(2027, 4))
    private val renovation = Goal("goal-renovation", "שיפוץ מטבח", shekels(40_000), YearMonth.of(2027, 4))
    private val data = MoneySamples.filled.copy(goals = listOf(laptop, MoneySamples.trip, MoneySamples.car))
    // The renovation takes 6,667 a month, so it needs levers and leaves the others short.
    private val crowded = data.copy(goals = data.goals + renovation)

    @Test fun goalsList_light_font10() = list(dark = false, font = 1.0f)
    @Test fun goalsList_light_font13() = list(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun goalsList_dark_font10() = list(dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun goalsList_dark_font13() = list(dark = true, font = 1.3f)

    @Test fun goalsEmpty_light_font10() = empty(dark = false, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun goalsEmpty_dark_font13() = empty(dark = true, font = 1.3f)

    @Test fun goalsSheet_light_font10() = sheet(dark = false, font = 1.0f)
    @Test fun goalsSheet_light_font13() = sheet(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun goalsSheet_dark_font10() = sheet(dark = true, font = 1.0f)

    private fun list(dark: Boolean, font: Float) = screen(GoalsUi(true, canWe(crowded, october, MoneySamples.rate), october), dark, font)

    private fun empty(dark: Boolean, font: Float) =
        screen(GoalsUi(true, canWe(MoneySamples.filled, october, MoneySamples.rate), october), dark, font)

    private fun screen(ui: GoalsUi, dark: Boolean, font: Float) {
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { GoalsContent(ui = ui, actions = GoalsActions()) }
        }
    }

    private fun sheet(dark: Boolean, font: Float) {
        val goal = MoneySamples.trip
        val editor = GoalEditor(
            draft = goalDraftOf(goal),
            isNew = false,
            preview = previewAnswer(goal, data, october, MoneySamples.rate),
        )
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.appColors.card)) {
                    GoalEditorForm(editor = editor, month = october, actions = GoalEditorActions())
                }
            }
        }
    }

}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
