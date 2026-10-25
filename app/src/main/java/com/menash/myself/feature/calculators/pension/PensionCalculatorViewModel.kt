package com.menash.myself.feature.calculators.pension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.PensionFundRepository
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.PensionFund
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PensionCalculatorViewModel(
    private val pensionFundRepository: PensionFundRepository,
    errorReporter: ErrorReporter
) : BaseViewModel(errorReporter) {

    val funds: StateFlow<List<PensionFund>?> = pensionFundRepository.getAllPensionFunds()
        .map<List<PensionFund>, List<PensionFund>?> { it }
        .catch { e ->
            if (e is Exception) handleException("getAllPensionFunds", e) else throw e
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<UiState<List<PensionFund>>> = combine(funds, _errorState) { list, error ->
        when {
            error != null && list.isNullOrEmpty() -> UiState.Error(error)
            list == null -> UiState.Loading
            list.isEmpty() -> UiState.Empty
            else -> UiState.Content(list)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun insertPensionFund(fund: PensionFund) {
        safeLaunch("insertPensionFund") {
            pensionFundRepository.insertPensionFund(fund)
        }
    }

    fun insertPensionFunds(fundsList: List<PensionFund>) {
        safeLaunch("insertPensionFunds") {
            pensionFundRepository.insertPensionFunds(fundsList)
        }
    }

    fun updatePensionFund(fund: PensionFund) {
        safeLaunch("updatePensionFund") {
            pensionFundRepository.updatePensionFund(fund)
        }
    }

    fun deletePensionFund(fund: PensionFund) {
        safeLaunch("deletePensionFund") {
            pensionFundRepository.deletePensionFund(fund)
        }
    }

    companion object {
        fun factory(
            pensionFundRepository: PensionFundRepository,
            errorReporter: ErrorReporter
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PensionCalculatorViewModel(pensionFundRepository, errorReporter) as T
                }
            }
    }
}
