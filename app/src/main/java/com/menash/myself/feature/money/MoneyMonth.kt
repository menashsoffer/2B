package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.CardCharge
import com.menash.myself.feature.money.domain.Currency
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.MoneyItems
import com.menash.myself.feature.money.domain.MonthPlan
import com.menash.myself.feature.money.domain.amountFor
import com.menash.myself.feature.money.domain.cushionMonths
import com.menash.myself.feature.money.domain.isStale
import com.menash.myself.feature.money.domain.monthPlan
import com.menash.myself.feature.money.domain.PensionSnapshot
import com.menash.myself.feature.money.domain.netWorth
import com.menash.myself.feature.money.domain.pensionSnapshot
import com.menash.myself.feature.money.domain.totalWorth
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.StoredTransaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Screen-ready view of one month. Every amount here comes from an engine call
 * (monthPlan, netWorth, cushionMonths, isStale, pensionSnapshot, totalWorth); nothing is added or divided in this file.
 * A single item's monthly amount is monthPlan of a [MoneyItems] that holds only that item.
 */
enum class MonthRowKind { INCOME, FIXED, CURRENT, PERIODIC, INSTALLMENTS, DEBTS, SAVINGS }

/**
 * [monthly] is null for a USD subscription left out because no rate is stored.
 * [actual] stays null until imported transactions exist (planVsActual); the screen shows it only when set.
 */
data class MonthRowItem(val entry: MoneyEntry, val name: String, val monthly: Money?, val actual: Money? = null)

data class MonthRow(val kind: MonthRowKind, val total: Money, val items: List<MonthRowItem>, val actual: Money? = null)

/** One stacked-bar segment; the bar draws segments in proportion to [amount]. */
data class BarSegment(val kind: MonthRowKind?, val amount: Money)

sealed interface MonthView {
    data class Empty(val usdRateMissing: Boolean = false) : MonthView
    data class Planned(
        val plan: MonthPlan.Planned,
        val rows: List<MonthRow>,
        val cardCharges: List<CardCharge>,
        val bar: List<BarSegment>,
        val usdRateMissing: Boolean,
        /** Imported spending this month with no known category (uncategorised lines, cash, card bills without details). */
        val unassigned: Money? = null,
        /** Imported lines billed this month, shown or not; 0 hides the transactions entry. */
        val importedLines: Int = 0,
        /** The headline; null only before [buildMoneyMonth] fills it. */
        val status: MonthStatus? = null,
        /** Plan vs actual per category once lines are imported (planVsActual), for the month's charts. */
        val categoryActuals: List<com.menash.myself.feature.money.domain.CategoryActual> = emptyList(),
        /** The month's largest categories by actual spending, for the chart. */
        val top: CategoryTop = CategoryTop(emptyList(), 0),
    ) : MonthView
}

data class AssetRow(val asset: Asset, val stale: Boolean)

data class BigPicture(
    /** Null when no asset or liability is stored, so no invented zero is shown. */
    val netWorth: Money?,
    val assets: List<AssetRow>,
    /** Null while the month has no plan or no planned expenses. */
    val cushionMonths: Double?,
    val debts: List<MoneyEntry.DebtEntry>,
    val savings: List<MoneyEntry.SavingEntry>,
    /** Stored "האם אפשר?" goals; the entry row says how many. */
    val goalCount: Int = 0,
    /** Signed maaser balance (positive is still owed); null until the count has a start month. */
    val titheBalance: Money? = null,
    /** The closed-months average saving; null until a month is closed. */
    val monthlySaving: Money? = null,
    /** True when [cushionMonths] divides by the closed months' average spending rather than the plan. */
    val cushionFromClosed: Boolean = false,
    /** Today's pension, hishtalmut and gemel picture; null while no fund is stored. Not liquid, so not in the cushion. */
    val pension: PensionSnapshot? = null,
    /** [netWorth] plus the funds' value; null while neither exists. */
    val totalWorth: Money? = netWorth,
    /** Income and spending of the last 6 months, for the chart; closed, so far, or missing. */
    val flows: List<MonthFlowPoint> = emptyList(),
    /** Net worth after each closed month with balances, then today. */
    val worth: List<WorthPoint> = emptyList(),
)

/** [month] is the month shown; [current] is today's month. The big picture is always today's. */
data class MoneyMonth(val month: YearMonth, val view: MonthView, val bigPicture: BigPicture, val current: YearMonth = month) {
    val canGoNext: Boolean get() = month < current
    val canGoBack: Boolean get() = month > current.minusMonths(MonthsBack)
}

/** How far back the month screen goes (three years). */
const val MonthsBack = 36L

/**
 * [usdToIls] is the stored Bank of Israel rate, or null when none is stored. [transactions] are the imported
 * lines; with none the month shows the plan only.
 */
fun buildMoneyMonth(
    data: MoneyData,
    month: YearMonth,
    today: LocalDate,
    usdToIls: BigDecimal?,
    transactions: List<StoredTransaction> = emptyList(),
    memory: ImportMemory = ImportMemory(),
): MoneyMonth {
    val rate = usdToIls?.takeIf { it > BigDecimal.ZERO }
    val missingUsd = if (rate == null) {
        data.items.subscriptions.filter { it.currency == Currency.USD && amountFor(it, month) != null }
    } else {
        emptyList()
    }
    val missingIds = missingUsd.map { it.id }.toSet()
    val items = data.items.copy(subscriptions = data.items.subscriptions.filter { it.id !in missingIds })
    val plan = monthPlan(month, items, rate)
    val view = when (plan) {
        // A month whose only items are USD subscriptions without a rate stays a set-up month, with the chip.
        MonthPlan.Empty -> MonthView.Empty(usdRateMissing = missingUsd.isNotEmpty())
        is MonthPlan.Planned -> MonthView.Planned(
            plan = plan,
            rows = rows(data.items, plan, month, rate, missingIds),
            cardCharges = plan.cardCharges,
            bar = bar(plan),
            usdRateMissing = missingUsd.isNotEmpty(),
        ).let { withActuals(it, data.copy(items = items), month, rate, transactions, memory) }
            .let { it.copy(top = topCategories(it.categoryActuals, data.categories)) }
            .let { it.copy(status = monthStatus(it, data.closedMonths.lastOrNull { c -> c.month == month }, transactions, month)) }
    }
    val planned = (view as? MonthView.Planned)?.plan
    val closed = closedFigures(data.closedMonths)
    val closedCushion = closed.averageExpenses?.let { cushionMonths(data.assets, it) }
    val liquidWorth = data.assets.takeIf { it.isNotEmpty() }?.let { netWorth(it) }
    val pension = data.pensionFunds.takeIf { it.isNotEmpty() }?.let { pensionSnapshot(it, today) }
    return MoneyMonth(
        month = month,
        view = view,
        bigPicture = BigPicture(
            netWorth = liquidWorth,
            assets = data.assets.map { AssetRow(it, isStale(it, today)) },
            cushionMonths = closedCushion ?: planned?.let { cushionMonths(data.assets, it) },
            debts = data.items.debts.map { MoneyEntry.DebtEntry(it) },
            savings = data.items.savings.map { MoneyEntry.SavingEntry(it) },
            goalCount = data.goals.size,
            titheBalance = titheFromImports(data, month, transactions, memory)?.balance,
            monthlySaving = closed.monthlySaving,
            cushionFromClosed = closedCushion != null,
            pension = pension,
            totalWorth = totalWorth(liquidWorth, pension),
            flows = monthFlows(data.closedMonths, transactions, memory, data.categories, YearMonth.from(today)),
            worth = worthHistory(data.closedMonths, liquidWorth, YearMonth.from(today)),
        ),
    )
}

private fun rows(
    all: MoneyItems,
    plan: MonthPlan.Planned,
    month: YearMonth,
    rate: BigDecimal?,
    missingIds: Set<String>,
): List<MonthRow> {
    fun single(items: MoneyItems): MonthPlan.Planned = monthPlan(month, items, rate) as MonthPlan.Planned

    fun classItems(klass: ExpenseClass): List<MonthRowItem> =
        all.fixedExpenses.filter { it.klass == klass }.map {
            MonthRowItem(MoneyEntry.FixedExpenseEntry(it), it.name, single(MoneyItems(fixedExpenses = listOf(it))).fixed)
        } + all.subscriptions.filter { it.klass == klass }.map {
            val monthly = if (it.id in missingIds) null else single(MoneyItems(subscriptions = listOf(it))).subscriptions
            MonthRowItem(MoneyEntry.SubscriptionEntry(it), it.name, monthly)
        } + all.variableBudgets.filter { it.klass == klass }.map {
            MonthRowItem(MoneyEntry.VariableBudgetEntry(it), it.name, single(MoneyItems(variableBudgets = listOf(it))).variable)
        } + if (klass == ExpenseClass.PERIODIC) {
            all.periodicBudgets.map {
                MonthRowItem(MoneyEntry.PeriodicBudgetEntry(it), it.name, single(MoneyItems(periodicBudgets = listOf(it))).periodic)
            }
        } else {
            emptyList()
        }

    val rows = listOf(
        MonthRow(MonthRowKind.INCOME, plan.income, all.incomes.map {
            MonthRowItem(MoneyEntry.IncomeEntry(it), it.name, single(MoneyItems(incomes = listOf(it))).income)
        }),
        MonthRow(MonthRowKind.FIXED, plan.byClass[ExpenseClass.FIXED] ?: Money.ZERO, classItems(ExpenseClass.FIXED)),
        MonthRow(MonthRowKind.CURRENT, plan.byClass[ExpenseClass.CURRENT] ?: Money.ZERO, classItems(ExpenseClass.CURRENT)),
        MonthRow(MonthRowKind.PERIODIC, plan.byClass[ExpenseClass.PERIODIC] ?: Money.ZERO, classItems(ExpenseClass.PERIODIC)),
        MonthRow(MonthRowKind.INSTALLMENTS, plan.installments, all.installments.map {
            MonthRowItem(MoneyEntry.InstallmentEntry(it), it.name, single(MoneyItems(installments = listOf(it))).installments)
        }),
        MonthRow(MonthRowKind.DEBTS, plan.debtPayments, all.debts.map {
            MonthRowItem(MoneyEntry.DebtEntry(it), it.creditor, single(MoneyItems(debts = listOf(it))).debtPayments)
        }),
        MonthRow(MonthRowKind.SAVINGS, plan.savingDeposits, all.savings.map {
            MonthRowItem(MoneyEntry.SavingEntry(it), it.name, single(MoneyItems(savings = listOf(it))).savingDeposits)
        }),
    )
    return rows.filterNot { it.total.isZero() && it.items.isEmpty() }
}

private fun bar(plan: MonthPlan.Planned): List<BarSegment> = listOf(
    BarSegment(MonthRowKind.FIXED, plan.byClass[ExpenseClass.FIXED] ?: Money.ZERO),
    BarSegment(MonthRowKind.CURRENT, plan.byClass[ExpenseClass.CURRENT] ?: Money.ZERO),
    BarSegment(MonthRowKind.PERIODIC, plan.byClass[ExpenseClass.PERIODIC] ?: Money.ZERO),
    BarSegment(MonthRowKind.INSTALLMENTS, plan.installments),
    BarSegment(MonthRowKind.DEBTS, plan.debtPayments),
    BarSegment(MonthRowKind.SAVINGS, plan.savingDeposits),
    BarSegment(null, plan.expectedSurplus),
).filter { it.amount.isPositive() }
