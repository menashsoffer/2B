package com.menash.myself.core.data.repository

import com.menash.myself.core.domain.model.Car
import kotlinx.coroutines.flow.Flow

interface CarRepository {
    fun getAllCars(): Flow<List<Car>>
    suspend fun getAllCarsList(): List<Car>
    suspend fun insertCar(car: Car): Long
    suspend fun insertCars(cars: List<Car>)
    suspend fun updateCar(car: Car)
    suspend fun deleteCar(car: Car)
    suspend fun deleteAllCars()
}
