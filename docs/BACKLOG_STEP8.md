# BACKLOG_STEP8.md

- remove BoiRate.rate / rateDate fallbacks (0.0 and today);
- remove the duplicate city DropdownMenu in SettingsDialog (use CityPickerDialog);
- move all Hebrew UI literals to strings.xml with meaningful names, and have ErrorMapper return @StringRes;
- ErrorMapper: classify network errors by exception type only;
- live Hebcal city search;
- MainViewModel: handle the startup refresh Result instead of runCatching;
- rate display with 4 decimals and bidi isolation;
- real recorded API samples (BOI latest, BOI window, Hebcal);
- document in docs/RULES.md: the worker schedule and policy, the 10-day window with no fallback, Hebcal d=on.

## Holidays and holiday eves (out of scope)

The official page `https://www.boi.org.il/roles/markets/reprate/` says there is no representative rate on Israeli holidays (ימי שבתון), and that on Hol Hamoed and holiday eves the rate is published shortly after 12:15, same as Friday. The time is not guaranteed.

`BoiStalenessPolicy` and `BoiSyncWorker` do not know that calendar:

- Every Monday–Thursday still uses the 15:30 staleness cutoff and the 15:45 sync.
- Every Friday still uses 12:30 and 12:45.
- A holiday that falls on Monday–Friday is still treated as a publication day.
- A holiday eve that falls on Monday–Thursday is not moved to the 12:30 / 12:45 Friday clock.

In the window `startPeriod=2026-09-01&endPeriod=2026-10-26` the series `RER_USD_ILS` (`DATA_TYPE=OF00`) had weekday gaps on 2026-09-11 (Friday) and 2026-09-21 (Monday). Those two dates were not confirmed against a holiday calendar.
