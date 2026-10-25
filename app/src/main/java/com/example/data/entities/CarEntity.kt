package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val yearlyRunningCost: Double
        get() = energyCostYearly + insuranceYearly + trackerYearly + maintenanceYearly + annualTestYearly

    val monthlyRunningCost: Double
        get() = yearlyRunningCost / 12.0

    val fiveYearRunningCost: Double
        get() = yearlyRunningCost * 5.0

    val total5YearCost: Double
        get() = purchasePrice + fiveYearRunningCost + tires5YearsTotal

    val net5YearCost: Double
        get() = total5YearCost - resaleValueAfter5Years

    val netMonthlyCost: Double
        get() = if (net5YearCost > 0) net5YearCost / 60.0 else 0.0
}
