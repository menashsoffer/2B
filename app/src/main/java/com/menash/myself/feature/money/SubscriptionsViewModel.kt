package com.menash.myself.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.feature.money.data.MoneyRepository
import com.menash.myself.feature.money.data.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.LocalDate

data class SubscriptionsUi(val open: Boolean = false, val view: SubscriptionsView? = null)

/** "מנויים": read only here; adding and editing go through the money item sheet. */
class SubscriptionsViewModel(
    money: MoneyRepository,
    transactions: TransactionRepository,
    usdRates: Flow<BigDecimal?>,
    private val today: () -> LocalDate,
    errorReporter: ErrorReporter,
) : BaseViewModel(errorReporter) {

    private val open = MutableStateFlow(false)

    val ui: StateFlow<SubscriptionsUi> = combine(
        open, money.observe(), transactions.observeTransactions(), transactions.observeMemory(), usdRates,
    ) { isOpen, data, stored, memory, rate ->
        if (!isOpen || !data.householdReady) SubscriptionsUi(isOpen)
        else SubscriptionsUi(true, subscriptionsView(data, today(), rate, stored, memory))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SubscriptionsUi())

    fun open() {
        open.value = true
    }

    fun close() {
        open.value = false
    }

    companion object {
        fun factory(
            money: MoneyRepository,
            transactions: TransactionRepository,
            usdRates: Flow<BigDecimal?>,
            today: () -> LocalDate,
            errorReporter: ErrorReporter,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SubscriptionsViewModel(money, transactions, usdRates, today, errorReporter) as T
        }
    }
}
