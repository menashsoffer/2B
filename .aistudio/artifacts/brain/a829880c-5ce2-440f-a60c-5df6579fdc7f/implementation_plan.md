# Implementation Plan - Review Fixes & Step 8 Completion

1. **Manual Override & SettingsDialog**:
   - Add explicit "Use manual rate" toggle and "Return to official rate" button in `SettingsDialog`.
   - Ensure saving settings or updating official rate does not silently turn on manual override.
   - Display both manual and official rate with date when override is active.

2. **Step 8 End-to-End Wiring**:
   - Enqueue `BoiSyncWorker` as unique periodic work (`ExistingPeriodicWorkPolicy.KEEP`) with network constraint (`NetworkType.CONNECTED`), scheduled after 16:00 Israel time on business days. Document in `RULES.md`.
   - On app startup (`MyselfApp` or `MainViewModel`), trigger `exchangeRateRepository.refreshRateIfNeeded()` and `hebcalRepository.refreshDataIfNeeded()` via `safeLaunch` (non-blocking).
   - Add compact Hebcal card on Home screen showing Hebrew date, next holiday, candle lighting, havdalah, and "נתונים: Hebcal.com". If no city is chosen, show "בחר עיר" opening city picker.

3. **Staleness & Historical Rates**:
   - Compute staleness at read/check time (`isStale(now)`).
   - Implement historical rate lookup querying a window of ~10 days back and returning the latest valid representative rate with its actual publication date.

4. **Real Test Resources & Hebrew Dates**:
   - Record real sample responses from BOI SDMX CSV latest, BOI historical window, and Hebcal under `app/src/test/resources/`. Write exact fetch date in `RULES.md`.
   - Parse daily Hebrew date from proper Hebcal category (not holiday items).

5. **Hebrew Strings Extraction & ErrorMapper**:
   - Move user-facing Hebrew strings to `res/values/strings.xml`, including ErrorMapper messages returning `@StringRes` IDs.
   - Classify Firestore errors by `FirebaseFirestoreException.code` (PERMISSION_DENIED, UNAVAILABLE), and network exceptions by `UnknownHostException`, `SocketTimeoutException`, `ConnectException`.

6. **City Picker Search & Tests**:
   - Implement city search/autocomplete using Hebcal lookup endpoint or verified offline fallback list with geonameid and Hebrew names.
   - Add comprehensive unit tests covering override precedence, staleness cases, historical rate on Saturday, Hebcal parsing, and remove unselectedCity test.
