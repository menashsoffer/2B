package com.menash.myself.feature.money.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MonthStatus
import com.menash.myself.feature.money.importer.LineFilter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MonthStatusHeadlineTest {
    @get:Rule val compose = createComposeRule()

    @Test fun positiveUnassignedOpensFilteredTransactions() {
        var selected: LineFilter? = null
        compose.setContent {
            MonthStatusHeadline(MonthStatus.NoCurrentBudget(Money(713_200), Money(612_000), null),
                onTransactions = { selected = it })
        }
        compose.onNodeWithTag("money_status_unassigned").performClick()
        assertEquals(LineFilter.UNASSIGNED, selected)
    }

    @Test fun openMonthUnassignedOpensFilteredTransactions() {
        var selected: LineFilter? = null
        compose.setContent {
            MonthStatusHeadline(MonthStatus.Open(Money(100), Money(200), Money(100), Money.ZERO,
                Money(100), null, Money(50)), onTransactions = { selected = it })
        }
        compose.onNodeWithTag("money_status_unassigned").performClick()
        assertEquals(LineFilter.UNASSIGNED, selected)
    }

    @Test fun zeroUnassignedIsHidden() {
        compose.setContent { MonthStatusHeadline(MonthStatus.NoCurrentBudget(Money.ZERO, Money.ZERO, null)) }
        compose.onNodeWithTag("money_status_unassigned").assertDoesNotExist()
    }

    @Test fun monthClosePreviousDisabledAtEarliestBoundary() {
        val month = java.time.YearMonth.of(2025, 11)
        val today = java.time.LocalDate.of(2026, 11, 3)
        val data = com.menash.myself.feature.money.data.MoneyData(householdReady = true)
        val view = com.menash.myself.feature.money.closeView(data, month, today, null, emptyList(), com.menash.myself.feature.money.importer.ImportMemory())
        compose.setContent {
            com.menash.myself.core.designsystem.theme.MyApplicationTheme {
                MonthCloseContent(
                    ui = com.menash.myself.feature.money.MonthCloseUi(open = true, view = view, currentMonth = java.time.YearMonth.from(today), earliestMonth = month),
                    actions = MonthCloseActions(),
                )
            }
        }
        compose.onNodeWithTag("close_previous").assertIsNotEnabled()
    }

    @Test fun emptyAndActualOnlyPastMonthsExposeCloseMonthTag() {
        val current = java.time.YearMonth.of(2026, 11)
        val past = current.minusMonths(1)
        val big = com.menash.myself.feature.money.BigPicture(null, emptyList(), null, emptyList(), emptyList())
        val data = com.menash.myself.feature.money.data.MoneyData(householdReady = true)
        var closeClicks = 0

        val state = androidx.compose.runtime.mutableStateOf(
            com.menash.myself.feature.money.MoneyScreenState(
                money = com.menash.myself.feature.money.MoneyMonth(past, com.menash.myself.feature.money.MonthView.Empty(), big, current = current),
                data = data,
            )
        )
        compose.setContent {
            com.menash.myself.core.designsystem.theme.MyApplicationTheme {
                MoneyContent(state.value, MoneyTabMonth, emptySet(), MoneyActions(onCloseMonth = { closeClicks++ }))
            }
        }
        compose.onNodeWithTag("money_close_month").performScrollTo().performClick()
        assertEquals(1, closeClicks)

        state.value = state.value.copy(
            money = com.menash.myself.feature.money.MoneyMonth(
                past,
                com.menash.myself.feature.money.MonthView.ActualOnly(Money.ZERO, Money(500), null, 1, com.menash.myself.feature.money.CategoryTop(emptyList(), 0), false),
                big,
                current = current,
            )
        )
        compose.onNodeWithTag("money_close_month").performScrollTo().performClick()
        assertEquals(2, closeClicks)
        compose.onNodeWithTag("money_actual_saving").performScrollTo().assertExists()
    }
}
