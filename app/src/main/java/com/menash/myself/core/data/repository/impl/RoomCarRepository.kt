package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.repository.CarRepository
import com.menash.myself.core.domain.model.Car
import com.menash.myself.data.dao.AppDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCarRepository(private val dao: AppDao) : CarRepository {
    override fun getAllCars(): Flow<List<Car>> {
        return dao.getAllCarsFlow().map { list -> list.map { ModelMappers.carToDomain(it) } }
    }

    override suspend fun getAllCarsList(): List<Car> {
        return dao.getAllCars().map { ModelMappers.carToDomain(it) }
    }

    override suspend fun insertCar(car: Car): Long {
        return dao.insertCar(ModelMappers.carToEntity(car))
    }

    override suspend fun insertCars(cars: List<Car>) {
        dao.insertCars(cars.map { ModelMappers.carToEntity(it) })
    }

    override suspend fun updateCar(car: Car) {
        dao.updateCar(ModelMappers.carToEntity(car))
    }

    override suspend fun deleteCar(car: Car) {
        dao.deleteCar(ModelMappers.carToEntity(car))
    }

    override suspend fun deleteAllCars() {
        dao.deleteAllCars()
    }
}
