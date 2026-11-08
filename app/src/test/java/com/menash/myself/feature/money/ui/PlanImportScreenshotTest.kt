package com.menash.myself.feature.money.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.feature.money.ImportUi
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.draftPlanImport
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.readPaamonimPlan
import com.menash.myself.feature.money.importer.readWorkbook
import com.menash.myself.testing.DarkQualifiers
import com.menash.myself.testing.LightQualifiers
import com.menash.myself.testing.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.YearMonth

/**
 * Paamonim plan import goldens, light/dark at font 1.0 and 1.3: the synthetic רישומון against the sample
 * household (changed, new, already-listed subscriptions) with one line unticked, and the done screen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class PlanImportScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var ids = 0
    private val draft = draftPlanImport(
        requireNotNull(readPaamonimPlan(readWorkbook(ImportFixtures.bytes("paamonim_plan.xls")))),
        MoneySamples.filled, YearMonth.of(2026, 11), { "id-${ids++}" },
    )
    private val review = ImportUi.PlanReview(draft, draft.selectableKeys - draft.lines.first { it.name == "שכר דירה" }.key)

    @Test fun planReview_light_font10() = capture(review, dark = false, font = 1.0f)
    @Test fun planReview_light_font13() = capture(review, dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun planReview_dark_font10() = capture(review, dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun planReview_dark_font13() = capture(review, dark = true, font = 1.3f)

    @Test fun planDone_light_font10() = capture(ImportUi.PlanDone(31, YearMonth.of(2026, 11)), dark = false, font = 1.0f)

    private fun capture(ui: ImportUi, dark: Boolean, font: Float) {
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { ImportContent(ui = ui, actions = ImportActions()) }
        }
    }
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
