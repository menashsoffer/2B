package com.menash.myself.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.data.repository.InboxRepository
import com.menash.myself.core.di.AppContainer
import com.menash.myself.core.domain.model.Household
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val appContainer: AppContainer,
    private val householdRepository: HouseholdRepositoryInterface,
    private val inboxRepository: InboxRepository
) : ViewModel() {

    private val _household = MutableStateFlow<Household?>(null)
    val household: StateFlow<Household?> = _household.asStateFlow()

    val activeError: StateFlow<String?> = appContainer.errorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun initializeHouseholdForUser() {
        viewModelScope.launch {
            runCatching {
                _household.value = householdRepository.getOrCreateHousehold()
            }
            householdRepository.observeHousehold().collect { h ->
                if (h != null) _household.value = h
            }
        }
    }

    fun clearErrors() {
        appContainer.clearErrors()
    }

    fun addQuickCapture(text: String, space: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val (forWhom, isPrivate) = ModelMappers.legacySpaceToForWhom(space)
            inboxRepository.addInboxItem(text = text, forWhom = forWhom, isPrivate = isPrivate)
            onDone()
        }
    }

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(
                        appContainer = appContainer,
                        householdRepository = appContainer.householdRepository,
                        inboxRepository = appContainer.inboxRepository
                    ) as T
                }
            }
    }
}
