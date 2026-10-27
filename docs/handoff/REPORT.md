# Task 2b — Fix the BOI schedule

commit: see git log

No test was deleted, `@Ignore`d, or weakened. The old Sunday-as-business-day expectations were replaced because that schedule was the spec's mistake: publication is Monday–Friday. Task 3 is not in this commit.

## Raw Gradle line

`./gradlew testDebugUnitTest`:

```
BUILD SUCCESSFUL in 45s
```

Gradle printed no test-count line on success. `app/build/test-results/testDebugUnitTest/TEST-*.xml` (25 files): tests=111, failures=0, errors=0, skipped=0.

## What changed

Business days are Monday–Friday. A rate fetched before today's cutoff is stale once the clock is past 15:30 Israel (Monday–Thursday) or 12:30 (Friday). Those cutoffs are 15 minutes after the official "shortly after 15:15 / 12:15". Saturday and Sunday are not publication days. `doWork` skips them and does not skip Friday. `computeInitialDelay` aims at 15:45 Monday–Thursday and 12:45 Friday, or the next such slot. An instant equal to the slot rolls forward. The unused `isStale` read of `Instant.now()` in `BoiRateStore.rateFlow` is gone.

Holidays and holiday eves are written in `docs/BACKLOG_STEP8.md` and are not implemented. The previous backlog bullets in that file are still there.

## Task table

| item | status | files changed | test that covers it |
| --- | --- | --- | --- |
| 1. Mon–Fri cutoffs 15:30 and Friday 12:30 | done | `BoiStalenessPolicy.kt` | `testStalenessScenarios`; `officialRateStale_whileOverrideActive_fetches`; `officialRateFresh_doesNotFetch` |
| 2. Worker skips Sat–Sun; delay 15:45 / Friday 12:45 | done | `BoiSyncWorker.kt` | `testWorkerDelay` |
| 3. Rewrite the old-schedule tests | done | `Step8RoundThreeTest.kt`, `BoiExchangeRateRepositoryTest.kt` | Thursday 17:00 → Friday 12:45; Friday 13:00 → Monday 15:45; Sunday 09:00 → Monday 15:45; Friday 12:40 fetch fresh on Saturday and Sunday; Thursday rate stale on Friday 13:00 |
| 4. Docs | done | `docs/RULES.md` section 6.5; two step-8 sentences in `implementation_plan.md` | no unit test |
| 5. Remove dead `isStale` in `rateFlow` | done | `BoiRateStore.kt` | `testOverridePrecedence` still reads `rateFlow` |

## Who did the work

chief-architect scoped and dispatched; executed by a Cursor cloud coding agent.

## Noticed, not changed

- `enqueuePeriodicWork` is still a 1-day `PeriodicWorkRequest`. `computeInitialDelay` sets only the first slot. A first run at Friday 12:45 repeats at 12:45, including Monday 12:45, which is before the 15:30 cutoff. A first run at 15:45 repeats at 15:45, including Friday 15:45.
- `doWork` still uses the wall clock. Only `computeInitialDelay` takes a `Clock`.
- `SettingsDialog` still calls `isStale` with `Instant.now()` for the open screen. That call uses the result.
- `isStale` still returns true when `rateDate` is more than 4 days before today. That is the morning-before-cutoff backstop, not a holiday calendar.
- `testBoiStalenessLogic` still uses rate date 2026-10-21 and `2026-10-22T17:00:00Z` (Thursday 20:00 Israel). It stays stale under the new cutoff and did not encode Sunday as a business day, so it was not rewritten.
- The 10-day historical window, Hebcal, and the other pre-existing `docs/BACKLOG_STEP8.md` bullets were not done.

## Open questions

1. Should the worker reschedule itself to the next 15:45 / 12:45 slot, instead of a flat 24-hour period? As written, one daily period cannot hit both clocks.
