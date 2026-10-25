package com.menash.myself.feature.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.HouseholdRepositoryInterface
import com.menash.myself.core.domain.model.Household
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HouseholdViewModel(
    private val householdRepository: HouseholdRepositoryInterface
) : ViewModel() {

    private val _household = MutableStateFlow<Household?>(null)
    val household: StateFlow<Household?> = _household.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                _household.value = householdRepository.getOrCreateHousehold()
            }
            householdRepository.observeHousehold().collect { h ->
                if (h != null) _household.value = h
            }
        }
    }

    suspend fun invitePartner(householdId: String, email: String) {
        householdRepository.invitePartner(householdId, email)
    }

    companion object {
        fun factory(householdRepository: HouseholdRepositoryInterface): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HouseholdViewModel(householdRepository) as T
                }
            }
    }
}
