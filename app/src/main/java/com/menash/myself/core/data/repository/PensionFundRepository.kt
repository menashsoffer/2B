package com.menash.myself.core.data.repository

import com.menash.myself.core.domain.model.PensionFund
import kotlinx.coroutines.flow.Flow

interface PensionFundRepository {
    fun getAllPensionFunds(): Flow<List<PensionFund>>
    suspend fun getAllPensionFundsList(): List<PensionFund>
    suspend fun insertPensionFund(fund: PensionFund): Long
    suspend fun insertPensionFunds(funds: List<PensionFund>)
    suspend fun updatePensionFund(fund: PensionFund)
    suspend fun deletePensionFund(fund: PensionFund)
    suspend fun deleteAllPensionFunds()
}
