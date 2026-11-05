package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.Donation
import com.menash.myself.feature.money.domain.TitheSettings
import java.time.LocalDate
import java.time.YearMonth

/** The donation sheet; the amount stays text until save. */
data class DonationDraft(
    val id: String? = null,
    val amount: String = "",
    val organisation: String = "",
    val date: LocalDate,
    val submitted: Boolean = false,
)

fun donationDraftOf(donation: Donation) = DonationDraft(
    donation.id, amountText(donation.amount), donation.organisation, donation.date, donation.submittedForTaxRefund,
)

/** Null when the amount is missing or not above zero; the organisation may stay empty. */
fun buildDonation(draft: DonationDraft, newId: () -> String): Donation? {
    val amount = parseAmount(draft.amount)?.takeIf { it > Money.ZERO } ?: return null
    return Donation(draft.id ?: newId(), draft.date, amount, draft.organisation.trim(), draft.submitted)
}

/** The settings sheet. [ahead] means the opening amount was donated ahead rather than still owed. */
data class TitheSettingsDraft(
    val startMonth: YearMonth,
    val opening: String = "",
    val ahead: Boolean = false,
    val portfolioAssetId: String? = null,
    val depositsBefore: String = "",
    val fromFirstData: Boolean = true,
)

/** A first count starts this month, with nothing owed from before. */
fun settingsDraftOf(settings: TitheSettings?, month: YearMonth): TitheSettingsDraft = settings?.let {
    TitheSettingsDraft(
        startMonth = it.startMonth,
        opening = it.opening.takeUnless { o -> o.isZero() }?.let { o -> amountText(Money(Math.abs(o.agorot))) }.orEmpty(),
        ahead = it.opening.isNegative(),
        portfolioAssetId = it.portfolioAssetId,
        fromFirstData = it.fromFirstData,
        depositsBefore = it.depositsBefore.takeUnless { d -> d.isZero() }?.let(::amountText).orEmpty(),
    )
} ?: TitheSettingsDraft(startMonth = month)

/** Empty amounts are zero; text that is not a number is treated as empty. */
fun buildSettings(draft: TitheSettingsDraft): TitheSettings {
    val opening = parseAmount(draft.opening) ?: Money.ZERO
    return TitheSettings(
        startMonth = draft.startMonth,
        opening = if (draft.ahead) Money(-opening.agorot) else opening,
        portfolioAssetId = draft.portfolioAssetId,
        depositsBefore = parseAmount(draft.depositsBefore) ?: Money.ZERO,
        fromFirstData = draft.fromFirstData,
    )
}
