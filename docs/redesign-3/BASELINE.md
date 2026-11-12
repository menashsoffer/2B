# Stage 0 baseline

Recorded on `grok/redesign-3` after merging `origin/main`. No source, test, golden, workflow, Firebase, or Gradle edits in this commit. Nothing was pushed to `main`.

## Main

- `origin/main`: `1e1c27261dba173d8ad9a11eba6101121b55566f` (Fix import E2E selector for renamed return-to-month button)
- Merge commit parents: `32860bcd375408675648ff45d71e105b7bb35bc9` (previous baseline) and `1e1c27261dba173d8ad9a11eba6101121b55566f`
- This tree matches that main plus `docs/redesign-3/BASELINE.md`

## Counts

| Item | Value |
| --- | --- |
| Roborazzi goldens `app/src/test/snapshots/**/*.png` | 176 |
| Maestro YAML `e2e/maestro` | 9 (8 flows + `subflows/fresh_start.yaml`) |
| JUnit XML `testDebugUnitTest` | 978 tests, 0 failures, 0 errors, 0 skipped (165 files) |
| Lint `lint-results-debug.xml` | 194 warnings, 0 errors |

Kover (`app/build.gradle.kts`, verified by `koverVerifyDebug`):

| Variant | Threshold | Scope |
| --- | --- | --- |
| moneyDomain | 98% line / 85% branch | `feature.money.domain` |
| moneyImport | 95% line / 75% branch | `feature.money.importer` |
| moneyPresentation | 96% line | `feature.money` except domain, importer, data, and `ui` |

## Environment

- JDK: OpenJDK 21.0.13.0.3. Gradle: 9.3.1 (wrapper).
- Android SDK: `platforms;android-36.1`, `build-tools;36.0.0`, `platform-tools`.
- Gitignored and not committed: fake `app/google-services.json`, `debug.keystore`, `local.properties`. Release used `VERSION_CODE=1` and throwaway `/tmp/throwaway-release.jks` (`KEY_ALIAS=throwaway`). That APK is not the Play upload key.

## Commands

| Command | Result |
| --- | --- |
| `./gradlew testDebugUnitTest verifyRoborazziDebug koverVerifyDebug lintDebug assembleRelease --no-daemon --continue` | PASS, exit 0, 7m 17s |

```
> Task :app:testDebugUnitTest
> Task :app:verifyRoborazziDebug
> Task :app:koverVerifyMoneyDomain
> Task :app:koverVerifyMoneyImport
> Task :app:koverVerifyMoneyPresentation
> Task :app:koverVerifyDebug
> Task :app:lintDebug
> Task :app:assembleRelease
BUILD SUCCESSFUL in 7m 17s
111 actionable tasks: 104 executed, 7 from cache
```

No test or Gradle check failed. KSP printed an AWT `NullPointerException` on a background thread during processing; the build still finished exit 0.

## Maestro

Not run on this SHA. The API 34 Google APIs system image finished downloading after the Gradle suite, and no emulator was booted. Flows on main, unchanged by this merge: `01_sign_in_money_tab`, `02_import_same_file_twice`, `03_month_navigation`, `10_story_new_month`, `11_story_mid_month`, `12_story_close_month`, `13_story_retro_and_period`, `14_story_states_gallery`, plus `subflows/fresh_start.yaml`.
