package com.menash.myself.ui.calculators.car

import android.content.Context
import com.menash.myself.R
import com.menash.myself.feature.calculators.car.domain.CarCostCalculator
import com.menash.myself.feature.calculators.car.domain.EnergyCostInput
import java.text.NumberFormat

data class BaselineDiffResult(
    val diffFromBaseline: Double,
    val isSaving: Boolean,
    val diffMonthly: Int,
    val text: String
)

object CarCalculationVerbatimHelpers {

    /**
     * Delegates to feature/calculators/car/domain/CarCostCalculator.
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
        return CarCostCalculator.calculateEnergyCost(
            EnergyCostInput(
                isFuel = isFuel,
                annualKm = annualKm,
                kmPerLiter = kmPerLiter,
                fuelPrice = fuelPrice,
                kwhPer100Km = kwhPer100Km,
                homeKwhPrice = homeKwhPrice,
                publicSessionsCount = publicSessionsCount,
                publicSessionCost = publicSessionCost
            )
        )
    }

    /**
     * Delegates pure numbers calculation to CarCostCalculator, then formats the UI string.
     */
    fun calculateBaselineDiffVerbatim(
        context: Context,
        carNet5YearCost: Double,
        baselineNet5YearCost: Double,
        baselineCarName: String,
        fmt: NumberFormat
    ): BaselineDiffResult {
        val comparison = CarCostCalculator.calculateBaselineComparison(
            carNet5YearCost = carNet5YearCost,
            baselineNet5YearCost = baselineNet5YearCost
        )
        val text = if (comparison.isSaving) {
            context.getString(R.string.ui_fmtformatcomparisondifffrombaselinetoint_59, fmt.format(comparison.diffFromBaseline.toInt()), baselineCarName, comparison.diffMonthly)
        } else {
            context.getString(R.string.ui_fmtformatcomparisondifffrombaselinetoint_baselinecarname_58, fmt.format((-comparison.diffFromBaseline).toInt()), baselineCarName, -comparison.diffMonthly)
        }
        return BaselineDiffResult(
            diffFromBaseline = comparison.diffFromBaseline,
            isSaving = comparison.isSaving,
            diffMonthly = comparison.diffMonthly,
            text = text
        )
    }

    /**
     * Delegates to CarCostCalculator.parseKmPerLiter.
     */
    fun parseKmPerLiterVerbatim(input: String): Double {
        return CarCostCalculator.parseKmPerLiter(input)
    }
}
