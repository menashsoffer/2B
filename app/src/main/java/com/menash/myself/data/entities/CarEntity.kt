package com.menash.myself.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.menash.myself.feature.calculators.car.domain.CarCostCalculator
import com.menash.myself.feature.calculators.car.domain.CarCostInput

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val purchasePrice: Double,
    val resaleValueAfter5Years: Double,
    val energyCostYearly: Double,
    val insuranceYearly: Double,
    val trackerYearly: Double,
    val maintenanceYearly: Double,
    val annualTestYearly: Double,
    val tires5YearsTotal: Double,
    val isBaseline: Boolean = false
) {
    private fun costResult() = CarCostCalculator.calculate5YearCost(
        CarCostInput(
            purchasePrice = purchasePrice,
            resaleValueAfter5Years = resaleValueAfter5Years,
            energyCostYearly = energyCostYearly,
            insuranceYearly = insuranceYearly,
            trackerYearly = trackerYearly,
            maintenanceYearly = maintenanceYearly,
            annualTestYearly = annualTestYearly,
            tires5YearsTotal = tires5YearsTotal
        )
    )

    val yearlyRunningCost: Double
        get() = costResult().yearlyRunningCost

    val monthlyRunningCost: Double
        get() = costResult().monthlyRunningCost

    val fiveYearRunningCost: Double
        get() = costResult().fiveYearRunningCost

    val total5YearCost: Double
        get() = costResult().total5YearCost

    val net5YearCost: Double
        get() = costResult().net5YearCost

    val netMonthlyCost: Double
        get() = costResult().netMonthlyCost
}
