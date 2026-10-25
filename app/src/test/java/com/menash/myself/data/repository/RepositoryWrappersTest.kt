package com.menash.myself.data.repository

import com.menash.myself.core.data.repository.impl.RoomCarRepository
import com.menash.myself.core.data.repository.impl.RoomAiSubscriptionRepository
import com.menash.myself.core.data.repository.impl.RoomPensionFundRepository
import com.menash.myself.data.dao.AppDao
import com.menash.myself.data.entities.AiSubscriptionEntity
import com.menash.myself.data.entities.AppSettings
import com.menash.myself.data.entities.CarEntity
import com.menash.myself.data.entities.PensionFundEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying that core/data repository interfaces and their Room wrappers
 * correctly delegate to the underlying DAO.
 */
class RepositoryWrappersTest {

    private val fakeDao = object : AppDao {
        val cars = mutableListOf<CarEntity>()
        val aiSubs = mutableListOf<AiSubscriptionEntity>()
        val pensionFunds = mutableListOf<PensionFundEntity>()

        override fun getSettingsFlow(): Flow<AppSettings?> = flowOf(null)
        override suspend fun getSettings(): AppSettings? = null
        override suspend fun saveSettings(settings: AppSettings) {}

        override fun getAllCarsFlow(): Flow<List<CarEntity>> = flowOf(cars)
        override suspend fun getAllCars(): List<CarEntity> = cars
        override suspend fun insertCar(car: CarEntity): Long {
            cars.add(car)
            return car.id
        }
        override suspend fun insertCars(cars: List<CarEntity>) {
            this.cars.addAll(cars)
        }
        override suspend fun updateCar(car: CarEntity) {}
        override suspend fun deleteCar(car: CarEntity) {
            cars.remove(car)
        }
        override suspend fun deleteAllCars() {
            cars.clear()
        }

        override fun getAllAiSubscriptionsFlow(): Flow<List<AiSubscriptionEntity>> = flowOf(aiSubs)
        override suspend fun getAllAiSubscriptions(): List<AiSubscriptionEntity> = aiSubs
        override suspend fun insertAiSubscription(sub: AiSubscriptionEntity): Long {
            aiSubs.add(sub)
            return sub.id
        }
        override suspend fun insertAiSubscriptions(subs: List<AiSubscriptionEntity>) {
            aiSubs.addAll(subs)
        }
        override suspend fun updateAiSubscription(sub: AiSubscriptionEntity) {}
        override suspend fun deleteAiSubscription(sub: AiSubscriptionEntity) {
            aiSubs.remove(sub)
        }
        override suspend fun deleteAllAiSubscriptions() {
            aiSubs.clear()
        }

        override fun getAllPensionFundsFlow(): Flow<List<PensionFundEntity>> = flowOf(pensionFunds)
        override suspend fun getAllPensionFunds(): List<PensionFundEntity> = pensionFunds
        override suspend fun insertPensionFund(fund: PensionFundEntity): Long {
            pensionFunds.add(fund)
            return fund.id
        }
        override suspend fun insertPensionFunds(funds: List<PensionFundEntity>) {
            pensionFunds.addAll(funds)
        }
        override suspend fun updatePensionFund(fund: PensionFundEntity) {}
        override suspend fun deletePensionFund(fund: PensionFundEntity) {
            pensionFunds.remove(fund)
        }
        override suspend fun deleteAllPensionFunds() {
            pensionFunds.clear()
        }
    }

    @Test
    fun carRepository_delegatesToDao() = runBlocking {
        val repo = RoomCarRepository(fakeDao)
        val car = CarEntity(
            id = 1L,
            name = "טסלה 3",
            purchasePrice = 200000.0,
            resaleValueAfter5Years = 100000.0,
            energyCostYearly = 3000.0,
            insuranceYearly = 4000.0,
            trackerYearly = 500.0,
            maintenanceYearly = 1000.0,
            annualTestYearly = 2000.0,
            tires5YearsTotal = 2500.0
        )
        repo.insertCar(car)
        val list = repo.getAllCarsList()
        assertEquals(1, list.size)
        assertEquals("טסלה 3", list.first().name)

        repo.deleteCar(car)
        assertTrue(repo.getAllCarsList().isEmpty())
    }

    @Test
    fun aiSubscriptionRepository_delegatesToDao() = runBlocking {
        val repo = RoomAiSubscriptionRepository(fakeDao)
        val sub = AiSubscriptionEntity(
            id = 1L,
            name = "ChatGPT Plus",
            category = "LLM",
            price = 20.0
        )
        repo.insertAiSubscription(sub)
        val list = repo.getAllAiSubscriptionsList()
        assertEquals(1, list.size)
        assertEquals("ChatGPT Plus", list.first().name)

        repo.deleteAllAiSubscriptions()
        assertTrue(repo.getAllAiSubscriptionsList().isEmpty())
    }

    @Test
    fun pensionFundRepository_delegatesToDao() = runBlocking {
        val repo = RoomPensionFundRepository(fakeDao)
        val fund = PensionFundEntity(
            id = 1L,
            fundType = "קרן פנסיה",
            provider = "הראל",
            currentValue = 100000.0,
            totalDeposits = 70000.0
        )
        repo.insertPensionFund(fund)
        val list = repo.getAllPensionFundsList()
        assertEquals(1, list.size)
        assertEquals("הראל", list.first().provider)

        repo.deleteAllPensionFunds()
        assertTrue(repo.getAllPensionFundsList().isEmpty())
    }
}
