package com.menash.myself.feature.calculators.pension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.repository.PensionFundRepository
import com.menash.myself.data.entities.PensionFundEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PensionCalculatorViewModel(
    private val pensionFundRepository: PensionFundRepository
) : ViewModel() {

    val funds: StateFlow<List<PensionFundEntity>> = pensionFundRepository.getAllPensionFunds()
        .map { list -> list.map { ModelMappers.pensionFundToEntity(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertPensionFund(fund: PensionFundEntity) {
        viewModelScope.launch {
            pensionFundRepository.insertPensionFund(ModelMappers.pensionFundToDomain(fund))
        }
    }

    fun insertPensionFunds(fundsList: List<PensionFundEntity>) {
        viewModelScope.launch {
            pensionFundRepository.insertPensionFunds(fundsList.map { ModelMappers.pensionFundToDomain(it) })
        }
    }

    fun updatePensionFund(fund: PensionFundEntity) {
        viewModelScope.launch {
            pensionFundRepository.updatePensionFund(ModelMappers.pensionFundToDomain(fund))
        }
    }

    fun deletePensionFund(fund: PensionFundEntity) {
        viewModelScope.launch {
            pensionFundRepository.deletePensionFund(ModelMappers.pensionFundToDomain(fund))
        }
    }

    companion object {
        fun factory(pensionFundRepository: PensionFundRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PensionCalculatorViewModel(pensionFundRepository) as T
                }
            }
    }
}
