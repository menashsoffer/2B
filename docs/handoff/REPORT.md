# Task R1 — One design system, Night Blue

commit: see git log

No test was deleted, `@Ignore`d, or weakened. No secret value is in this report. `firestore.rules`, `firestore.indexes.json`, `firestore.test.js`, `firebase.json`, and `.github/workflows/*` were not changed.

## Raw Gradle lines

`./gradlew testDebugUnitTest --no-daemon` exit 0:

```
BUILD SUCCESSFUL in 25s
```

`app/build/test-results/testDebugUnitTest/TEST-*.xml` (26 files): tests=116, failures=0, errors=0, skipped=0.

`./gradlew assembleRelease --no-daemon` exit 0, with a throwaway PKCS12 keystore whose key password equals its store password (not the release key):

```
BUILD SUCCESSFUL in 57s
```

The distribute run URL is not in this file. It starts when this commit is pushed, and it is in the reply that follows the push.

## What moved where

`ui/theme` and `core/designsystem/tokens` are gone. The source of truth is `core/designsystem/theme`.

- `Palette.kt` holds `Palette`, `PaletteFamily`, `NightBlue`, and the one selection line.
- `Theme.kt` maps that palette into the Material 3 `ColorScheme`, exposes `MaterialTheme.appColors`, follows the system light/dark setting, and still forces RTL.
- `Type.kt` holds Rubik, the four styles (`AppType.number`, `title`, `body`, `caption`), and the previous `TypographyTokens` sizes.
- `Spacing.kt` and `Shape.kt` hold the spacing steps and the 12dp card and control radii.
- `LegacyColors.kt` keeps the old Navy / Emerald / Amber names.
- `ColorTokens.kt` keeps the three-colour ForWhom badge colours. `SpaceBadge` still uses them. New screens use `SharedBadge`.
- `MyApplicationTheme` is the same function. `MainActivity` only changed its import.

The line that selects the palette, in `Palette.kt`:

```
val ActivePalette: PaletteFamily = NightBlue
```

`MyApplicationTheme` reads `ActivePalette` and then picks `light` or `dark` from the system setting. A future palette is a new `PaletteFamily` plus a change to that one line.

Rubik is the variable font `Rubik[wght].ttf` from https://github.com/google/fonts/tree/main/ofl/rubik, stored unmodified as `app/src/main/res/font/rubik.ttf`. The OFL text is `docs/licenses/rubik/OFL.txt`. The source URL is also in `docs/licenses/rubik/SOURCE.txt`. `AppType.number` and money text set tabular figures (`tnum`). On API 26 and newer the weight axis is applied. On API 24 and 25 the font's default instance is used.

## Contrast

Status colours against the card colour. WCAG AA for normal text is 4.5:1. Ratios use the same relative-luminance formula as `DesignSystemTokensTest`.

| role | theme | colour | card | ratio |
| --- | --- | --- | --- | --- |
| success | light | #3E6B4F | #FFFFFF | 6.14:1 |
| warning | light | #7A5414 | #FFFFFF | 6.76:1 |
| danger | light | #8E3D3D | #FFFFFF | 7.28:1 |
| success | dark | #A9CDB6 | #1A1F29 | 9.51:1 |
| warning | dark | #E4C98A | #1A1F29 | 10.24:1 |
| danger | dark | #E4B0B0 | #1A1F29 | 8.76:1 |

One colour cannot reach 4.5:1 on both `#FFFFFF` and `#1A1F29`, so success, warning, and danger differ by theme. `statusColours_meetWcagAaOnCard` checks the floor.

## Allow-list

`noNewHardCodedColoursOutsideTheme` scans `app/src/main` for `Color(0x` outside `core/designsystem/theme`. The colour allow-list has **16** files:

1. `ui/calculators/CalculatorsHubCards.kt`
2. `ui/calculators/ai/AiSubscriptionCardComponents.kt`
3. `ui/calculators/car/CarCardComponents.kt`
4. `ui/calculators/pension/PensionCardComponents.kt`
5. `ui/calculators/pension/PensionSummaryComponents.kt`
6. `ui/calendar/CalendarScreen.kt`
7. `ui/catalog/FileCatalogScreen.kt`
8. `ui/dreams/DreamCardComponents.kt`
9. `ui/fitness/FitnessScreen.kt`
10. `ui/home/LifeHomeCards.kt`
11. `ui/home/LifeHomeScreen.kt`
12. `ui/home/LifeHomeSections.kt`
13. `ui/household/HouseholdCardComponents.kt`
14. `ui/inbox/InboxTriageScreen.kt`
15. `ui/tasks/TaskCardComponents.kt`
16. `ui/tasks/TaskDialogs.kt`

A file on the list that no longer contains `Color(0x` fails the test, so the list has to shrink when a screen is cleaned. `noNewRawSpOutsideTheme` does the same for raw `.sp` sizes. That allow-list has 32 files (the 16 above, plus the other screens and dialogs that already set `.sp`, plus `MainActivity.kt` and `auth/SignInScreen.kt`). The theme package is exempt because that is where `AppType` is defined.

## Gallery

The gallery is `DesignSystemGallery`, in the release source, with Hebrew sample text and `@Preview` for light and dark. It is not a navigation destination and not a visible menu item. The only settings surface is `SettingsDialog`. At the bottom, the version line (`גרסה` plus `versionName` and `versionCode`) opens the gallery on a long-press. A tap does nothing.

Screenshots were not produced. Roborazzi or Paparazzi was not added. Publishing images from GitHub Actions would mean editing a workflow, and workflows were left untouched so the rules deploy does not run.

## Screen files

No existing screen file needed a mechanical import change. None of them imported `ui.theme` or `core.designsystem.tokens`.

| file | why it was edited |
| --- | --- |
| `MainActivity.kt` | import of `MyApplicationTheme` moved to `core.designsystem.theme` |
| `ui/settings/SettingsDialog.kt` | version line and the long-press gallery only |

`ListRow` and `EmptyState` are existing design-system components, not screens. They now use `AppType` and `MaterialTheme.appColors`. `EmptyState`'s action is `PrimaryButton`. Call sites were not edited. `MoneyText` keeps its size and adds tabular figures. These component files only changed their token import: `AppCard`, `CaptureSheet`, `ErrorState`, `LoadingState`, `OfflineIndicator`, `SpaceBadge`, `StatusChip`.

## Task table

| item | status | files changed | test that covers it |
| --- | --- | --- | --- |
| 1. One Night Blue palette | done | `core/designsystem/theme/Palette.kt`, `Theme.kt`, `LegacyColors.kt`, `ColorTokens.kt`; deleted `ui/theme`, `core/designsystem/tokens` | `activePalette_isChosenOnOneLine`, `statusColours_meetWcagAaOnCard`, `colorTokens_badges_haveSufficientContrastInBothThemes` |
| 2. Rubik, four styles, spacing, shape | done | `Type.kt`, `Spacing.kt`, `Shape.kt`, `res/font/rubik.ttf`, `docs/licenses/rubik/` | `noNewRawSpOutsideTheme` |
| 3. Shared components and gallery | done | `MetricCard.kt`, `ListRow.kt`, `ScreenHeader.kt`, `SharedBadge.kt`, `PrimaryButton.kt`, `QuickAddButton.kt`, `TextInput.kt`, `EmptyState.kt`, `ProgressBar.kt`, `DesignSystemGallery.kt`, `SettingsDialog.kt` | no separate component test; the gallery is reachable in the release build |
| 4. Hard-coded colour and `.sp` guards | done | `NightBlueGuardTest.kt` | `noNewHardCodedColoursOutsideTheme`, `noNewRawSpOutsideTheme` |

## Who did the work

chief-architect scoped and dispatched; executed by a Cursor cloud coding agent.

## Noticed, not changed

- Existing screens were not migrated. Their `Color(0x` values stay, which is why the allow-list is 16.
- `MaterialTheme.shapes` was not replaced, so existing buttons and fields keep their current corners. The new components use 12dp.
- `TypographyTokens` still has the previous sizes, in Rubik, so `SpaceBadge` and the other older components do not jump in size. New components use the four `AppType` styles.
- Material typography keeps the default sizes and switches the family to Rubik. Text that inherits the theme font changes typeface and not size.
- Mapping the palette into `ColorScheme` does change screens that read `colorScheme`. `secondary` is `accentStrong`. `tertiary` is `warning`. `error` is `danger`.
- `Spacing.sm` was 12dp and is now 8dp, on the five-step scale. `ListRow` and `StatusChip` used it. `StatusChip` is not used by a screen.
- `AppCard` still defaults to a 16dp corner. No screen calls it.
- The task names eight components and then lists nine. All nine were built: MetricCard, ListRow, ScreenHeader, SharedBadge, PrimaryButton, QuickAddButton, TextInput, EmptyState, ProgressBar.
- `SpaceBadge` is still the three-colour badge on current screens.
- The KSP `ApplicationManager.getApplication()` annotation is still the non-fatal one from https://github.com/google/ksp/issues/2763.
- Rules deploy is still blocked on the `serviceusage` permission from the previous run. This commit does not touch the rules workflow paths.

## Open questions

1. Should the screen migration also apply the 12dp shape to `MaterialTheme.shapes`, or only to new components?
2. When a screen leaves the colour allow-list, should its `.sp` sizes move to `AppType` in the same change?
