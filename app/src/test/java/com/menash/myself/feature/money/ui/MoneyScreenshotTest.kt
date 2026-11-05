package com.menash.myself.feature.money.ui

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.MonthSnapshot
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.StoredTransaction
import java.time.LocalDate
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
import com.menash.myself.feature.money.EditKind
import com.menash.myself.feature.money.EditorState
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.MonthCellState
import com.menash.myself.feature.money.MonthPickerCell
import com.menash.myself.feature.money.MonthStatus
import com.menash.myself.feature.money.MoneyScreenState
import com.menash.myself.feature.money.buildMoneyMonth
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.draftPreview
import com.menash.myself.feature.money.newDraft
import com.menash.myself.testing.DarkQualifiers
import com.menash.myself.testing.LightQualifiers
import com.menash.myself.testing.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Money tab goldens, light/dark at font 1.0 and 1.3: month empty, month filled
 * (two cards, an installment, a periodic budget, a debt), big picture, and the add-item sheet body.
 * Data is [MoneySamples] on 2026-10-27; every number on screen comes from the engine via buildMoneyMonth.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class MoneyScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun monthPicker_dark() {
        val months = (0L..36L).map { MonthPickerCell(MoneySamples.month.minusMonths(it),
            if (it == 1L) MonthCellState.CLOSED else if (it == 0L) MonthCellState.IMPORTED else MonthCellState.EMPTY, it == 0L) }
        composeRule.captureR2Screen(darkTheme = true, fontScale = 1.0f) {
            Rtl { MonthPickerGrid(months, onSelect = {}) }
        }
    }

    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun noCurrentBudget_dark() {
        composeRule.captureR2Screen(darkTheme = true, fontScale = 1.0f) {
            Rtl { MonthStatusHeadline(MonthStatus.NoCurrentBudget(Money(713_200), Money(612_000), null)) }
        }
    }

    @Test fun moneyMonthEmpty_light_font10() = month(MoneySamples.empty, dark = false, font = 1.0f)
    @Test fun moneyMonthEmpty_light_font13() = month(MoneySamples.empty, dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun moneyMonthEmpty_dark_font10() = month(MoneySamples.empty, dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun moneyMonthEmpty_dark_font13() = month(MoneySamples.empty, dark = true, font = 1.3f)

    @Test fun moneyMonthFilled_light_font10() = month(MoneySamples.filled, dark = false, font = 1.0f)
    @Test fun moneyMonthFilled_light_font13() = month(MoneySamples.filled, dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun moneyMonthFilled_dark_font10() = month(MoneySamples.filled, dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun moneyMonthFilled_dark_font13() = month(MoneySamples.filled, dark = true, font = 1.3f)

    @Test fun moneyBigPicture_light_font10() = month(MoneySamples.filled, dark = false, font = 1.0f, tab = MoneyTabBigPicture)
    @Test fun moneyBigPicture_light_font13() = month(MoneySamples.filled, dark = false, font = 1.3f, tab = MoneyTabBigPicture)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun moneyBigPicture_dark_font10() = month(MoneySamples.filled, dark = true, font = 1.0f, tab = MoneyTabBigPicture)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun moneyBigPicture_dark_font13() = month(MoneySamples.filled, dark = true, font = 1.3f, tab = MoneyTabBigPicture)

    @Test fun money_actual_only_light() = month(MoneySamples.empty.copy(categories = MoneySamples.filled.categories), dark = false, font = 1.0f, lines = octoberLines())

    @Test fun moneyMonthImported_light_font10() = month(MoneySamples.filled, dark = false, font = 1.0f, lines = octoberLines())
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun moneyMonthImported_dark_font13() = month(MoneySamples.filled, dark = true, font = 1.3f, lines = octoberLines())
    @Test fun moneyMonthClosed_light_font10() = month(MoneySamples.filled.copy(closedMonths = listOf(octoberClose)), dark = false, font = 1.0f)

    @Test fun moneyAddSheet_light_font10() = sheet(dark = false, font = 1.0f)
    @Test fun moneyAddSheet_light_font13() = sheet(dark = false, font = 1.3f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun moneyAddSheet_dark_font10() = sheet(dark = true, font = 1.0f)
    @Test @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun moneyAddSheet_dark_font13() = sheet(dark = true, font = 1.3f)

    private fun octoberLines() = ImportFixtures.parse("hapoalim_oct.xlsx").rows.map { StoredTransaction(it) }

    private val octoberClose = MoneySamples.month.let { m ->
        ClosedMonth(m, MonthSnapshot(m, Money(1_517_300), Money(1_281_000)), emptyMap(), emptyMap(), emptyMap(), emptyMap(), LocalDate.of(2026, 11, 2))
    }

    private fun month(data: MoneyData, dark: Boolean, font: Float, tab: Int = MoneyTabMonth, lines: List<StoredTransaction> = emptyList()) {
        val state = MoneyScreenState(
            money = buildMoneyMonth(data, MoneySamples.month, MoneySamples.today, MoneySamples.rate, lines),
            data = data,
        )
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl { MoneyContent(state = state, selectedTab = tab, expanded = emptySet(), actions = MoneyActions()) }
        }
    }

    private fun sheet(dark: Boolean, font: Float) {
        val draft = newDraft(EditKind.FIXED, MoneySamples.month, MoneySamples.today, MoneySamples.visa.id)
            .copy(name = "ביטוח דירה", amount = "180", categoryId = "fixed-5")
        val editor = EditorState(draft = draft, isNew = true)
        composeRule.captureR2Screen(darkTheme = dark, fontScale = font) {
            Rtl {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.appColors.card)) {
                    MoneyEditorForm(
                        editor = editor,
                        choices = EditorChoices(
                            cards = MoneySamples.filled.cards,
                            categories = MoneySamples.filled.categories,
                            preview = draftPreview(draft, MoneySamples.month),
                        ),
                        actions = EditorActions(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}
