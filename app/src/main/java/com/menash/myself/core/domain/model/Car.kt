package com.menash.myself.core.domain.model

import com.menash.myself.feature.calculators.car.domain.CarCostCalculator
import com.menash.myself.feature.calculators.car.domain.CarCostResultMoney

data class Car(
    val id: Long = 0,
    val name: String,
    val purchasePrice: Money,
    val resaleValueAfter5Years: Money,
    val energyCostYearly: Money,
    val insuranceYearly: Money,
    val trackerYearly: Money,
    val maintenanceYearly: Money,
    val annualTestYearly: Money,
    val tires5YearsTotal: Money,
    val isBaseline: Boolean = false
) {
    val costResult: CarCostResultMoney
        get() = CarCostCalculator.calculate5YearCost(this)

    val yearlyRunningCost: Money get() = costResult.yearlyRunningCost
    val monthlyRunningCost: Money get() = costResult.monthlyRunningCost
    val fiveYearRunningCost: Money get() = costResult.fiveYearRunningCost
    val total5YearCost: Money get() = costResult.total5YearCost
    val net5YearCost: Money get() = costResult.net5YearCost
    val netMonthlyCost: Money get() = costResult.netMonthlyCost
}
