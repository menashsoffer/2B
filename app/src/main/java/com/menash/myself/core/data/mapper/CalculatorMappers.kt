package com.menash.myself.core.data.mapper

import com.menash.myself.core.domain.model.AiSubscription
import com.menash.myself.core.domain.model.Car
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.PensionFund
import com.menash.myself.data.entities.AiSubscriptionEntity
import com.menash.myself.data.entities.CarEntity
import com.menash.myself.data.entities.PensionFundEntity

object CalculatorMappers {

    fun carToDomain(entity: CarEntity): Car {
        return Car(
            id = entity.id,
            name = entity.name,
            purchasePrice = Money.fromShekels(entity.purchasePrice),
            resaleValueAfter5Years = Money.fromShekels(entity.resaleValueAfter5Years),
            energyCostYearly = Money.fromShekels(entity.energyCostYearly),
            insuranceYearly = Money.fromShekels(entity.insuranceYearly),
            trackerYearly = Money.fromShekels(entity.trackerYearly),
            maintenanceYearly = Money.fromShekels(entity.maintenanceYearly),
            annualTestYearly = Money.fromShekels(entity.annualTestYearly),
            tires5YearsTotal = Money.fromShekels(entity.tires5YearsTotal),
            isBaseline = entity.isBaseline
        )
    }

    fun carToEntity(domain: Car): CarEntity {
        return CarEntity(
            id = domain.id,
            name = domain.name,
            purchasePrice = domain.purchasePrice.shekels,
            resaleValueAfter5Years = domain.resaleValueAfter5Years.shekels,
            energyCostYearly = domain.energyCostYearly.shekels,
            insuranceYearly = domain.insuranceYearly.shekels,
            trackerYearly = domain.trackerYearly.shekels,
            maintenanceYearly = domain.maintenanceYearly.shekels,
            annualTestYearly = domain.annualTestYearly.shekels,
            tires5YearsTotal = domain.tires5YearsTotal.shekels,
            isBaseline = domain.isBaseline
        )
    }

    fun aiSubscriptionToDomain(entity: AiSubscriptionEntity): AiSubscription {
        return AiSubscription(
            id = entity.id,
            name = entity.name,
            category = entity.category,
            isActive = entity.isActive,
            billingCycle = entity.billingCycle,
            currency = entity.currency,
            price = Money.fromShekels(entity.price),
            notes = entity.notes
        )
    }

    fun aiSubscriptionToEntity(domain: AiSubscription): AiSubscriptionEntity {
        return AiSubscriptionEntity(
            id = domain.id,
            name = domain.name,
            category = domain.category,
            isActive = domain.isActive,
            billingCycle = domain.billingCycle,
            currency = domain.currency,
            price = domain.price.shekels,
            notes = domain.notes
        )
    }

    fun pensionFundToDomain(entity: PensionFundEntity): PensionFund {
        return PensionFund(
            id = entity.id,
            fundType = entity.fundType,
            provider = entity.provider,
            currentValue = Money.fromShekels(entity.currentValue),
            totalDeposits = Money.fromShekels(entity.totalDeposits),
            feeOnBalancePercent = entity.feeOnBalancePercent,
            feeOnDepositsPercent = entity.feeOnDepositsPercent,
            notes = entity.notes
        )
    }

    fun pensionFundToEntity(domain: PensionFund): PensionFundEntity {
        return PensionFundEntity(
            id = domain.id,
            fundType = domain.fundType,
            provider = domain.provider,
            currentValue = domain.currentValue.shekels,
            totalDeposits = domain.totalDeposits.shekels,
            feeOnBalancePercent = domain.feeOnBalancePercent,
            feeOnDepositsPercent = domain.feeOnDepositsPercent,
            notes = domain.notes
        )
    }
}
