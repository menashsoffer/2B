package com.menash.myself.core.domain.model

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
    val yearlyRunningCost: Money
        get() = energyCostYearly + insuranceYearly + trackerYearly + maintenanceYearly + annualTestYearly

    val monthlyRunningCost: Money
        get() = yearlyRunningCost / 12

    val fiveYearRunningCost: Money
        get() = yearlyRunningCost * 5L

    val total5YearCost: Money
        get() = purchasePrice + fiveYearRunningCost + tires5YearsTotal

    val net5YearCost: Money
        get() = total5YearCost - resaleValueAfter5Years

    val netMonthlyCost: Money
        get() = if (net5YearCost.isPositive()) net5YearCost / 60 else Money.ZERO
}
