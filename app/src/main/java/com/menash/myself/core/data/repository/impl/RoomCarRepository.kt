package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.repository.CarRepository
import com.menash.myself.data.dao.AppDao
import com.menash.myself.data.entities.CarEntity
import kotlinx.coroutines.flow.Flow

class RoomCarRepository(private val dao: AppDao) : CarRepository {
    override fun getAllCars(): Flow<List<CarEntity>> = dao.getAllCarsFlow()

    override suspend fun getAllCarsList(): List<CarEntity> = dao.getAllCars()

    override suspend fun insertCar(car: CarEntity): Long = dao.insertCar(car)

    override suspend fun insertCars(cars: List<CarEntity>) = dao.insertCars(cars)

    override suspend fun updateCar(car: CarEntity) = dao.updateCar(car)

    override suspend fun deleteCar(car: CarEntity) = dao.deleteCar(car)

    override suspend fun deleteAllCars() = dao.deleteAllCars()
}
