package com.menash.myself.core.data.repository

import com.menash.myself.core.external.hebcal.HebcalData
import com.menash.myself.core.external.hebcal.IsraeliCity
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for accessing Hebrew date, upcoming holidays, and Shabbat times from Hebcal.
 */
interface HebcalRepository {
    val hebcalDataFlow: StateFlow<HebcalData?>
    val selectedCityFlow: StateFlow<IsraeliCity?>
    suspend fun selectCity(city: IsraeliCity)
    suspend fun refreshDataIfNeeded(): Result<HebcalData?>
}
