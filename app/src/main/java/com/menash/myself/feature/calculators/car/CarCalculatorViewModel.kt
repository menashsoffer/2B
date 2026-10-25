package com.menash.myself.feature.calculators.car

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menash.myself.core.data.repository.CarRepository
import com.menash.myself.core.data.session.ErrorReporter
import com.menash.myself.core.domain.model.Car
import com.menash.myself.core.domain.model.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CarCalculatorViewModel(
    private val carRepository: CarRepository,
    private val errorReporter: ErrorReporter? = null
) : ViewModel() {

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    val cars: StateFlow<List<Car>> = carRepository.getAllCars()
        .catch { e ->
            handleException("getAllCars", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<UiState<List<Car>>> = combine(cars, _errorState) { list, error ->
        when {
            error != null && list.isEmpty() -> UiState.Error(error)
            list.isEmpty() -> UiState.Empty
            else -> UiState.Content(list)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun clearError() {
        _errorState.value = null
        errorReporter?.clearError()
    }

    private fun handleException(action: String, e: Throwable) {
        if (e is CancellationException) throw e
        val msg = e.localizedMessage ?: e.message ?: "שגיאה ב-$action"
        _errorState.value = msg
        errorReporter?.reportError(action, e)
    }

    private fun launchCatching(action: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleException(action, e)
            }
        }
    }

    fun insertCar(car: Car) {
        launchCatching("insertCar") {
            carRepository.insertCar(car)
        }
    }

    fun insertCars(carsList: List<Car>) {
        launchCatching("insertCars") {
            carRepository.insertCars(carsList)
        }
    }

    fun updateCar(car: Car) {
        launchCatching("updateCar") {
            carRepository.updateCar(car)
        }
    }

    fun deleteCar(car: Car) {
        launchCatching("deleteCar") {
            carRepository.deleteCar(car)
        }
    }

    fun setBaselineCar(carId: Long) {
        launchCatching("setBaselineCar") {
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
