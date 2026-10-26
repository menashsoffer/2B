# Task 1 — Onboarding and baseline

No product code was changed. The only tracked additions are the Gradle wrapper scripts and jar (the wrapper properties file was already on `main` and was not modified) and this report.

## Commits pushed to main

- Full hash of `chore: add Gradle wrapper + Task 1 baseline report` is recorded in the follow-up commit on `main` (a commit cannot contain its own hash).
- Follow-up message: `docs: record Task 1 baseline commit hash`.

## a. Access

| Check | Result | How |
| --- | --- | --- |
| Read the private repo | yes | Workspace is `https://github.com/menashsoffer/Myself-`. `git fetch origin main` succeeded. Local `main` matched `origin/main` at `b9199d0` before this task's commit. |
| Push to `main` | pending this push | Will be updated in the follow-up commit after the push attempt. The owner authorized a direct push to `main`. |
| Run Gradle (build + unit tests) | yes | Commands were `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` (Gradle wrapper 9.3.1). JDK already on the VM: OpenJDK 21.0.12.1 (Ubuntu package `21.0.12.1+1-1-24.04.4-Ubuntu`). AGP 9.1.1's minimum JDK is 17. Android SDK was not installed; I installed it. |

Android SDK, installed by me under `/home/ubuntu/android-sdk` (`/opt` was not writable):

- Command-line tools: `commandlinetools-linux-15859902_latest.zip` from `dl.google.com`, placed at `cmdline-tools/latest`. Licenses accepted.
- `platforms;android-36.1` — matches `compileSdk { version = release(36) { minorApiLevel = 1 } }`.
- `build-tools;36.0.0` (AGP 9.1 default) and `build-tools;36.1.0`.
- `platform-tools`.
- Gitignored `local.properties` with `sdk.dir=/home/ubuntu/android-sdk`. `ANDROID_HOME` and `ANDROID_SDK_ROOT` were set to that path for the Gradle runs. Not committed.

Wrapper version: AGP `9.1.1` (`gradle/libs.versions.toml`). The AGP compatibility table lists Gradle **9.3.1** as both the minimum and the default for AGP 9.1 (Android Gradle plugin 9.1.1 release notes and https://developer.android.com/build/releases/about-agp). `gradle/wrapper/gradle-wrapper.properties` already pointed at `gradle-9.3.1-bin.zip`. I generated the missing `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` with the Gradle 9.3.1 binary: `gradle wrapper --gradle-version 9.3.1 --distribution-type bin`. The properties file content is unchanged (`git diff` empty).

## b. `git log -5 --oneline` of `origin/main` before this task's commit

```
b9199d0 feat: improve error tracking and BOI sync
fe04f75 refactor: improve BOI exchange rate staleness logic
51b2980 fix(exchange-rate): improve BOI rate handling robustness
d31d220 refactor: enhance manual exchange rate override
e141373 feat(ui): add Hebcal integration to home screen
```

## c. Gradle wrapper

Added (only these, plus this report):

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`

Already present, not modified: `gradle/wrapper/gradle-wrapper.properties` (`distributionUrl` = Gradle 9.3.1 bin).

## d. Build and unit tests

`google-services.json` is not in the repo. The google-services plugin is set to `MissingGoogleServicesStrategy.WARN`, and `gradle.properties` has `googleServices.missing.passthrough=true`.

`app/build.gradle.kts` signs debug with signing config `debugConfig`, which requires `/workspace/debug.keystore`. That file is gitignored and was not in the repo. The first `assembleDebug` (no json, no keystore) died at signing before Kotlin compile finished.

I then created a **local, gitignored** debug keystore with the alias and passwords already written in `app/build.gradle.kts` (`androiddebugkey` / `android`). It was not committed. I also created an **untracked, fake** `app/google-services.json` (package name `com.aistudio.mycalculators.qvxmzt`, project id `fake-local-not-a-real-firebase-project`, API key `FAKE_LOCAL_NOT_A_REAL_API_KEY`, web client id `000000000000-fakelocalonlynotarealclient.apps.googleusercontent.com`). It was not committed. `app/google-services.json` is **not** in `.gitignore`.

### Without `google-services.json` (local debug keystore present, so compile actually ran)

`./gradlew assembleDebug`

```
BUILD FAILED in 19s
```

`./gradlew testDebugUnitTest`

```
BUILD FAILED in 23s
```

Both failed in `:app:compileDebugKotlin` before any test ran. Compiler lines:

- `e: file:///workspace/app/src/main/java/com/menash/myself/auth/AuthManager.kt:36:40 Unresolved reference 'default_web_client_id'.`
- `e: file:///workspace/app/src/main/java/com/menash/myself/auth/AuthManager.kt:82:40 Unresolved reference 'default_web_client_id'.`

Earlier attempt, no keystore and no json — `./gradlew assembleDebug`:

```
BUILD FAILED in 1m 32s
```

First line of that failure: `Keystore file '/workspace/debug.keystore' not found for signing config 'debugConfig'.` The plugin had already printed `File google-services.json is missing.` as a warning, not as the failure.

### With the untracked fake `app/google-services.json` (and the local debug keystore)

`./gradlew assembleDebug`

```
BUILD SUCCESSFUL in 38s
```

`./gradlew testDebugUnitTest`

```
BUILD FAILED in 14s
```

No test method ran. `:app:compileDebugUnitTestKotlin` failed first. There is no test class / test name / assertion failure to list. The compiler lines are:

- class file `com.menash.myself.Step8ExternalDataAndFixesTest` — no test name (did not execute) — `e: file:///workspace/app/src/test/java/com/menash/myself/Step8ExternalDataAndFixesTest.kt:23:18 Conflicting import: imported name 'LocalDate' is ambiguous.`
- same class — no test name — `e: file:///workspace/app/src/test/java/com/menash/myself/Step8ExternalDataAndFixesTest.kt:33:18 Conflicting import: imported name 'LocalDate' is ambiguous.`

Cause, not fixed: `import java.time.LocalDate` appears twice in that file (lines 23 and 33). Nothing was disabled, ignored, or deleted.

Gradle also printed, on these builds: `Warning: SDK processing. This version only understands SDK XML versions up to 3 but an SDK XML file of version 4 was encountered.` The assemble with the fake json still finished successfully.

## e. Implementation plan (not edited)

File: `.aistudio/artifacts/brain/a829880c-5ce2-440f-a60c-5df6579fdc7f/implementation_plan.md`.

We are on **Step 8** (official external sources and DataStore: Bank of Israel and Hebcal), **Round 3**, which the plan titles "Current Fixes" and does **not** mark complete. Steps 0, 1, 2 (called 1.5), 3, 4, 5, 6, and 7 are marked complete.

Round 3, as written, is: separate `officialRate` from `manualRate`; staleness via an injectable clock (`BoiStalenessPolicy.isStale`); HTTP/CSV back in `BoiExchangeRateClient.fetchRateWindow`; WorkManager `UPDATE` and an exact delay; home Hebcal card opens `CityPickerDialog`; `Step8RoundThreeTest.kt`.

Still open in the plan:

- Step 8 itself has no completion mark (unlike steps 0–7). Round 3 is labeled current, not done.
- Step 9: sign in, create/join household, copy calculators from Room to `households/{hid}/...`, schema migration of existing Firestore docs (`space` to `ownerUid` + `forWhom` + `isPrivate`, money to agorot, dates to ISO `yyyy-MM-dd`), then remove Room only after a verified backup and counts. Not started.
- Step 9.5: calculators move from `Double` to `Money`. Not started.
- Phase 2 stays locked until Phase 1 is finished. Not started.

## f. Architecture (10 lines)

1. Package `com.menash.myself`, applicationId stays `com.aistudio.mycalculators.qvxmzt`. `core/` is shared infrastructure (repository interfaces, manual DI, design system, session, BOI and Hebcal clients). `feature/` modules (home, capture, dreams, tasks, fitness, calendar, import catalog, settings, household, calculators) use `ui/`, `viewmodel/`, and pure `domain/`.
2. Features depend only on interfaces in `core/data/repository`. They do not import Firestore.
3. `AppContainer` is manual DI, owned by `MyselfApp`, exposed as `LocalAppContainer`, and it survives activity recreation. It builds Room repositories for calculators and settings (`my_calculators.db`, usable with no signed-in user) and Firestore repositories for household data.
4. `HouseholdSession` is the active `householdId`. Firestore observe flows follow it with `flatMapLatest` and emit an empty list when the id is missing. `requireHouseholdId()` throws if a write would use an empty path.
5. Shared items live under `households/{id}/...`. Items with `isPrivate = true` live only under `users/{uid}/private/...`. Fitness stays on the user.
6. ViewModel actions go through `BaseViewModel.safeLaunch`, which stores a Hebrew message from `ErrorMapper` on `errorState` and on `ErrorReporter`. Cancellation is rethrown. JVM `Error` is rethrown.
7. Firestore `LifeHubRepository` and `HouseholdRepository` each expose `errorFlow`. `AppContainer.errorFlow` combines the reporter with those two flows.
8. `MainActivity` collects that flow and `MainViewModel.errorState` and shows `GlobalErrorBanner`. Screens also render `UiState.Error` with the shared `ErrorState` composable. Loading, empty, error, and offline are the four screen states.

## Task table

| item | status | files changed | test that covers it |
| --- | --- | --- | --- |
| a. Access: read repo, push, Gradle | done | none | no unit test; `git fetch` and the two `./gradlew` commands |
| b. `git log -5 --oneline` of `origin/main` | done | none | no unit test |
| c. Gradle wrapper matching AGP 9.1.1 (Gradle 9.3.1) | done | `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` (`gradle-wrapper.properties` unchanged) | no unit test; both Gradle commands ran via `./gradlew` |
| d. `assembleDebug` and `testDebugUnitTest`, nothing fixed | done | none committed (untracked fake `app/google-services.json`; gitignored `debug.keystore` and `local.properties`) | no test ran. `compileDebugUnitTestKotlin` failed on duplicate `LocalDate` import in `Step8ExternalDataAndFixesTest` |
| e. Plan step and round, file not edited | done | none | no unit test |
| f. Architecture note | done | none | no unit test |

## Who did the work

chief-architect (eng manager) scoped and dispatched; executed by a Cursor cloud coding agent; android-kotlin-expert and devops-expert were not involved in execution of this task.

## Noticed, not changed

- `Step8ExternalDataAndFixesTest.kt` imports `java.time.LocalDate` twice, so `testDebugUnitTest` does not compile. Not fixed.
- `AuthManager` reads `R.string.default_web_client_id`, which exists only after the google-services plugin processes a json that has an OAuth client of type 3. Without that file, `assembleDebug` does not compile.
- `app/google-services.json` is not listed in `.gitignore`. The fake file is untracked. `git add -A` would pick it up. I did not add a gitignore rule.
- Debug signing requires a gitignored `debug.keystore` that is not in the repo. `assembleDebug` cannot finish without one (or a signing change, which I did not make).
- SDK cmdline-tools warned that they only understand SDK XML up to version 3 and saw version 4. Assemble still succeeded with the fake json.
- `docs/KNOWN_ISSUES.md` locked calculator behaviors were not touched. Calculators are still on `Double` until Step 9.5.
- Instrumentation tests and `firestore.test.js` were not run. This task asked only for `assembleDebug` and `testDebugUnitTest`.

## Open questions for Claude

1. Is Step 8 Round 3 accepted? The plan still says "Current Fixes" and does not check Step 8 off, while `origin/main` already has the BOI and Hebcal commits through `b9199d0`.
2. May a later task fix the duplicate `LocalDate` import so unit tests compile? This task forbade that fix.
3. Should `google-services.json` be gitignored, and should CI inject a fake one so `default_web_client_id` resolves? I did not change ignore rules.
4. Should `debug.keystore` be supplied another way so `assembleDebug` works on a clean clone? I did not change signing.
