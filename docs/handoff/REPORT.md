# Step 3a: "האם אפשר?" (can we afford a goal)

Branch `claude/money-can-we-3`, built on `main` after PR #10. Claude built it at Menash's request ("האם אתה יכולה במקביל אליו להמשיך למשימה הבאה?"), in parallel with Codex's month-close engine (2b). Every step was pushed as its own commit.

## Task table

| Item | Status | Main files |
| --- | --- | --- |
| Goals stored in Firestore (`goals` collection), with rules and a rules test | done | `data/MoneyDocuments.kt`, `data/MoneyDoc.kt` (reader moved out), `data/MoneyData.kt`, `FirestoreMoneyRepository.kt`, `firestore.rules`, `firestore.test.js` |
| Each goal answered by the existing engine (`isItPossible`), the other goals taking their share first | done | `feature/money/CanWe.kt` |
| Goal sheet: name, amount, target month, already saved, monthly cost after, live answer while typing, delete after a question | done | `GoalDraft.kt`, `GoalsViewModel.kt`, `ui/money/GoalEditorForm.kt`, `GoalsScreen.kt` |
| "האם אפשר?" screen: what the answers are based on, each goal with its answer and levers | done | `ui/money/GoalsContent.kt`, `GoalAnswerText.kt`, `res/values/strings_goals.xml` |
| Entry row in "התמונה הגדולה" | done | `BigPictureContent.kt`, `MoneyMonth.kt` (`goalCount`), `MoneyContent.kt`, `MoneyScreen.kt` |

## Behaviour

- No month is closed yet, so the monthly saving is this month's planned surplus. The screen says so. Once 2b lands, the closed-months average goes into `canWe(closedSaving = …)` and the note changes.
- The safety cushion (three months of planned expenses) is filled first, as the engine already does. The screen says this under the list.
- Liquid assets are the liquid ASSET balances. Liabilities are not subtracted.
- A goal whose month has passed gets no answer ("התאריך עבר"), so the engine is never called with a past month.

## Golden changes

The 4 `MoneyScreenshotTest.moneyBigPicture_*` goldens were redrawn, because the big picture now has the "האם אפשר?" row. Nothing else on that screen changed. New goldens: `GoalsScreenshotTest` (list, empty, sheet).

## Tests

New: `CanWeTest` 9, `GoalDraftTest` 4, `GoalsViewModelTest` 4, `MoneyDocumentsTest` +1 (goals round trip), `GoalsScreenshotTest` 9 goldens.

- Claude sandbox, pure-JVM run of the engine, importer, editor, month and can-we sources: `tests 171, failures 0, errors 0`.
- Full `testDebugUnitTest` runs in the branch CI. See the PR for the run.

No test was disabled, skipped or loosened, and no guard allow-list changed. Every file is under 300 lines.

## Noticed, not changed

- "Already saved" for a goal is a number typed by hand; it is not linked to a saving or an asset.
- Levers are listed, not applied. Choosing "לדחות" does not change the goal's month by itself.
