# Task 2 — Close step 8

commit: see git log

No test was deleted, `@Ignore`d, or weakened. Signing was not changed. `app/google-services.json` and `debug.keystore` stayed untracked. Step 9 was not started.

## Raw Gradle lines

First `./gradlew testDebugUnitTest` after removing the duplicate `import java.time.LocalDate` and before any other fix (fake `app/google-services.json` present):

```
106 tests completed, 4 failed
```

```
BUILD FAILED in 31s
```

Final `./gradlew testDebugUnitTest` after the fixes. Gradle printed no test count on success:

```
BUILD SUCCESSFUL in 12s
```

`app/build/test-results/testDebugUnitTest/TEST-*.xml` (25 files): tests=111, failures=0, errors=0, skipped=0.

## Why an assertion changed

`Step6And7ReviewFixesTest` expected `lastTechnicalError` to be `Active household session required`. The failure was `expected:<...old session required[]> but was:<...old session required[. User must be in a household.]>`. That expectation was wrong. `ErrorReporter.reportError` stores `throwable.message`, and `HouseholdSession.requireHouseholdId` throws `IllegalStateException("Active household session required. User must be in a household.")`. The friendly `MSG_NO_HOUSEHOLD` check in the same test was already right. Production was not changed. The assertion is now the full message.

`testSafeLaunchDoesNotSwallowJvmError` failed `assertTrue(caught)` with a bare `AssertionError`. That expectation was wrong. The `CoroutineExceptionHandler` sat on a `GlobalScope` job that only called `testSafeLaunchError()` and then finished normally. `safeLaunch` starts a different coroutine on `viewModelScope`. It catches `Exception` and rethrows other `Throwable`s, so `AssertionError` is not stored in `errorState`. The handler never saw it, so `caught` stayed false. The test now passes a `CoroutineScope` with a `CoroutineExceptionHandler` into `ViewModel` (that scope is `viewModelScope`) and asserts the `AssertionError` arrives with message `JVM AssertionError must propagate` and that `errorState` stays null. `BaseViewModel` gained a protected constructor that forwards that scope. The existing constructor is unchanged.

`testWorkerDelay` failed `expected:<27000000> but was:<51805260>`. The assertion was not changed. `computeInitialDelay` used `ZonedDateTime.now(clock.zone)`, which reads the wall clock in that zone and ignores the fixed instant. The run was about 02:06 Asia/Jerusalem on 2026-10-27, so the delay until 16:30 that day was 51805260 ms, not the Sunday 09:00 fixture's 27000000 ms (7.5 hours). The fix is `ZonedDateTime.now(clock)`. Thursday 17:00 still targets Friday 16:30. It was not moved to Sunday.

The old combined "fresh" check never ran (the class crashed earlier on `ApplicationProvider`). Its fixture was still wrong for "does not fetch": official date 2026-10-25, fetched `2026-10-25T10:00:00Z` (12:00 Israel), clock `2026-10-25T17:00:00Z` (19:00 Israel). `BoiStalenessPolicy` treats that Sunday as a business day and a fetch before 16:00 as stale once now is after 16:00. Case (c) uses a rate fetched at 16:30 Israel (`2026-10-25T14:30:00Z`), after that cutoff, and asserts no fetch.

## Item 4 — what I opened

Schedule page `https://www.boi.org.il/roles/markets/reprate/` (curl HTTP 200). Sentences on that page:

- "בנק ישראל מחשב את השערים היציגים פעם אחת ביום, בימי העבודה במטבע חוץ בלבד"
- "בימי שבת וראשון ובימי שבתון בישראל אין שערים יציגים"
- "השער היציג מתפרסם כיום זמן קצר אחרי השעה 15.15. (בימי ו', בחול המועד ובערבי חגים – זמן קצר אחרי השעה 12.15)"
- The same page says this time is not guaranteed ("מועד זה אינו מובטח").

Friday is a publication day (shortly after 12:15). Sunday is not. The "Thursday after publication → Sunday" condition was not met, so `computeInitialDelay` / `testWorkerDelay` were not retargeted for that reason.

SDMX, series `RER_USD_ILS`, `DATA_TYPE=OF00`:

- `https://edge.boi.org.il/FusionEdgeServer/sdmx/v2/data/dataflow/BOI.STATISTICS/EXR/1.0/RER_USD_ILS?c%5BDATA_TYPE%5D=OF00&lastNObservations=3&format=csv` returned `2026-10-22` 3.107, `2026-10-23` 3.112, `2026-10-26` 3.078.
- Same host, `startPeriod=2026-09-01&endPeriod=2026-10-26`: 38 rows, Monday–Friday only, no Sunday and no Saturday. Weekday gaps in that window: `2026-09-11` (Friday) and `2026-09-21` (Monday). I did not confirm those two dates from a holiday calendar.
- `https://edge.boi.gov.il/FusionEdgeServer/sdmx/v2/data/dataflow/BOI.STATISTICS/EXR/1.0/RER_USD_ILS?c%5BDATA_TYPE%5D=OF00&lastNObservations=1&format=csv` via curl: HTTP 200, one row, `RER_USD_ILS`, `2026-10-26`, `3.078`. A WebFetch of that same URL returned HTTP 500; curl returned the CSV. `edge.boi.org.il` with `lastNObservations=1` returned the same row.

This matches the israel-finance-expert note (Mon–Fri, Friday is a publication day, Sunday is not, ~15:15 / Friday ~12:15, the three latest values, 38 rows since 2026-09-01, series id correct). I also saw the two weekday gaps and the gov.il WebFetch 500 versus curl 200.

## Task table

| item | status | files changed | test that covers it |
| --- | --- | --- | --- |
| 1a. Remove duplicate `LocalDate` import | done | `app/src/test/java/com/menash/myself/Step8ExternalDataAndFixesTest.kt` | class compiles; final suite 111/0 |
| 1b. First unit run, before other fixes | done | none | the run above: `BUILD FAILED in 31s`, 106 tests, 4 failed |
| 1c. Fix the four failures | done | `BaseViewModel.kt`, `BoiSyncWorker.kt`, `Step6And7ReviewFixesTest.kt`, `Step8ExternalDataAndFixesTest.kt` | `testSafeLaunchDoesNotSwallowJvmError`, `fix 1 - createTask with no household...`, `testWorkerDelay`; final suite 111/0 |
| 2a. Gitignore `app/google-services.json`, `*.jks`, `*.keystore` | done | `.gitignore` | no unit test |
| 2b. Local build section | done | `docs/RULES.md` section 7 | no unit test |
| 3. Split BOI repository tests (a–f) | done | `BoiExchangeRateRepositoryTest.kt` | `manualOverrideOnly_noOfficialRate_fetchesOnce`, `officialRateStale_whileOverrideActive_fetches`, `officialRateFresh_doesNotFetch`, `historical_saturdayRequest_returnsThursdayRateAndDate`, `historical_emptyWindow_isFailure`, `historical_sourceFailure_isFailure` |
| 4. Verify BOI days; do not move Thursday → Sunday | done | `docs/RULES.md` section 6.5 (schedule, URL, series). `computeInitialDelay` only gained use of the clock instant | `testWorkerDelay` still expects Thursday 17:00 → Friday 16:30 and passed |
| 5. Mark step 8 complete | done | `implementation_plan.md` heading only | suite green, so the mark was applied |

Historical cases (d–f) passed with no production change. `fetchHistoricalRate` already returns Thursday's rate for a Saturday request when the window is Thursday then Sunday, and `Result.failure` for an empty window and for a source failure.

## Who did the work

chief-architect scoped and dispatched; executed by a Cursor cloud coding agent. israel-finance-expert independently verified BOI schedule and SDMX series from official sources.

## Noticed, not changed

- `BoiStalenessPolicy.kt` lines 18–22 treat Sunday–Thursday as business days and omit Friday. Line 16 sets `PUBLISH_TIME` to 16:00. Lines 36–40 use that cutoff. Official page: no representative rate on Saturday or Sunday; Friday is a publication day; publish shortly after 15:15, and on Friday / Hol Hamoed / holiday eves shortly after 12:15; the time is not guaranteed.
- `BoiSyncWorker.doWork` lines 53–54 return success without fetching on Friday and Saturday, so Sunday is still treated as a sync day and Friday is skipped.
- `computeInitialDelay` still aims at 16:30, including Friday, not at ~15:15 or Friday ~12:15. Only the ignored-clock bug was fixed.
- `BoiRateStore.rateFlow` computes `isStale` with `Instant.now()` and does not use the result. Not this task.
- The step 8 body in `implementation_plan.md` still says a daily check after 16:00 and a worker that skips Friday–Saturday. Item 5 allowed only the completion mark on the heading.
- `edge.boi.gov.il` WebFetch returned HTTP 500 while curl of the same URL returned HTTP 200 and the CSV. Not a code change.

## Open questions for Claude

1. Next task: retarget `BoiStalenessPolicy` and `BoiSyncWorker.doWork` (and the 16:30 delay) from Sunday–Thursday / 16:00 to Monday–Friday, shortly after 15:15, and Friday / eves shortly after 12:15? The time is officially not guaranteed.
2. Should a later edit also replace the step 8 plan sentences that still say 16:00 and "skip Friday–Saturday"?
