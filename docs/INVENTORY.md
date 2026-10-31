# Feature inventory

Facts below are from the tree at `4204866` (`feat: add the Night Blue design system`). Paths are repo-relative. This pass read source and tests. It did not run Gradle, instrumented tests, `firestore.test.js`, or the app, and it did not call Firebase, the Bank of Israel API, or Hebcal. Where a path was not executed, the row says so.

There is no Gemini (or other generative-AI) client in this repository: no dependency, no type, no call site. The AI screen is a local subscription-cost calculator.

## How state was assigned

| State | Meaning used here |
| --- | --- |
| works | Entry point reaches a finished read/write or calculation path, and a unit test exercises that logic (or a pure function it calls). Device, Firestore, and network runs are still unverified. |
| partial | Reachable and partly real, with a named gap (disabled switch, dropped fields, unused parameter, no test of the behavior). |
| stub | The UI is a placeholder: fixed copy, a disabled control, or a toast instead of the feature. |
| dead code | Not on `NavHost` and not called from application code. A unit test may still reference it. |
| unverified | Could not be judged from the files that were read. Not used as a substitute for the four states above. |

`NavHost` routes are in `app/src/main/java/com/menash/myself/MainActivity.kt` (`MyselfNavGraph`). Bottom tabs: `home`, `dreams`, `tasks`, `calculators`, `more`. Signed-out users see `auth/SignInScreen.kt` before the graph. "Calculators only" sets `bypassSignIn` and start destination `calculators` (`MainActivity.kt`).

## Table 1 — screens and features

| Feature | Screens | ViewModel | Repository | Firestore or Room path | Tests | State |
| --- | --- | --- | --- | --- | --- | --- |
| Google sign-in and auto sign-in | `auth/SignInScreen.kt`, reached from `MainActivity.kt` `MyselfRootContent` when `currentUser == null` and bypass is off | none. `auth/AuthManager.kt` calls Credential Manager and `Firebase.auth.signInWithCredential` | none | Firebase Auth only. No user document is written here | none | partial. The call sequence is complete. No unit test. Live sign-in unverified |
| Calculators-only entry | Same sign-in screen, button `skip_to_calculators_button`. Shell still shows all five tabs | `ui/MainViewModel.kt` is created, but `initializeHouseholdForUser` runs only when `currentUser != null` | none for the bypass | none | none | partial. Navigation code is complete and untested. Household and Firestore features have no session in this mode |
| Life home | `ui/home/LifeHomeScreen.kt` route `home`, plus `LifeHomeCards.kt`, `LifeHomeSections.kt`, `HomeHebcalCard.kt` | `feature/home/LifeHomeViewModel.kt` | task, dream, inbox, fitness, household interfaces in `core/data/repository/` | Same paths as those features. Hebcal card reads `HebcalRepository` (DataStore, not Firestore) | `Step6And7ReviewFixesTest` builds `LifeHomeViewModel` and checks default `VisibilityFilter.MINE_AND_BOTH` | partial. Lists, task toggle, and today-fitness save are wired. `OfflineIndicator(isOffline = false)`. Roadmap card is static strings (`LifeHomeSections.kt` `HomeRoadmapCard`). No home-screen test |
| Tasks | `ui/tasks/TasksScreen.kt` route `tasks` (tab). Dialogs in `TaskDialogs.kt`, rows in `TaskCardComponents.kt` | `feature/tasks/TasksViewModel.kt` | `core/data/repository/impl/FirestoreTaskRepository.kt` → `data/firestore/LifeHubRepository.kt` → `TaskFirestoreDataSource.kt` | `households/{householdId}/tasks/{taskId}`. Private: `users/{uid}/private/tasks/items/{id}` (`LifeHubRepository.privateCollection`) | `PrivateItemsRepositoryTest` (private create does not write household; merge hides the partner's private row), `TaskFlowReactivityTest` (observe waits for household id), `Step6And7ReviewFixesTest` (create with no household sets an error), `Commit534f2feReviewFixesTest` (list starts null), `VisibilityFilterTest`, `ModelMappersTest.task_roundTrip`, `LegacyMappingTest`. Rules: `firestore.test.js` | works as a code path: create, toggle, top-3, five-before-11, date, and delete are all implemented in the ViewModel and `FirestoreTaskRepository`. Tests do not call toggle, top-3, date, or delete. Firestore listener unverified. Undo snackbar exists only for toggle-done (`TasksScreen.kt`) |
| Dreams and savings | `ui/dreams/DreamsScreen.kt` route `dreams` (tab). `DreamDialogs.kt`, `DreamCardComponents.kt` | `feature/dreams/DreamsViewModel.kt` | `FirestoreDreamRepository.kt` → `DreamFirestoreDataSource.kt` | `households/{householdId}/dreams/{dreamId}`. Private: `users/{uid}/private/dreams/items/{id}` | `ModelMappersTest.dream_roundTrip`. `DreamSavingsCalculatorTest` covers `feature/dreams/domain/DreamSavingsCalculator.kt`, which no screen calls. VM fakes in `Commit534f2feReviewFixesTest` and `Step6And7ReviewFixesTest` do not assert writes | partial. Update/delete/status/savings calls exist. Create dialog (`DreamDialogs.kt`) omits `status`, so the domain default `"active"` is stored (`core/domain/model/Dream.kt`). The board filters Hebrew `DreamStatus` names (`DreamCardComponents.kt`). `Dream.isCompleted` looks for `"completed"`, not `"בוצע"`. `+₪100` / `+₪500` are fixed buttons. Progress on screen is `Dream.progressPercent` (0–1), not the calculator (0–100) |
| Inbox triage | `ui/inbox/InboxTriageScreen.kt` route `inbox` from More and from the home inbox chip | `feature/inbox/InboxViewModel.kt` | `FirestoreInboxRepository.kt` → `InboxFirestoreDataSource.kt`, plus task and dream repos when triaging | `households/{householdId}/inbox/{itemId}` with `status == "inbox"`. Private: `users/{uid}/private/inbox/items/{id}` | Fake `InboxRepository` only (`Commit534f2feReviewFixesTest`, `Step6And7ReviewFixesTest`) | partial. Triage to task, dream (`status` default `"חלום"`), or note, and delete, are implemented. No test asserts those writes |
| Quick capture | `ui/quickcapture/QuickCaptureDialog.kt`. FAB `persistent_capture_fab` in `MainActivity.kt` `AuthenticatedShell` (all tabs) | `MainViewModel.addInboxItem` | `InboxRepository` | Same inbox paths | `Commit534f2feReviewFixesTest` fix 2 checks the source has one FAB. It does not call `addInboxItem` | partial. Dialog → `forWhom` / `isPrivate` → inbox write is complete in code and untested |
| Fitness log | `ui/fitness/FitnessScreen.kt` route `fitness` from More and home | `feature/fitness/FitnessViewModel.kt`. Home can also call `LifeHomeViewModel.saveTodayFitness` | `FirestoreFitnessRepository.kt` → `FitnessFirestoreDataSource.kt` | `users/{uid}/fitness/{yyyy-MM-dd}` (document id is the day string). Not under `private` | `ModelMappersTest.fitness_roundTrip`. `Step6And7ReviewFixesTest` fake only | partial. Today read/write is implemented. `FitnessRepository.saveTodayFitness` accepts a `date` that `FirestoreFitnessRepository` ignores. No datasource test |
| Device calendar (read-only) | `ui/calendar/CalendarScreen.kt` route `calendar` | none | none. `calendar/CalendarHelper.kt` queries `CalendarContract.Instances` for today | Device calendar provider. Not Firestore or Room | none | partial. Permission request and query are real. Result unverified (no test, not run) |
| File catalog and CSV import | `ui/catalog/FileCatalogScreen.kt` route `catalog`. Mapping UI: `ui/common/CsvMappingDialog.kt` | `feature/importcatalog/FileCatalogViewModel.kt` | On confirm, task or dream repository. Parse: `core/domain/util/CsvParser.kt` | Writes the task or dream paths above. Does not write Room | none for this importer. `ExampleUnitTest.csvHelper_parsesAndExportsCorrectly` covers `data/io/CsvHelper.kt`, which this screen does not call | partial. Dream columns match the ViewModel keys. Task ViewModel reads `"תאריך יעד"` and `"מרחב (שלי/של אשתי/משותף)"` (`FileCatalogViewModel.kt`); the catalog fields are `"תאריך"` and `"מרחב"` (`FileCatalogScreen.kt`), so those columns are dropped. `targetType` `"calculators"` is not `"tasks"`, so those rows go through the dream branch. Bank and credit cards are stubs (below) |
| Bank and credit CSV cards | Same catalog screen, `targetType` `bank_phase2` and `credit_phase2`, `isPhase2 = true` | import function exists but the button never calls it | none | Sample CSV strings only, inside `FileCatalogScreen.kt` | none | stub. Upload is disabled. Tap shows a toast (`ui_str_211`). Template share still sends the sample CSV |
| Household and partner invite | `ui/household/HouseholdScreen.kt` route `household`. Cards in `HouseholdCardComponents.kt` | `feature/household/HouseholdViewModel.kt` | `FirestoreHouseholdRepository.kt` → `data/firestore/HouseholdRepository.kt` | `households/{householdId}` (`memberUids`, `partnerEmail`, …). Invites: `invites/{emailLower}` | `ModelMappersTest.household_roundTrip`. VM tests use a fake. `firestore.test.js` covers rules (member access, join, private denial), not the Android client | partial. `getOrCreateHousehold`, observe, invite, and sign-out (`AuthManager.signOut`) are implemented. No unit test of `HouseholdRepository`. Runtime unverified |
| Legacy calculators (removed 31/10/2026) | none. Hub, car, AI and pension screens were deleted | none | none | Room tables `cars`, `ai_subscriptions`, `pension_funds` are dropped by `MIGRATION_1_2` in `data/AppDatabase.kt`; only `app_settings` is left | `RepositoryWrappersTest` (settings) | replaced by the money pillar: subscriptions, pension snapshot and car screens on Firestore |
| Settings, JSON backup, manual USD | `ui/settings/SettingsDialog.kt`, opened from the calculators hub. Not a `NavHost` route | none. The hub ViewModel saves Room settings; the dialog also calls repositories on `LocalAppContainer` | `RoomSettingsRepository` → `data/io/BackupRestoreManager.kt`. BOI and Hebcal repos for the rate and city blocks | Room `app_settings` id `1` (`usdToIlsRate`, `lastBackupTimestamp`). BOI keys in DataStore `boi_rate_prefs` (`core/external/boi/BoiRateStore.kt`). Hebcal keys in `hebcal_prefs` | No backup test and no dialog test. BOI store tests are in Table 2 | partial. Backup share and JSON restore are implemented with `org.json`. Save writes the Room rate and, if the checkbox is on, `ExchangeRateRepository.setManualRate`. Calculators do not read the BOI value |
| City picker | `ui/settings/CityPickerDialog.kt` from the home Hebcal card. `SettingsDialog.kt` also has a `DropdownMenu` over the same list | none | `HebcalRepositoryImpl.selectCity` | DataStore `hebcal_prefs` (`HebcalStore.kt`) | Hebcal JSON tests in Table 2. No picker test | partial. Search filters 14 fixed cities in `core/external/hebcal/HebcalModels.kt` `IsraeliCity.DEFAULT_CITIES`. Not a live city search |
| Hebcal card on home | `ui/home/HomeHebcalCard.kt` | none | `HebcalRepository` | `hebcal_prefs` plus Hebcal HTTPS (Table 2) | `Step8ExternalDataAndFixesTest.testHebcalSampleJsonParsingAndNoDefaultCity`, `Step8RoundThreeTest.testHebcalCurrentHebrewDateMatching` | partial. Shows "בחר עיר" until a city is stored, then Hebrew date, candles, havdalah, and "נתונים: Hebcal.com". Parameter `onOpenSettings` is unused; the card opens `CityPickerDialog`. Caller still passes `onNavigate("household")` (`LifeHomeScreen.kt`) |
| More menu | `ui/more/MoreScreen.kt` route `more` | none | none | none | none | works as a list of five routes: `inbox`, `fitness`, `calendar`, `catalog`, `household`. `OfflineIndicator(isOffline = false)`. No test |
| Design-system gallery | `core/designsystem/gallery/DesignSystemGallery.kt` | none | none | none | Token tests in Table 2. No gallery test | partial. Not a route. Long-press on the version line in `SettingsDialog.kt` opens it. Tap does nothing. `@Preview` light/dark exist in that file |
| Global error banner | `MainActivity.kt` `GlobalErrorBanner` | `MainViewModel.errorFlow` combines `AppContainer.errorFlow` | `core/data/session/ErrorReporter.kt`, `LifeHubRepository.errorFlow`, `HouseholdRepository.errorFlow`. Text from `core/ui/ErrorMapper.kt` and `data/firestore/FirestoreErrors.kt` | none | `Step8ExternalDataAndFixesTest` (`ErrorMapper`, `safeLaunch`), `Commit534f2feReviewFixesTest` fix 3 | works for mapping exceptions to four Hebrew strings. The banner composable is untested. There is no connectivity listener, so this is not an offline detector |
| Old home alias | `ui/home/HomeScreen.kt` | none. It only calls `CalculatorsHubScreen` | n/a | n/a | none | dead code. No caller. Route `home` is `LifeHomeScreen` |
| Phase 2 energy / DO NOW scheduler | no screen | none | none | none. Types are in-memory (`Phase2TaskItem`) | `domain/TaskSchedulerTest.kt` | dead code in the app. `feature/tasks/domain/phase2/TaskSchedulerPhase2.kt` is referenced only by that test. The file comment says Phase 1 screens do not use it |

`core/domain/model/DateRange.kt` is the same kind of dead code (only `domain/DateRangeTest.kt` references it). It is not a screen.

## Reachability

Checked against `MyselfNavGraph` and the `onNavigate` / `navController.navigate` call sites.

| Route or surface | How a user gets there | Not reachable from |
| --- | --- | --- |
| Sign-in | Cold start with no Firebase user and bypass off | Bottom nav |
| `home` | Default start when signed in. Bottom tab | — |
| `dreams`, `tasks`, `calculators`, `more` | Bottom tabs in `MainActivity.kt` `bottomNavTabs` | — |
| `calculators` as start | Sign-in "calculators only" (`startInCalculatorsOnly`) | — |
| `inbox` | More item; home inbox chip (`LifeHomeScreen.kt`) | Bottom tab |
| `fitness` | More item; home fitness action (`LifeHomeSections.kt` `onOpenFitness`) | Bottom tab |
| `calendar` | More item; home calendar action | Bottom tab |
| `catalog` | More item; home quick-nav (`LifeHomeSections.kt`) | Bottom tab |
| `household` | More item; home household chip | Bottom tab. The Hebcal card does not open it (`onOpenSettings` is unused) |
| `car_calculator`, `ai_calculator`, `pension_calculator` | Cards in `ui/calculators/CalculatorsHubCards.kt` (`route` fields). Hub navigates in `CalculatorsHubScreen.kt` | Bottom tab, More |
| Settings dialog | Button on the calculators hub | `NavHost`. Not linked from More |
| City picker | Tap on `HomeHebcalCard` | The settings dropdown is a second, separate control over the same 14 cities |
| Design gallery | Long-press the version line in `SettingsDialog.kt` | `NavHost`, menus, a normal tap |
| Quick capture | FAB on the authenticated shell, including calculators-only mode | Its own route |
| `HomeScreen` | No call site | Everywhere |
| Phase 2 scheduler, `DateRange` | No call site from `app/src/main` | Everywhere |

`MainActivity.kt` still builds the full bottom bar in calculators-only mode. The other tabs are on the graph. Tapping them does not require a user. Writes that need a household id then fail through `ItemLocation` / `HouseholdSession.requireHouseholdId`.

## What each test file locks

Read from `@Test` names. Not re-run.

| File | Locks |
| --- | --- |
| `app/src/test/java/com/menash/myself/domain/CarCostCalculatorTest.kt`, `CarMathParityTest.kt`, `CalculatorParityTest.kt`, `CharacterizationTests.kt`, `ExampleUnitTest.kt` | Car, AI, and pension formulas, including the locked edge cases in `docs/KNOWN_ISSUES.md`. `CharacterizationTests.step1_5_migrationManager_autoMigrateDisabledByDefault` locks migration off. `ExampleUnitTest` also round-trips `CsvHelper` |
| `app/src/test/java/com/menash/myself/domain/AiSubscriptionCalculatorTest.kt`, `PensionSavingsCalculatorTest.kt` | Same calculators, narrower cases |
| `app/src/test/java/com/menash/myself/domain/MoneyTest.kt` | Agorot convert, half-up, Hebrew format |
| `app/src/test/java/com/menash/myself/domain/DreamSavingsCalculatorTest.kt` | Progress 40 and 100 for the unused calculator |
| `app/src/test/java/com/menash/myself/domain/ForWhomTest.kt`, `VisibilityFilterTest.kt` | Viewer labels when a name is passed, and mine+both vs all |
| `app/src/test/java/com/menash/myself/domain/TaskSchedulerTest.kt`, `DateRangeTest.kt` | Phase 2 filter and `DateRange` only |
| `app/src/test/java/com/menash/myself/domain/` is not a screen test | No Compose test anywhere under `app/src/test` |
| `app/src/test/java/com/menash/myself/data/repository/RepositoryWrappersTest.kt` | Room repos map domain ↔ fake `AppDao` for car, AI, pension |
| `app/src/test/java/com/menash/myself/data/repository/PrivateItemsRepositoryTest.kt`, `TaskFlowReactivityTest.kt` | Task location and household-id subscription |
| `app/src/test/java/com/menash/myself/core/data/mapper/ModelMappersTest.kt`, `LegacyMappingTest.kt` | Round-trip for car, AI, pension, task, dream, fitness, household; legacy space strings; date parse |
| `app/src/test/java/com/menash/myself/BoiExchangeRateRepositoryTest.kt`, `Step8ExternalDataAndFixesTest.kt`, `Step8RoundThreeTest.kt` | BOI refresh policy, CSV parse, staleness, historical window, Hebcal sample JSON, worker delay |
| `app/src/test/java/com/menash/myself/Commit534f2feReviewFixesTest.kt`, `Step6And7ReviewFixesTest.kt` | ViewModel initial null, one FAB (source scan), `safeLaunch`, file-length and import boundaries, `ItemLocation`, default visibility filter |
| `app/src/test/java/com/menash/myself/core/designsystem/DesignSystemTokensTest.kt`, `NightBlueGuardTest.kt` | Badge contrast, bidi money format, Night Blue hex, allow-lists (16 files with `Color(0x`, 32 files with raw `.sp`) |
| `app/src/test/java/com/menash/myself/ExampleRobolectricTest.kt` | Reads one string resource |
| `app/src/androidTest/java/com/menash/myself/ExampleInstrumentedTest.kt` | `packageName` equals `BuildConfig.APPLICATION_ID` |
| `firestore.test.js` | Rules only, ten tests (member, non-member, invite join, private denial). Not run here |

No test file names a screen composable (`LifeHomeScreen`, `TasksScreen`, `CarComparisonScreen`, and the rest do not appear as test subjects). `Commit534f2feReviewFixesTest` fix 2 reads `MainActivity.kt` as text to assert a single FAB.

## Money types at the boundary

`Money` (`core/domain/model/Money.kt`) stores `agorot: Long`. `fromShekels(Double)` is the boundary. `CalculatorMappers.kt` converts Room `Double` shekels to `Money` and back. The screens' displayed totals for cars use `Car.costResult`, which calls `CarCostCalculator.calculate5YearCost(car: Car): CarCostResultMoney` (`core/domain/model/Car.kt`). `CarEntity` still computes a parallel `Double` result for its own getters. AI domain methods (`AiSubscription.monthlyCostIls`) use `Money`; `AiSubscriptionEntity.priceInIls` and `AiSubscriptionCalculator` use `Double` and the Room rate. Pension domain `PensionFund` uses `Money` for balances and `Double` for fee percents (`CalculatorMappers.kt`).

## What `AppContainer` constructs

`core/di/AppContainer.kt` is the only composition root. `MyselfApp` holds one instance. Screens read it through `LocalAppContainer`.

| Field | Concrete type | Backing store |
| --- | --- | --- |
| `householdSession` | `DefaultHouseholdSession` | memory |
| `errorReporter` | `DefaultErrorReporter` | memory |
| `boiRateStore` / `exchangeRateRepository` | `BoiRateStore`, `BoiExchangeRateRepository` with `BoiExchangeRateClient` | DataStore `boi_rate_prefs` and HTTPS |
| `hebcalStore` / `hebcalRepository` | `HebcalStore`, `HebcalRepositoryImpl` with `HebcalClient` | DataStore `hebcal_prefs` and HTTPS |
| `carRepository` | `RoomCarRepository` | `cars` |
| `aiSubscriptionRepository` | `RoomAiSubscriptionRepository` | `ai_subscriptions` |
| `pensionFundRepository` | `RoomPensionFundRepository` | `pension_funds` |
| `settingsRepository` | `RoomSettingsRepository` | `app_settings` |
| `taskRepository` | `FirestoreTaskRepository` | `LifeHubRepository` |
| `dreamRepository` | `FirestoreDreamRepository` | `LifeHubRepository` |
| `inboxRepository` | `FirestoreInboxRepository` | `LifeHubRepository` |
| `fitnessRepository` | `FirestoreFitnessRepository` | `LifeHubRepository` |
| `householdRepository` | `FirestoreHouseholdRepository` | `HouseholdRepository` (the Firestore class, not the interface) |
| `errorFlow` | combine of reporter, life hub, household | memory |

`BoiSyncWorker.enqueue` is called from `MyselfApp.onCreate`, not from `AppContainer`. It builds its own `BoiRateStore(applicationContext.boiDataStore)` (`BoiSyncWorker.kt`), a second store object on the same DataStore file.

Startup refresh of BOI and Hebcal is inside `MainViewModel.initializeHouseholdForUser`, which runs only when `currentUser != null` (`MainActivity.kt`). The worker still runs with no user. Calculators-only mode therefore does not call `refreshRateIfNeeded` from the ViewModel.

## Room seed on first create

`AppDatabase.getDatabase` `onCreate` (`data/AppDatabase.kt`) inserts, only when the file is created:

| Row | Values written |
| --- | --- |
| `app_settings` id 1 | `usdToIlsRate = 3.11`, `lastBackupTimestamp = 0` |
| one `cars` row | name `טויוטה קורולה היברידית`, purchase `148000.0`, resale `88000.0`, energy `7200.0`, insurance `4800.0`, tracker `650.0`, maintenance `1800.0`, test `1950.0`, tires `2400.0`, `isBaseline = true` |
| one `ai_subscriptions` row | name `ChatGPT Plus`, category `מודלי שפה (LLM)`, monthly, USD, price `20.0` |
| two `pension_funds` rows | study fund אלטשולר שחם current `145000.0` deposits `110000.0` balance fee `0.28`; pension מנורה מבטחים current `320000.0` deposits `240000.0` balance fee `0.19` deposit fee `1.49` |

There is no later migration that removes these rows. Room version is 1.

## Catalog headers vs importer keys

`FileCatalogViewModel.importCsvRows` looks up Hebrew map keys. `FileCatalogScreen.getCatalogItems` is what the mapping dialog offers.

| Catalog `targetType` | Dialog fields | ViewModel actually reads | Result |
| --- | --- | --- | --- |
| `dreams` | כותרת חלום, רשימה, עלות משוערת, סכום שנחסך, צעד הבא, מרחב | the same keys, plus `SpaceSelection.parseFromCsv` on מרחב | titles import; missing cost becomes `0.0` via `toDoubleOrNull` |
| `tasks` | כותרת משימה, צעד הבא, תאריך, מרחב | כותרת משימה, צעד הבא, `תאריך יעד`, `מרחב (שלי/של אשתי/משותף)` | title and next action can import; date and space keys do not match, so due date stays null and space falls through to the default `"משותף"` in the ViewModel |
| `calculators` | דגם רכב, מחיר קנייה, מכירה בעוד 5 שנים | not read. The `else` branch is the dream importer | a row imports only if a dream-title column was mapped; car Room tables are not touched |
| `bank_phase2`, `credit_phase2` | sample fields in the template string | not called | upload button `enabled = !item.isPhase2` |

Calculator screens do their own CSV mapping (`CarComparisonScreen.kt`, `AiSubscriptionsScreen.kt`, `PensionSavingsScreen.kt`) through `CsvMappingDialog` and write Room. Those paths are separate from the catalog.

## Design-system call sites

Searched for `Name(` under `app/src/main`.

| Composable | Called from |
| --- | --- |
| `EmptyState`, `ErrorState`, `LoadingState` | home, tasks, dreams, inbox, fitness, household, calendar (empty only), catalog (error only), the three calculators and the hub |
| `OfflineIndicator` | every screen above, always `false` or the default `false` |
| `SpaceBadge` | `TaskCardComponents.kt`, `DreamCardComponents.kt`, `LifeHomeCards.kt`, `InboxTriageScreen.kt`. No call passes `partnerName` |
| `PrimaryButton` | `EmptyState.kt` only |
| `ScreenHeader`, `MetricCard`, `ListRow`, `SharedBadge`, `QuickAddButton`, `TextInput`, `ProgressBar` | `DesignSystemGallery.kt` (`MetricCard` also calls `ProgressBar`) |
| `AppCard`, `CaptureSheet`, `StatusChip`, `MoneyText` | declaration file only |

`NightBlueGuardTest.colorAllowList` has 16 files. `fontSizeAllowList` has 32 files. `assertAllowList` skips paths under `java/com/menash/myself/core/designsystem/theme/` and fails if an allow-list entry no longer contains the literal. The test does not prove a screen uses `AppType`.

`MyApplicationTheme` is applied once in `MainActivity.onCreate`. `ActivePalette` is `NightBlue` (`Palette.kt`). `MyApplicationTheme` picks `ActivePalette.dark` or `ActivePalette.light` from `isSystemInDarkTheme()` (`core/designsystem/theme/Theme.kt`).

## Table 2 — shared infrastructure

| Component | Files | Used by | Storage or external API | Tests | State |
| --- | --- | --- | --- | --- | --- |
| Auth | `auth/AuthManager.kt`, `auth/SignInScreen.kt`, auth listener in `MainActivity.kt` | Sign-in, household sign-out, `currentUid` inside `LifeHubRepository` and `HouseholdRepository` | Firebase Auth. Web client id is `R.string.default_web_client_id` (from `google-services.json`, not in git; `docs/RULES.md` section 7) | none | partial. Sign-in, auto sign-in, and sign-out are implemented. Missing client id is handled with a Hebrew error. Live auth unverified |
| Household session | `core/data/session/HouseholdSession.kt` (`DefaultHouseholdSession`), created in `core/di/AppContainer.kt`. Cleared on sign-out in `MainActivity.kt` | Task, dream, and inbox repositories call `ItemLocation.resolve` (`core/data/model/ItemLocation.kt`). `FirestoreHouseholdRepository.observeHousehold` writes the id back | In-memory `StateFlow` only. The id itself comes from Firestore `households` | `Step6And7ReviewFixesTest` fix 2 (session survives the `AppContainer` holder) and fix 5 (`ItemLocation`). `TaskFlowReactivityTest` | works for "no household id → empty task flow / throw on write". Process death is unverified. Bypass-calculators mode never sets an id |
| Error handling | `core/ui/BaseViewModel.kt`, `core/ui/ErrorMapper.kt`, `core/data/session/ErrorReporter.kt`, `data/firestore/FirestoreErrors.kt`, banner in `MainActivity.kt` | Every feature ViewModel extends `BaseViewModel` | none | `Step8ExternalDataAndFixesTest`, `Commit534f2feReviewFixesTest` fix 3 | works for `CancellationException` rethrow, JVM `Error` not swallowed, and Hebrew text for permission denied, unavailable, network, and missing household. `ErrorMapper` still also matches message substrings (`"network"`, `"household"`). Banner UI unverified |
| BOI (Bank of Israel) | `core/external/boi/BoiExchangeRateClient.kt`, `BoiModels.kt`, `BoiRateSource.kt`, `BoiRateStore.kt`, `BoiStalenessPolicy.kt`, `BoiSyncWorker.kt`, `core/data/repository/impl/BoiExchangeRateRepository.kt` | `MyselfApp.onCreate` enqueues the worker. `MainViewModel.initializeHouseholdForUser` calls `refreshRateIfNeeded` inside `runCatching` (result ignored). `SettingsDialog` displays and can override the rate | HTTPS SDMX CSV. Code constant is `https://edge.boi.gov.il/FusionEdgeServer/sdmx/v2/data/dataflow/BOI.STATISTICS/EXR/1.0/RER_USD_ILS` with `DATA_TYPE=OF00`. Cache: DataStore `boi_rate_prefs`. Not Firestore | `BoiExchangeRateRepositoryTest` (override vs fetch, historical Saturday, empty window). `Step8ExternalDataAndFixesTest` (CSV parse, staleness). `Step8RoundThreeTest` (override precedence, worker delay) | partial. Latest rate, staleness, manual override, and `fetchHistoricalRate` are implemented and unit-tested with fakes. No screen calls `fetchHistoricalRate`. Calculators do not read this store. Worker schedule skips Saturday and Sunday (`BoiSyncWorker.syncTime`); holidays are not in the policy (`docs/BACKLOG_STEP8.md` matches the code). Live HTTP unverified |
| Hebcal | `core/external/hebcal/HebcalClient.kt`, `HebcalModels.kt`, `HebcalStore.kt`, `core/data/repository/impl/HebcalRepositoryImpl.kt` | Home card, settings dropdown, city picker. Startup refresh in `MainViewModel` (`runCatching`, result ignored) | `https://www.hebcal.com/hebcal` with `cfg=json`, holiday flags, `ss=on`, `mf=on`, `c=on`, `d=on`, `geo=geoname`. Cache: DataStore `hebcal_prefs` | `Step8ExternalDataAndFixesTest.testHebcalSampleJsonParsingAndNoDefaultCity`, `Step8RoundThreeTest.testHebcalCurrentHebrewDateMatching` (sample JSON, not the network) | partial. No default city. Weekly cache and city selection are implemented. City list is the 14 hardcoded cities, not the Hebcal geoname search. Live HTTP unverified |
| DataStore | `BoiRateStore.kt` (`Context.boiDataStore`, file name `boi_rate_prefs`). `HebcalStore.kt` (`hebcalDataStore`, file name `hebcal_prefs`). Wired in `AppContainer.kt` | BOI repository and Hebcal repository only | Preferences DataStore. Separate from Room and from Firestore | `BoiExchangeRateRepositoryTest` uses an in-memory `DataStore`. `Step8RoundThreeTest.testOverridePrecedence` uses Robolectric `context.boiDataStore` | works for those two caches. No other feature uses DataStore. Migration uses `SharedPreferences` name `life_hub_migration` (`MigrationManager.kt`), not DataStore |
| Design-system tokens | `core/designsystem/theme/Palette.kt` (`NightBlue`, `ActivePalette`), `Theme.kt` (`MyApplicationTheme`, `MaterialTheme.appColors`), `Type.kt` (Rubik, `AppType`, `TypographyTokens`), `Spacing.kt` (4/8/16/24/32 dp), `Shape.kt` (`AppShapes` 12 dp), `ColorTokens.kt` (ForWhom badge colours), `LegacyColors.kt` | `MainActivity` wraps the tree in `MyApplicationTheme`. `SettingsDialog` version line uses `AppType.caption`. `SpaceBadge` uses `ColorTokens` and `TypographyTokens`. Newer components use `AppType` | none | `core/designsystem/DesignSystemTokensTest.kt` (money bidi isolation, badge contrast). `core/designsystem/NightBlueGuardTest.kt` (one palette line, WCAG on card, no new `Color(0x` or raw `.sp` outside an allow-list) | works for the token files and the guard tests. Most screens are still on the colour/`.sp` allow-list (`NightBlueGuardTest`, also listed in `docs/handoff/REPORT.md`). `MaterialTheme.shapes` is not replaced by `AppShapes` |
| Design-system components | `core/designsystem/component/` | Screens call `EmptyState`, `ErrorState`, `LoadingState`, `OfflineIndicator`, `SpaceBadge`. `EmptyState` calls `PrimaryButton`. Gallery calls `ScreenHeader`, `MetricCard`, `ListRow`, `SharedBadge`, `PrimaryButton`, `QuickAddButton`, `TextInput`, `ProgressBar`, `EmptyState` | none | No component test. Token tests only | partial. `AppCard`, `CaptureSheet`, `StatusChip`, and `MoneyText` have no call site outside their own files (dead code). `OfflineIndicator` is passed `false` from every screen; calculator screens take `isOffline` but every caller uses the default `false`. No `ConnectivityManager` |
| Room | `data/AppDatabase.kt`, `data/dao/AppDao.kt`, entities under `data/entities/`, repositories `Room*Repository.kt`, mappers `core/data/mapper/CalculatorMappers.kt` | Three calculators, settings, backup, disabled migration | File `my_calculators.db`, version 1, `exportSchema = false`. Money columns are `Double` shekels | `RepositoryWrappersTest`, `ModelMappersTest` | works for calculator CRUD through fakes of `AppDao`. No in-memory Room test. Seed rows on `onCreate` are example data, not empty state |
| Firestore access | `data/firestore/FirestoreManager.kt`, `LifeHubRepository.kt`, `HouseholdRepository.kt`, `*FirestoreDataSource.kt`, rules in `firestore.rules` | Tasks, dreams, inbox, fitness, household, disabled migration | Paths in Table 1. `schemaVersion` does not appear in any `.kt` file | Android: private-merge and task-flow tests with subclasses, not the SDK. `firestore.test.js` (node:test, emulator): membership, join, field restrictions, private denial | partial. Client paths match the rules collections (`tasks`, `dreams`, `inbox`, `users/{uid}/fitness`, `users/{uid}/private`, `invites`, `calculator_sync`). Rules file was not executed here |
| Manual DI | `core/di/AppContainer.kt`, `MyselfApp.kt`, `LocalAppContainer` | Every screen that takes a ViewModel | Constructs Room, Firestore, DataStore, BOI, Hebcal | `Step6And7ReviewFixesTest` fix 2 (Robolectric: same `AppContainer` after a second `getApplicationContext`, household id still set) | works as the composition root. Not a DI framework |
| CSV parse | `core/domain/util/CsvParser.kt` (screens). `data/io/CsvHelper.kt` (older helper) | Catalog and the three calculator screens use `CsvParser`. `ExampleUnitTest` uses `CsvHelper` | none | `ExampleUnitTest` covers `CsvHelper` only | partial. Two parsers. Screen behavior of `CsvParser` is untested |
| Phase 2 scheduler and `DateRange` | See Table 1 | tests only | none | their own tests | dead code relative to the app |

## Discrepancies

Docs or comments disagree with the code. Nothing here was edited except this file.

- `docs/RULES.md` section 2 lists `feature/capture`, `feature/calendar`, and `feature/settings`. Those packages are not in the tree. Quick capture is `ui/quickcapture/`. Calendar is `ui/calendar/` plus `calendar/CalendarHelper.kt`. Settings is `ui/settings/`. `feature/importcatalog/` contains only the ViewModel.
- `docs/RULES.md` section 3 says money is never `Double`. Domain `Money` is agorot `Long` (`core/domain/model/Money.kt`). Room entities, `CarCostInput`, `AiSubscriptionInput`, and `PensionFundInput` still use `Double`. `CarCostCalculator` has both a `Double` result and a `Money` result.
- `docs/RULES.md` section 3 says backup and import use Moshi only. `BackupRestoreManager.kt`, `MigrationManager.kt`, `HebcalClient.kt`, and `HebcalStore.kt` use `org.json`. There is no Moshi import in `*.kt`.
- `docs/RULES.md` section 3 says every Firestore document has `schemaVersion`. No Kotlin file contains `schemaVersion`.
- `docs/RULES.md` section 3 says a bad import row is rejected and no default is invented for a missing field. `FileCatalogViewModel.kt` skips a blank title and uses `toDoubleOrNull() ?: 0.0` for dream cost and saved amount. `DreamDialogs.kt` uses the same `?: 0.0` for estimated cost.
- `docs/RULES.md` section 4 says the partner badge uses the partner's first name. `SpaceBadge` can take `partnerName`, but no call site passes it, so the label is string `for_whom_partner_default` (`בן/בת הזוג`) in `res/values/strings.xml`. `ForWhom.labelForViewer` is tested with explicit names in `domain/ForWhomTest.kt`. The home subtitle uses `ui_str_386` / `ui_str_387`, not a profile field (`LifeHomeScreen.kt`).
- `docs/RULES.md` section 5 says every reversible action has an undo snackbar. The only undo snackbar found is task toggle-done in `TasksScreen.kt`.
- `docs/RULES.md` section 5 says every feature has an offline state. Every `OfflineIndicator` is passed a literal or default `false`.
- `docs/RULES.md` section 6 prints the Hebcal URL without `d=on`. `HebcalClient.kt` sends `d=on`. `docs/BACKLOG_STEP8.md` already says to document `d=on`.
- `docs/RULES.md` section 6 leads with host `edge.boi.org.il` and says `edge.boi.gov.il` returned the same row. The client constant is `edge.boi.gov.il` only (`BoiExchangeRateClient.kt`).
- `docs/RULES.md` section 6 says a historical rate can be fetched for a requested date. `BoiExchangeRateRepository.fetchHistoricalRate` exists and is unit-tested. No production caller uses it.
- `core/domain/model/Dream.kt` comment says status is `active`, `completed`, `archived`. The board uses `DreamStatus` Hebrew labels (`חלום`, `בבדיקה`, `חוסכים/מתכננים`, `בוצע`, `נגנז`). `isCompleted` still checks `"completed"`.
- `HomeHebcalCard`'s `onOpenSettings` is unused. `LifeHomeScreen` passes a navigation lambda to `household` that never runs.
- `docs/handoff/REPORT.md` says `AppCard` and `StatusChip` are unused by screens, and that the gallery is not a navigation destination. That matches this tree. The report's Gradle counts were not re-run here.

`docs/ROADMAP.md` Phase 2 (energy, anchors, subscription radar, decision matrix, Friday checklist, trading guardrail) is not implemented as screens. The scheduler file matches the "locked, not started" note. `docs/KNOWN_ISSUES.md` car and pension locks match the characterization test names. `docs/BACKLOG_STEP8.md` holiday handling matches `BoiStalenessPolicy` / `BoiSyncWorker` (weekdays only).

## Gaps found

Dead code: `ui/home/HomeScreen.kt`. `TaskSchedulerPhase2` and `DateRange` (tests only). Composables with no caller: `AppCard`, `CaptureSheet`, `StatusChip`, `MoneyText`. `DreamSavingsCalculator` is tested and unused by the dream screen. `CsvHelper` is tested and unused by screens. `FitnessRepository.saveTodayFitness`'s `date` argument is ignored.

Stubs: catalog cards `bank_phase2` and `credit_phase2` (disabled upload, sample CSV). Home roadmap card (three string resources, no data). `MigrationManager` upload is real code behind `AUTO_MIGRATE_ENABLED = false`.

Untested or only fake-tested: sign-in, navigation, quick capture, inbox triage writes, fitness datasource, device calendar, file-catalog import, household repository, backup/restore, settings dialog, every composable, and the Firestore Android SDK. `firestore.test.js` and `app/src/androidTest/.../ExampleInstrumentedTest.kt` (package-name assert only) were not run. `ExampleUnitTest` and `ExampleRobolectricTest` are sample-style tests, not screen tests.

Money pillar, highest risk first:

1. Two USD rates. Calculators and hub headlines read Room `app_settings.usdToIlsRate` (default and seed `3.11` in `AppSettings.kt` and `AppDatabase.kt`). BOI lives in DataStore and is shown in settings. Saving settings writes both, and nothing copies the official BOI rate into Room when the manual checkbox is off. A stale `3.11` still prices every USD subscription.
2. Historical BOI lookup is not connected to pension deposits or any other screen.
3. Room and the `*Input` calculators use `Double`. Domain `Money` rounds half-up to agorot. `docs/KNOWN_ISSUES.md` section 1.5 documents a sub-agora gap and locks it. Entity getters (`CarEntity.costResult`, `AiSubscriptionEntity.priceInIls`) still call the `Double` calculators.
4. New dreams are stored as status `"active"`, which matches no board column. Savings buttons add a fixed 100 or 500 shekels. The tested savings calculator (remaining, months) is not on the screen. `isCompleted` does not recognize `"בוצע"`.
5. Catalog cannot import cars or pension rows into Room. The calculators target is parsed as dreams. Task due-date and space headers do not match the ViewModel. Bank and credit import, the obvious money inbox, is a stub.
6. Calculator rows are device-local. The Firestore copy (`calculator_sync`) does not run. There is no household id on `cars`, `ai_subscriptions`, or `pension_funds`.
7. First launch seeds a car, a ChatGPT Plus row, and two pension funds (`AppDatabase.kt`). They look like user data.
8. Known locked calculator bugs (net cost clamped at 0, `toInt()` truncation, empty km/liter treated as 1.0, zero-deposit profit shown as 0) are still the behavior. See `docs/KNOWN_ISSUES.md`.
9. No Gemini API exists to keep, rebuild, or drop. AI subscriptions are manual rows.

Unit tests were not executed in this pass.
