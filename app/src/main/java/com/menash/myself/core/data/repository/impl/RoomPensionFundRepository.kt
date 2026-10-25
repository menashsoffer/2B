package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.repository.PensionFundRepository
import com.menash.myself.core.domain.model.PensionFund
import com.menash.myself.data.dao.AppDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPensionFundRepository(private val dao: AppDao) : PensionFundRepository {
    override fun getAllPensionFunds(): Flow<List<PensionFund>> {
        return dao.getAllPensionFundsFlow().map { list -> list.map { ModelMappers.pensionFundToDomain(it) } }
    }

    override suspend fun getAllPensionFundsList(): List<PensionFund> {
        return dao.getAllPensionFunds().map { ModelMappers.pensionFundToDomain(it) }
    }

    override suspend fun insertPensionFund(fund: PensionFund): Long {
        return dao.insertPensionFund(ModelMappers.pensionFundToEntity(fund))
    }

    override suspend fun insertPensionFunds(funds: List<PensionFund>) {
        dao.insertPensionFunds(funds.map { ModelMappers.pensionFundToEntity(it) })
    }

    override suspend fun updatePensionFund(fund: PensionFund) {
        dao.updatePensionFund(ModelMappers.pensionFundToEntity(fund))
    }

    override suspend fun deletePensionFund(fund: PensionFund) {
        dao.deletePensionFund(ModelMappers.pensionFundToEntity(fund))
    }

    override suspend fun deleteAllPensionFunds() {
        dao.deleteAllPensionFunds()
    }
}
