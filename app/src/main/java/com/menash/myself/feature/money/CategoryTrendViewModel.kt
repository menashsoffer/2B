package com.menash.myself.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.core.ui.ErrorMapper
import com.menash.myself.core.ui.UiEvent
import com.menash.myself.core.ui.UiEvents
import com.menash.myself.feature.money.data.MoneyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CategoryTrendUi(
    val categoryId: String? = null,
    val view: CategoryTrendView? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
) {
    val open: Boolean get() = categoryId != null
}

private data class TrendQuery(val categoryId: String? = null, val saving: Boolean = false, val saveError: String? = null)

/** "מגמה": one category over twelve months, and the suggested plan in one tap. */
class CategoryTrendViewModel(
    private val money: MoneyRepository,
    usdRates: Flow<BigDecimal?>,
    private val today: () -> LocalDate,
    errorReporter: ErrorReporter,
    private val events: UiEvents? = null,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : BaseViewModel(errorReporter) {

    private val query = MutableStateFlow(TrendQuery())
    private var saving = false

    val ui: StateFlow<CategoryTrendUi> = combine(query, money.observe(), usdRates) { q, data, rate ->
        val id = q.categoryId
        if (id == null || !data.householdReady) CategoryTrendUi(id, saving = q.saving, saveError = q.saveError)
        else CategoryTrendUi(id, categoryTrend(data, id, today(), rate), q.saving, q.saveError)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CategoryTrendUi())

    fun open(categoryId: String) {
        query.value = TrendQuery(categoryId, saving = saving)
    }

    fun close() {
        query.value = TrendQuery()
    }

    /** Sets the category's plan to the suggestion from this month on. Waits up to 3s for the server, then trusts the cache. */
    fun acceptSuggestion() {
        if (saving) return
        val view = ui.value.view ?: return
        val amount = view.suggested ?: return
        val target = view.target ?: return
        val entry = planChange(target, view, amount, newId) ?: return
        saving = true
        query.update { it.copy(saving = true, saveError = null) }
        safeLaunch("acceptPlanSuggestion", onError = { error ->
            saving = false
            val message = ErrorMapper.toFriendlyMessage(error)
            query.update { it.copy(saving = false, saveError = message) }
            events?.send(UiEvent.SaveError(message) { acceptSuggestion() })
        }, reportError = false) {
            val acknowledged = withTimeoutOrNull(3_000) { money.save(entry); true } == true
            saving = false
            query.update { it.copy(saving = false) }
            events?.send(if (acknowledged) UiEvent.MONEY_SAVED else UiEvent.SavedOffline)
        }
    }

    companion object {
        fun factory(
            money: MoneyRepository,
            usdRates: Flow<BigDecimal?>,
            today: () -> LocalDate,
            errorReporter: ErrorReporter,
            events: UiEvents? = null,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CategoryTrendViewModel(money, usdRates, today, errorReporter, events) as T
        }
    }
}
