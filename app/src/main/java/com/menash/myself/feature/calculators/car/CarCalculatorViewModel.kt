package com.menash.myself.feature.calculators.car

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.repository.CarRepository
import com.menash.myself.data.entities.CarEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CarCalculatorViewModel(
    private val carRepository: CarRepository
) : ViewModel() {

    val cars: StateFlow<List<CarEntity>> = carRepository.getAllCars()
        .map { list -> list.map { ModelMappers.carToEntity(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertCar(car: CarEntity) {
        viewModelScope.launch {
            carRepository.insertCar(ModelMappers.carToDomain(car))
        }
    }

    fun insertCars(carsList: List<CarEntity>) {
        viewModelScope.launch {
            carRepository.insertCars(carsList.map { ModelMappers.carToDomain(it) })
        }
    }

    fun updateCar(car: CarEntity) {
        viewModelScope.launch {
            carRepository.updateCar(ModelMappers.carToDomain(car))
        }
    }

    fun deleteCar(car: CarEntity) {
        viewModelScope.launch {
            carRepository.deleteCar(ModelMappers.carToDomain(car))
        }
    }

    fun setBaselineCar(selectedCar: CarEntity) {
        viewModelScope.launch {
            cars.value.forEach { c ->
                carRepository.updateCar(
                    ModelMappers.carToDomain(c.copy(isBaseline = c.id == selectedCar.id))
                )
            }
        }
    }

    companion object {
        fun factory(carRepository: CarRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CarCalculatorViewModel(carRepository) as T
                }
            }
    }
}
