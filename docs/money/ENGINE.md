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
- `TitheSettings.fromFirstData` is true for new settings. Documents lacking it decode as false and retain their stored start month. When enabled, imported billing months can move the effective start earlier than the stored start; without imports the effective start is the current month. Portfolio deposits use that same effective start. Income still follows the existing source/rate and linking rules.
- Counted imported expenses assigned to the stable donation categories contribute to the billing month's donated total, alongside manual donations. A manual donation with the same amount within three days of an imported donation is presented as a possible duplicate. Confirming the match stores `matchedLineId` on the manual donation and excludes that manual amount from the balance; the imported line remains counted.
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
- `Debt(id, creditor, balance, asOf, annualInterestPercent, remainingPayments, monthlyPayment)`: monthlyPayment is planned in each of the remainingPayments months after the month of asOf (`Planned.debtPayments`). `Saving(id, name, monthlyDeposit, accumulated, maturity?, startMonth?)`: the deposit is planned from a known start through maturity; legacy documents without a start retain their engine behavior (`Planned.savingDeposits`). Both reduce `expectedSurplus`; `plannedExpenses` includes periodic and debt payments but not saving deposits. Interest is stored, not computed.

## Month close

- `monthsAwaitingClose(today, stored, closed)` returns distinct ended months with imported rows and no close, oldest first. `closeTarget` chooses the first, or the previous month when there are none. The close screen never navigates into the current month. After a confirmed save, it offers the next pending month explicitly; it does not navigate automatically.
- `earliestMonth(current, stored, closed)` extends month navigation to the oldest imported or closed month, with at least twelve months before the current month available.
- `buildMoneyMonth` uses `MonthView.ActualOnly` when imported lines exist and no plan is effective for that month. It displays counted income, spending, savings, top categories and unassigned spending without a plan comparison. New savings carry a `startMonth`. Legacy savings without one have an unknown start and are excluded from past month screens; they remain available in the current month. Editing a legacy saving preserves the unknown start.
- `importCoverage` groups stored rows by source and source id and returns the first and last billing months and internal gaps. A gap indicates no row, not proof of a missing file.
- `closeView` expects sources with rows in the previous three months, except a source marked `closedFrom` on or before the close month. This mark changes only the missing-source check, never transaction counting. Members can undo it.
- `CloseInput(month, today, sourcesThisMonth, sourcesExpected, unassignedCount, pendingCount, assets)` supplies every checklist input. `closeChecks` returns `CloseCheck(kind, blocking, detail)` in `CloseCheckKind` order: MONTH_NOT_OVER, MISSING_SOURCE, UNASSIGNED, PENDING, STALE_BALANCE. Missing sources and stale assets are sorted by id. Details contain the month, source key, count, or asset id.
- The close screen can assign all still-unassigned counted lines of the selected month to the household’s `שונות` category after confirmation. It creates that category when needed and writes each line individually without teaching merchant memory. Unassigned lines remain a blocking check.
- Closing is blocked through the month's last day, for each missing expected source, for a positive unassigned count, and for each asset last updated strictly before `month.atEndOfMonth().minusDays(7)`. The exact seven-day boundary is fresh. This close-specific rule differs from the existing three-month `isStale` rule. Pending lines produce a nonblocking warning; `canClose(checks)` means no blocking check remains.
- `closeMonth(month, items, transactions, actualIncome, assets: List<Asset>, usdToIls: BigDecimal?, today)` freezes `ClosedMonth(month, snapshot, byCategory, periodic, balances, liabilities, closedOn)`. It sums supplied counted transactions whose **billing month** matches, with checked agorot arithmetic. Refunds reduce expenses; only the final total is clamped to zero. Actual income is supplied separately. Category actuals come directly from `planVsActual.categories` (null means uncategorised); matched installments count once in total expenses and remain outside category actuals.
- The snapshot retains the existing `MonthSnapshot` type with `balances = emptyList()`. Historical balances instead live in separate `ClosedMonth.balances` and `ClosedMonth.liabilities` maps, each asset id to its nonnegative `Money` magnitude, including zero. Duplicate asset ids are rejected even across kinds. No asset names, types, liquidity flags or invented metadata are frozen. Later changes to input lists do not change the closed values.
- Periodic budgets renew annually. A budget is omitted from `periodic` **only** if its `yearStartMonth` is after the closing month. Otherwise `yearsElapsed = MONTHS.between(yearStartMonth, month) / 12`; a copy advances the start by that many whole years, preserving id, category, name and annual amount. `periodicStatus` receives that copy and the complete transaction history, and counts spending only from the renewed start through the closing month. The original budget is unchanged; prior-year spending does not carry forward. A January budget closed in March of the following year accrues three months of the new year. Non-January years renew at their own anniversary. Spending ahead of accrual remains visible as positive `advance`.
- The caller must enforce the checklist, identify expected sources, supply counted transactions and complete periodic history, actual income, the required exchange rate, and explicit dates. `closeMonth` sets `closedOn = today` without enforcing eligibility itself. Import classification, UI, persistence writes and authorization remain caller responsibilities.
- `upsert(closed, new)` supports re-closing: the new close replaces its month, the last existing entry wins other duplicate months, and the result is sorted ascending. `averageMonthlyExpenses` uses the latest three closed months (or fewer), rounds HALF_UP to agorot, and returns null for no closes. Calendar gaps add no zero months; duplicate months are rejected, including older entries outside the latest three. `monthlySaving` delegates directly to `savingRate` on the snapshots.
- `ClosedMonthDocuments.encode` / `decode(id, map)` are pure mappings for canonical `yyyy-MM` document ids. Exactly seven fields are encoded: `actualIncomes`, `actualExpenses`, `byCategory`, `periodic`, `balances`, `liabilities`, `closedOn`. Money is stored as `Long` agorot, close dates as ISO strings. Periodic values contain accrued, spent and remaining amounts; `advance` is derived. Null category is `_none`; that literal category id is reserved and rejected on encode. Income, expense and close date are required; absent optional maps become empty, while malformed present values fail.

## Pension snapshot (CODEX-6)

Current values only, no projection (Menash's decision).
- `PensionFund(kind PENSION | HISHTALMUT | GEMEL, value, totalDeposits, monthlyDeposit, feeOnBalance %, feeOnDeposits %, openedOn?, lastUpdated)`.
- `fundSnapshot`: `profit = value - totalDeposits` (may be negative; it is cumulative profit, not a return). `yearlyFee = value * feeOnBalance% + monthlyDeposit * 12 * feeOnDeposits%`. `liquidOn = openedOn + 6 years` for HISHTALMUT only.
- `pensionSnapshot(funds, today)`: totals, `priciest` = highest yearly fee in shekels (ties by id, null when every fee is zero), `liquidNow`, `stale` = last updated more than 4 months ago.
- Example: 200,000 at 0.5% from balance and 0% from deposits costs 1,000 a year. 100,000 at 0.2% plus 2,000 a month at 2% costs 200 + 480 = 680. Then the first is the priciest, although its deposit fee is lower.

## Car cost (CODEX-6)

- `CarProfile` with `Energy.Fuel | Electric | Known`. km/liter and kWh/100km must be above zero: the legacy fallbacks (empty = 1.0, zero = free fuel) are gone. `yearlyRunning` / `monthlyRunning` round once.
- `compareOptions(car, options, horizonMonths = 60)` compares `Cash` (with compound lost return), `BankLoan` (Shpitzer), `DealerFinancing` (0% priced against the cash discount, balloon), and `Leasing` (included items leave the running cost, extra km charged). Each option can take an optional trade-in. A loan longer than the horizon is settled at the horizon (remaining principal plus `earlyRepaymentFee`). A lease shorter than the horizon is `NotComparable(LEASE_SHORTER_THAN_HORIZON)`; no extension is invented. `netMonthly` is never clamped, so negative means a gain.
- Example: 100,000 at 6% over 60 months pays 1,933.28 a month and 15,996.81 interest in total (Excel PMT reference in `CarFinancingTest`).
- `carActualYearly(rows, carCategoryIds, endMonth)`: counted transactions in the car categories over the 12 billing months ending at `endMonth`, refunds reducing it. Null when there is none. It feeds `Energy.Known` and the other yearly costs of the current car.
- Noticed, not changed: `BankLoan` does not charge a lost return on its down payment while `Cash` does. Compare cash and loan with a down payment of zero, or add the field later.

## Rounding

Uses existing `core.domain.model.Money` in integer minor units. Annual equivalents divide by 12. USD conversion multiplies by the caller's decimal rate; annual division and currency conversion round together once to agorot, HALF_UP. Installments use integer division and put the entire remainder on the first payment, so their sum exactly equals the total, even when the total is smaller than the payment count. Snapshot averages use Money's HALF_UP division. Summation and surplus/net-worth subtraction detect Long overflow instead of wrapping. Cushion ratios use Double and are not rounded to currency.

## Not included / Noticed, not changed

No persistence, UI, Android dependencies, imports, bank connections, actual purchase tracking or household permissions. Existing Android minimum-version/java.time compatibility and build configuration are outside this package's scope; callers must provide compatible runtime support. No Gradle or credential configuration is added.

Month close leaves legacy snapshot balances empty and stores historical balances in separate asset/liability maps. Firestore rules and repository support `closedMonths` and `sourceClosures`; deployment of rules is separate from a code change.

## Year summary

Only closed months of the requested calendar year count. Missing months between the first and last close are reported, not treated as zero; months outside that span are not reported missing. Duplicate closes for a selected year are rejected. Money uses checked agorot arithmetic and published averages round HALF_UP once; percentages round to one decimal.

- `yearSummary(closed, 2026)` returns totals, saving, saving percentage, average expenses and best/worst months, or `null` without closes. For example, January and March closes report February as missing.
- `categoryYear(closed, 2026, plans)` sums plans only for closed months. A plan of 100 and actual of 120 gives a difference of 20; a category without a plan has a null difference. The null category is retained.
- `classYear(rows, categories)` sums actuals by each category's own class; an unknown or null category maps to the null class. For example, a CURRENT child contributes to CURRENT even if its parent is FIXED.
- `periodicAtYearEnd(closed, 2026)` returns the last close's periodic map unchanged, or an empty map if there is no close.
- `netWorthChange(closed, 2026)` compares the last known balance in 2026 with the last known balance in 2025, or the first known balance in 2026. A month with both balance maps empty is unknown and skipped. For example, net worth of 100 followed by 80 gives a change of -20.
- `compareYears(closed, 2026, plans)` compares monthly averages, not totals, and returns the number of closes in each year. For example, 600 over six months compared with 900 over three gives a monthly difference of -200. A category absent in one year has an average of zero there.

## Plan calibration

The engine only suggests changes; the caller decides what to accept. Money is in agorot.

- `categoryBaseline(closed, endMonth, 6)` summarizes the latest six closed months through the end month. Missing calendar months are unknown, not zero; an absent category in a closed month is zero. It returns average, median, range, latest and active months. Duplicate closes are rejected.
- `suggestPlan(baseline, currentPlan, categories)` compares non-periodic categories with their plans. For example, a current category averaging ₪120 against a ₪100 plan is under-planned at the default 10% tolerance. Fixed categories use the latest month; other categories use the average. At least three closes are needed. Proposed amounts round up to ₪50 by default.
- `suggestPeriodic(closed, budgets, endMonth)` proposes annual budgets only with all twelve calendar months closed. For example, ₪1,020 spent over a complete year rounds up to ₪1,100 with the default ₪100 step. Partial years return no spending or suggestion.
- `planImpact(currentPlan, suggestions, accepted, periodic, acceptedPeriodic, budgets)` compares monthly totals after only accepted, available suggestions are applied. For example, accepting a ₪50 monthly increase gives a ₪50 difference; annual budgets use their monthly reserve.

## Period summary

The year summary generalised to any inclusive run of months (`MonthRange`), for the period screen. Only closed months count; the caller picks the end, normally the last month that can be closed.

- `MonthRange(from, to)` rejects an end before the start. `MonthRange.last(end, n)` is the n months ending with `end` (across years). `MonthRange.year(y, end)` is January through `end` when `end` is in `y`, otherwise the whole year.
- `periodSummary(closed, range)` returns totals, saving and saving percentage, monthly averages of income, expenses and saving (HALF_UP once), a `MonthFlow` per closed month, best/worst month, or `null` with no close in the range. **Every month of the range without a close is reported in `missingMonths`, never counted as zero.** Duplicate closes are rejected.
- `categoryPeriod(closed, range, plans)` is `categoryYear` over the range: plans are summed only for closed months. For a calendar year it equals `categoryYear`.
- `periodicAtPeriodEnd(closed, range)` returns the periodic map of the last close in the range.
- `netWorthChange(closed, range)` compares the last close in the range with known balances against the last known close before the range (any earlier month), or the first known close inside it. A close with both balance maps empty is unknown and skipped.
