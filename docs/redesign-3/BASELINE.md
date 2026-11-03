# Stage 0 baseline

Recorded on `grok/redesign-3` from `origin/main`, with no source, test, golden, workflow, Firebase, or Gradle edits. Nothing was pushed to `main`. No pull request.

## Main

- `origin/main`: `9c639725616dc0edd2d880213bec017032cf5c2b` (Merge pull request #43)
- First parent, PR #45: `7783527a911a5dfd00840fee1d6f74634fcd0005`
- Second parent, PR #43 tip: `0d1ec44f98ecb8d3c6134aadc8c282a857a90fd3`

## Environment

- JDK: OpenJDK 21.0.12.1 (Ubuntu). Gradle: 9.3.1.
- Android SDK: `platforms;android-36.1`, `build-tools;36.0.0`, `platform-tools`.
- Emulator: yes. AVD `baseline34`, API 34 `google_apis` x86_64. `emulator -accel-check` reported KVM usable. Maestro 2.12.1. Firebase emulators: Auth `127.0.0.1:9099`, Firestore `127.0.0.1:8085`, project `demo-no-project`.
- Gitignored and not committed: fake `app/google-services.json`, `debug.keystore`, `local.properties`. `git check-ignore` matched all three. `git status --short` was empty before this file. Release used `VERSION_CODE=1` and a throwaway keystore `/tmp/throwaway-release.jks` (`KEY_ALIAS=throwaway`). That APK is not the Play upload key.

## Commands and results

| Command | Result |
| --- | --- |
| `./gradlew testDebugUnitTest --no-daemon --continue` | PASS, exit 0 |
| `./gradlew verifyRoborazziDebug --no-daemon --continue` | PASS, exit 0 |
| `./gradlew koverXmlReportDebug koverHtmlReportDebug koverVerifyDebug --no-daemon` | PASS, exit 0 |
| `./gradlew lintDebug --no-daemon` | PASS, exit 0. Lint XML: 172 warnings, 0 errors |
| `./gradlew assembleRelease --no-daemon` with the env above | PASS, exit 0 |
| `maestro test e2e/maestro/12_story_close_month.yaml` | PASS, 34s, exit 0 |
| `maestro test e2e/maestro` | PASS, 8/8 flows, 5m 14s, exit 0 |

Gradle did not print `N tests completed, M failed`. Counts are from JUnit XML. `:app:compileDebugKotlin` succeeded inside the unit-test build.

### Raw Gradle summaries

```
> Task :app:testDebugUnitTest
> Task :app:finalizeTestRoborazziDebug
BUILD SUCCESSFUL in 2m 34s
37 actionable tasks: 37 executed
```

```
> Task :app:testDebugUnitTest
> Task :app:verifyRoborazziDebug
> Task :app:finalizeTestRoborazziDebug
BUILD SUCCESSFUL in 45s
37 actionable tasks: 2 executed, 35 up-to-date
```

```
> Task :app:koverVerifyMoneyDomain
> Task :app:koverVerifyMoneyPresentation
> Task :app:koverVerifyMoneyImport
> Task :app:koverVerifyDebug
BUILD SUCCESSFUL in 11s
51 actionable tasks: 15 executed, 1 from cache, 35 up-to-date
```

```
> Task :app:lintDebug
BUILD SUCCESSFUL in 1m 55s
33 actionable tasks: 9 executed, 24 up-to-date
```

```
> Task :app:assembleRelease
BUILD SUCCESSFUL in 2m 27s
54 actionable tasks: 46 executed, 7 from cache, 1 up-to-date
```

## JUnit XML

Only module `:app` (`app/build/test-results/testDebugUnitTest`).

| Suite | tests | failures | errors | skipped |
| --- | ---: | ---: | ---: | ---: |
| testDebugUnitTest | 839 | 0 | 0 | 0 |
| Maestro `e2e/maestro` | 8 | 0 | 0 | 0 |

## Failures

No test or Gradle check failed on this SHA.

| Test | File | Assertion | Pre-existing on main |
| --- | --- | --- | --- |
| (none) | | | |

## money_close_month

`money_close_month` is the close-month row. The flow is `e2e/maestro/12_story_close_month.yaml` (`Story - close last month`, tag `smoke`), added in PR #43.

It signs in with `seed=ready_to_close&today=2026-11-03`, opens Money, scrolls to and taps `money_close_month`, waits for `close_ready`, taps `close_button`, waits for `close_done`, opens the previous month, asserts `סגור` and `money_status_value` matching optional bidi isolates around `₪13,000`, opens `money_month_summary`, waits for `summary_overruns`, and asserts `מזון`.

Those targets exist on main:

- `money_close_month` `MoneyContent.kt:197`; `money_month_summary` `MoneyContent.kt:189` (closed months only)
- `close_ready` `MonthCloseContent.kt:161` (`strings_close.xml:11`); `close_button` `MonthCloseContent.kt:135`; `close_done` `MonthCloseContent.kt:100`
- `money_month_previous` `MonthStatusCard.kt:60`; `סגור` is `status_closed` (`strings_month_status.xml:8`) drawn at `MonthStatusCard.kt:76`
- `money_status_value` `MonthStatusCard.kt:155`. `MoneyFormatter` wraps amounts in U+2066/U+2069. ₪13,000 is 20,000 income minus 20 charges of 350 (`Stories.kt`, `ready_to_close`). `MoneyStoriesTest.previousMonthCanCloseWithTheExpectedResult` asserts 1,300,000 agorot and passed.
- `summary_overruns` `MonthSummaryContent.kt:68`; the row title is `row.name` (`MonthSummaryContent.kt:72`). `מזון` is category `story-food` (`Stories.kt:26`).
- Launch wait `nav_home|google_sign_in_button`: `FloatingNavBar.kt:177`, `SignInScreen.kt:133`, in unnamed `e2e/maestro/subflows/fresh_start.yaml`.

It passed here (34s, and 30s inside the full suite). CI on PR tip `0d1ec44` (run 41628891430) also passed it (45s). It would fail if the cleared app draws nothing, if the status text is not ₪13,000 with those isolates, or if the summary omits `מזון`. None of that happened on this SHA.

## PR #43 / #45 residue

PR #45 head `a4749bbdac0041f5753e2a45bdef97690ae24847` failed CI run 41608860424 with `Unresolved reference: MoneyEntry` at `MoneyContent.kt:216` (the `MoneyEntry.CardEntry` call had no import). Commit `0903972` restored the import before the #45 merge. On main the import is `MoneyContent.kt:46` and the call is `MoneyContent.kt:217`. `:app:compileDebugKotlin` and all 839 unit tests passed, so that error is gone.

PR #43's E2E job exited 1 on intermediate heads. Several died before Maestro, so no flow was named: runs 41590338014 and 41592740228 (`AssertionError: release clock provider missing`), run 41593876016 (`BUILD FAILED` in the release/e2e compile). Later heads reached Maestro and failed named smoke flows (blank first screen, `money_status_value`, a literal `₪13,000` without bidi marks, `import_category_story-rent`). The blank-screen wait lives in `fresh_start.yaml`, which has no `name:`. Merged tip `0d1ec44` passed every smoke flow, including close-month. Main also contains the #45 chart edits to `MoneyContent.kt` and `MoneyScreen.kt`. The close-month flow was run again on that combined tree and passed.

Flows on main, all of which passed here:

| File | Name | Tags |
| --- | --- | --- |
| `01_sign_in_money_tab.yaml` | Sign in and open the money tab | smoke |
| `02_import_same_file_twice.yaml` | Import a shared bank file, then the same file again | smoke, import |
| `03_month_navigation.yaml` | Navigate months | smoke, navigation |
| `10_story_new_month.yaml` | Story - new month | smoke |
| `11_story_mid_month.yaml` | Story - mid month | smoke |
| `12_story_close_month.yaml` | Story - close last month | smoke |
| `13_story_retro_and_period.yaml` | Story - retroactive close and period | stories |
| `14_story_states_gallery.yaml` | Story states gallery | states |
| `subflows/fresh_start.yaml` | (no name; shared launch) | |

Every id and literal those flows use still exists, including `לחודש`, `התמונה הגדולה`, `נשאר לשוטפות`, `עוד לא יובאו תנועות לחודש הזה`, `שכר דירה` (`ImportCategorySheet.kt:125`), `לדלג, לא ידוע` (`CloseBalancesContent.kt:39`), `3 ח׳` (`PeriodContent.kt:84`), `סגור`, and `מזון`. `import_category_${category.id}` is still set at `ImportCategorySheet.kt:129`. No current flow points at a missing tag or text.

## Not run

No requested Gradle task was skipped. Firestore rules tests were outside this request. The release APK uses the throwaway key above, not the Play upload key.
