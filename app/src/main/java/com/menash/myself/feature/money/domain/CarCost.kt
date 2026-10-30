package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal val carPrecision = MathContext.DECIMAL128
internal fun Money.decimal(): BigDecimal = agorot.toBigDecimal()
internal fun BigDecimal.carMoney(): Money = Money(setScale(0, RoundingMode.HALF_UP).longValueExact())
internal fun nonnegative(vararg amounts: Money) = require(amounts.all { it >= Money.ZERO })

sealed interface Energy {
    data class Fuel(val kmPerYear: Int, val kmPerLiter: BigDecimal, val pricePerLiter: Money) : Energy {
        init { require(kmPerYear >= 0 && kmPerLiter > BigDecimal.ZERO); nonnegative(pricePerLiter) }
    }
    data class Electric(
        val kmPerYear: Int, val kwhPer100Km: BigDecimal, val homePricePerKwh: Money,
        val publicShare: BigDecimal, val publicPricePerKwh: Money,
    ) : Energy {
        init {
            require(kmPerYear >= 0 && kwhPer100Km > BigDecimal.ZERO)
            require(publicShare >= BigDecimal.ZERO && publicShare <= BigDecimal.ONE)
            nonnegative(homePricePerKwh, publicPricePerKwh)
        }
    }
    data class Known(val yearly: Money) : Energy { init { nonnegative(yearly) } }
}

data class CarProfile(
    val id: String, val name: String, val energy: Energy,
    val insuranceYearly: Money = Money.ZERO, val licenseAndTestYearly: Money = Money.ZERO,
    val maintenanceYearly: Money = Money.ZERO, val otherYearly: Money = Money.ZERO,
    val tiresPerYear: Money = Money.ZERO,
) {
    init {
        require(id.isNotBlank())
        nonnegative(insuranceYearly, licenseAndTestYearly, maintenanceYearly, otherYearly, tiresPerYear)
    }
}

internal fun energyYearly(energy: Energy): BigDecimal = when (energy) {
    is Energy.Fuel -> energy.kmPerYear.toBigDecimal().multiply(energy.pricePerLiter.decimal())
        .divide(energy.kmPerLiter, carPrecision)
    is Energy.Electric -> {
        val price = energy.homePricePerKwh.decimal().multiply(BigDecimal.ONE.subtract(energy.publicShare))
            .add(energy.publicPricePerKwh.decimal().multiply(energy.publicShare))
        energy.kmPerYear.toBigDecimal().multiply(energy.kwhPer100Km).multiply(price).movePointLeft(2)
    }
    is Energy.Known -> energy.yearly.decimal()
}

internal fun runningExact(car: CarProfile): BigDecimal = energyYearly(car.energy)
    .add(car.insuranceYearly.decimal()).add(car.licenseAndTestYearly.decimal())
    .add(car.maintenanceYearly.decimal()).add(car.otherYearly.decimal()).add(car.tiresPerYear.decimal())

fun yearlyRunning(car: CarProfile): Money = runningExact(car).carMoney()
/** Divide the unrounded yearly amount, so each published result rounds only once. */
fun monthlyRunning(car: CarProfile): Money = runningExact(car).divide(BigDecimal(12), carPrecision).carMoney()
