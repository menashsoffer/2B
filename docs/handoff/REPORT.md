# Step 2a: the month's transactions screen, plus the R3 leftovers

Branch `claude/money-transactions-2a`, built on `main` after PR #9. Claude built it at Menash's request ("אפשר להמשיך לצעד הבא"). After the first real import, Menash noted there was no way to see the imported data. Closing a month (2b) requires no unassigned line, so this screen comes first. Every step was pushed as a `wip(money)` commit.

## Task table

| Item | Status | Main files |
| --- | --- | --- |
| The month's lines: counted, income, pending, waiting for the rate, unassigned | done | `importer/MonthLines.kt` |
| Transactions screen: month back/forward, spent and income, counts, filters (all / unassigned / pending) | done | `ui/money/TransactionsContent.kt`, `TransactionsScreen.kt`, `res/values/strings_transactions.xml` |
| Category for a line (kept on re-import) that also teaches the merchant memory; own-transfer for transfers | done | `feature/money/TransactionsViewModel.kt`, reuses `ImportCategorySheet` |
| Remove a cancelled pending card line, after a question | done | `TransactionRepository.delete`, `FirestoreTransactionRepository` |
| Entry: "תנועות החודש" row and a tappable "לא משויך" row in the month | done | `MoneyContent.kt`, `MonthActuals.kt` (`importedLines`), `MoneyScreen.kt` |
| R3: "כרטיס חדש" inside an item sheet no longer loses the item | done | `MoneyViewModel.kt` (`startNewCard`, `returnTo`) |
| R3: maturity date for a saving | done | `MoneyPickers.kt` (`MaturityChips`), `MoneyEditorForm.kt` |
| R3: editing a debt no longer restarts its payments count | done | `MoneyDraft.kt` (`paymentsLeft`), `MoneyEditor.kt` |

## Behaviour

- The list and the month screen agree by construction. Both come from `toTransactions` and `actualIncome`, and a test checks that the month's "לא משויך" equals the sum of the list's unassigned lines.
- Card bills and cash withdrawals get a category for that line only and are never taught to the merchant memory.
- Only pending lines can be removed. If the same pending line comes back in a later file, it is imported again.
- A month with no imported lines looks exactly as before, so no existing golden changed.
- A debt keeps its stored `asOf` and count when only its name or payment changes. A new balance or count is taken as "from this month on".

## Tests

New: 36 tests. `MonthLinesTest` 9, `TransactionsViewModelTest` 4, `MonthActualsTest` +1, `MoneyEditorTest` +3 (debt) and the debt added to the round trip, `MoneyViewModelTest` +2 (new card), and `TransactionsScreenshotTest` 8 goldens. The goldens show the list and an empty month, light/dark, at font 1.0 and 1.3, recorded with the `record=true` dispatch.

- Claude sandbox, pure-JVM run of the engine, importer, editor and month sources: `tests 158, failures 0, errors 0`.
- Full `testDebugUnitTest` runs in the branch CI. See the PR for the run.

No test was disabled, skipped or loosened, no existing golden changed, and no guard allow-list changed. Every file is under 300 lines.

## Noticed, not changed

- A pending line with no charge date is listed in the month of its purchase date, since its billing month is not known yet.
- A line matched to an installment purchase is not "לא משויך" on the month screen, but it is listed as unassigned here. This only matters once real "תשלום N מתוך M" rows arrive.
- A category cannot be cleared back to automatic; it can only be changed.
- Codex is building the month-close engine (2b) in parallel on `codex/month-close-engine` (prompt: `reviews/prompt-codex-task-4-month-close.md`). It does not touch these files.
