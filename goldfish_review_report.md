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

---

# Round 2: independent Goldfish reviewers

**Reviewers (all completed):** `codebase_details` (money-logic correctness), `design_critic` (architecture and data flow), `comprehension` (Hebrew UI as a household member reads it). Each ran in a fresh context, read-only.
**Round 1 fixes checked by reviewers:** 2, 3 and 4 work. Fix 1 (periodic) is re-opened as R2-C2/C3.
**Clean round?** No: 1 Blocker and 15 Majors open.

## Blocker

### [R2-C1] [`codebase_details`] One missing USD rate stops the money tab and the home card
*   **Affected Scope:** `MonthTasks.kt:51` → `closeView` → `closeMonth` → `planVsActual` → `rateFor` (`domain/MonthPlan.kt:77-80`, `requireNotNull`)
*   **Discrepancy:** `monthTasks` runs `closeView` on every emission; unlike `buildMoneyMonth`, it does not drop USD subscriptions when no rate is stored. The throw reaches `.catch`, which ends the flow (see R2-D5).
*   ***AI Suggestion:*** PROPOSE FIX. Verified in code.
*   ***Proposed Edit:*** `monthTasks` builds the close view from items without USD subscriptions when the rate is null (the same rule as `buildMoneyMonth`). Together with R2-D5, a failure in the steps can no longer freeze the screen. Test: USD subscription, no rate, an open month with lines.

## Major

### [R2-C2 + R2-C3] [`codebase_details`] Periodic change still reaches closed and past months; the closed-year check ignores renewal and empty months (re-opens round 1, issue 1)
*   **Affected Scope:** `PlanImport.kt` (`yearIsClosedBefore`, periodic CHANGED)
*   ***AI Suggestion:*** PROPOSE FIX. Verified: closed months draw their plan from the live `annualAmount`; `periodicStatus` accrues from the year start; the year renews (`MonthCloseSnapshot.kt:46-47`).
*   ***Proposed Edit:*** the import never changes an existing periodic budget. Any different amount is `PERIODIC_IN_USE` ("update manually"), and `yearIsClosedBefore` is removed. New periodic budgets still start at the import month. Amount history on `PeriodicBudget` stays out of this PR (schema and rules change). IMPORT.md is corrected.

### [R2-C4] [`codebase_details`] History plan turns a yearly fixed expense into a monthly one ×12
*   ***AI Suggestion:*** PROPOSE FIX. ***Proposed Edit:*** `planFromHistory` leaves out categories that hold a YEARLY fixed expense; the subtitle already says the draft is partial. Test.

### [R2-C5] [`codebase_details`] Case-only or sub-category name matches create a second item
*   ***AI Suggestion:*** PROPOSE FIX. ***Proposed Edit:*** one matcher (trimmed, spaces collapsed, case-insensitive) for the category lookup and for `target()`. When the only match is a sub-category, the line is SPLIT (not guessed). Tests: "Yes"/"YES", "גוגל" as FIXED, "Netflix"/"netflix".

### [R2-C6] [`codebase_details`] Confirm writes the snapshot taken when the review opened
*   ***AI Suggestion:*** PROPOSE FIX. ***Proposed Edit:*** the draft keeps its source lines; `confirmPlan` rebuilds the draft from fresh data and the current month, then writes only the selected keys that are still changeable (keys are the stable source index). Test: data changes between review and confirm.

### [R2-D1] [`design_critic`] A step tapped on Home opens under a money screen that is already open
*   ***AI Suggestion:*** PROPOSE FIX. Verified: `moneyDestination` priority (`MoneyDestination.kt:37-50`).
*   ***Proposed Edit:*** before opening the step, close the import, transactions and other screens that rank above the target (the same pattern as the `pendingUnassigned` handler).

### [R2-D2] [`design_critic`] Heavy work on the main thread, twice per snapshot
*   ***AI Suggestion:*** PROPOSE FIX. ***Proposed Edit:*** `.flowOn(Dispatchers.Default)` before `stateIn` in `MoneyViewModel` and `HomeViewModel`. The KDoc says "one function", not "one computation". An app-scoped shared flow is not proposed in this PR.

### [R2-D3] [`design_critic`] `spendingHistory` computed in full only to read its size
*   ***AI Suggestion:*** PROPOSE FIX. ***Proposed Edit:*** skip it when the month is planned (the row is hidden), and run `planVsActual` only for the last six candidate months.

### [R2-D4] [`design_critic`] The offered month count can lead to an empty draft; a null draft closes silently
*   ***AI Suggestion:*** PROPOSE FIX. ***Proposed Edit:*** the row is offered only when `planFromHistory` has at least one line. `openHistoryPlan` shows "not enough months with categorised spending" instead of closing silently.

### [R2-D5] [`design_critic`] One exception stops the money state for good
*   ***AI Suggestion:*** PROPOSE FIX. ***Proposed Edit:*** the steps, freshness and history count are computed inside a per-emission try/catch (reported, empty on failure), so the month screen and the flow survive.

### [R2-U1] [`comprehension`] "לייבא את הקובץ…" opens the close screen
*   ***AI Suggestion:*** PROPOSE FIX (wording; the close screen is the right place, since it offers import, "no charges" and "source closed"). ***Proposed Edit:*** `task_missing_file` = "חסר הקובץ של %1$s (מסתיים ב־%2$s)"; the detail stays "לפני שסוגרים את X".

### [R2-U2] [`comprehension`] A changed line does not say it replaces today's amount
*   ***AI Suggestion:*** PROPOSE FIX. ***Proposed Edit:*** `plan_import_changed` = "יחליף את %1$s שבתוכנית היום".

### [R2-U3] [`comprehension`] Close-screen action buttons look like status text
*   ***AI Suggestion:*** PROPOSE FIX. "לבטל את הסגירה" is new in this PR; the other two buttons were already on `main`. ***Proposed Edit:*** all three use the accent colour like "לסווג את השאר כ'שונות'". New wording: "לא היו חיובים בכרטיס החודש", "הכרטיס נסגר, לא לחכות לקובץ". **This changes existing `MonthCloseScreenshotTest` goldens, which needs your approval.**

### [R2-U4] [`comprehension`] TalkBack says "פתוח"/"סגור" for expand state, the same words as the month status
*   ***AI Suggestion:*** PROPOSE FIX. ***Proposed Edit:*** `money_expanded` = "מורחב", `money_collapsed` = "מקופל".

### [R2-U5] [`comprehension`] One number, two names ("פנוי החודש, צפי" on Home and "צפוי להישאר לפי התוכנית" in the month tab)
*   ***AI Suggestion:*** PROPOSE FIX. ***Proposed Edit:*** `home_money_planned` = "צפוי להישאר לפי התוכנית".

### [R2-U6] [`comprehension`] The freshness line reads as two separate facts
*   ***AI Suggestion:*** PROPOSE FIX. ***Proposed Edit:*** `home_money_fresh` = "%2$s %3$s מעודכן עד %1$s. שאר החשבונות עדכניים יותר" (the home golden changes; it is new in this PR).

## Nits

| Id | Finding | Suggestion |
|---|---|---|
| R2-U7 | "להכניס 1 לתוכנית", "1 שורות" | PROPOSE FIX: plurals |
| R2-U8 | Quote marks disappear in `plan_import_done_detail` | PROPOSE FIX: escape them |
| R2-U9 | "מנובמבר" can read as "came from November" | PROPOSE FIX: "החל מ" |
| R2-U10 | "לחודש" button | PROPOSE FIX: "חזרה לחודש" |
| R2-U11 | "7788" not explained | Fixed with R2-U1 |
| R2-U12 | `task_close_detail` unclear | PROPOSE FIX: "הכל מוכן. אחרי הסגירה החודש נשמר ולא משתנה" |
| R2-U13 | "תנועות" and "קטגוריה" vs "שורות" and "סעיף" | DECLINE: the close-screen wording predates this PR; a wording pass of its own |
| R2-C7 | SAME compares against an override of the import month | PROPOSE FIX: compare against the amount without that override |
| R2-C8 | History: unclosed months count installments | DECLINE: needs a change to `planVsActual`; the effect is a higher average, which the review screen shows before saving |
| R2-C9 | Periodic budgets starting after `from` counted in "current" | DECLINE: after R2-C2, a periodic line never changes an existing budget |
| R2-D6 | Two clocks, up to one minute apart at month end | DECLINE: at most one minute, no data written |
| R2-D7 | A step left in the inbox across a household switch | DECLINE: it opens a screen only and writes nothing |
