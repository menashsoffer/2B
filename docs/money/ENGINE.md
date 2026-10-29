# Money engine

Pure Kotlin under `feature.money.domain`; all inputs are supplied by the caller. No clock, network or mutable global state is used.

## Public inputs and functions

- `AmountHistory(changes, overrides)` stores dated defaults and month-specific amounts. `amountFor(RecurringItem, YearMonth): Money?` selects the month's override first, otherwise the latest default effective on or before that month, otherwise null. Histories may be unsorted; duplicate effective months and negative amounts are rejected. A zero override is valid. An explicit override can establish an amount before the first default. Copy a history with an added change for “from now on”; copy its override map for “this month only”.
- `Income`, `FixedExpense`, `Subscription`, and `VariableBudget` carry histories. Fixed expenses and subscriptions support MONTHLY/YEARLY. Subscription history amounts use the declared ILS or USD currency (minor units); all results are ILS. A YEARLY subscription is billed in the month of its renewalDate; a YEARLY fixed expense requires `billingMonth`.
- Every card, item and asset has a non-blank `id`. `Card(id, name, billingDay)` accepts days 1–31. Card charges are grouped by card id, so two cards with the same name and billing day stay separate. `chargeDate(card, month)` clamps the day to the month's last day.
- `InstallmentPurchase` takes total, positive payment count (default 1), an explicit current month, first month (default current month + 1), and optional card. `installmentSchedule` returns dated payments. No system date is consulted.
- `monthPlan(month, MoneyItems, usdToIls: BigDecimal?): MonthPlan` returns `Empty` when all five item lists are empty. Otherwise `Planned` contains income, fixed, subscription, variable and installment totals, expected surplus, and `CardCharge(card, date, amount)` values. Existing items inactive that month produce real zero totals. `Planned.plannedExpenses` = fixed + subscriptions + variable + installments. Totals and surplus use monthly equivalents (a yearly item counts amount / 12 every month). Card charges are what the card is really billed: a monthly item every month, a yearly item its full amount only in its billing month and zero in other months. Card totals include only assigned expenses, never income. A given exchange rate must be positive; null is allowed only when no USD subscription is active in the month, otherwise `IllegalArgumentException`.
- `Asset(name, type, balance, kind, liquid, lastUpdated)` stores a nonnegative balance; kind determines asset versus liability. `netWorth(assets): Money` subtracts liabilities from assets. `cushionMonths(assets, averageMonthlyExpenses: Money?): Double?` divides liquid assets (excluding liabilities) by expenses; unknown or nonpositive expenses return null. The overload `cushionMonths(assets, plan: MonthPlan.Planned)` uses `plan.plannedExpenses`.
- `MonthSnapshot(month, actualIncomes, actualExpenses, balances)` represents one month close. `savingRate(snapshots): Money?` is the **monthly monetary average surplus, not a percentage**, from the latest three distinct snapshot months (or fewer). Empty input returns null; duplicate month closes are rejected. Snapshot balances are historical values and do not affect this surplus calculation.
- `isStale(asset, today)` is true strictly after `lastUpdated.plusMonths(3)`. Exactly three calendar months is fresh; month-end uses Java calendar clamping. Future updates are fresh.

## Is it possible? (`isItPossible(FeasibilityInput): Feasibility`)

- `Goal(id, name, amount, targetMonth, allocated, newMonthlyCost?)`. Months left = months from current to target, at least 1; a main goal whose target is already past is rejected. Other approved goals whose target has passed count as one month.
- `neededPerMonth = ceil((amount − allocated) / months left)`, rounded up so the goal is really reached.
- `free = savingRate − Σ neededPerMonth(other approved goals)`.
- Safety cushion first: target = `cushionMinMonths` (default 3) × average monthly expenses. While liquid assets are below it, `gap / cushionFillMonths` (default 12) is taken from `free` before the goal, and `TakeFromSavings` is never offered.
- `Possible(monthly)` when needed ≤ free after the cushion and, if a new monthly cost is given, `free − newMonthlyCost > 0`.
- `PossibleIf(monthly, levers)` with up to 3 levers in this order: `Postpone(earliest month, ≤ 60 months)`, `Cut(per month, ≤ variable budgets total)`, `TakeFromSavings(amount, only above the cushion)`, `Installments(payments ≤ 36, per month)`. When the only problem is the new monthly cost, the only lever is `Cut` (postponing does not fix a recurring cost).
- `NotNow(gap per month, closest lever)` when no lever closes it; the closest lever is a postpone beyond 60 months, or null.
- `Unknown` when savingRate is null. Postpone is conservative: it does not assume the cushion share is freed later.

## Charity (`charityBalance`, `unsubmittedThisYear`)

- `Income.titheRate`: `MAASER` (10%), `CHOMESH` (20%), `NONE`. `ActualIncome(incomeId, month, net)` is actual net income per source; an unknown income id is rejected.
- balance = 10% × Σ MAASER actual income + 20% × Σ CHOMESH actual income + 20% × (current portfolio value − total deposits) − Σ donations. Each percentage rounds once, HALF_UP. The portfolio part uses current values and is negative on a loss, so a losing portfolio reduces the debt and can produce credit.
- Result: `Owed(amount)`, `Credit(amount)` (positive amount), or `Settled`.
- `unsubmittedThisYear(donations, year)` returns the count and sum of donations dated in that year with `submittedForTaxRefund = false`.

## Paamonim model (CODEX-3)

- `ExpenseClass`: `FIXED` (חודשיות קבועות), `CURRENT` (שוטפות, monthly budget), `PERIODIC` (תקופתיות, annual budget). `FixedExpense` and `Subscription` default to FIXED, `VariableBudget` to CURRENT; each also takes an optional `categoryId`. `Planned.byClass` gives the monthly equivalent per class; installments, debts and savings are outside the classes.
- `Category(id, name, klass, parentId?)`. `defaultCategories()` returns the Paamonim רישומון list with stable ids `fixed-N`, `sub-N` (sub-items of מנויים), `periodic-N`, `current-N`. Only append to the lists, so existing ids never shift.
- `PeriodicBudget(id, categoryId, name, annualAmount, yearStartMonth)`: the plan reserves `annualAmount / 12` (HALF_UP) every month (`Planned.periodic`). `periodicStatus(budget, month, transactions)` (month within the 12-month budget year) returns accruedToDate = annual × months elapsed / 12, spentToDate (transactions of the category billed from the year start through the month), remainingThisYear = annual − spent, and advance = spent − accrued (positive = spent ahead).
- `Transaction(id, date, billingMonth, merchant, amount, paymentMethod, sourceId, categoryId?, originalAmount?, originalCurrency, installment?)`. amount is the ILS charged on this statement, refunds negative; billingMonth is the statement month. `PaymentMethod`: CASH, CHECK, CREDIT_CARD, STANDING_ORDER, VOUCHER.
- `planVsActual(month, items, transactions, usdToIls)`: per categoryId, planned (monthly equivalents of items with that category, periodic reserve) vs actual (transactions billed that month), plus a null-category "uncategorised" row; `difference = planned − actual`. A transaction with installment info that matches an `InstallmentPurchase` (purchase card id = transaction sourceId, a payment scheduled that month, amount within 1 agora) counts once, against the purchase (`installments`), never also in a category. Each purchase matches at most one transaction.
- Merchant memory: `normalizeMerchant` trims, collapses spaces, lower-cases Latin letters and drops trailing branch/card numbers ("123", "סניף 12", "*1234", "#5"); Hebrew words are kept, so "רמי לוי" and "רמי לוי תקשורת" stay separate. `suggestCategory(merchant, memory)` and `learn(memory, merchant, categoryId)` (returns a new `MerchantMemory`).
- `importCheck(rows, statementTotal)`: `Ok` or `Mismatch(Σ rows − total)` in exact agorot.
- `Debt(id, creditor, balance, asOf, annualInterestPercent, remainingPayments, monthlyPayment)`: monthlyPayment is planned in each of the remainingPayments months after the month of asOf (`Planned.debtPayments`). `Saving(id, name, monthlyDeposit, accumulated, maturity?)`: the deposit is planned every month up to and including maturity (`Planned.savingDeposits`). Both reduce `expectedSurplus`; `plannedExpenses` includes periodic and debt payments but not saving deposits. Interest is stored, not computed.

## Rounding

Uses existing `core.domain.model.Money` in integer minor units. Annual equivalents divide by 12. USD conversion multiplies by the caller's decimal rate; annual division and currency conversion round together once to agorot, HALF_UP. Installments use integer division and put the entire remainder on the first payment, so their sum exactly equals the total, even when the total is smaller than the payment count. Snapshot averages use Money's HALF_UP division. Summation and surplus/net-worth subtraction detect Long overflow instead of wrapping. Cushion ratios use Double and are not rounded to currency.

## Not included / Noticed, not changed

No persistence, UI, Android dependencies, imports, bank connections, actual purchase tracking or household permissions. Existing Android minimum-version/java.time compatibility and build configuration are outside this package's scope; callers must provide compatible runtime support. No Gradle or credential configuration is added.
