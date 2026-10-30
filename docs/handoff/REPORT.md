# Step 3b: "מעשרות" (maaser and chomesh), with the One Zero portfolio

Branch `claude/money-tithe-3b`, built on `main` after PR #12. Claude built it as the next step of the money plan. Menash decided that maaser counts what actually came in each month, because salaries vary: "צריך כל חודש לראות מה בפועל נכנס וכמה צדקה נתנו באותו חודש". Every step was pushed as its own commit.

## Task table

| Item | Status | Main files |
| --- | --- | --- |
| Maaser or chomesh per income source, chosen in the income sheet | done | `MoneyDraft.kt` (`titheRate`), `MoneyEditor.kt`, `MoneyEditorForm.kt` |
| Donations and the maaser settings stored (`donations`, `tithe/settings`), with rules | done | `domain/TitheSettings.kt`, `data/TitheDocuments.kt`, `data/MoneyData.kt`, `MoneyDocuments.kt`, `FirestoreMoneyRepository.kt`, `firestore.rules`, `firestore.test.js` |
| An imported income line is linked once to its source; the payer is remembered for next month | done | `importer/IncomeLinks.kt`, `importer/MonthLines.kt` (`incomeId`), `TransactionsViewModel.linkIncome`, `ui/money/IncomeSourceSheet.kt` |
| Month by month: each source's actual income when linked, else its plan (marked "לפי התכנון"), the 10% or 20% it owes, and what was donated that month | done | `feature/money/Tithe.kt` |
| Chomesh on portfolio gains: 20% of (asset value − deposits); deposits before the start are typed once, later ones come from One Zero imports | done | `importer/PortfolioDeposits.kt`, `Tithe.kt` |
| "מעשרות" screen: balance, where it comes from, the months, the donations, donations not yet submitted for a tax refund (section 46) | done | `ui/money/TitheContent.kt`, `TitheMonthCard.kt`, `TitheForms.kt`, `TitheScreen.kt`, `TitheViewModel.kt`, `TitheDraft.kt`, `res/values/strings_tithe.xml` |
| Entry row in "התמונה הגדולה" with the balance | done | `BigPictureContent.kt`, `MoneyMonth.kt` (`titheBalance`), `MoneyScreen.kt` |

## Behaviour

- The count starts at a month the user picks, with what was open then (still owed, or given ahead).
- An income source counts its linked imported lines in a month. When none is linked, it counts its planned net and the month says "לפי התכנון". A month with income lines not linked yet shows "N שורות הכנסה לשייך"; tapping it opens that month's transactions.
- Only donations from the start month on count. The section 46 reminder covers the whole calendar year.
- A losing portfolio reduces what is owed (the engine's existing rule), so the balance can show "נתתם מעבר".
- The transactions screen now opens over the maaser screen, so linking a month returns to it.

## Golden changes

The 4 `MoneyScreenshotTest.moneyBigPicture_*` goldens were redrawn for the new "מעשרות" row. New goldens: `TitheScreenshotTest` (balance, first time, donation sheet, settings sheet). No other golden changed: an unlinked income line still reads "הכנסה".

## Tests

New: `TitheTest` 11, `IncomeLinksTest` 4, `TitheDraftTest` 5, `TitheViewModelTest` 4, `MonthLinesTest` +1, `TransactionsViewModelTest` +1, `MoneyEditorTest` +1, `MoneyDocumentsTest` +2, `TitheScreenshotTest` 10 goldens.

- Claude sandbox, pure-JVM run of the engine, importer, editor, month, documents and maaser sources: `tests 213, failures 0, errors 0`.
- Full `testDebugUnitTest` runs in the branch CI. See the PR for the run.

No test was disabled, skipped or loosened, and no guard allow-list changed. Every file is under 300 lines.

## Noticed, not changed

- A source that has no linked line in a month counts its plan, even when the bank file for that month was imported. A salary that really did not arrive needs its line linked or the month's amount edited.
- An income source is remembered by payer in the same memory as merchant categories. If the same payer also receives money from the household, that outgoing line shows as "לא משויך" until it gets a category.
- Merchant memory is saved under `ImportIds.merchantKey` (long digit runs masked) but `suggestCategory` looks it up by `normalizeMerchant`. They differ only for descriptions with long digit runs. Income links use `merchantKey` on both sides.
