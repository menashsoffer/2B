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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DreamsViewModel(
    private val dreamRepository: DreamRepository,
    private val householdRepository: HouseholdRepositoryInterface,
    private val errorReporter: ErrorReporter? = null
) : ViewModel() {

    private val _selectedListFilter = MutableStateFlow("הכל")
    val selectedListFilter: StateFlow<String> = _selectedListFilter.asStateFlow()

    private val _visibilityFilter = MutableStateFlow(VisibilityFilter.MINE_AND_BOTH)
    val visibilityFilter: StateFlow<VisibilityFilter> = _visibilityFilter.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    val household: StateFlow<Household?> = householdRepository.observeMyHousehold()
        .catch { e -> handleException("observeHousehold", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dreams: StateFlow<List<Dream>> = combine(_selectedListFilter, _visibilityFilter) { listFilter, visFilter ->
        Pair(if (listFilter == "הכל") null else listFilter, visFilter)
    }.flatMapLatest { (listFilter, visFilter) ->
        dreamRepository.observeDreams(listFilter = listFilter, filter = visFilter)
    }.catch { e ->
        handleException("observeDreams", e)
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<UiState<List<Dream>>> = combine(dreams, _errorState) { list, error ->
        when {
            error != null && list.isEmpty() -> UiState.Error(error)
            list.isEmpty() -> UiState.Empty
            else -> UiState.Content(list)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun setListFilter(listFilter: String) {
        _selectedListFilter.value = listFilter
    }

    fun setVisibilityFilter(filter: VisibilityFilter) {
        _visibilityFilter.value = filter
    }

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

    fun createDream(dream: Dream) {
        launchCatching("createDream") {
            dreamRepository.createDream(dream)
        }
    }

    fun updateDreamStatus(dream: Dream, status: String) {
        launchCatching("updateDreamStatus") {
            dreamRepository.updateDreamStatus(dream.id, status, dream.isPrivate)
        }
    }

    fun updateDreamSavings(dream: Dream, savedAmount: Money) {
        launchCatching("updateDreamSavings") {
            dreamRepository.updateDreamSavings(dream.id, savedAmount, dream.isPrivate)
        }
    }

    fun updateDreamAppIdea(dream: Dream, questions: Map<String, String>, nextAction: String) {
        launchCatching("updateDreamAppIdea") {
            dreamRepository.updateDreamAppIdea(dream.id, questions, nextAction, dream.isPrivate)
        }
    }

    fun deleteDream(dream: Dream) {
        launchCatching("deleteDream") {
            dreamRepository.deleteDream(dream.id, dream.isPrivate)
        }
    }

    companion object {
        fun factory(
            dreamRepository: DreamRepository,
            householdRepository: HouseholdRepositoryInterface,
            errorReporter: ErrorReporter? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DreamsViewModel(dreamRepository, householdRepository, errorReporter) as T
            }
        }
    }
}
