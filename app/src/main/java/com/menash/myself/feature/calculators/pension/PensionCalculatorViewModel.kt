package com.menash.myself.feature.calculators.pension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.PensionFundRepository
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.PensionFund
import com.menash.myself.core.domain.model.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PensionCalculatorViewModel(
    private val pensionFundRepository: PensionFundRepository,
    private val errorReporter: ErrorReporter? = null
) : ViewModel() {

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    val funds: StateFlow<List<PensionFund>> = pensionFundRepository.getAllPensionFunds()
        .catch { e ->
            handleException("getAllPensionFunds", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<UiState<List<PensionFund>>> = combine(funds, _errorState) { list, error ->
        when {
            error != null && list.isEmpty() -> UiState.Error(error)
            list.isEmpty() -> UiState.Empty
            else -> UiState.Content(list)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun clearError() {
        _errorState.value = null
        errorReporter?.clearError()
    }

    private fun handleException(action: String, e: Throwable) {
        if (e is CancellationException) throw e
        val msg = e.localizedMessage ?: e.message ?: "שגיאה ב-$action"
        _errorState.value = msg
        errorReporter?.reportError(action, e)
    }

    private fun launchCatching(action: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException(action, e)
            }
        }
    }

    fun insertPensionFund(fund: PensionFund) {
        launchCatching("insertPensionFund") {
            pensionFundRepository.insertPensionFund(fund)
        }
    }

    fun insertPensionFunds(fundsList: List<PensionFund>) {
        launchCatching("insertPensionFunds") {
            pensionFundRepository.insertPensionFunds(fundsList)
        }
    }

    fun updatePensionFund(fund: PensionFund) {
        launchCatching("updatePensionFund") {
            pensionFundRepository.updatePensionFund(fund)
        }
    }

    fun deletePensionFund(fund: PensionFund) {
        launchCatching("deletePensionFund") {
            pensionFundRepository.deletePensionFund(fund)
        }
    }

    companion object {
        fun factory(
            pensionFundRepository: PensionFundRepository,
            errorReporter: ErrorReporter? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PensionCalculatorViewModel(pensionFundRepository, errorReporter) as T
                }
            }
    }
}
