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
import java.time.LocalDate

data class PeriodUi(val open: Boolean = false, val view: PeriodView? = null)

/** "סיכום תקופה": read only, over closed months. Opens on this year, or on last year in January. */
class PeriodViewModel(
    money: MoneyRepository,
    usdRates: Flow<BigDecimal?>,
    private val today: () -> LocalDate,
    errorReporter: ErrorReporter,
) : BaseViewModel(errorReporter) {

    private val open = MutableStateFlow(false)
    private val preset = MutableStateFlow(PeriodPreset.THIS_YEAR)

    val ui: StateFlow<PeriodUi> = combine(open, preset, money.observe(), usdRates) { isOpen, picked, data, rate ->
        if (!isOpen || !data.householdReady) PeriodUi(isOpen) else PeriodUi(true, periodView(data, picked, today(), rate))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PeriodUi())

    fun open() {
        preset.value = defaultPreset(today())
        open.value = true
    }

    fun close() {
        open.value = false
    }

    fun select(value: PeriodPreset) {
        preset.value = value
    }

    companion object {
        fun factory(
            money: MoneyRepository,
            usdRates: Flow<BigDecimal?>,
            today: () -> LocalDate,
            errorReporter: ErrorReporter,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PeriodViewModel(money, usdRates, today, errorReporter) as T
        }
    }
}
