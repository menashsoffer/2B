package com.menash.myself.data.repository

import com.menash.myself.core.data.repository.impl.RoomCarRepository
import com.menash.myself.core.data.repository.impl.RoomAiSubscriptionRepository
import com.menash.myself.core.data.repository.impl.RoomPensionFundRepository
import com.menash.myself.core.domain.model.AiSubscription
import com.menash.myself.core.domain.model.Car
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.PensionFund
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
 * correctly delegate to the underlying DAO and map domain models.
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
    fun carRepository_delegatesToDaoWithDomainModel() = runBlocking {
        val repo = RoomCarRepository(fakeDao)
        val car = Car(
            id = 1L,
            name = "טסלה 3",
            purchasePrice = Money.fromShekels(200000.0),
            resaleValueAfter5Years = Money.fromShekels(100000.0),
            energyCostYearly = Money.fromShekels(3000.0),
            insuranceYearly = Money.fromShekels(4000.0),
            trackerYearly = Money.fromShekels(500.0),
            maintenanceYearly = Money.fromShekels(1000.0),
            annualTestYearly = Money.fromShekels(2000.0),
            tires5YearsTotal = Money.fromShekels(2500.0)
        )
        repo.insertCar(car)
        val list = repo.getAllCarsList()
        assertEquals(1, list.size)
        assertEquals("טסלה 3", list.first().name)
        assertEquals(200000.0, list.first().purchasePrice.shekels, 0.001)

        repo.deleteCar(car)
        assertTrue(repo.getAllCarsList().isEmpty())
    }

    @Test
    fun aiSubscriptionRepository_delegatesToDaoWithDomainModel() = runBlocking {
        val repo = RoomAiSubscriptionRepository(fakeDao)
        val sub = AiSubscription(
            id = 1L,
            name = "ChatGPT Plus",
            category = "LLM",
            price = Money.fromShekels(20.0)
        )
        repo.insertAiSubscription(sub)
        val list = repo.getAllAiSubscriptionsList()
        assertEquals(1, list.size)
        assertEquals("ChatGPT Plus", list.first().name)
        assertEquals(20.0, list.first().price.shekels, 0.001)

        repo.deleteAllAiSubscriptions()
        assertTrue(repo.getAllAiSubscriptionsList().isEmpty())
    }

    @Test
    fun pensionFundRepository_delegatesToDaoWithDomainModel() = runBlocking {
        val repo = RoomPensionFundRepository(fakeDao)
        val fund = PensionFund(
            id = 1L,
            fundType = "קרן פנסיה",
            provider = "הראל",
            currentValue = Money.fromShekels(100000.0),
            totalDeposits = Money.fromShekels(70000.0)
        )
        repo.insertPensionFund(fund)
        val list = repo.getAllPensionFundsList()
        assertEquals(1, list.size)
        assertEquals("הראל", list.first().provider)
        assertEquals(100000.0, list.first().currentValue.shekels, 0.001)

        repo.deleteAllPensionFunds()
        assertTrue(repo.getAllPensionFundsList().isEmpty())
    }
}
