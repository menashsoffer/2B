package com.menash.myself.ui.calculators.car

import java.text.NumberFormat

data class BaselineDiffResult(
    val diffFromBaseline: Double,
    val isSaving: Boolean,
    val diffMonthly: Int,
    val text: String
)

object CarCalculationVerbatimHelpers {

    /**
     * Extracted VERBATIM from EnergyHelperDialog in CarComparisonScreen.kt
     */
    fun calculateEnergyCostVerbatim(
        isFuel: Boolean,
        annualKm: Double,
        kmPerLiter: Double,
        fuelPrice: Double,
        kwhPer100Km: Double,
        homeKwhPrice: Double,
        publicSessionsCount: Double,
        publicSessionCost: Double
    ): Double {
        return if (isFuel) {
            if (kmPerLiter > 0.0) (annualKm / kmPerLiter) * fuelPrice else 0.0
        } else {
            val homeTotal = (annualKm / 100.0) * kwhPer100Km * homeKwhPrice
            val publicTotal = publicSessionsCount * publicSessionCost
            homeTotal + publicTotal
        }
    }

    /**
     * Extracted VERBATIM from CarCard in CarComparisonScreen.kt
     */
    fun calculateBaselineDiffVerbatim(
        carNet5YearCost: Double,
        baselineNet5YearCost: Double,
        baselineCarName: String,
        fmt: NumberFormat
    ): BaselineDiffResult {
        val diffFromBaseline = baselineNet5YearCost - carNet5YearCost
        val isSaving = diffFromBaseline >= 0
        val diffMonthly = (diffFromBaseline / 60.0).toInt()
        val text = if (isSaving) {
            "חיסכון של ₪${fmt.format(diffFromBaseline.toInt())} מול $baselineCarName (₪$diffMonthly/חודש)"
        } else {
            "יקר ב-₪${fmt.format((-diffFromBaseline).toInt())} מול $baselineCarName (₪${-diffMonthly}/חודש)"
        }
        return BaselineDiffResult(
            diffFromBaseline = diffFromBaseline,
            isSaving = isSaving,
            diffMonthly = diffMonthly,
            text = text
        )
    }
}
