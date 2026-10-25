package com.menash.myself.feature.calculators.car

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.CarRepository
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.Car
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.ui.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CarCalculatorViewModel(
    private val carRepository: CarRepository,
    errorReporter: ErrorReporter? = null
) : BaseViewModel(errorReporter) {

    val cars: StateFlow<List<Car>?> = carRepository.getAllCars()
        .map<List<Car>, List<Car>?> { it }
        .catch { e ->
            handleException("getAllCars", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<UiState<List<Car>>> = combine(cars, _errorState) { list, error ->
        when {
            error != null && list.isNullOrEmpty() -> UiState.Error(error)
            list == null -> UiState.Loading
            list.isEmpty() -> UiState.Empty
            else -> UiState.Content(list)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun insertCar(car: Car) {
        safeLaunch("insertCar") {
            carRepository.insertCar(car)
        }
    }

    fun insertCars(carsList: List<Car>) {
        safeLaunch("insertCars") {
            carRepository.insertCars(carsList)
        }
    }

    fun updateCar(car: Car) {
        safeLaunch("updateCar") {
            carRepository.updateCar(car)
        }
    }

    fun deleteCar(car: Car) {
        safeLaunch("deleteCar") {
            carRepository.deleteCar(car)
        }
    }

    fun setBaselineCar(carId: Long) {
        safeLaunch("setBaselineCar") {
            val allCars = carRepository.getAllCarsList()
            for (car in allCars) {
                val shouldBeBaseline = car.id == carId
                if (car.isBaseline != shouldBeBaseline) {
                    carRepository.updateCar(car.copy(isBaseline = shouldBeBaseline))
                }
            }
        }
    }

    companion object {
        fun factory(
            carRepository: CarRepository,
            errorReporter: ErrorReporter? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CarCalculatorViewModel(carRepository, errorReporter) as T
            }
        }
    }
}
