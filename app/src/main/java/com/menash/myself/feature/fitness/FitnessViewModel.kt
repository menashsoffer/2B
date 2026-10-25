package com.menash.myself.feature.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.FitnessRepository
import com.menash.myself.core.domain.model.FitnessEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FitnessViewModel(
    private val fitnessRepository: FitnessRepository
) : ViewModel() {

    val todayFitness: StateFlow<FitnessEntry?> = fitnessRepository.observeTodayFitness()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weeklyFitness: StateFlow<List<FitnessEntry>> = fitnessRepository.observeWeeklyFitness()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTodayFitness(moved: Boolean, minutes: Int, type: String) {
        viewModelScope.launch {
            fitnessRepository.saveTodayFitness(moved = moved, minutes = minutes, type = type)
        }
    }

    companion object {
        fun factory(fitnessRepository: FitnessRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FitnessViewModel(fitnessRepository) as T
                }
            }
    }
}
