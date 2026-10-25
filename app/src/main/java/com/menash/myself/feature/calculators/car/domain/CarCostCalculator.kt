package com.menash.myself.feature.calculators.car.domain

import com.menash.myself.core.domain.model.Car
import com.menash.myself.core.domain.model.Money
import kotlin.math.max

data class CarCostInput(
    val purchasePrice: Double = 0.0,
    val resaleValueAfter5Years: Double = 0.0,
    val energyCostYearly: Double = 0.0,
    val insuranceYearly: Double = 0.0,
    val trackerYearly: Double = 0.0,
    val maintenanceYearly: Double = 0.0,
    val annualTestYearly: Double = 0.0,
    val tires5YearsTotal: Double = 0.0
)

data class CarCostResult(
    val yearlyRunningCost: Double,
    val monthlyRunningCost: Double,
    val fiveYearRunningCost: Double,
    val total5YearCost: Double,
    val net5YearCost: Double,
    val netMonthlyCost: Double
)

data class CarCostResultMoney(
    val yearlyRunningCost: Money,
    val monthlyRunningCost: Money,
    val fiveYearRunningCost: Money,
    val total5YearCost: Money,
    val net5YearCost: Money,
    val netMonthlyCost: Money
)

data class EnergyCostInput(
    val isFuel: Boolean = true,
    val annualKm: Double = 0.0,
    val kmPerLiter: Double = 0.0,
    val fuelPrice: Double = 0.0,
    val kwhPer100Km: Double = 0.0,
    val homeKwhPrice: Double = 0.0,
    val publicSessionsCount: Double = 0.0,
    val publicSessionCost: Double = 0.0
)

data class BaselineComparisonResult(
    val isSaving: Boolean,
    val diffFromBaseline: Double,
    val diffMonthly: Int
)

object CarCostCalculator {

    /**
     * Calculates 5-year and monthly costs for a vehicle.
     * Locked by CharacterizationTests: car_standard5YearCost_locksCurrentOutput
     * Clamping locked by: car_netCostClampedToZero_whenResaleExceedsTotalCost (KNOWN_ISSUES § 1.1)
     */
    fun calculate5YearCost(input: CarCostInput): CarCostResult {
        val yearlyRunningCost = input.energyCostYearly +
                input.insuranceYearly +
                input.trackerYearly +
                input.maintenanceYearly +
                input.annualTestYearly

        val monthlyRunningCost = yearlyRunningCost / 12.0
        val fiveYearRunningCost = yearlyRunningCost * 5.0
        val total5YearCost = input.purchasePrice + fiveYearRunningCost + input.tires5YearsTotal
        val net5YearCost = total5YearCost - input.resaleValueAfter5Years

        // Verbatim behavior: netMonthlyCost is clamped to 0.0 if negative
        val netMonthlyCost = max(0.0, net5YearCost / 60.0)

        return CarCostResult(
            yearlyRunningCost = yearlyRunningCost,
            monthlyRunningCost = monthlyRunningCost,
            fiveYearRunningCost = fiveYearRunningCost,
            total5YearCost = total5YearCost,
            net5YearCost = net5YearCost,
            netMonthlyCost = netMonthlyCost
        )
    }

    /**
     * Calculates 5-year and monthly costs for a domain Car using Money (Long agorot with HALF_UP).
     * Single implementation of car math for domain Car models.
     */
    fun calculate5YearCost(car: Car): CarCostResultMoney {
        val yearlyRunningCost = car.energyCostYearly +
                car.insuranceYearly +
                car.trackerYearly +
                car.maintenanceYearly +
                car.annualTestYearly

        val monthlyRunningCost = yearlyRunningCost / 12
        val fiveYearRunningCost = yearlyRunningCost * 5L
        val total5YearCost = car.purchasePrice + fiveYearRunningCost + car.tires5YearsTotal
        val net5YearCost = total5YearCost - car.resaleValueAfter5Years
        val netMonthlyCost = if (net5YearCost.isPositive()) net5YearCost / 60 else Money.ZERO

        return CarCostResultMoney(
            yearlyRunningCost = yearlyRunningCost,
            monthlyRunningCost = monthlyRunningCost,
            fiveYearRunningCost = fiveYearRunningCost,
            total5YearCost = total5YearCost,
            net5YearCost = net5YearCost,
            netMonthlyCost = netMonthlyCost
        )
    }

    /**
     * Calculates energy cost for fuel or EV vehicles.
     * Locked by: car_energyHelper_fuelCalculation_locksCurrentOutput and car_energyHelper_evCalculation_locksCurrentOutput
     */
    fun calculateEnergyCost(input: EnergyCostInput): Double {
        return if (input.isFuel) {
            if (input.kmPerLiter > 0.0) {
                (input.annualKm / input.kmPerLiter) * input.fuelPrice
            } else {
                0.0
            }
        } else {
            val homeTotal = (input.annualKm / 100.0) * input.kwhPer100Km * input.homeKwhPrice
            val publicTotal = input.publicSessionsCount * input.publicSessionCost
            homeTotal + publicTotal
        }
    }

    /**
     * Parses km per liter input string with verbatim fallback to 1.0.
     * Locked by: car_energyHelper_emptyKmPerLiterFallbackToOne_locksInflatedFuelCost (KNOWN_ISSUES § 1.4)
     */
    fun parseKmPerLiter(input: String): Double {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return 1.0
        return trimmed.toDoubleOrNull() ?: 1.0
    }

    /**
     * Compares a car's net 5-year cost to a baseline car.
     * Pure calculation: returns numbers only (isSaving, diffFromBaseline, diffMonthly).
     * UI formats the string using strings.xml and NumberFormat.
     * Truncation towards zero via .toInt() locked by:
     * car_baselineDiff_savingTruncationTowardZero_locksCurrentOutput and
     * car_baselineDiff_expensiveTruncationTowardZero_locksCurrentOutput (KNOWN_ISSUES § 1.2)
     */
    fun calculateBaselineComparison(
        carNet5YearCost: Double,
        baselineNet5YearCost: Double
    ): BaselineComparisonResult {
        val diff = baselineNet5YearCost - carNet5YearCost
        val isSaving = diff >= 0
        // Verbatim behavior: diffMonthly truncates toward zero via .toInt()
        val diffMonthly = (diff / 60.0).toInt()

        return BaselineComparisonResult(
            isSaving = isSaving,
            diffFromBaseline = diff,
            diffMonthly = diffMonthly
        )
    }
}
