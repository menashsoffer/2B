# R3 v2 — money pillar, first screens (Paamonim)

Branch `claude/money-screens-r3`, PR #7, one commit on top of `main` `361b227`. Built by Claude at Menash's request (same exception as CODEX-2/3). The engine under `feature/money/domain` and `docs/money/ENGINE.md` are untouched: screens read and call it only.

## Task table

| Item | Status | Main files |
| --- | --- | --- |
| 1 Persistence | done | `feature/money/data/MoneyData.kt`, `MoneyDocuments.kt`, `MoneyRepository.kt`, `core/data/repository/impl/FirestoreMoneyRepository.kt`, `firestore.rules`, `firestore.test.js` |
| 2 Money tab "החודש" | done | `ui/money/MoneyContent.kt`, `MoneyParts.kt`, `feature/money/MoneyMonth.kt` |
| 3 Add and edit | done | `feature/money/MoneyDraft.kt`, `MoneyEditor.kt`, `MoneyViewModel.kt`, `ui/money/MoneyEditorSheet.kt`, `MoneyEditorForm.kt`, `MoneyPickers.kt` |
| 4 "התמונה הגדולה" | done | `ui/money/BigPictureContent.kt` |
| 5 Home hero | done | `feature/home/HomeViewModel.kt`, `ui/home/HomeContent.kt`, `HomeUiState.kt`, `HomeScreen.kt` |
| 6 Shell seam + tests | done | `ui/shell/AppShell.kt` (`AppShellContent`, `AppShellState`), tests below |

## Firestore paths

`households/{hid}/{collection}/{id}`, document id = engine id (UUID for new items, `fixed-N` / `sub-N` / `periodic-N` / `current-N` for the Paamonim defaults). Collections: `cards`, `incomes`, `fixedExpenses`, `subscriptions`, `variableBudgets`, `installments`, `periodicBudgets`, `debts`, `savings`, `assets`, `categories`. Amounts are agorot (Long), months `YYYY-MM`, dates `YYYY-MM-DD`, `AmountHistory` as `changes` [{from, amount}] + `overrides` {month: amount}. Items reference a card by `cardId`. Categories are seeded once from `defaultCategories()` when the household has none.

Rules: one `match /{moneyCollection}/{docId}` under the household, members read and write, everyone else denied; ids must pass `isValidId`, at most 20 fields, non-empty `name`/`creditor` when present.

Rules tests (`firebase emulators:exec --only firestore "node --test firestore.test.js"`, run locally in the Claude sandbox): `tests 22, pass 22, fail 0` (10 existing + 11 per-collection allow/deny + 1 bad id/empty name/unknown collection).

## How the numbers are made

No money arithmetic in UI or ViewModels. `buildMoneyMonth` calls `monthPlan`, `netWorth`, `cushionMonths`, `isStale`; an item's own monthly amount is `monthPlan` of a `MoneyItems` holding only that item. Rows: הכנסות, the three Paamonim classes from `Planned.byClass`, תשלומים, החזרי חובות, הפקדות לחיסכון. Each row and item has an `actual` field, null until import (for the import thread). USD: the stored Bank of Israel rate (`ExchangeRateRepository.currentRateFlow`); with no rate, active USD subscriptions are left out and the chip "חסר שער דולר" shows.

## Tests

CI for this branch is `.github/workflows/branch-tests.yml` (new): `./gradlew testDebugUnitTest` with a fake `google-services.json`, no secrets, nothing signed or uploaded. A manual run with `record=true` commits only goldens that did not exist and fails if an existing golden changed. The Claude sandbox has no Android SDK (dl.google.com is blocked), so Gradle ran only there.

Last red run before the goldens (run 40951335245, head `d6892f9`): `XML: tests=316 failures=16`, all 16 = the new `MoneyScreenshotTest` goldens not yet recorded. Main has 258 tests; this PR adds 58:

- `MoneyDocumentsTest` 17 (round trip of every type, agorot Longs, doubles from Firestore, unknown card, malformed docs, seed rule)
- `MoneyViewModelTest` 16 (Empty, Planned equals the engine, per-item amounts, missing USD rate, from-now vs this-month, no question when the amount is unchanged, defaults, last card, category sets class, periodic gets its own category, field errors, delete, categories seeded once, no seed without household)
- `MoneyEditorTest` 7, `HomeMoneyHeroTest` 2
- `MoneyScreenshotTest` 16 goldens (added, none changed): month empty, month filled, big picture, add-item sheet; light/dark; font 1.0/1.3.

Changed tests: `AppShellScreenshotTest` now renders `AppShellContent` (its 2 goldens unchanged and passing). `PlaceholderTabStructureTest` no longer lists Money, which is a real screen now. No test disabled, skipped or loosened. Guard allow-lists unchanged (no `Color(0x`, raw `.sp` or `TopAppBar`). No new text/background colour pair: texts use textPrimary, textSecondary and danger on card, already covered.

## Noticed, not changed

- "כרטיס חדש" inside an item sheet replaces that sheet with the card sheet; the item draft is lost.
- The saving sheet has no maturity field yet (shown when stored).
- A debt edit resets `asOf` to the end of last month, so "payments left" always counts from this month.
- Home and the money tab each open their own Firestore listeners.
- Merging to main deploys `firestore.rules` through the Firestore rules workflow.
