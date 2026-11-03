package com.menash.myself.ui.money

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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

    @Test fun zeroUnassignedIsHidden() {
        compose.setContent { MonthStatusHeadline(MonthStatus.NoCurrentBudget(Money.ZERO, Money.ZERO, null)) }
        compose.onNodeWithTag("money_status_unassigned").assertDoesNotExist()
    }
}
