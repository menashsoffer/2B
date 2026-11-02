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

data class SubscriptionsUi(
    val open: Boolean = false,
    /** Null means the data has not loaded, unless loadError is set. */
    val view: SubscriptionsView? = null,
    val loadError: Boolean = false,
)

/** "מנויים": read only here; adding and editing go through the money item sheet. */
class SubscriptionsViewModel(
    money: MoneyRepository,
    transactions: TransactionRepository,
    usdRates: Flow<BigDecimal?>,
    private val today: () -> LocalDate,
    errorReporter: ErrorReporter,
) : BaseViewModel(errorReporter) {

    private val open = MutableStateFlow(false)

    private val retry = MutableStateFlow(0)
    private val inputs = screenLoad(retry) {
        combine(money.observeForScreen(), transactions.observeTransactionsForScreen(), transactions.observeMemoryForScreen(), usdRates) { data, stored, memory, rate ->
            Inputs(data, stored, memory, rate)
        }
    }
    val ui: StateFlow<SubscriptionsUi> = combine(open, inputs) { isOpen, input ->
        when (input) {
            ScreenLoad.Loading -> SubscriptionsUi(isOpen)
            ScreenLoad.Error -> SubscriptionsUi(isOpen, loadError = true)
            is ScreenLoad.Ready -> {
                val (data, stored, memory, rate) = input.value
                if (isOpen && data.householdReady) SubscriptionsUi(true, subscriptionsView(data, today(), rate, stored, memory)) else SubscriptionsUi(isOpen)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SubscriptionsUi())

    private data class Inputs(
        val data: com.menash.myself.feature.money.data.MoneyData,
        val stored: List<com.menash.myself.feature.money.importer.StoredTransaction>,
        val memory: com.menash.myself.feature.money.importer.ImportMemory,
        val rate: BigDecimal?,
    )

    fun retry() { retry.value += 1 }

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
