package com.menash.myself.ui.money

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.SubscriptionsUi
import com.menash.myself.feature.money.domain.Frequency
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.subscriptionsView
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.YearMonth

/** "מנויים" goldens, light/dark at font 1.0 and 1.3: three listed (one in USD, one yearly) and two charges not listed yet. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class SubscriptionsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 30)
    private val october = YearMonth.of(2026, 10)
    private val subs = listOf(
        MoneySamples.netflix.copy(categoryId = "sub-2"),
        MoneySamples.chatGpt.copy(categoryId = "sub-7"),
        MoneySamples.netflix.copy(id = "sub-ins", name = "ביטוח דירה", frequency = Frequency.YEARLY,
            renewalDate = LocalDate.of(2026, 3, 1), amounts = MoneySamples.history(1_200), categoryId = null),
    )
    private val data = MoneySamples.empty.copy(items = MoneySamples.empty.items.copy(subscriptions = subs))
    private var n = 0
    private fun line(month: YearMonth, agorot: Long, name: String, category: String) = StoredTransaction(
        StatementRow("r${n++}", Source.MAX, "4521", month.atDay(12), month.atDay(12), name, charge = Money(agorot)),
        categoryId = category,
    )
    private val lines = listOf(
        line(october, 7_702, "OPENAI *CHATGPT", "sub-7"),
        line(october, 5_490, "NETFLIX.COM", "sub-2"),
        line(october.minusMonths(1), 2_990, "SPOTIFY", "sub-3"),
        line(october.minusMonths(2), 2_990, "SPOTIFY", "sub-3"),
        line(october, 18_900, "הולמס פלייס", "sub-6"),
    )

    @Test fun subscriptions_light_font10() = screen(dark = false, font = 1.0f)
    @Test fun subscriptions_light_font13() = screen(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun subscriptions_dark_font10() = screen(dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun subscriptions_dark_font13() = screen(dark = true, font = 1.3f)

    private fun screen(dark: Boolean, font: Float) {
        val ui = SubscriptionsUi(true, subscriptionsView(data, today, MoneySamples.rate, lines, ImportMemory()))
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { SubscriptionsContent(ui = ui, actions = SubscriptionsActions()) }
        }
    }
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
