package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.mapper.CalculatorMappers
import com.menash.myself.core.data.repository.CarRepository
import com.menash.myself.core.domain.model.Car
import com.menash.myself.data.dao.AppDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCarRepository(private val dao: AppDao) : CarRepository {
    override fun getAllCars(): Flow<List<Car>> {
        return dao.getAllCarsFlow().map { list -> list.map { CalculatorMappers.carToDomain(it) } }
    }

    override suspend fun getAllCarsList(): List<Car> {
        return dao.getAllCars().map { CalculatorMappers.carToDomain(it) }
    }

    override suspend fun insertCar(car: Car): Long {
        return dao.insertCar(CalculatorMappers.carToEntity(car))
    }

    override suspend fun insertCars(cars: List<Car>) {
        dao.insertCars(cars.map { CalculatorMappers.carToEntity(it) })
    }

    override suspend fun updateCar(car: Car) {
        dao.updateCar(CalculatorMappers.carToEntity(car))
    }

    override suspend fun deleteCar(car: Car) {
        dao.deleteCar(CalculatorMappers.carToEntity(car))
    }

    override suspend fun deleteAllCars() {
        dao.deleteAllCars()
    }
}
