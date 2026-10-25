package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.repository.HebcalRepository
import com.menash.myself.core.external.hebcal.HebcalClient
import com.menash.myself.core.external.hebcal.HebcalData
import com.menash.myself.core.external.hebcal.HebcalStore
import com.menash.myself.core.external.hebcal.IsraeliCity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Implementation of HebcalRepository backed by HebcalStore DataStore and HebcalClient REST API.
 */
class HebcalRepositoryImpl(
    private val store: HebcalStore,
    private val client: HebcalClient,
    externalScope: CoroutineScope
) : HebcalRepository {

    override val hebcalDataFlow: StateFlow<HebcalData?> = store.hebcalDataFlow
        .stateIn(externalScope, SharingStarted.Eagerly, null)

    override val selectedCityFlow: StateFlow<IsraeliCity?> = store.selectedCityFlow
        .stateIn(externalScope, SharingStarted.Eagerly, null)

    override suspend fun selectCity(city: IsraeliCity) {
        store.setSelectedCity(city)
        refreshForCity(city)
    }

    override suspend fun refreshDataIfNeeded(): Result<HebcalData?> {
        val city = selectedCityFlow.value ?: return Result.success(null)
        val current = hebcalDataFlow.value
        if (current == null || current.isStale || current.items.isEmpty()) {
            return refreshForCity(city)
        }
        return Result.success(current)
    }

    private suspend fun refreshForCity(city: IsraeliCity): Result<HebcalData?> {
        val res = client.fetchCalendarData(city.geonameid, city.nameHebrew)
        res.getOrNull()?.let { store.saveCalendarData(it) }
        return res
    }
}
