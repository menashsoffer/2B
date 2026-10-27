# Money engine

Pure Kotlin under `feature.money.domain`; all inputs are supplied by the caller. No clock, network or mutable global state is used.

## Public inputs and functions

- `AmountHistory(changes, overrides)` stores dated defaults and month-specific amounts. `amountFor(RecurringItem, YearMonth): Money?` selects the month's override first, otherwise the latest default effective on or before that month, otherwise null. Histories may be unsorted; duplicate effective months and negative amounts are rejected. A zero override is valid. An explicit override can establish an amount before the first default. Copy a history with an added change for “from now on”; copy its override map for “this month only”.
- `Income`, `FixedExpense`, `Subscription`, and `VariableBudget` carry histories. Fixed expenses and subscriptions support MONTHLY/YEARLY. Subscription history amounts use the declared ILS or USD currency (minor units); all results are ILS. Renewal date is metadata, not a billing trigger.
- `Card(name, billingDay)` accepts days 1–31. Card equality uses both fields; callers must consistently reuse the same card value. `chargeDate(card, month)` clamps the day to the month's last day.
- `InstallmentPurchase` takes total, positive payment count (default 1), an explicit current month, first month (default current month + 1), and optional card. `installmentSchedule` returns dated payments. No system date is consulted.
- `monthPlan(month, MoneyItems, usdToIls: BigDecimal): MonthPlan` returns `Empty` when all five item lists are empty. Otherwise `Planned` contains income, fixed, subscription, variable and installment totals, expected surplus, and `CardCharge(card, date, amount)` values. Existing items inactive that month produce real zero totals. Card totals include only assigned expenses, never income; they are monthly planning equivalents, including annual items, not predictions of an annual renewal's lump-sum bank debit. The supplied exchange rate must be positive.
- `Asset(name, type, balance, kind, liquid, lastUpdated)` stores a nonnegative balance; kind determines asset versus liability. `netWorth(assets): Money` subtracts liabilities from assets. `cushionMonths(assets, averageMonthlyExpenses: Money?): Double?` divides liquid assets (excluding liabilities) by expenses; unknown or nonpositive expenses return null.
- `MonthSnapshot(month, actualIncomes, actualExpenses, balances)` represents one month close. `savingRate(snapshots): Money?` is the **monthly monetary average surplus, not a percentage**, from the latest three distinct snapshot months (or fewer). Empty input returns null; duplicate month closes are rejected. Snapshot balances are historical values and do not affect this surplus calculation.
- `isStale(asset, today)` is true strictly after `lastUpdated.plusMonths(3)`. Exactly three calendar months is fresh; month-end uses Java calendar clamping. Future updates are fresh.

## Rounding

Uses existing `core.domain.model.Money` in integer minor units. Annual equivalents divide by 12. USD conversion multiplies by the caller's decimal rate; annual division and currency conversion round together once to agorot, HALF_UP. Installments use integer division and put the entire remainder on the first payment, so their sum exactly equals the total, even when the total is smaller than the payment count. Snapshot averages use Money's HALF_UP division. Summation and surplus/net-worth subtraction detect Long overflow instead of wrapping. Cushion ratios use Double and are not rounded to currency.

## Not included / Noticed, not changed

No persistence, UI, Android dependencies, imports, bank connections, actual purchase tracking, household permissions, or “is it possible” decision tool. No existing file is changed. Annual planned card equivalents are intentionally not cash-flow billing schedules. Existing Android minimum-version/java.time compatibility and build configuration are outside this package's scope; callers must provide compatible runtime support. No Gradle or credential configuration is added.
