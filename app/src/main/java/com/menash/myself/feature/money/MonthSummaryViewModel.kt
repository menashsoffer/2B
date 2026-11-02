package com.menash.myself.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.feature.money.data.MoneyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.YearMonth

/** [view] is null while data loads, or when the month is not closed (any more). */
data class MonthSummaryUi(val month: YearMonth? = null, val view: MonthSummaryView? = null) {
    val open: Boolean get() = month != null
}

/** "סיכום החודש": read only, for one closed month. */
class MonthSummaryViewModel(
    money: MoneyRepository,
    usdRates: Flow<BigDecimal?>,
    errorReporter: ErrorReporter,
) : BaseViewModel(errorReporter) {

    private val month = MutableStateFlow<YearMonth?>(null)

    val ui: StateFlow<MonthSummaryUi> = combine(month, money.observe(), usdRates) { shown, data, rate ->
        if (shown == null || !data.householdReady) MonthSummaryUi(shown) else MonthSummaryUi(shown, monthSummary(data, shown, rate))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MonthSummaryUi())

    fun open(value: YearMonth) {
        month.value = value
    }

    fun close() {
        month.value = null
    }

    companion object {
        fun factory(money: MoneyRepository, usdRates: Flow<BigDecimal?>, errorReporter: ErrorReporter): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = MonthSummaryViewModel(money, usdRates, errorReporter) as T
            }
    }
}
