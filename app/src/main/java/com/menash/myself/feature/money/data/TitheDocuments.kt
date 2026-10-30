package com.menash.myself.feature.money.data

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.Donation
import com.menash.myself.feature.money.domain.TitheSettings
import java.time.LocalDate
import java.time.YearMonth

/** Donations and the maaser settings document, in the same conventions as [MoneyDocuments]. */
internal object TitheDocuments {

    fun encode(donation: Donation): Map<String, Any> = mapOf(
        "date" to donation.date.toString(),
        "amount" to donation.amount.agorot,
        "organisation" to donation.organisation,
        "submittedForTaxRefund" to donation.submittedForTaxRefund,
    )

    fun encode(settings: TitheSettings): Map<String, Any> = buildMap {
        put("startMonth", settings.startMonth.toString())
        put("opening", settings.opening.agorot)
        settings.portfolioAssetId?.let { put("portfolioAssetId", it) }
        put("depositsBefore", settings.depositsBefore.agorot)
    }

    fun donation(id: String, doc: MoneyDoc): Donation = Donation(
        id = id,
        date = LocalDate.parse(doc.string("date")),
        amount = doc.money("amount"),
        organisation = doc.stringOrNull("organisation").orEmpty(),
        submittedForTaxRefund = doc.boolean("submittedForTaxRefund"),
    )

    fun settings(id: String, doc: MoneyDoc): TitheSettings {
        require(id == TitheSettings.ID) { "Unexpected maaser settings id '$id'" }
        return TitheSettings(
            startMonth = YearMonth.parse(doc.string("startMonth")),
            opening = Money(doc.longOrNull("opening") ?: 0),
            portfolioAssetId = doc.stringOrNull("portfolioAssetId"),
            depositsBefore = Money(doc.longOrNull("depositsBefore") ?: 0),
        )
    }
}
