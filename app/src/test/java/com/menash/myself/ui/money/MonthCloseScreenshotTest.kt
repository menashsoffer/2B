package com.menash.myself.ui.money

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.CloseBalanceDraft
import com.menash.myself.feature.money.MonthCloseUi
import com.menash.myself.feature.money.closeView
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
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

/**
 * "סגירת החודש" goldens, light/dark at font 1.0 and 1.3: October with everything still to do (a missing card,
 * two unassigned lines, a pending charge, a stale balance), October ready, and October already closed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class MonthCloseScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val october = YearMonth.of(2026, 10)
    private val today = LocalDate.of(2026, 11, 2)
    private val bank = Asset("asset-bank", "עו\"ש", "bank", Money(3_450_000), AssetKind.ASSET, true, LocalDate.of(2026, 11, 1))
    private val fund = Asset("asset-fund", "קרן השתלמות", "fund", Money(8_000_000), AssetKind.ASSET, false, LocalDate.of(2026, 9, 1))
    private val data = MoneyData(householdReady = true, assets = listOf(bank), categories = defaultCategories())

    private fun line(id: String, day: Int, agorot: Long, name: String, category: String? = "current-1", card: String = "4521",
                     month: YearMonth = october, status: RowStatus = RowStatus.COUNTED) = StoredTransaction(
        StatementRow(id, Source.MAX, card, month.atDay(day), month.atDay(day), name, charge = Money(agorot), status = status),
        categoryId = category,
    )
    private val salary = StoredTransaction(
        StatementRow("in-1", Source.HAPOALIM, "0042", october.atDay(10), null, "משכורת", charge = Money(-1_724_000), operation = "משכורת"),
    )
    private val ready = listOf(
        salary,
        line("a", 3, 45_230, "שופרסל דיל"), line("b", 12, 18_900, "סופר יודה"), line("c", 21, 120_000, "פז דלק", "current-2"),
    )
    private val todo = ready + listOf(
        line("d", 25, 8_900, "אמזון", category = null), line("e", 27, 4_500, "קפה נחת", category = null),
        line("f", 30, 12_000, "איקאה", status = RowStatus.PENDING),
        line("g", 14, 9_900, "כאל", card = "7788", month = october.minusMonths(1)),
    )

    @Test fun closeTodo_light_font10() = screen(todo, data.copy(assets = listOf(bank, fund)), dark = false, font = 1.0f)
    @Test fun closeTodo_light_font13() = screen(todo, data.copy(assets = listOf(bank, fund)), dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun closeTodo_dark_font10() = screen(todo, data.copy(assets = listOf(bank, fund)), dark = true, font = 1.0f)

    @Test fun closeReady_light_font10() = screen(ready, data, dark = false, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun closeReady_dark_font13() = screen(ready, data, dark = true, font = 1.3f)

    @Test fun closeDone_light_font13() = done(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun closeDone_dark_font10() = done(dark = true, font = 1.0f)

    /** September closed in November: the following month is over, so the balances are typed for September. */
    @Test fun closeHistorical_light_font10() {
        val september = october.minusMonths(1)
        val d = data.copy(assets = listOf(bank, fund))
        val draft = CloseBalanceDraft(entering = true, values = mapOf(bank.id to "31250"))
        val stored = listOf(line("s", 8, 30_000, "שופרסל דיל", month = september))
        val ui = MonthCloseUi(
            true, closeView(d, september, today, null, stored, ImportMemory(), pastBalances = draft.balances(d.assets)),
            currentMonth = YearMonth.from(today), showBalancesStep = true, assets = d.assets, balanceDraft = draft,
        )
        composeRule.captureR2Screen(darkTheme = false, fontScale = 1.0f) {
            Rtl { MonthCloseContent(ui = ui, actions = MonthCloseActions()) }
        }
    }

    private fun done(dark: Boolean, font: Float) {
        val preview = closeView(data, october, today, null, ready, ImportMemory()).preview
        screen(ready, data.copy(closedMonths = listOf(preview)), dark, font)
    }

    private fun screen(lines: List<StoredTransaction>, d: MoneyData, dark: Boolean, font: Float) {
        val ui = MonthCloseUi(true, closeView(d, october, today, null, lines, ImportMemory()), currentMonth = YearMonth.from(today))
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { MonthCloseContent(ui = ui, actions = MonthCloseActions()) }
        }
    }
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
