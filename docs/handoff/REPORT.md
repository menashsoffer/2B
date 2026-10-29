# IMPORT-1 part 1b: the import screen and sharing files to the app

Branch `claude/import-screen-1b`, built on `main` after PR #8 (1a). Claude built it at Menash's request ("כן, אבל אל תעבוד מקומית"), so every step was pushed as a `wip(import)` commit. This PR covers section 4 of `reviews/prompt-import-1.md` and the rest of its section 5 tests: the review screen, the category question for new merchants, "בפועל" on the month, and files shared from WhatsApp, mail or Files. The engine from 1a is called as it is.

## Task table

| Item | Status | Main files |
| --- | --- | --- |
| Review before saving (per-file summary, total check, balance break, refused files) | done | `importer/ImportSession.kt` (`reviewImport`), `ui/money/ImportContent.kt` |
| Only new merchants are asked, grouped, with Max's category as a suggestion | done | `ImportSession.kt` (`newMerchants`, `categoryForHint`), `ui/money/ImportCategorySheet.kt` |
| Confirm writes once and teaches the merchant memory | done | `ImportSession.kt` (`applyDecisions`), `feature/money/ImportViewModel.kt` |
| "בפועל" on the month rows plus a "לא משויך" line | done | `feature/money/MonthActuals.kt`, `MoneyMonth.kt`, `MoneyViewModel.kt`, `MoneyContent.kt` |
| Entry from the money tab ("להוסיף" → "ייבוא קובץ") and a file picker | done | `MoneyEditorSheet.kt`, `MoneyScreen.kt`, `ui/money/ImportFiles.kt` |
| Share / open with (SEND, SEND_MULTIPLE, VIEW for xls/xlsx/xlsm) | done | `AndroidManifest.xml`, `MainActivity.kt`, `feature/money/ImportInbox.kt`, `AppShell.kt` |

## Behaviour

- Nothing is written until "שמירה". Cancel or back leaves Firestore untouched.
- A file that is the same as one already saved shows "אין מה לשמור" and asks nothing.
- A suggested category counts as chosen when saving. "אחר כך" leaves the line without a category, and it shows in "לא משויך".
- A failed save goes back to the review with the error reported, so nothing is lost.
- An actual is shown on an item only when no other item shares its category. Otherwise it shows on the row only.
- A card category suggests only a single matching current-spending category. "חשמל ומחשבים" is a shop, so it never suggests the electricity bill.
- A month with no imported lines looks exactly as before, so the existing goldens did not change.

## Tests

New: 28 tests. `ImportSessionTest` 6, `MonthActualsTest` 4, `ImportViewModelTest` 5, `ImportShareTest` 5 (Robolectric), `ImportScreenshotTest` 8 goldens (review and category list, light/dark, font 1.0/1.3, synthetic fixtures only). The goldens were recorded with the branch workflow's `record=true` dispatch, which only adds new files. Full `testDebugUnitTest` results are in the PR.

No test was disabled, skipped or loosened, no existing golden changed, and no guard allow-list changed. Every file is under 300 lines.

## Noticed, not changed

- `MainActivity` is now `launchMode="singleTask"`, so a second share reaches the open app through `onNewIntent` instead of a second copy.
- A cancelled pending card line still stays stored and uncounted. It is not yet offered for removal.
- There is no UI yet to change or clear a category after saving (the repository has `setCategory`).
- The import entry sits inside the "להוסיף" picker rather than as its own button, to keep the money screen goldens unchanged. It can move once Menash sees it.
- The header strings still come from `import-formats.md`. Menash's first real file is the real test. There is still no "תשלום N מתוך M" sample.
