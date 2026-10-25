# Master Implementation Plan

## Step 1: Core Foundation & Calculators Setup
- Establish Room database, data entities, and DAOs.
- Implement calculators for Car Comparison, AI Subscriptions, and Pension Savings.
- Set up M3 theme, navigation, and core architecture.

## Step 2: Firestore Integration & Authentication
- Provision Firebase Firestore and Authentication via Credential Manager (Google Sign-In).
- Implement Firestore repositories for Tasks, Dreams, Inbox, and Household.
- Set up secure zero-trust Firestore security rules.

## Step 3: Offline-First & Local Persistence
- Ensure offline caching and synchronization flows.
- Implement backup and restore features via JSON.

## Step 4: UI Refinement & Cognitive Load Management
- Implement 5-tab bottom navigation with persistent quick capture FAB.
- Enforce strict cognitive load rules (Top 3, Five Before 11, clean views).

## Step 5: Accessibility, RTL, and Design System
- Apply Material Design 3 tokens, minimum 48dp touch targets, RTL layout direction, and content descriptions.

## Step 6: External Data Sources (BOI & Hebcal)
- Set up Bank of Israel SDMX CSV client and Hebcal REST integration.
- Implement DataStore caches for exchange rates and Hebrew calendar data.

## Step 7: Migration & CSV Imports
- Implement CSV import wizard with column mapping for bank statements, cards, and list data.

## Step 8: External Data Wiring & Automated Sync
- Enqueue `BoiSyncWorker` as unique periodic work (`ExistingPeriodicWorkPolicy.UPDATE`) with network constraints and 16:30 Israel time initial delay (skipping Friday/Saturday).
- On app startup, trigger non-blocking refreshes for exchange rates and Hebcal calendar data.
- Add HomeHebcalCard showing Hebrew date, next holiday, candle lighting, havdalah, and "נתונים: Hebcal.com". If no city is chosen, show "בחר עיר" opening city picker.
- **Current Round Fixes (Review Round 2)**:
  1. Restore master plan and document in docs/RULES.md.
  2. Use real downloaded samples for BOI and Hebcal test resources.
  3. Strict historical rate query without fallback to today's rate.
  4. Precise daily Hebrew date parsing via `d=on` parameter in Hebcal.
  5. Official vs manual rate separation in DataStore and SettingsDialog, with periodic staleness check.
  6. Staleness computed at check time (`isStale(now)`).
  7. Correct work delay calculation and Friday/Saturday skip.
  8. Remove hardcoded 3.11 from HomeHebcalCard.
  9. Move RULES.md to docs/RULES.md (deleted app/RULES.md).
  10. Extract all Hebrew strings to strings.xml.
  11. Precise ErrorMapper classification.
  12. Move HomeHebcalCard to `ui/home/HomeHebcalCard.kt` (<300 lines per file).
  13. Hebcal city search/autocomplete in city picker.
  14. Comprehensive unit tests for override, staleness, historical rates, and Hebrew dates.
  15. Non-blocking error reporting via ErrorReporter in MainViewModel.
  16. 4 decimals formatting with bidi isolation.

## Step 9: Advanced Analytics & Phase 2 Features
- Monthly cashflow analysis, automated bank/card statement parsing, and advanced budgeting.
