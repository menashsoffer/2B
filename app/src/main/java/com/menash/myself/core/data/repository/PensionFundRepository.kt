package com.menash.myself.core.data.repository

import com.menash.myself.data.entities.PensionFundEntity
import kotlinx.coroutines.flow.Flow

interface PensionFundRepository {
    fun getAllPensionFunds(): Flow<List<PensionFundEntity>>
    suspend fun getAllPensionFundsList(): List<PensionFundEntity>
    suspend fun insertPensionFund(fund: PensionFundEntity): Long
    suspend fun insertPensionFunds(funds: List<PensionFundEntity>)
    suspend fun updatePensionFund(fund: PensionFundEntity)
    suspend fun deletePensionFund(fund: PensionFundEntity)
    suspend fun deleteAllPensionFunds()
}
