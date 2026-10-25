package com.menash.myself.feature.dreams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.core.ui.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class DreamsViewModel(
    private val dreamRepository: DreamRepository,
    private val householdRepository: HouseholdRepositoryInterface,
    errorReporter: ErrorReporter
) : BaseViewModel(errorReporter) {

    private val _selectedListFilter = MutableStateFlow("הכל")
    val selectedListFilter: StateFlow<String> = _selectedListFilter.asStateFlow()

    private val _visibilityFilter = MutableStateFlow(VisibilityFilter.MINE_AND_BOTH)
    val visibilityFilter: StateFlow<VisibilityFilter> = _visibilityFilter.asStateFlow()

    val household: StateFlow<Household?> = householdRepository.observeMyHousehold()
        .catch { e -> if (e is Exception) handleException("observeHousehold", e) else throw e }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dreams: StateFlow<List<Dream>?> = combine(_selectedListFilter, _visibilityFilter) { listFilter, visFilter ->
        Pair(if (listFilter == "הכל") null else listFilter, visFilter)
    }.flatMapLatest { (listFilter, visFilter) ->
        dreamRepository.observeDreams(listFilter = listFilter, filter = visFilter)
            .map<List<Dream>, List<Dream>?> { it }
    }.catch { e ->
        if (e is Exception) handleException("observeDreams", e) else throw e
        emit(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<UiState<List<Dream>>> = combine(dreams, _errorState) { list, error ->
        when {
            error != null && list.isNullOrEmpty() -> UiState.Error(error)
            list == null -> UiState.Loading
            list.isEmpty() -> UiState.Empty
            else -> UiState.Content(list)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun setListFilter(listFilter: String) {
        _selectedListFilter.value = listFilter
    }

    fun setVisibilityFilter(filter: VisibilityFilter) {
        _visibilityFilter.value = filter
    }

    fun createDream(dream: Dream) {
        safeLaunch("createDream") {
            dreamRepository.createDream(dream)
        }
    }

    fun updateDreamStatus(dream: Dream, status: String) {
        safeLaunch("updateDreamStatus") {
            dreamRepository.updateDreamStatus(dream.id, status, dream.isPrivate)
        }
    }

    fun updateDreamSavings(dream: Dream, savedAmount: Money) {
        safeLaunch("updateDreamSavings") {
            dreamRepository.updateDreamSavings(dream.id, savedAmount, dream.isPrivate)
        }
    }

    fun updateDreamAppIdea(dream: Dream, questions: Map<String, String>, nextAction: String) {
        safeLaunch("updateDreamAppIdea") {
            dreamRepository.updateDreamAppIdea(dream.id, questions, nextAction, dream.isPrivate)
        }
    }

    fun deleteDream(dream: Dream) {
        safeLaunch("deleteDream") {
            dreamRepository.deleteDream(dream.id, dream.isPrivate)
        }
    }

    companion object {
        fun factory(
            dreamRepository: DreamRepository,
            householdRepository: HouseholdRepositoryInterface,
            errorReporter: ErrorReporter
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DreamsViewModel(dreamRepository, householdRepository, errorReporter) as T
            }
        }
    }
}
