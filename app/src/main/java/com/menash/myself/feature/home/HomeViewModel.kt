package com.menash.myself.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.core.domain.model.dreamsActiveValue
import com.menash.myself.core.ui.BaseViewModel
import com.menash.myself.ui.home.HomeUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MonthView
import com.menash.myself.feature.money.buildMoneyMonth
import com.menash.myself.feature.money.data.MoneyRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class HomeViewModel(
    dreamRepository: DreamRepository,
    errorReporter: ErrorReporter,
    moneyRepository: MoneyRepository? = null,
    usdRates: Flow<BigDecimal?> = flowOf(null),
    today: () -> LocalDate = { LocalDate.now() },
) : BaseViewModel(errorReporter) {

    /** The expected surplus when the month is planned, otherwise null (no invented 0). */
    private val surplus: Flow<Money?> = if (moneyRepository == null) {
        flowOf(null)
    } else {
        combine(moneyRepository.observe(), usdRates) { data, rate ->
            val day = today()
            (buildMoneyMonth(data, YearMonth.from(day), day, rate).view as? MonthView.Planned)?.plan?.expectedSurplus
        }.catch { error ->
            if (error is Exception) {
                handleException("observeMoneyHome", error)
                emit(null)
            } else {
                throw error
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(observeDreams(dreamRepository), surplus) { dreams, money ->
        HomeUiState(dreamsActive = dreamsActiveValue(dreams), moneySurplus = money)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun observeDreams(dreamRepository: DreamRepository): Flow<UiState<List<Dream>>> {
        return dreamRepository.observeDreams(listFilter = null, filter = VisibilityFilter.MINE_AND_BOTH)
            .map { dreams ->
                if (dreams.isEmpty()) UiState.Empty else UiState.Content(dreams)
            }
            .catch { error ->
                if (error is Exception) {
                    handleException("observeDreams", error)
                    emit(UiState.Error(errorState.value.orEmpty()))
                } else {
                    throw error
                }
            }
    }

    companion object {
        fun factory(
            dreamRepository: DreamRepository,
            errorReporter: ErrorReporter,
            moneyRepository: MoneyRepository,
            usdRates: Flow<BigDecimal?>,
            today: () -> LocalDate,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(dreamRepository, errorReporter, moneyRepository, usdRates, today) as T
            }
        }
    }
}
