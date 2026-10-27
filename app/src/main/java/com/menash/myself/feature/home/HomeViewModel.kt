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

class HomeViewModel(
    dreamRepository: DreamRepository,
    errorReporter: ErrorReporter,
) : BaseViewModel(errorReporter) {

    val uiState: StateFlow<HomeUiState> = observeDreams(dreamRepository)
        .map { dreams -> HomeUiState(dreamsActive = dreamsActiveValue(dreams)) }
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
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(dreamRepository, errorReporter) as T
            }
        }
    }
}
