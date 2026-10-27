# Task 5 — Groundwork before the redesign

commit: see git log

No test was deleted, `@Ignore`d, or weakened. No secret value is in this report. `firestore.rules`, `firestore.indexes.json`, `firestore.test.js`, `firebase.json`, and `.github/workflows/*` were not changed.

## Raw Gradle lines

`./gradlew testDebugUnitTest --no-daemon` exit 0:

```
BUILD SUCCESSFUL in 1m 15s
```

`app/build/test-results/testDebugUnitTest/TEST-*.xml` (28 files): tests=125, failures=0, errors=0, skipped=0.

`./gradlew assembleRelease --no-daemon` exit 0, with the gitignored `app/google-services.json` already on disk and a throwaway PKCS12 keystore whose key password equals its store password (not the release key):

```
BUILD SUCCESSFUL in 1m 34s
```

Not run locally, and why:

- The workflow's secret check, SDK install, `google-services.json` write, and keystore decode use GitHub Actions secrets. The unit-test and release-build commands were run instead, with the gitignored json and the throwaway keystore.
- `uploadCrashlyticsMappingFileRelease` did not run. Release `isMinifyEnabled` is false, so R8 writes no `mapping.txt` and the Crashlytics plugin does not create an upload task. `injectCrashlyticsMappingFileIdRelease` did run inside `assembleRelease`.
- `firebase appdistribution:distribute` was not run. It needs `FIREBASE_SERVICE_ACCOUNT` and `FIREBASE_APP_ID`, which are not on this machine.

## Task table

| item | status | files | test |
| --- | --- | --- | --- |
| 1. BOI worker schedules the next run without cancelling itself | integrated | `BoiSyncWorker.kt`, `BoiSyncWorkerTest.kt` | `BoiSyncWorkerTest` (SUCCEEDED and exactly one ENQUEUED; retry path) |
| 2. Crashlytics, release only, sanitized non-fatals | integrated | `CrashlyticsNonFatal.kt`, `SanitizingUncaughtExceptionHandler.kt`, `ErrorReporter.kt`, `FirestoreErrors.kt`, `ErrorMapper.kt`, `MyselfApp.kt`, debug and release manifests, Gradle plugin 3.0.8, `CrashlyticsNonFatalTest.kt`, `docs/RULES.md` | `CrashlyticsNonFatalTest` |
| 3. Codebase inventory | integrated | `docs/INVENTORY.md` | docs only; no unit test |

## Files added or changed

Every file is at or under 400 lines.

| file | lines |
| --- | --- |
| `app/build.gradle.kts` | 156 |
| `app/src/debug/AndroidManifest.xml` | 12 |
| `app/src/release/AndroidManifest.xml` | 9 |
| `MyselfApp.kt` | 21 |
| `CrashlyticsNonFatal.kt` | 99 |
| `SanitizingUncaughtExceptionHandler.kt` | 59 |
| `ErrorReporter.kt` | 46 |
| `BoiSyncWorker.kt` | 141 |
| `ErrorMapper.kt` | 79 |
| `FirestoreErrors.kt` | 25 |
| `CrashlyticsNonFatalTest.kt` | 289 |
| `BoiSyncWorkerTest.kt` | 214 |
| `build.gradle.kts` | 9 |
| `docs/INVENTORY.md` | 232 |
| `docs/RULES.md` | 207 |
| `gradle/libs.versions.toml` | 111 |

## Who did what

- chief-architect: scoped, split and routed the task, decided scope (no workflow change, out-of-scope items), reviewed and ordered integration.
- android-kotlin-expert: BoiSyncWorker `APPEND_OR_REPLACE` fix; Crashlytics 3.0.8 release-only; sanitized non-fatals from ErrorReporter and FirestoreErrors; `SanitizingUncaughtExceptionHandler` (fail-safe); `CrashlyticsNonFatalTest`; reviewed qa's test seam (accepted).
- qa-expert: `BoiSyncWorkerTest` with Robolectric and `WorkManagerTestInitHelper` (SUCCEEDED and exactly one ENQUEUED; retry path) plus the `rateSource` test seam; proved the test fails when `doWork` is switched back to `REPLACE` (local only): `finished BoiSyncWork run should be SUCCEEDED, not CANCELLED expected:<SUCCEEDED> but was:<null>`.
- security-expert: PII review; found raw messages in fatal crashes (fixed) and a non-fail-safe handler (fixed); verified no secrets in history and debug collection off.
- devops-expert: verified `distribute.yml` needs no change (release minify off, so no `mapping.txt` and no upload task; `google-services.json` is written before Gradle; no new secret or IAM). When R8 is turned on, check Android API key restrictions.
- docs-writer: `docs/INVENTORY.md`.
- Cursor cloud coding agent: integration, local CI run, push.

## Worker semantics

App start still enqueues `BoiSyncWork` with `ExistingWorkPolicy.REPLACE`. Inside `doWork`, the next slot is enqueued with `ExistingWorkPolicy.APPEND_OR_REPLACE`, so the running worker is not cancelled.

On a permanent fetch failure, `doWork` enqueues the next slot and returns success. A failed predecessor would fail the `APPEND` dependent. A network failure with `runAttemptCount < 3` returns retry and does not enqueue.

## Doc citations

WorkManager:

- https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work
- https://developer.android.com/reference/androidx/work/ExistingWorkPolicy#APPEND_OR_REPLACE
- https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/chain-work

`REPLACE` cancels pending unique work, including a run that is still `RUNNING`. `APPEND_OR_REPLACE` appends the new request after that run. https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work says a `REPLACE` enqueue is one of the reasons a running worker is stopped and marked cancelled.

Crashlytics is free on the Spark plan: https://firebase.google.com/docs/projects/billing/firebase-pricing-plans and https://firebase.google.com/products/crashlytics ("There's no cost to using Crashlytics.").

- Setup: https://firebase.google.com/docs/crashlytics/android/get-started
- Opt-in reporting: https://firebase.google.com/docs/crashlytics/android/customize-crash-reports#enable-opt-in-reporting
- Mapping upload when a mapping file exists: https://firebase.google.com/docs/crashlytics/android/get-deobfuscated-reports
- R8: https://developer.android.com/build/shrink-code

## Firebase console for Menash

Open Crashlytics in the Firebase console. If it prompts, enable it for the Android app `com.aistudio.mycalculators.qvxmzt`. The first report from a release build completes setup. No plan upgrade, no new secret, and no IAM change.

## Noticed, not changed

- Release logcat still contains uid, email, household ids, and raw messages (`AuthManager.kt:54`, `HouseholdRepository.kt`, `MigrationManager.kt:40`, `BaseViewModel.kt:41`, `ErrorReporter.kt:37`, `FirestoreErrors.kt`). That stays on the device.
- Firestore errors can be recorded twice, once from `FirestoreErrors` and once from `BaseViewModel`.
- An `initCause` edge case in the sanitizer can drop a sanitized cause. That is diagnostics only.
- `actions/checkout@v5` and `actions/setup-java@v5` are tag-pinned, not SHA-pinned.
- Release minify is off, so stack traces are not obfuscated and there is no mapping upload.
- The docs-writer also opened PR #1 for its branch. This commit supersedes that branch. The PR is left for Menash to close.
- Specialist branches `task5/*` are left on origin.

## Privacy notes

Crashlytics sends installation IDs, device model, OS, app version, and stack traces to Google, with 90-day retention. A one-line disclosure in the app for Menash's wife is recommended. Restrict the `google-services` Android API key to the package plus the signing certificate.

## Open questions

None beyond Claude's review.
