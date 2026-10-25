package com.menash.myself.ui.calculators.ai

import android.content.Context
import android.content.Intent
import com.menash.myself.R
import com.menash.myself.core.domain.model.AiSubscription
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.util.CsvParser

object AiSubscriptionCsvHelpers {

    fun createDefaultAiSub(context: Context): AiSubscription = AiSubscription(
        name = context.getString(R.string.ui_ai_167),
        category = context.getString(R.string.ui_str_156),
        isActive = true,
        billingCycle = "MONTHLY",
        currency = "USD",
        price = Money.fromShekels(20.0),
        notes = ""
    )

    fun shareAiSubscriptionsCsv(
        context: Context,
        subscriptions: List<AiSubscription>,
        usdRate: Double
    ) {
        val headers = listOf(
            context.getString(R.string.ui_str_164),
            context.getString(R.string.ui_str_150),
            context.getString(R.string.ui_str_160),
            context.getString(R.string.ui_str_152),
            context.getString(R.string.ui_str_158),
            context.getString(R.string.ui_str_161),
            context.getString(R.string.ui_str_137),
            context.getString(R.string.ui_str_121)
        )
        val rows = subscriptions.map {
            listOf(
                it.name,
                it.category,
                if (it.isActive) context.getString(R.string.ui_str_160) else context.getString(R.string.ui_str_171),
                it.billingCycle,
                it.currency,
                it.price.shekels.toString(),
                it.monthlyCostIls(usdRate).shekels.toInt().toString(),
                it.notes
            )
        }
        val csv = CsvParser.exportToCsv(headers, rows)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_TEXT, csv)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.ui_aicsv_157))
        }
        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.ui_ai_155)))
    }

    fun parseMappedAiSubs(
        context: Context,
        dataRows: List<List<String>>,
        mapping: Map<String, Int>
    ): List<AiSubscription> = dataRows.mapNotNull { row ->
        try {
            val name = row.getOrNull(mapping[context.getString(R.string.ui_str_164)] ?: -1) ?: context.getString(R.string.ui_str_153)
            val category = row.getOrNull(mapping[context.getString(R.string.ui_str_150)] ?: -1) ?: context.getString(R.string.ui_str_162)
            val price = row.getOrNull(mapping[context.getString(R.string.ui_str_161)] ?: -1)?.toDoubleOrNull() ?: 15.0
            val currencyRaw = row.getOrNull(mapping[context.getString(R.string.ui_usdils_159)] ?: -1)?.uppercase() ?: "USD"
            val currency = if (currencyRaw.contains("ILS") || currencyRaw.contains("₪") || currencyRaw.contains("שח")) "ILS" else "USD"
            val cycleRaw = row.getOrNull(mapping[context.getString(R.string.ui_str_165)] ?: -1)?.uppercase() ?: "MONTHLY"
            val cycle = if (cycleRaw.contains("YEAR") || cycleRaw.contains("שנתי")) "YEARLY" else "MONTHLY"
            val notes = row.getOrNull(mapping[context.getString(R.string.ui_str_121)] ?: -1) ?: ""

            AiSubscription(
                name = name,
                category = category,
                isActive = true,
                billingCycle = cycle,
                currency = currency,
                price = Money.fromShekels(price),
                notes = notes
            )
        } catch (e: Exception) {
            null
        }
    }
}
