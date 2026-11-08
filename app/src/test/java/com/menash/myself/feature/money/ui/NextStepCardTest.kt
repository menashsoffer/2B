package com.menash.myself.feature.money.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MonthTask
import com.menash.myself.feature.money.StatementSource
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.testing.DarkQualifiers
import com.menash.myself.testing.LightQualifiers
import com.menash.myself.testing.captureR2Screen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.YearMonth

/** "הצעד הבא": the first step one tap away, the rest folded; goldens light/dark at font 1.0 and 1.3, as first shown. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class NextStepCardTest {
    @get:Rule
    val rule = createComposeRule()

    private val october = YearMonth.of(2026, 10)
    private val november = YearMonth.of(2026, 11)
    private val tasks = listOf(
        MonthTask.MissingFile(october, StatementSource(Source.MAX, "7788")),
        MonthTask.Assign(october, 3),
        MonthTask.Assign(november, 5),
        MonthTask.Tithe(Money(42_000)),
    )

    @Test fun firstStepOpensItsScreen() {
        var opened: MonthTask? = null
        rule.setContent { MyApplicationTheme(darkTheme = false) { NextStepCard(tasks, november) { opened = it } } }
        rule.onAllNodesWithTag("next_step_0").assertCountEquals(0)
        rule.onNodeWithTag("next_step_first").performClick()
        assertEquals(tasks.first(), opened)
    }

    @Test fun moreStepsUnfold() {
        var opened: MonthTask? = null
        rule.setContent { MyApplicationTheme(darkTheme = false) { NextStepCard(tasks, november) { opened = it } } }
        rule.onNodeWithTag("next_step_more").performClick()
        rule.onNodeWithTag("next_step_2").performClick()
        assertEquals(tasks.last(), opened)
    }

    @Test fun noStepsShowsNothing() {
        rule.setContent { MyApplicationTheme(darkTheme = false) { NextStepCard(emptyList(), november) {} } }
        rule.onAllNodesWithTag("money_next_step").assertCountEquals(0)
    }

    @Test fun nextStep_light_font10() = golden(dark = false, font = 1.0f)
    @Test fun nextStep_light_font13() = golden(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun nextStep_dark_font10() = golden(dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun nextStep_dark_font13() = golden(dark = true, font = 1.3f)

    private fun golden(dark: Boolean, font: Float) {
        rule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl {
                Column(Modifier.fillMaxSize().background(MaterialTheme.appColors.background).padding(Spacing.s16)) {
                    NextStepCard(tasks, november) {}
                }
            }
        }
    }
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
