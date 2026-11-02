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

## Not verified yet

- The real header strings come from `import-formats.md`, because the real files were deleted after they were checked. The fixtures are synthetic and come from `tools/import-fixtures/make_fixtures.py`. The first real import, in 1b, is the true test.
- No real sample of "תשלום N מתוך M" exists. The parser looks for it in סוג עסקה and הערות (TODO in `installmentOf`).
- Cal's total line may also leave out FX lines that already carry an ILS amount. The check excludes every FX line.
