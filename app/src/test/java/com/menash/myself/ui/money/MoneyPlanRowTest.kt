package com.menash.myself.ui.money

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.MonthView
import com.menash.myself.feature.money.buildMoneyMonth
import com.menash.myself.feature.money.data.MoneyEntry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MoneyPlanRowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun expandedSubscriptionWithoutExchangeRateCanBeEdited() {
        val view = buildMoneyMonth(MoneySamples.filled, MoneySamples.month, MoneySamples.today, null).view as MonthView.Planned
        val row = view.rows.first { row -> row.items.any { it.entry.id == "sub-gpt" } }
        var selected: MoneyEntry? = null
        compose.setContent { PlanRow(row, true, MoneyActions(onEdit = { selected = it })) }
        compose.onNodeWithTag("money_item_sub-gpt").performClick()
        assertEquals("sub-gpt", selected?.id)
    }
}
