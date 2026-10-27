# R2 — Floating 5-tab shell, home, Night Blue components, screenshot tests

commit: see git log

Integrator only. The four specialist patches were applied with `git am` in order (android, android-fix1, qa-spike, qa) on top of `8f3534dec0f120090949b83bfbd7d420653b72d6`, then squashed into this commit. No new feature code was written here. No test was deleted, `@Ignore`d, re-recorded, or given a threshold. No secret is in this report. `firestore.rules`, `firestore.indexes.json`, `firestore.test.js`, `firebase.json`, and `.github/workflows/*` were not changed.

## Raw Gradle lines

JDK `21.0.12.1` (OpenJDK 64-Bit, Ubuntu), arch `x86_64`. Roborazzi `verify=true` is in `gradle.properties`. A gitignored `app/google-services.json` was already on disk. `assembleRelease` used a throwaway PKCS12 keystore whose key password equals its store password (not the release key).

`./gradlew testDebugUnitTest assembleRelease --no-daemon` exit 0:

```
BUILD SUCCESSFUL in 2m 40s
88 actionable tasks: 81 executed, 7 from cache
```

`finalizeTestRoborazziDebug` ran. No golden mismatched.

`app/build/test-results/testDebugUnitTest/TEST-*.xml` (48 files): tests=190, failures=0, errors=0, skipped=0.

`./gradlew lintDebug` exit 1 (expected; not a CI gate):

```
Lint found 130 errors, 84 warnings.
BUILD FAILED in 52s
```

First failure:

```
AppContainer.kt:84: Error: Call requires API level 26, or core library desugaring (current min is 24): java.time.Clock#system [NewApi]
```

| severity | id | count |
| --- | --- | ---: |
| Error | NewApi | 122 |
| Error | StringFormatMatches | 8 |
| Warning | UnusedResources | 37 |
| Warning | GradleDependency | 24 |
| Warning | NewerVersionAvailable | 9 |
| Warning | IconLauncherShape | 5 |
| Warning | AndroidGradlePluginVersion | 2 |
| Warning | CredentialManagerMisuse | 2 |
| Warning | RedundantLabel, SimpleDateFormat, StaticFieldLeak, TypographyEllipsis, UseKtx | 1 each |

R2-added files have 0 NewApi. `SettingsDialog.kt` is touched and still has 3 NewApi issues, the same `java.time` calls at lines 143 and 151 that existed at `8f3534d`. The 8 StringFormatMatches errors are in legacy calculator and catalog files.

## Task table

| item | status | files | test |
| --- | --- | --- | --- |
| Shapes 20dp / pill, colorScheme surface guards, successSoft / warningSoft | done | `Palette.kt`, `Shape.kt`, `Theme.kt` | `NewComponentsContrastTest`, `NightBlueGuardTest` |
| HeroCard, FloatingNavBar, PillTabs, Chip, ProfileAvatar | done | `core/designsystem/component/` | screenshot tests, `ProfileAvatarSemanticsTest` |
| MetricCard half-width uses MetricValueStyle.Medium (20sp tabular). numberCompact not added | done | `MetricCard.kt`, `HomeContent.kt` | `HomeHalfWidthValueLayoutTest` (lineCount=1, font 1.3, dreams=128) |
| LegacyColors | not present, nothing deleted | — | — |
| 5-tab shell, per-tab back stack, no TopAppBar, start = home | done | `AppShell.kt`, `AppNavHost.kt`, `ShellTab.kt` | `HomeChromeTest`, `FloatingNavBarStructureTest` |
| Account sheet (settings, household, sign-in / sign-out) | done | `AccountSheet.kt`, `SignOutConfirmDialog.kt`, `AuthManager.kt` | `AuthManagerSignOutTest`; contrast cases. No screenshot |
| Home: money hero, dreams + shopping half width, tracking full width, no fake numbers | done | `HomeContent.kt`, `HomeViewModel.kt`, `HomeDreams.kt` | `HomeNoFakeNumberTest`, `HomeContentScreenshotTest`, `HomeDreamsTest` |
| Money, dreams, shopping, tracking placeholders | done | `PlaceholderTab.kt` | `PlaceholderTabStructureTest`, `PlaceholderEmptyStateScreenshotTest` |
| Gallery index "מסכים ישנים" | done | `GalleryIndex.kt`, `SettingsDialog.kt` | gallery entry unchanged (long-press on the version line) |
| Greeting and Hebrew date | done | `HomeGreeting.kt` | `GreetingForTest`, `HebrewDateLineTest` |
| Drop java.time from Home, remove the dead capture FAB | done | `HomeGreeting.kt`, `MainActivity.kt`, `Commit534f2feReviewFixesTest.kt` | FAB-absent assertion (stricter) |
| Roborazzi 1.77.0, verify=true | done | `gradle.properties`, `libs.versions.toml`, `build.gradle.kts` | 44 goldens under `app/src/test/snapshots/designsystem` |
| Desugaring / minSdk change | not in R2 | — | — |

## Who did what

- chief-architect: plan, brief, split, routing, decisions (MetricValueStyle.Medium instead of numberCompact; no desugaring in R2), integration review. Wrote no code.
- ux-ui-designer: design spec R2-spec.md (tokens, shapes, components, home layout, contrast table); final design review of goldens — APPROVE.
- android-kotlin-expert: spikes (icons, Dream status strings); all production code (tokens/shapes/colorScheme guards, HeroCard, FloatingNavBar, PillTabs, Chip, ProfileAvatar, MetricCard, 5-tab shell with per-tab back stack, AccountSheet, Home + HomeViewModel/HomeContent, placeholders, gallery index "מסכים ישנים", NightBlueGuardTest extension, GreetingForTest/HebrewDateLineTest/HomeDreamsTest, AuthManagerSignOutTest, SignOutConfirmDialog); fix round 1 (android-fix1).
- qa-expert: Roborazzi 1.77.0 spike (works on AGP 9.1.1, verify=true in gradle.properties); full §8 test matrix (44 goldens, NewComponentsContrastTest, semantics/structure tests, ContentPaddingTest, HomeNoFakeNumberTest, half-width number at font 1.3 with dreams=128).
- security-expert: reviewed android.patch (1 fix: sample name), android-fix1, qa.patch PNGs — APPROVE.
- code-reviewer: reviewed android.patch (blocking: java.time on minSdk 24, dead FAB, hardcoded greetings), android-fix1, qa patches — APPROVE.
- integrator (this cloud agent): applied patches, single commit, local CI, push, Distribute.

## Allow-lists (NightBlueGuardTest)

| list | at 8f3534d | now |
| --- | ---: | ---: |
| colour (`Color(0x` outside theme) | 16 | 16 |
| raw `.sp` | 32 | 31 |
| legacyChrome | absent | 28 |

Colour: unchanged (same 16 calculator, home, and task files). `.sp`: `MainActivity.kt` removed, nothing added. legacyChrome: 28 pre-existing files (raw `Color.White/Black/Gray/LightGray`, `TopAppBar(`, `containerColor = MaterialTheme.colorScheme.primary`).

## Contrast

Expected ratios are from R2-spec §8.2. `NewComponentsContrastTest` reads `NightBlue` and asserts the floors. It does not hardcode the ratios. Text floor 4.5:1. `AppType.number` (40sp) and icons 3:1. Closest pairs: textSecondary/bg 4.55 and Hero α0.82 4.83. Do not lower that alpha or lighten textSecondary.

| component | pair | light | dark | floor |
| --- | --- | ---: | ---: | ---: |
| Header greeting / date | textPrimary/bg · textSecondary/bg | 14.56 · 4.55 | 15.15 · 7.09 | 4.5 |
| ProfileAvatar | accentStrong/accentSoft | 6.89 | 7.72 | 4.5 |
| MetricCard label·detail / value | textSecondary/card · textPrimary/card | 4.97 · 15.90 | 6.36 · 13.58 | 4.5 |
| HeroCard value | onAccent/accent | 6.28 | 7.38 | 3.0 |
| HeroCard label·message α0.82 | onAccent82/accent | 4.83 | 5.39 | 4.5 |
| FloatingNavBar active label / icon | accentStrong/card · accentStrong/accentSoft | 8.40 · 6.89 | 8.87 · 7.72 | 4.5 · 3.0 |
| FloatingNavBar · PillTabs inactive | textSecondary/card | 4.97 | 6.36 | 4.5 |
| PillTabs selected · Chip Default | accentStrong/accentSoft | 6.89 | 7.72 | 4.5 |
| Chip Success / Warning | success/successSoft · warning/warningSoft | 5.15 · 5.66 | 8.51 · 9.18 | 4.5 |
| PrimaryButton | onAccent/accent | 6.28 | 7.38 | 4.5 |
| Sheet "התנתקות" | danger/card | 7.28 | 8.76 | 4.5 |
| Forbidden (not a passing case) | textSecondary/accentSoft | 4.08 fail | 5.53 | — |

The test also asserts, at the same floors, an opaque Hero message (`onAccent/accent`, 4.5), EmptyState description (`textSecondary/background`, 4.5), and the AccountSheet settings icon (`accent/card`, 3.0). Spec §8.2 does not list a ratio for that icon pair.

## Home screenshots

Goldens, verified on this machine (x86_64, OpenJDK 21.0.12.1). Fixed clock 2026-10-27 21:14, sample name "דנה".

- `app/src/test/snapshots/designsystem/HomeContentScreenshotTest.homeEmpty_light_font10.png`
- `app/src/test/snapshots/designsystem/HomeContentScreenshotTest.homeEmpty_light_font13.png`
- `app/src/test/snapshots/designsystem/HomeContentScreenshotTest.homeEmpty_dark_font10.png`
- `app/src/test/snapshots/designsystem/HomeContentScreenshotTest.homeEmpty_dark_font13.png`
- `app/src/test/snapshots/designsystem/HomeContentScreenshotTest.homeDreams4_light_font10.png`
- `app/src/test/snapshots/designsystem/HomeContentScreenshotTest.homeDreams4_light_font13.png`
- `app/src/test/snapshots/designsystem/HomeContentScreenshotTest.homeDreams4_dark_font10.png`
- `app/src/test/snapshots/designsystem/HomeContentScreenshotTest.homeDreams4_dark_font13.png`
- `app/src/test/snapshots/designsystem/HomeContentScreenshotTest.homeDreams128_light_font13.png`
- `app/src/test/snapshots/designsystem/HomeContentScreenshotTest.homeDreams128_dark_font13.png`

## Decisions

- Half-width cards use MetricValueStyle.Medium (AppType.title.tabular(), 20sp) per spec §3; no numberCompact added. Proven by screenshot + lineCount=1 assertion at font 1.3, dreams=128. (Claude may overrule.)
- 2 PrimaryButton spike goldens re-recorded because AppShapes.control changed 12dp → pill (spec §1); deterministic, no threshold.
- Commit534f2feReviewFixesTest: "capture FAB present" assertion inverted to "FAB absent" (spec §5 hides it) — reviewer judged stricter.
- Settings: last-backup row now hidden when no real backup timestamp (old "טרם בוצע גיבוי" text dropped).

## Noticed, not changed

- lintDebug: 122 NewApi (pre-existing java.time call sites: BOI, Hebcal, mappers, fitness, tasks, SettingsDialog:143/151) + 8 StringFormatMatches (legacy calculator/catalog). This run: 130 errors (122 NewApi + 8 StringFormatMatches) and 84 warnings. minSdk 24 without desugaring → possible crash on Android 7.0–7.1. Proposed follow-up: enable coreLibraryDesugaring + desugar_jdk_libs and make lintDebug a CI gate. Needs owner sign-off.
- Legacy SettingsDialog last-backup line uses 12.sp + Color.DarkGray (unreadable in dark mode).
- SettingsDialog.kt ~298 lines — split before next edit. Commit534f2feReviewFixesTest.kt ~312 lines (was ~308) and its test name still says "visible on all tabs".
- SignOutConfirmDialog.kt:25 uses colorScheme.error instead of appColors.danger; ui_str_249 may be unused.
- Tab state lost on sign-in (sign-in replaces AppShell); legacy root routes leave no tab selected; PillTabs uses alpha 0f instead of Color.Transparent.
- Tests follow-ups: HomeChromeTest renders a test shell not real AppShell; ContentPaddingTest reflects on internal setInsets (breaks on BOM upgrade, loudly); FloatingNavBarStructureTest only covers Home selected; RTL not pinned via LocalLayoutDirection in captureTagged; home/shell goldens live under snapshots/designsystem; AccountSheet not screenshotted.
- Goldens recorded on x86_64 OpenJDK 21.0.12.1; if CI (ubuntu-latest) disagrees, investigate, never add a threshold.
- Auto sign-in at launch (MainActivity) relies on signOut clearing credential state — now guarded by AuthManagerSignOutTest.

## Open questions

1. Desugaring vs minSdk 26 for the 122 pre-existing java.time sites (recommend desugaring, separate task).
2. Accept MetricValueStyle.Medium for half-width cards (no numberCompact)?
