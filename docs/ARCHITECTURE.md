# ארכיטקטורה — 2B

המסמך מתאר את הקוד כפי שהוא ב-`main` (31/10/2026). כללי הפיתוח ב-[RULES.md](RULES.md), בעיות פתוחות ב-[KNOWN_ISSUES.md](KNOWN_ISSUES.md), מה הלאה ב-[ROADMAP.md](ROADMAP.md).

## מודול אחד, חבילה אחת

אפליקציית Android אחת (`app/`), Kotlin + Jetpack Compose. Namespace: `com.menash.myself`. Application id: `com.aistudio.mycalculators.qvxmzt` (קבוע לתמיד, אחרת הטלפון לא מקבל עדכון).

```
com.menash.myself
├── MainActivity, MyselfApp      כניסה; MyselfApp מחזיק את AppContainer
├── core/
│   ├── auth/                    כניסה עם Google (Credential Manager + Firebase Auth)
│   ├── calendar/                קריאת יומן המכשיר
│   ├── crash/                   Crashlytics: non-fatals ו-handler שמנקה הודעות
│   ├── data/
│   │   ├── legacy/              Firestore ו-Room של העמודים הישנים: firestore/, models/, entities/, dao/
│   │   ├── mapper/              המרות בין מסמכי Firestore הישנים למודל ה-domain
│   │   ├── model/               ItemLocation (פריט בבית או פרטי)
│   │   ├── repository/          ממשקי Repository שהמסכים תלויים בהם
│   │   │   └── impl/            מימושים: Firestore, Room (הגדרות), BOI, Hebcal
│   │   └── session/             HouseholdSession, ErrorReporter
│   ├── designsystem/            Night Blue: theme/ (טוקנים), component/, gallery/
│   ├── di/                      AppContainer: הזרקת תלויות ידנית
│   ├── domain/                  מודלים משותפים (Money באגורות, ForWhom, Task, Dream…) ו-util/
│   ├── external/                בנק ישראל (boi/) ו-Hebcal (hebcal/), עם מטמון DataStore
│   └── ui/                      BaseViewModel (safeLaunch), ErrorMapper
├── feature/
│   ├── money/                   עמוד הכסף (ראו למטה)
│   └── home, tasks, dreams, inbox, fitness, household, importcatalog
│                                ViewModels של העמודים הישנים
└── ui/
    ├── shell/                   AppShell, ניווט, 5 הלשוניות
    ├── money/                   מסכי הכסף (Content טהור + Screen שמחבר ViewModel)
    └── home, tasks, dreams, …   מסכי העמודים הישנים
```

## הלשוניות והניווט

`ui/shell/ShellTab.kt`: בית, כסף, חלומות, קניות, מעקב. בית וכסף בנויים; שלוש האחרות הן `PlaceholderTab`. המסכים הישנים (משימות, חלומות הישן, אינבוקס, כושר, יומן, קטלוג קבצים, משק בית) נשארו כ-routes נפרדים ב-`AppNavHost`. מגיעים אליהם רק מהגלריה הנסתרת (`GalleryIndex`): לחיצה ארוכה על שורת הגרסה בהגדרות.

## זרימת נתונים

```
Screen (Compose) ──> ViewModel (BaseViewModel.safeLaunch) ──> Repository interface ──> impl ──> Firestore / DataStore / Room
                         │                                                                        │
                         └── domain: פונקציות טהורות, בלי Android ──────────────────────────────────┘
```

- **AppContainer** (`core/di`) בונה הכל פעם אחת. מסכים מגיעים אליו דרך `LocalAppContainer`, ו-ViewModels מקבלים ממנו את ה-repositories ב-`factory`.
- **שגיאות**: כל repository חושף `errorFlow`. `AppContainer.errorFlow` מאחד אותם עם `ErrorReporter`, ו-`AppShell` מציג באנר אחד.
- **משק בית**: `HouseholdSession` מחזיק את ה-household הפעיל. פריט משותף נשמר תחת `households/{hid}/…`, ופריט פרטי תחת `users/{uid}/private/…` (`ItemLocation`).

## עמוד הכסף (`feature/money`)

| חבילה | מה יש בה | Android? |
|---|---|---|
| `feature/money/domain` | המנוע: תכנון חודשי, פעמונים, סגירת חודש, "האם אפשר", צדקה, פנסיה, רכב, סיכום שנה, כיול תכנון. פירוט ב-[money/ENGINE.md](money/ENGINE.md) | לא |
| `feature/money/importer` | קריאת קבצי בנק ואשראי (XLS/XLSX/CFB משלנו, בלי POI), זיהוי, מיזוג, סיווג. פירוט ב-[money/IMPORT.md](money/IMPORT.md) | לא |
| `feature/money/data` | המרה בין מודל ה-domain למסמכי Firestore (`*Documents`, `MoneyDoc`), וממשקי `MoneyRepository`/`TransactionRepository` | לא |
| `feature/money/*.kt` | מצב מסכים (drafts, views) ו-ViewModels | רק ה-ViewModels |
| `feature/money/ui` | המסכים. כל מסך מחולק ל-`*Content` (טהור, נבדק ב-screenshot) ו-`*Screen` (מחבר ViewModel) | כן |
| `core/data/repository/impl/FirestoreMoneyRepository`, `FirestoreTransactionRepository` | הגישה ל-Firestore | כן |

כסף הוא תמיד `Money` (אגורות ב-`Long`), לעולם לא `Double`.

## בדיקות

- **בדיקות יחידה** (`app/src/test`): JUnit + Robolectric. המנוע וה-importer נבדקים כ-Kotlin טהור.
- **Screenshot** (Roborazzi): goldens ב-`app/src/test/snapshots/designsystem/`. שם הקובץ הוא שם מחלקת הבדיקה, אז לא משנים שם למחלקת screenshot בלי להקליט מחדש.
- **שומרי ארכיטקטורה**: `LayerBoundariesTest` (אין import של `data.` ב-`feature/` וב-`ui/`, כל קובץ פחות מ-300 שורות), `ShellAndViewModelGuardsTest`, `NightBlueGuardTest` (צבעים ו-sp רק מה-theme, עם allow-list שנכשלת גם על רשומה מיותרת).
- **חוקי Firestore**: `firestore.test.js` מול האמולטור.

## CI

| Workflow | מתי | מה |
|---|---|---|
| `branch-tests.yml` | push ל-`claude/**`, או ידני | בדיקות יחידה. ידני עם `record=true` מקליט goldens חדשים בלבד ועושה commit "test: record new goldens" (הריצה עצמה תמיד מסומנת failure) |
| `distribute.yml` | ידני, או הרצה מתוזמנת יומית כשיש שינויים | בדיקות, APK חתום, Firebase App Distribution לקבוצה `family` |
| `firestore-rules.yml` | נקרא על ידי Distribute, או ידני | בדיקות אמולטור ו-deploy לחוקים ולאינדקסים |
