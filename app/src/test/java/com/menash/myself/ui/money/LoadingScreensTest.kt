package com.menash.myself.ui.money

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.menash.myself.feature.money.MonthCloseUi
import com.menash.myself.feature.money.SubscriptionsUi
import com.menash.myself.feature.money.TransactionsQuery
import com.menash.myself.feature.money.TransactionsUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LoadingScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun transactionsInitiallyShowsLoading() {
        compose.setContent { TransactionsContent(TransactionsUi(query = TransactionsQuery(month = YearMonth.of(2026, 11))), TransactionsActions()) }
        compose.onNodeWithTag("tx_loading").assertExists()
        compose.onNodeWithTag("tx_empty").assertDoesNotExist()
    }

    @Test fun monthCloseInitiallyShowsLoading() {
        compose.setContent { MonthCloseContent(MonthCloseUi(open = true), MonthCloseActions()) }
        compose.onNodeWithTag("close_loading").assertExists()
        compose.onNodeWithTag("close_checks").assertDoesNotExist()
    }

    @Test fun subscriptionsInitiallyShowsLoading() {
        compose.setContent { SubscriptionsContent(SubscriptionsUi(open = true), SubscriptionsActions()) }
        compose.onNodeWithTag("subs_loading").assertExists()
        compose.onNodeWithTag("subs_rows").assertDoesNotExist()
    }
}
