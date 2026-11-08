# Importing bank and card files (IMPORT-1, part 1a)

Pure Kotlin under `feature/money/importer/`, with no Android imports and no spreadsheet library. Persistence goes through `feature/money/data/ImportDocuments.kt` and `TransactionRepository`. The edge-case list Menash asked for is in `/mnt/project-files/myself/reviews/import-edge-cases.md`, which is Hebrew and numbers each case. Every case has a test whose name is listed there.

## Flow

```
bytes ──readWorkbook──▶ Workbook ──parseStatement──▶ ParsedStatement (rows + total + warnings)
                                                   │
existing stored lines + [ParsedStatement] ──planImport──▶ ImportPlan (toWrite, toDelete, counts, checks)
                                                   │                      └─ user confirms (1b) ─▶ TransactionRepository.apply
stored lines + ImportMemory ──toTransactions──▶ engine Transaction ──planVsActual (unchanged engine)
                            ──reconcileCards / actualIncome
```

## Readers

- `XlsxReader` reads xlsx and xlsm using the platform zip and SAX parser. It handles shared strings, inline strings and numbers, and ignores styles. Unzipped content is capped at 60 MB, and DOCTYPE and external entities are disabled.
- `CfbReader` and `XlsReader` read old .xls: an OLE2 container (FAT, DIFAT, mini stream) holding BIFF8 records (SST with CONTINUE, LABELSST, LABEL, NUMBER, RK, MULRK, FORMULA with STRING). They refuse an Excel 95 file (BIFF5), a FILEPASS record and an `EncryptedPackage` stream, which means a password-protected xlsx.
- **Why no library:** Apache POI adds several MB to the APK and needs StAX for xlsx on Android. jxl is unmaintained. The readers here add about 25 KB of source, so the APK grows by tens of KB (not measured).
- The source is detected from the header row of each sheet, never from the file name. Columns are matched by header text after normalisation, which strips bidi marks, folds every space and line break into one space, and drops quote marks of every kind. Column order does not matter.

## Ids: the rule that prevents duplicates

`id = "tx-<source>-" + sha256(key + occurrence)[0..24]`. The key is built from content only:

| Source | Key |
| --- | --- |
| Hapoalim | account last 4, date, אסמכתא, signed amount, normalised description |
| One Zero | file currency, אסמכתא, transaction date-time, signed amount, normalised description |
| Max / Cal | card last 4, purchase date, normalised merchant, purchase amount (the original amount for FX), payment N/M |

- The charge date is **not** in a card key, so a pending line and the same line once charged get one id.
- An installment line with no readable N/M has its charge month added to the key, so the payments of different months never merge.
- `occurrence` numbers identical keys within one file (0, 1, …). Two real identical purchases stay two, and an overlapping file maps onto the same ids.
- Descriptions are masked (runs of 6 or more digits become `••••`) before they are hashed or stored.

## Merge (`planImport`)

1. Within the batch, lines with the same id are kept once, and a charged version wins over a pending one.
2. An unknown id is **new**. An identical line is **already imported** and is not written.
3. A line with different bank fields is **updated**, and the stored `categoryId` is kept.
4. A stored charged line is never turned back into pending by an older file.
5. A stored line that is not yet counted (pending or waiting) and is missing from the batch is deleted **only** when the batch holds another line with the same card, date and merchant. That case is a purchase re-issued with its final amount. A cancelled authorisation stays, uncounted, for the 1b screen to offer removing it.
6. `FileCheck` per file carries the total check (Max, Cal) or the warnings: skipped rows by Excel row number, and a break in the running balance for bank files. Nothing is ever auto-fixed.

## Classification (`classify`, computed on read, never stored)

| Source | Rule | Kind |
| --- | --- | --- |
| Hapoalim | debit to Max / Cal / Isracard / Amex / Diners | CARD_TOTAL |
| Hapoalim | משיכה מבנקט, משיכת מזומן, הפק מזומן, כספומט | CASH_WITHDRAWAL |
| Hapoalim | העברה, העב', bit, פייבוקס, זיכוי מבנק | TRANSFER (OWN_TRANSFER once the name is marked) |
| Hapoalim | הוראת קבע, הו"ק | STANDING_ORDER |
| Hapoalim | other debit / credit | EXPENSE / INCOME |
| One Zero | ניירות ערך, המרת מט"ח | TRADING |
| One Zero | העברות in / out | PORTFOLIO_DEPOSIT / TRANSFER |
| One Zero | אחר debit / credit | EXPENSE / INCOME |
| Max, Cal | הוראת קבע | STANDING_ORDER |
| Max, Cal | anything else | EXPENSE (a refund is a negative charge) |

`toTransactions` keeps only COUNTED lines that carry an ILS charge, of the kinds EXPENSE, STANDING_ORDER, CASH_WITHDRAWAL and outgoing TRANSFER. A CARD_TOTAL is kept only while the purchases for that issuer and billing month are missing, so no money disappears. Categories come from the user's choice first, then the merchant memory, then uncategorised. Cash and card bills always start uncategorised.

## Storage

`households/{hid}/transactions/{id}` has 15 fields at most. Amounts are agorot and dates are `YYYY-MM-DD`. `households/{hid}/merchantMemory/m-<hash>` holds `{key, categoryId?, ownTransfer, alias?, askEachTime?}`. `alias` (1–60 characters) is only a display name: matching and the document id always use the bank's text. `askEachTime` marks a generic merchant (ביט, העברה): it is never learnt, its stored `categoryId` is ignored, and turning it on first copies the learnt category onto the lines that followed it. A category chosen with "רק לתנועה הזו" is saved on that line only. Both are added to `isMoneyCollection` in `firestore.rules`: members read and write, everyone else is denied. `apply` writes in batches of 450.

## Closed sources and historical coverage

A household can mark a missing account or card closed from the month currently being closed. A `sourceClosures/s-<hash>` document stores `v: 1`, `source`, `sourceId`, and `closedFrom` (`YYYY-MM`); undo deletes the mark. `closedFrom` prevents that source from being expected for that month and later months. Existing and later imported lines still count normally. This does not change import identifiers or merging.

`importCoverage(stored)` groups by source and source id, returning the first and last billing months with rows and gaps within that range. A gap means no stored rows; it does not establish whether a file is missing or a source had no activity. The coverage function has no screen in this task.

## Not verified yet

- The real header strings come from `import-formats.md`, because the real files were deleted after they were checked. The fixtures are synthetic and come from `tools/import-fixtures/make_fixtures.py`. The first real import, in 1b, is the true test.
- No real sample of "תשלום N מתוך M" exists. The parser looks for it in סוג עסקה and הערות (TODO in `installmentOf`).
- Cal's total line may also leave out FX lines that already carry an ILS amount. The check excludes every FX line.

## Paamonim plan (`importer/PaamonimPlanReader.kt`, `PlanImport.kt`)

A Paamonim רישומון shared into the same import flow opens a plan review instead of the statement review. Only a single shared file is checked; a plan file shared together with statements goes to the statement review, where it is refused as an unknown format.

- **Detection:** a sheet whose header row starts with "סעיף" and lists the 12 Hebrew month names ("רשימת תקציבים"). Never by the file name.
- **Read:** column A names the group (קבועות, תקופתיות, שוטפות, הכנסות) and the totals rows, column B the line. The month used is the latest one with any budget. An unused line carries the number 0 instead of a name and is skipped, and so is a line with no budget. "החזר חובות" is read as a total only; debts and savings from "שיקוף" are not read yet.
- **Draft (`draftPlanImport`):** each line is matched to a category of the same class by name ("דלק" → `VEHICLE_ENERGY_CATEGORY_NAME`) and to the plan items in it.
  - No item yet: **NEW**. A name with no category: **NEW_CATEGORY** (the category is added).
  - One item with another monthly amount (more than ₪1 apart): **CHANGED**. The change starts at the import month (`AmountChange`), so earlier months and closed months keep their plan. A yearly fixed expense gets the annual figure; a periodic budget gets `annualAmount` = monthly × 12.
  - The same amount: **SAME**. Two or more items in the category (or incomes with the name): **SPLIT**, shown but never changed.
  - "מנויים" while subscriptions are listed one by one: **SUBSCRIPTIONS**, shown but not added, so they are not counted twice.
  - Incomes are matched by name.
- **Write:** nothing before confirm. Every NEW, NEW_CATEGORY and CHANGED line starts selected and can be unticked; one `saveAll` writes the selection. A household with no categories gets the Paamonim defaults in the same save.
- **Fixture:** `paamonim_plan.xls`, built by `tools/import-fixtures/make_paamonim_fixture.py` from a real file with every amount and name replaced. The real file and the name map stay outside the repository.

## Plan from history (`PlanFromHistory.kt`)

The same plan review, built from the household's own months instead of a file. Offered on the month tab when the current month has no plan and at least three ended months have spending.

- **Months:** the last six ended months with spending. A closed month uses its frozen `byCategory`; an ended month that is not closed uses its imported lines (`planVsActual` with no plan). The current month is never used.
- **Amounts:** fixed categories take the latest month; current categories the average; periodic categories the average × 12 as the annual budget. Monthly amounts round up to ₪50 and annual ones to ₪100.
- **Left out:** sub-categories (single subscriptions, listed on their own screen), unassigned spending, and incomes (an import knows the total, not who earned it).
- The lines then go through `draftPlanImport`'s matching, so new, changed and same lines behave as for a file.
