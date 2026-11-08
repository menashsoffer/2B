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
import com.menash.myself.feature.home.ui.HomeUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MonthStatus
import com.menash.myself.feature.money.MonthTask
import com.menash.myself.feature.money.MonthView
import com.menash.myself.feature.money.SourceFreshness
import com.menash.myself.feature.money.moneyOverview
import com.menash.myself.feature.money.data.TransactionRepository
import com.menash.myself.feature.money.importer.ImportMemory
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
    transactions: TransactionRepository? = null,
) : BaseViewModel(errorReporter) {

    private data class HomeMoney(val surplus: Money?, val leftForCurrent: Money?, val tasks: List<MonthTask>, val freshness: SourceFreshness?)

    /** This month's money as the month tab shows it, from the same [moneyOverview]. Null without a repository. */
    private val money: Flow<HomeMoney?> = if (moneyRepository == null) {
        flowOf(null)
    } else {
        combine(
            moneyRepository.observe(), usdRates,
            transactions?.observeTransactions() ?: flowOf(emptyList()),
            transactions?.observeMemory() ?: flowOf(ImportMemory()),
            transactions?.observeSourceClosures() ?: flowOf(emptyMap()),
        ) { data, rate, lines, memory, closures ->
            val day = today()
            val now = buildMoneyMonth(data, YearMonth.from(day), day, rate, lines, memory)
            val overview = moneyOverview(data, day, rate, lines, memory, closures, now)
            HomeMoney(
                surplus = (now.view as? MonthView.Planned)?.plan?.expectedSurplus,
                leftForCurrent = (overview.headline as? MonthStatus.Open)?.leftForCurrent,
                tasks = overview.tasks,
                freshness = overview.oldestSource,
            )
        }.map<HomeMoney, HomeMoney?> { it }.catch { error ->
            if (error is Exception) {
                handleException("observeMoneyHome", error)
                emit(null)
            } else {
                throw error
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(observeDreams(dreamRepository), money) { dreams, money ->
        HomeUiState(
            dreamsActive = dreamsActiveValue(dreams),
            moneySurplus = money?.surplus,
            moneyLeftForCurrent = money?.leftForCurrent,
            moneyTasks = money?.tasks.orEmpty(),
            moneyFreshness = money?.freshness,
        )
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
            transactions: TransactionRepository? = null,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(dreamRepository, errorReporter, moneyRepository, usdRates, today, transactions) as T
            }
        }
    }
}
