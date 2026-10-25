package com.menash.myself.core.data.repository

import com.menash.myself.data.entities.CarEntity
import kotlinx.coroutines.flow.Flow

interface CarRepository {
    fun getAllCars(): Flow<List<CarEntity>>
    suspend fun getAllCarsList(): List<CarEntity>
    suspend fun insertCar(car: CarEntity): Long
    suspend fun insertCars(cars: List<CarEntity>)
    suspend fun updateCar(car: CarEntity)
    suspend fun deleteCar(car: CarEntity)
    suspend fun deleteAllCars()
}
