# Task 4 — Deploy rules from the repo + first device bugs

commit: see git log

Not pushed. The new workflow's `paths` include `.github/workflows/firestore-rules.yml` and `firestore.test.js`. This commit changes both, so a push to `main` would start that workflow and, after the emulator tests, run `firebase deploy --only firestore:rules,firestore:indexes`. The `github-distribution` service account is not known to have the rules and index roles yet, so the commit stays local.

No test was deleted, `@Ignore`d, or weakened. No secret value is in this report. `distribute.yml` was not changed.

## Raw Gradle lines

`./gradlew testDebugUnitTest --no-daemon` exit 0:

```
BUILD SUCCESSFUL in 57s
```

`app/build/test-results/testDebugUnitTest/TEST-*.xml` (25 files): tests=112, failures=0, errors=0, skipped=0.

`./gradlew assembleRelease --no-daemon` exit 0, with a throwaway PKCS12 keystore whose key password equals its store password (not the release key):

```
BUILD SUCCESSFUL in 55s
```

Firestore emulator command, the same one the new workflow runs (`firebase emulators:exec --only firestore --project demo-no-project "node --test firestore.test.js"`), exit 0:

```
# tests 10
# pass 10
# fail 0
# cancelled 0
```

## IAM roles for github-distribution

Least privilege, both on the Firebase project:

1. **Firebase Rules Admin** (`roles/firebaserules.admin`). It can create rulesets and releases. https://cloud.google.com/iam/docs/roles-permissions/firebaserules
2. **Cloud Datastore Index Admin** (`roles/datastore.indexAdmin`). The index page lists this role, not a broader owner role, as enough to create indexes. https://firebase.google.com/docs/firestore/query-data/indexing and https://firebase.google.com/docs/firestore/security/iam

Deploy command and named-database config: https://firebase.google.com/docs/rules/manage-deploy

## Enterprise edition

https://firebase.google.com/docs/firestore/enterprise/create-databases says an Enterprise database in Native mode has Cloud Firestore Security Rules (test mode or production mode) and can enable realtime listeners. https://firebase.google.com/docs/rules/manage-deploy says rules for a named database are deployed from the `database` entry in `firebase.json` (`firebase deploy --only firestore:rules`, or `firebase deploy --only firestore:<databaseId>`). Index deploy uses the same CLI (`firebase deploy --only firestore:indexes`). `firebase.json` already points rules and indexes at `ai-studio-android-a829880c-5ce2-440f-a60c-5df6579fdc7f`, and the workflow deploys only that config. A live deploy against that Enterprise database was not run.

## What changed

Rules deploy is a separate workflow, `.github/workflows/firestore-rules.yml`. It runs on `workflow_dispatch` and on push to `main` when one of these changes: `firestore.rules`, `firestore.indexes.json`, `firestore.test.js`, `firebase.json`, or the workflow file. It fails first if `FIREBASE_SERVICE_ACCOUNT` is missing, runs the emulator tests, and only then deploys. The project id is read from that service-account JSON at deploy time.

Firestore `errorFlow` values now go through `ErrorMapper`. The banner gets the Hebrew text. The technical `action (code): message` string is logged and stored in `lastTechnicalError`.

The BOI worker is a self-rescheduling `OneTimeWorkRequest` (`BoiSyncWork`, `ExistingWorkPolicy.REPLACE`) with exponential backoff. `doWork` uses the injected `Clock`. A run at Friday 12:45 schedules Monday 15:45. The old periodic name is cancelled.

`firestore.test.js` loads `firebase/compat` from `NODE_PATH` instead of a machine-specific absolute path. That is what the emulator run above used.

## Task table

| item | status | files changed | test that covers it |
| --- | --- | --- | --- |
| 1. CI deploys rules and indexes only after emulator tests | written, not pushed | `.github/workflows/firestore-rules.yml`, `firestore.test.js` | emulator: `# tests 10`, `# pass 10`, `# fail 0` |
| 2. Friendly Firestore errors | done | `FirestoreErrors.kt`, `LifeHubRepository.kt`, `HouseholdRepository.kt` | `firestoreErrorFlow_permissionDenied_isFriendlyHebrew` |
| 3. Worker reschedules itself | done | `BoiSyncWorker.kt`, `MyselfApp.kt` | `testWorkerDelay` (Friday 12:45 → Monday 15:45) |
| 4. Backlog note for the non-fatal KSP annotation | done | `docs/BACKLOG_STEP8.md` | no unit test |

## Who did the work

chief-architect scoped and dispatched; executed by a Cursor cloud coding agent.

## Noticed, not changed

- `distribute.yml` still builds and uploads an APK on every push to `main`. It does not deploy rules.
- KSP stays 2.3.5. The AWT `ApplicationManager.getApplication()` NPE is still the non-fatal annotation from https://github.com/google/ksp/issues/2763.
- `firebase.json` has no `edition` field. The edition is a property of the existing database.
- Holiday and holiday-eve rates are still not in the schedule.
- `enqueue` uses `REPLACE` while `doWork` is still the running unique work. That is what the task asked for.

## Open questions

1. After `roles/firebaserules.admin` and `roles/datastore.indexAdmin` are on `github-distribution`, may this commit be pushed?
