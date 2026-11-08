Status: resolved (round 1: all four fixes applied after "auto accept")

# Goldfish Annotated Review Report

**Target:** PR [menashsoffer/2B#58](https://github.com/menashsoffer/2B/pull/58), branch `claude/hopeful-goldberg-shjthl` (code, not a document)
**Round:** 1. The findings came from an external review supplied by the user; no Goldfish subagents ran in this round.

## Prioritized Findings & AI Suggestions

### [Issue 1] [`external-review`] - Severity: Blocker (P1)
*   **Affected Scope:** [PlanImport.kt](app/src/main/java/com/menash/myself/feature/money/PlanImport.kt) (`expenseLine` CHANGED branch for a periodic budget; `newItem` for `ExpenseClass.PERIODIC`)
*   **Discrepancy Description:** A changed periodic budget replaces its single `annualAmount`, and a new one starts in January of the import year even when the import month is later.
*   **Rationale:** Earlier months read the same value, so the import can change the budget and balance of earlier open months after the fact.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** Confirmed in the code. `MoneyItems.forMonth` (`domain/MonthPlan.kt:132`) keeps a periodic budget for every month from `yearStartMonth` on, and `monthlyReserve` / `periodicStatus` (`domain/Actuals.kt:87-95`) read the one `annualAmount`. `PeriodicBudget` has no amount history, so the import cannot express "from this month on" for an existing budget. Closed months are frozen and are not affected; open earlier months are. This contradicts what the review screen and `docs/money/IMPORT.md` promise ("changes start at the import month").
*   ***Proposed Edit:***
    *   Location: `PlanImport.kt`, `newItem` and the periodic CHANGED branch; `PlanLineStatus`; `docs/money/IMPORT.md`.
    *   Change:
        1. A new periodic budget starts its year at the import month: `yearStartMonth = from`, not January.
        2. An existing periodic budget is changed only when no open month comes before the import month inside its budget year: every month in `[yearStartMonth, from)` is closed, or `yearStartMonth >= from`. Otherwise the line gets a new status, `PERIODIC_IN_USE` ("התקציב השנתי כבר פועל בחודשים פתוחים. מעדכנים ידנית"), shown and not selectable.
        3. Tests: a new periodic budget starts at `from`; a change is refused while an earlier month of its year is open, and allowed once those months are closed.
    *   Not proposed: amount history on `PeriodicBudget`. It changes the stored schema and the Firestore rules, which is outside this PR.

### [Issue 2] [`external-review`] - Severity: Major (P2)
*   **Affected Scope:** [PlanImport.kt](app/src/main/java/com/menash/myself/feature/money/PlanImport.kt) (`AmountHistory.from`)
*   **Discrepancy Description:** Updating an amount history keeps an existing override for the import month.
*   **Rationale:** `amountFor` reads the override first (`domain/MonthPlan.kt:11-13`), so the review shows a saved change while the month keeps the old amount.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** Confirmed: `from()` only replaces `changes`; `overrides` is copied as is.
*   ***Proposed Edit:***
    *   Location: `AmountHistory.from` in `PlanImport.kt`.
    *   Change: `copy(changes = ..., overrides = overrides - month)`. Overrides of later months stay: they are "this month only" choices the household made for those months. Add a test with an override at the import month.

### [Issue 3] [`external-review`] - Severity: Major (P2)
*   **Affected Scope:** [PlanImport.kt](app/src/main/java/com/menash/myself/feature/money/PlanImport.kt) (`key`, `draftLines`)
*   **Discrepancy Description:** The line key is group plus name, and the reader allows repeated names. Ticking one line ticks both, and confirming can save two records while the counter shows one.
*   **Rationale:** Two source lines can also match the same category or income, producing two items for one category.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** Confirmed: `key(line) = "${group}:${name}"`, `readPaamonimPlan` keeps every named row, and the selection is a `Set<String>` of keys.
*   ***Proposed Edit:***
    *   Location: `key`, `draftLines`, `PlanLineStatus`.
    *   Change: the key includes the source line's index (`"$index:${group}:${name}"`). After matching, lines of the same group whose names are equal once normalised (or that resolve to the same category) all get a new status, `DUPLICATE` ("השם מופיע יותר מפעם אחת בקובץ. מעדכנים ידנית"), shown and not selectable. Add tests for a repeated name and for two names that map to the same category ("דלק" and "דלק / חשמל").

### [Issue 4] [`external-review`] - Severity: Major (P2)
*   **Affected Scope:** [HomeViewModel.kt](app/src/main/java/com/menash/myself/feature/home/HomeViewModel.kt) (money flow), [MoneyViewModel.kt](app/src/main/java/com/menash/myself/feature/money/MoneyViewModel.kt) (`state`)
*   **Discrepancy Description:** The date is read when data emits, but a date change does not emit.
*   **Rationale:** If the app stays open across a month change, the steps and overview keep showing the previous month until data changes or the app reopens.
*   ***AI Suggestion:*** PROPOSE FIX
*   ***Justification:*** Confirmed: both view models call `today()` inside `combine` over repository flows only. The home screen's composable refreshes its clock every minute (`HomeScreen.kt`), but the view model does not.
*   ***Proposed Edit:***
    *   Location: a new `dayChanges(today: () -> LocalDate): Flow<LocalDate>` next to `moneyOverview` (emits today, then again after each midnight); an injectable `days` parameter in both view models, added to their `combine`.
    *   Change: the computations use the emitted date instead of calling `today()` inside the lambda. Tests drive a fake `days` flow across October to November and assert that the steps move to the new month without any data change.

## Resolution (round 1)

All four PROPOSE FIX items were applied:

1. **Issue 1:** a new periodic budget starts at the import month; a change to an existing one needs every earlier month of its year closed, otherwise `PERIODIC_IN_USE`. Tests: `newItemsUseTheRightTypeCategoryAndStartMonth`, `aPeriodicBudgetChangesOnlyWhenItsEarlierMonthsAreClosed`.
2. **Issue 2:** `AmountHistory.from` removes the override of the import month. Test: `aChangeReplacesAThisMonthOnlyOverrideOfTheImportMonth`.
3. **Issue 3:** each source line's key includes its index; lines matched to the same income or category are `DUPLICATE`. Test: `repeatedNamesAreKeptApartAndNotGuessed`.
4. **Issue 4:** `dayChanges` feeds both view models. The first date is emitted at once, and the minute checks run on `Dispatchers.Default`, so `runTest` is not left waiting on an endless timer (an earlier version hung `MoneyFactoriesTest`). Tests: `DayChangeTest`.

Contested: 0. Escalated: 0. Declined: 0. Open: 0.
