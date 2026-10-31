# בעיות ידועות וחוב פתוח (Known Issues)

עודכן 31/10/2026. המחשבונים הישנים (רכב, מנויי AI, פנסיה) נמחקו ב-31/10 ועברו לעמוד הכסף, ולכן ההתנהגויות הנעולות שלהם שהיו כאן ירדו מהמסמך (הן נשארות בהיסטוריית git).
כל סעיף אומר מאיפה הוא בא. "לא נבדק מחדש" אומר שהסעיף הועתק ממקור ישן ולא אומת שוב מול הקוד.

## 1. עמוד הכסף

- **ליסינג ארוך מהתקופה שנבחרה**: נבנה כ"סופרים את כל התשלומים". מחכה לאישור סופי של מנש. מקור: PR #18.
- **קטגוריות ברירת המחדל של הרכב** (`DEFAULT_CAR_CATEGORIES` ב-`feature/money/CarView.kt`) לא אושרו על ידי מנש.
- **סגירת חודש רטרואקטיבית**: `closeMonth` שומר את היתרות של היום. סגירה של חודש עבר צריכה לאפשר להקליד את יתרות אותו חודש או לדלג (לשמור ריק). המנוע של סיכום השנה כבר מתייחס ליתרות ריקות כ"לא ידוע".
- **מנועים בלי מסך**: סיכום שנה (`YearSummary*.kt`), כיול תכנון (`PlanCalibration*.kt`) ו-`reconcileCards` (`importer/ImportMerge.kt`) נבדקים, אבל אף מסך עוד לא קורא להם.
- **ייבוא**: אין דוגמה אמיתית של "תשלום N מתוך M"; סך השורה של כאל אולי משמיט שורות מט"ח. מקור: `docs/money/IMPORT.md`, "Not verified yet".

## 2. בנק ישראל ו-Hebcal

מקור: `docs/BACKLOG_STEP8.md` (אוחד לכאן ב-31/10).

- להסיר את ברירות המחדל של `BoiRate.rate` / `rateDate` (0.0 ו"היום").
- `SettingsDialog` מחזיק `DropdownMenu` כפול לבחירת עיר; צריך להשתמש ב-`CityPickerDialog`.
- `ErrorMapper` צריך להחזיר `@StringRes` ולסווג שגיאות רשת לפי סוג החריגה בלבד.
- חיפוש עיר חי ב-Hebcal (היום 14 ערים קבועות).
- `MainViewModel`: לטפל ב-`Result` של הרענון בהפעלה במקום `runCatching`.
- הצגת השער עם 4 ספרות אחרי הנקודה ובידוד bidi.
- דגימות API אמיתיות מוקלטות (BOI אחרון, BOI חלון, Hebcal).
- לתעד ב-`RULES.md`: לוח העבודה של ה-worker, חלון 10 הימים בלי fallback, Hebcal `d=on`.
- אזהרת KSP "ApplicationManager.getApplication() is null" בכל build ב-CI, לא קטלנית (https://github.com/google/ksp/issues/2763).
- **חגים וערבי חג**: `BoiStalenessPolicy` ו-`BoiSyncWorker` לא מכירים את לוח החגים. כל שני–חמישי משתמש ב-15:30/15:45, כל שישי ב-12:30/12:45; חג באמצע השבוע נחשב יום פרסום, וערב חג לא עובר לשעון של שישי. בחלון `2026-09-01..2026-10-26` היו פערים ב-2026-09-11 (שישי) וב-2026-09-21 (שני) שלא אומתו מול לוח חגים.

## 3. העמודים הישנים (משימות, חלומות, אינבוקס, כושר, קטלוג קבצים)

מקור: `docs/archive/INVENTORY-4204866.md`. לא נבדק מחדש, חוץ ממה שמסומן "אומת 31/10".

- `Dream.isCompleted` מחפש `"completed"` ולא את הסטטוס העברי `"בוצע"` (אומת 31/10, `core/domain/model/Dream.kt`).
- קטלוג הקבצים: ה-ViewModel קורא את העמודות `"תאריך יעד"` ו-`"מרחב (שלי/של אשתי/משותף)"` (אומת 31/10), בעוד שהשדות במסך נקראים `"תאריך"` ו-`"מרחב"`, ולכן העמודות האלה נזרקות.
- `FirestoreFitnessRepository.saveTodayFitness` מקבל `date` ומתעלם ממנו.
- דיאלוג יצירת חלום לא שולח `status`, ולכן נשמר ברירת המחדל `"active"`.
- הלכידה המהירה (Quick Capture) נמחקה ב-31/10 כי לא הייתה לה כניסה מה-shell החדש; `LifeHomeScreen` עדיין מקבל פרמטר `onOpenQuickCapture` שלא עושה כלום.

## 4. חוב קוד (מתוך סקירת בריאות הקוד, 31/10)

- **שתי שכבות נתונים לעמודים הישנים**: `data/firestore/*` (DataSources + `LifeHubRepository`) ומעליה `core/data/repository/impl/*`. מועמד לאיחוד אחרי סקירת ה-UX.
- `FirestoreMoneyRepository` ו-`FirestoreTransactionRepository` יושבים ב-`core/data/repository/impl` אבל שייכים לעמוד הכסף (`feature/money/data`).
- כ-200 מחרוזות בעמודים הישנים נקראות `ui_str_NNN` בלי שם עם משמעות.
- אין `schemaVersion` במסמכי Firestore, למרות ש-`RULES.md` מבקש מיגרציות מפורשות.
- Room משמש רק לשורת הגדרות אחת (`app_settings`: `usdToIlsRate` שאף מסך לא קורא, ותאריך גיבוי היסטורי). אפשרות הגיבוי המקומית הוסרה כי לא כללה נתוני כסף.
- `firestore.rules` עדיין מתיר את האוסף `calculator_sync` של המחשבונים הישנים. ייתכן שיש בו מסמכים בענן; לא נמחק בלי אישור.
- בדיקות שקוראות קוד מקור (`ShellAndViewModelGuardsTest`, `LayerBoundariesTest`, `NightBlueGuardTest`) תלויות בנתיבי קבצים, ונשברות כשמזיזים קבצים.
- `testTag("skip_to_calculators_button")` ב-`SignInScreen` נשאר מהשם הישן.

- Money sub-screen headers currently scroll with their content; a pinned header is planned for a later UX batch.
