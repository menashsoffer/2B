package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.AiSubscriptionEntity
import com.example.data.entities.AppSettings
import com.example.data.entities.CarEntity
import com.example.data.entities.PensionFundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Settings
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)

    // Cars (Calculator 1)
    @Query("SELECT * FROM cars ORDER BY id ASC")
    fun getAllCarsFlow(): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars ORDER BY id ASC")
    suspend fun getAllCars(): List<CarEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: CarEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCars(cars: List<CarEntity>)

    @Update
    suspend fun updateCar(car: CarEntity)

    @Delete
    suspend fun deleteCar(car: CarEntity)

    @Query("DELETE FROM cars")
    suspend fun deleteAllCars()

    // AI Subscriptions (Calculator 2)
    @Query("SELECT * FROM ai_subscriptions ORDER BY id ASC")
    fun getAllAiSubscriptionsFlow(): Flow<List<AiSubscriptionEntity>>

    @Query("SELECT * FROM ai_subscriptions ORDER BY id ASC")
    suspend fun getAllAiSubscriptions(): List<AiSubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiSubscription(sub: AiSubscriptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiSubscriptions(subs: List<AiSubscriptionEntity>)

    @Update
    suspend fun updateAiSubscription(sub: AiSubscriptionEntity)

    @Delete
    suspend fun deleteAiSubscription(sub: AiSubscriptionEntity)

    @Query("DELETE FROM ai_subscriptions")
    suspend fun deleteAllAiSubscriptions()

    // Pension & Savings (Calculator 3)
    @Query("SELECT * FROM pension_funds ORDER BY id ASC")
    fun getAllPensionFundsFlow(): Flow<List<PensionFundEntity>>

    @Query("SELECT * FROM pension_funds ORDER BY id ASC")
    suspend fun getAllPensionFunds(): List<PensionFundEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPensionFund(fund: PensionFundEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPensionFunds(funds: List<PensionFundEntity>)

    @Update
    suspend fun updatePensionFund(fund: PensionFundEntity)

    @Delete
    suspend fun deletePensionFund(fund: PensionFundEntity)

    @Query("DELETE FROM pension_funds")
    suspend fun deleteAllPensionFunds()
}
