package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth

/** Money transferred into the One Zero investment account from [from] on: the portfolio's deposits for chomesh. */
fun portfolioDeposits(rows: List<StoredTransaction>, from: YearMonth, memory: ImportMemory = ImportMemory()): Money =
    Money(rows.filter { it.row.status == RowStatus.COUNTED && it.row.billingMonth >= from && it.row.charge != null }
        .filter { classify(it.row, memory) == RowKind.PORTFOLIO_DEPOSIT }
        .sumOf { -it.row.charge!!.agorot })
