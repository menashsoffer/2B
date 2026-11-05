# מבנה החבילות

```text
com.menash.myself
├── MainActivity.kt, MyselfApp.kt
├── shell/                     AppShell, AppNavHost, ShellTab, GalleryIndex, PlaceholderTab, MainViewModel
├── core/
│   ├── auth/  calendar/  crash/  di/  time/  ui/
│   ├── designsystem/          theme/, component/, gallery/
│   ├── domain/                model/, search/, util/
│   ├── external/              boi/, hebcal/
│   └── data/                  account/, mapper/, model/, repository/(impl/), session/, legacy/
├── feature/
│   ├── money/                 state + ViewModels; ui/, domain/, importer/, data/, search/
│   └── account, home, me, life, more, settings, tasks, dreams, inbox, fitness, household, calendar, importcatalog
│                              ViewModel(s) at the root (when present) + ui/
└── e2e/stories/               סיפורים בדיוניים לבניית e2e ולבדיקות
```

בבדיקות היחידה בלבד: `testing/` לעזרי צילום ו־UX, ו־`guards/` לשומרי ארכיטקטורה. בדיקת Android הקיימת `ExampleInstrumentedTest.kt` עדיין נמצאת בשורש מקור ה־androidTest.

- `shell/`: מעטפת האפליקציה והניווט.
- `core/`: קוד משותף, נתונים, מודלים ושירותים.
- `feature/`: מצב ו־ViewModels ב־`feature/<name>/`, ומסכי Compose ב־`feature/<name>/ui/`.
- `e2e/`: סיפורים לבדיקות קצה.
- הבדיקות משקפות את מבנה קוד המקור. שם הקובץ כשם המחלקה או ה־composable הראשי; בדיקות צילום נקראות `*ScreenshotTest` ולעולם אינן משנות שם.

## מה לא מזיזים

- `MainActivity.kt` ו־`MyselfApp.kt`: ה־manifest מפנה אליהם בחבילת השורש.
- `app/src/e2e/**`, כולל `E2eActivity`: מקור ייעודי לבניית e2e.
- `TodayProvider.kt` במקורות `debug`, `release` ו־`e2e`: מימושים לפי סוג הבנייה.
- `core/external/boi/BoiSyncWorker.kt`: WorkManager שומר את שם המחלקה במכשירים.
- `core/designsystem/**`: מערכת העיצוב המשותפת נשארת במקומה.
- `feature/money/domain`, `importer`, `data`, `search`: שכבות הכסף נשארות במקומן.
