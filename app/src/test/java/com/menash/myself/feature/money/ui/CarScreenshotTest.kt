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
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.CarCompareDraft
import com.menash.myself.feature.money.CarPlan
import com.menash.myself.feature.money.CarUi
import com.menash.myself.feature.money.DEFAULT_CAR_CATEGORIES
import com.menash.myself.feature.money.carCompare
import com.menash.myself.feature.money.carCurrent
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.testing.DarkQualifiers
import com.menash.myself.testing.LightQualifiers
import com.menash.myself.testing.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * "רכב" goldens, light/dark at font 1.0 and 1.3: a year of car lines from 7 months of files and four offers
 * (cash, loan, 0% dealer, a lease); the first-time screen; and both sheets.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class CarScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 30)
    private val data = MoneyData(householdReady = true, categories = defaultCategories())
    private var n = 0
    private fun line(month: Int, shekels: Long, category: String) = StoredTransaction(
        StatementRow(
            id = "row-${n++}", source = Source.MAX, sourceId = "1234", date = LocalDate.of(2026, month, 10),
            chargeDate = LocalDate.of(2026, month, 10), description = "line $n", charge = Money(shekels * 100), status = RowStatus.COUNTED,
        ),
        categoryId = category,
    )
    private val lines = listOf(line(3, 4_850, "periodic-8"), line(6, 1_920, "periodic-9")) +
        (3..9).map { line(it, 640, "current-3") } + (3..9).map { line(it, 95, "current-12") }
    private val draft = CarCompareDraft(
        kmPerYear = "18000", price = "142000", tradeIn = "38000", resale = "70000", lostReturn = "4",
        loanDown = "20000", loanRate = "6.5", loanMonths = "60",
        dealerList = "149900", dealerRate = "0", dealerMonths = "60",
        leaseMonthly = "2390", leaseUpfront = "15000", leaseMonths = "60", leaseKmLimit = "20000", leaseExtraKm = "0.35",
    )
    private val current = carCurrent(data, CarPlan(), lines, ImportMemory(), today)
    private val full = CarUi(true, current, CarPlan(compare = draft), carCompare(draft, current), data.categories.filter { it.parentId == null })
    private val empty = CarUi(true, carCurrent(data, CarPlan(), emptyList(), ImportMemory(), today), CarPlan(), null)

    @Test fun carOffers_light_font10() = screen(full, dark = false, font = 1.0f)
    @Test fun carOffers_light_font13() = screen(full, dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun carOffers_dark_font10() = screen(full, dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun carOffers_dark_font13() = screen(full, dark = true, font = 1.3f)

    @Test fun carEmpty_light_font10() = screen(empty, dark = false, font = 1.0f)

    @Test fun carCompareForm_light_font13() = sheet(dark = false, font = 1.3f) { CarCompareForm(draft, {}, {}) }

    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun carCategories_dark_font10() = sheet(dark = true, font = 1.0f) {
        CarCategoriesForm(full.categories, DEFAULT_CAR_CATEGORIES, {}, {})
    }

    private fun screen(ui: CarUi, dark: Boolean, font: Float) {
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { CarContent(ui = ui, actions = CarActions()) }
        }
    }

    private fun sheet(dark: Boolean, font: Float, content: @Composable () -> Unit) {
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.appColors.card)) { content() } }
        }
    }
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
