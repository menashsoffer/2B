# Task 3 — Install on our phones via Firebase App Distribution

commit: see git log

Item 4 was not run. No workflow was triggered. The commit message contains `[skip ci]` so this push does not start a run while secrets are missing. No test was deleted, `@Ignore`d, or weakened. `debugConfig` and the gitignored `debug.keystore` are unchanged. No release password, keystore, or base64 is in the repo or in this report.

## Raw Gradle line

`./gradlew testDebugUnitTest --rerun-tasks`:

```
BUILD SUCCESSFUL in 47s
```

Gradle printed no test-count line on success. `app/build/test-results/testDebugUnitTest/TEST-*.xml` (25 files): tests=111, failures=0, errors=0, skipped=0.

## Release key fingerprints

- SHA-1: `1B:A8:5C:43:31:B2:F0:83:88:E6:80:26:13:6A:88:FB:11:71:FF:A7`
- SHA-256: `B5:D3:22:D7:27:DB:1B:E9:BD:A4:FC:B6:93:9E:8E:80:6F:33:49:56:5D:B0:24:C9:8A:53:92:5D:62:E0:1E:22`

Add both to the Firebase Android app. They are public.

## Secrets

`gh secret set` for `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` failed: HTTP 403, `Resource not accessible by integration` on `https://api.github.com/repos/menashsoffer/Myself-/actions/secrets/public-key`. Values were passed on stdin, not on the command line.

The keystore, the passwords file, and the base64 file are only in the agent store. `chmod 600` was rejected by that filesystem (`EPERM`). Modes now: directory `777`, files `666`.

Paths:

- `/cursor/stores/self/signing/myself-release/myself-release.keystore`
- `/cursor/stores/self/signing/myself-release/passwords.env` — lines `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- `/cursor/stores/self/signing/myself-release/keystore.b64`

Secret names still to add in GitHub (Settings → Secrets and variables → Actions):

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `GOOGLE_SERVICES_JSON` — raw real `google-services.json`, not the fake unit-test file, not base64
- `FIREBASE_SERVICE_ACCOUNT` — service-account JSON key, role Firebase App Distribution Admin
- `FIREBASE_APP_ID`

Click by click for Menash:

1. Open `https://github.com/menashsoffer/Myself-/settings/secrets/actions`.
2. Click **New repository secret**.
3. Name: `KEYSTORE_BASE64`. Value: copy the whole contents of `keystore.b64`. Click **Add secret**.
4. Click **New repository secret** again. Name: `KEYSTORE_PASSWORD`. From `passwords.env`, copy only the text after `KEYSTORE_PASSWORD=`. Click **Add secret**.
5. Same for `KEY_ALIAS` and `KEY_PASSWORD`: name is the left side, value is only the text after `=`.
6. New secret `GOOGLE_SERVICES_JSON`: paste the real Firebase Android `google-services.json` as raw JSON.
7. In Firebase, create a service account with role **Firebase App Distribution Admin**, download its JSON key. New secret `FIREBASE_SERVICE_ACCOUNT`: paste that JSON.
8. New secret `FIREBASE_APP_ID`: the Android app id (`1:…:android:…`).
9. Firebase Console → App Distribution → Testers & groups: create a group whose alias is exactly `family`, and add both phones' accounts.
10. Project settings → the Android app: add the SHA-1 and SHA-256 above.
11. Copy `myself-release.keystore` and `passwords.env` to an offline backup outside this machine and outside the repo. Losing the keystore means the installed app can never be updated in place.

After those seven secrets exist, say so. Item 4 (one workflow run) waits for that.

## Task table

| item | status | files changed | test that covers it |
| --- | --- | --- | --- |
| 1a. One release keystore | done, not in git | agent store `signing/myself-release/` only | no unit test |
| 1b. GitHub secrets | blocked on permission | none in the repo | `gh secret set` → HTTP 403 |
| 1c. CI signing from env; local debug keystore kept | done | `app/build.gradle.kts` | `testDebugUnitTest` still uses `debugConfig`; suite 111/0 |
| 1d. Print SHA-1 and SHA-256 | done | this report and `docs/RULES.md` section 8 | no unit test |
| 2. Workflow: test, then signed APK, then Firebase | written, not run | `.github/workflows/distribute.yml` | fail-fast is the first step; unit suite must pass before `assembleRelease` |
| 3. Install and distribution docs | done | `docs/RULES.md` section 8 | no unit test |
| 4. First workflow run | not run | none | waiting until the secrets above are confirmed |

Docs used for the upload:

- `https://firebase.google.com/docs/app-distribution/android/distribute-cli`
- `https://firebase.google.com/docs/app-distribution/authenticate-service-account` (`GOOGLE_APPLICATION_CREDENTIALS` is a path to the JSON file)

`versionCode` is `VERSION_CODE` when set, otherwise 1. The workflow sets it from `github.run_number`.

## Who did the work

chief-architect scoped and dispatched; executed by a Cursor cloud coding agent.

## Noticed, not changed

- `debugConfig` still uses store password and key password `android` with alias `androiddebugkey`. That was already in the repo. Release passwords are not.
- The release signing block still falls back to `my-upload-key.jks` when `KEYSTORE_PATH` is unset. CI sets the path. Local `assembleRelease` is not how we build for the phones.
- `enqueuePeriodicWork` is still a 1-day period. Task 2b left that as an open question. This task did not change it.
- The agent-store files could not be mode `600`. They were not copied into the repo to work around that.

## Open questions

1. After the seven secrets and the `family` group exist, may item 4 run once?
2. The service-account JSON and the real `google-services.json` are not on this machine. They have to come from Menash.
