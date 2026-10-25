package com.menash.myself.feature.dreams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.Household
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.VisibilityFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DreamsViewModel(
    private val dreamRepository: DreamRepository,
    private val householdRepository: HouseholdRepositoryInterface
) : ViewModel() {

    private val _listFilter = MutableStateFlow<String?>(null)
    val listFilter: StateFlow<String?> = _listFilter.asStateFlow()

    private val _visibilityFilter = MutableStateFlow(VisibilityFilter.ALL)
    val visibilityFilter: StateFlow<VisibilityFilter> = _visibilityFilter.asStateFlow()

    val household: StateFlow<Household?> = householdRepository.observeHousehold()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dreams: StateFlow<List<Dream>> = combine(_listFilter, _visibilityFilter) { list, vis ->
        list to vis
    }.flatMapLatest { (list, vis) ->
        dreamRepository.observeDreams(listFilter = list, filter = vis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setListFilter(list: String?) {
        _listFilter.value = list
    }

    fun setVisibilityFilter(filter: VisibilityFilter) {
        _visibilityFilter.value = filter
    }

    fun createDream(dream: Dream) {
        viewModelScope.launch {
            dreamRepository.createDream(dream)
        }
    }

    fun updateDreamStatus(dream: Dream, newStatus: String) {
        viewModelScope.launch {
            dreamRepository.updateDreamStatus(dream.id, newStatus, dream.isPrivate)
        }
    }

    fun addDreamSavings(dream: Dream, additionalShekels: Double) {
        viewModelScope.launch {
            val newSaved = dream.savedAmount + Money.fromShekels(additionalShekels)
            dreamRepository.updateDreamSavings(dream.id, newSaved, dream.isPrivate)
        }
    }

    fun updateDreamAppIdea(dream: Dream, questions: Map<String, String>, nextAction: String) {
        viewModelScope.launch {
            dreamRepository.updateDreamAppIdea(dream.id, questions, nextAction, dream.isPrivate)
        }
    }

    fun deleteDream(dream: Dream) {
        viewModelScope.launch {
            dreamRepository.deleteDream(dream.id, dream.isPrivate)
        }
    }

    companion object {
        fun factory(
            dreamRepository: DreamRepository,
            householdRepository: HouseholdRepositoryInterface
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DreamsViewModel(dreamRepository, householdRepository) as T
            }
        }
    }
}
