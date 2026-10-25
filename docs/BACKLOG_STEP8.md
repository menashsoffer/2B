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
