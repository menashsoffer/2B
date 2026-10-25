package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.repository.PensionFundRepository
import com.menash.myself.data.dao.AppDao
import com.menash.myself.data.entities.PensionFundEntity
import kotlinx.coroutines.flow.Flow

class RoomPensionFundRepository(private val dao: AppDao) : PensionFundRepository {
    override fun getAllPensionFunds(): Flow<List<PensionFundEntity>> = dao.getAllPensionFundsFlow()

    override suspend fun getAllPensionFundsList(): List<PensionFundEntity> = dao.getAllPensionFunds()

    override suspend fun insertPensionFund(fund: PensionFundEntity): Long = dao.insertPensionFund(fund)

    override suspend fun insertPensionFunds(funds: List<PensionFundEntity>) = dao.insertPensionFunds(funds)

    override suspend fun updatePensionFund(fund: PensionFundEntity) = dao.updatePensionFund(fund)

    override suspend fun deletePensionFund(fund: PensionFundEntity) = dao.deletePensionFund(fund)

    override suspend fun deleteAllPensionFunds() = dao.deleteAllPensionFunds()
}
