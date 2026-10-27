# R2.1 — desugaring, numberCompact, danger dialogs, AppShell goldens

commit: see git log

Base is `832b188` (R2 `b493c92` plus the money engine from PR #2). The three patches applied with `git am --3way` in order (devops `0fa00be`, android `e759791`, qa `8f8a244`) and were squashed into this commit. No new product or test code was written here. No test was disabled, skipped, re-recorded, or loosened. No secret is in this report.

Money files are untouched. `git show --stat` for this commit has no path under `feature/money/domain`, `docs/money`, or `MoneyEngineTest.kt`. The other paths are the three patches plus this report:

```
.github/workflows/distribute.yml
app/build.gradle.kts
app/lint.xml
auth/SignOutConfirmDialog.kt
core/designsystem/component/MetricCard.kt
core/designsystem/theme/Type.kt
ui/home/HomeContent.kt
ui/settings/SettingsDialog.kt
core/designsystem/AppTypeNumberCompactTest.kt
ui/shell/AppShellScreenshotTest.kt
AppShellScreenshotTest.appShell_home_dark_font10.png
AppShellScreenshotTest.appShell_home_light_font10.png
HomeContentScreenshotTest.homeDreams128_dark_font13.png
HomeContentScreenshotTest.homeDreams128_light_font13.png
HomeContentScreenshotTest.homeDreams4_dark_font10.png
HomeContentScreenshotTest.homeDreams4_dark_font13.png
HomeContentScreenshotTest.homeDreams4_light_font10.png
HomeContentScreenshotTest.homeDreams4_light_font13.png
gradle/libs.versions.toml
docs/handoff/REPORT.md
```

`firestore.rules`, `firestore.indexes.json`, `firestore.test.js`, `firebase.json`, and `.github/workflows/firestore-rules.yml` were not changed.

## Raw Gradle lines

JDK 21. Placeholder `app/google-services.json` was already on disk and gitignored. `assembleRelease` used a throwaway PKCS12 keystore whose key password equals its store password. Roborazzi stayed in verify mode (`roborazzi.test.verify=true`).

`./gradlew clean testDebugUnitTest assembleRelease lintDebug --no-daemon` exit 0:

```
BUILD SUCCESSFUL in 3m 58s
99 actionable tasks: 92 executed, 7 from cache
```

`finalizeTestRoborazziDebug` ran. No golden mismatched.

JUnit XML (`app/build/test-results/testDebugUnitTest`, 51 files): tests=218, failures=0, errors=0, skipped=0.

Of those, `MoneyEngineTest` contributed tests=24, failures=0, errors=0, skipped=0. The other 194 are the R2.1 total (including `HomeContentScreenshotTest` 10, `AppShellScreenshotTest` 2, `HomeHalfWidthValueLayoutTest` 3, `AppTypeNumberCompactTest`).

## Task table

| item | status | files | test |
| --- | --- | --- | --- |
| 1. coreLibraryDesugaring + desugar_jdk_libs 2.1.5; lint.xml (all=warning, NewApi=fatal); `lintDebug` in distribute.yml after assembleRelease | done | `app/build.gradle.kts`, `app/lint.xml`, `gradle/libs.versions.toml`, `.github/workflows/distribute.yml` | `lintDebug` exit 0; NewApi count 0 |
| 2. AppType.numberCompact 28sp (tnum, line height 33.6sp) on half-width Dreams and Shopping | done | `Type.kt`, `MetricCard.kt`, `HomeContent.kt` | `AppTypeNumberCompactTest`; `HomeHalfWidthValueLayoutTest` unedited, lineCount=1 at font 1.3, dreams=128 |
| 3. Sign-out confirm uses appColors.danger and colorScheme.onError; backup timestamp uses AppType.caption and appColors.textSecondary | done | `SignOutConfirmDialog.kt`, `SettingsDialog.kt` | compiles in the unit-test and release build; no new golden |
| 4. Re-record the six HomeContent count goldens; add two AppShell goldens | done, with the deviation below | `AppShellScreenshotTest.kt` and the eight PNGs | both screenshot classes passed in verify mode |

## Lint before / after

Before, from devops.md at `b493c92` (`./gradlew lintDebug`): 130 errors (122 NewApi + 8 StringFormatMatches), 84 warnings. `BUILD FAILED`.

After, this run on `832b188` plus R2.1: 0 errors, 110 warnings. NewApi is 0, including inside the money files. Text report ends with `0 errors, 110 warnings`.

| severity | id | count |
| --- | --- | ---: |
| Warning | UnusedResources | 37 |
| Warning | GradleDependency | 24 |
| Warning | NewerVersionAvailable | 9 |
| Warning | StringFormatMatches | 8 |
| Warning | DuplicateStrings | 5 |
| Warning | IconLauncherShape | 5 |
| Warning | LogConditional | 5 |
| Warning | TypographyQuotes | 4 |
| Warning | AndroidGradlePluginVersion | 2 |
| Warning | CredentialManagerMisuse | 2 |
| Warning | InvalidPackage | 2 |
| Warning | ConvertToWebp, RedundantLabel, SimpleDateFormat, StaticFieldLeak, SyntheticAccessor, TypographyEllipsis, UseKtx | 1 each |

The original 84 warnings are still warnings. The 8 StringFormatMatches moved from Error to Warning. Eighteen more warnings come from checks that `id="all"` turns on (DuplicateStrings, LogConditional, TypographyQuotes, InvalidPackage, ConvertToWebp, SyntheticAccessor).

## Goldens

Changed (6), under `app/src/test/snapshots/designsystem/`. Cause: the Dreams count moved from 20sp Medium to 28sp numberCompact.

- `HomeContentScreenshotTest.homeDreams4_light_font10.png`
- `HomeContentScreenshotTest.homeDreams4_light_font13.png`
- `HomeContentScreenshotTest.homeDreams4_dark_font10.png`
- `HomeContentScreenshotTest.homeDreams4_dark_font13.png`
- `HomeContentScreenshotTest.homeDreams128_light_font13.png`
- `HomeContentScreenshotTest.homeDreams128_dark_font13.png`

Added (2):

- `AppShellScreenshotTest.appShell_home_light_font10.png`
- `AppShellScreenshotTest.appShell_home_dark_font10.png`

The four `homeEmpty_*` goldens were not re-recorded.

## Allow-lists (NightBlueGuardTest)

Read from the test source after this commit. Unchanged from R2.

| list | size |
| --- | ---: |
| colour (`Color(0x` outside theme) | 16 |
| raw `.sp` | 31 |
| legacyChrome | 28 |

`SettingsDialog.kt` stays on the `.sp` list and the legacyChrome list. It is not on the colour list.

## Who did what

| Item | Specialist | Patch | Summary |
| --- | --- | --- | --- |
| 1 desugaring + lint NewApi gate + CI step | devops-expert | devops.patch 0fa00be | coreLibraryDesugaring + desugar_jdk_libs 2.1.5; app/lint.xml (all=warning, NewApi=fatal) via lintConfig, abortOnError=true; distribute.yml `./gradlew lintDebug --no-daemon` after assembleRelease, before upload. Gate proven with probes (NotificationChannel -> Fatal exit 1; WrongConstant -> warning exit 0). |
| 2 AppType.numberCompact 28sp | android-kotlin-expert | android.patch e759791 | numberCompact 28sp tnum, lh 33.6sp; MetricValueStyle.Compact on half-width Dreams+Shopping cards; AppTypeNumberCompactTest; HomeHalfWidthValueLayoutTest unedited, passes (1.3, dreams=128, lineCount=1). |
| 3 dialogs | android-kotlin-expert | android.patch e759791 | SignOutConfirmDialog appColors.danger + contentColor colorScheme.onError (AA 7.28 light / 9.58 dark); SettingsDialog backup line AppType.caption + appColors.textSecondary. |
| 2 goldens + 4 AppShell goldens | qa-expert | qa.patch 8f8a244 | Re-recorded only 6 HomeContentScreenshotTest goldens: homeDreams4_{light,dark}_font{10,13}, homeDreams128_{light,dark}_font13. New AppShellScreenshotTest (2 goldens light/dark, RTL via LocalLayoutDirection). |
| Reviews | security-expert, code-reviewer, ux-ui-designer | security.md, review.md, design.md | All APPROVED. |
| Planning, decisions, integration routing | chief-architect | — | No code. Continued on `832b188` because the patches do not touch the money engine. |
| Integration, one commit, CI run, push | integrator (this cloud agent) | — | Applied the three patches on `832b188`, squashed, ran the clean CI command, amended this report. |

## Decisions

- Strict "NewApi only" via lint.xml rather than abortOnError + per-id downgrades.
- Item 4 deviation: the AppShell golden does not compose `AppShell()`. It composes `HomeContentAtFixedClock` plus `r2NavBar`, placed the way AppShell places the bar, with RTL pinned through `LocalLayoutDirection`. `AppShell()` builds `MainViewModel` and `AppContainer`, which reach Firestore. The golden can drift from the real shell without a failing test. Proposed follow-up: a stateless `AppShellContent(state, onNav...)` seam that `AppShell` calls, then point the golden at that seam.
- 28sp barely grows at fontScale 1.3 (39 to 40 px ink): Android 14+ nonlinear font scaling. The ux-ui-designer accepts that. No workaround was added.
- Continue on `832b188` instead of `b493c92`. The money engine from PR #2 stays as merged. This commit does not edit it.

## Noticed, not changed

- Other destructive buttons (DreamDialogs, TaskDialogs, household sign-out) still use colorScheme.error.
- 8 StringFormatMatches lint findings are real bugs to fix later.
- lintDebug runs after assembleRelease; a PR-time workflow would need google-services.json secret on PRs.
- docs/RULES.md stale: claims app builds without google-services.json; does not mention lint step.
- Pre-existing: firebase-tools version unpinned in upload step; actions/checkout@v5 and setup-java@v5 tag-pinned, not SHA.
- Never upload lint reports as artifacts while google-services.json/keystore are on disk.
- Shell goldens use dreamsActive=null; nav-bar modifiers copied into the test.
- Mockup .num.m is 22px; brief value 28sp is binding.
- Optional later: golden at fontScale 2.0.
