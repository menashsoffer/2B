# תוכנית רה-פקטורינג מקיפה ומדויקת: "המחשבונים שלי" (Myself)

תוכנית שלבית מוקפדת לרה-פקטורינג מבני מלא ללא שום שינוי התנהגות (Zero Behavior Change), נעילת תוצאות החישובים מראש באמצעות בדיקות מאפיינות (Characterization Tests), ייצוב תשתית ה-Firebase הקיימת (שלב 1.5), מודל משק בית משותף כברירת מחדל עם שיוך `forWhom`, שמירה על מקור אמת יחיד, ומיגרציית סכמה ומחשבונים אידמפוטנטית.

---

## תיקון עובדתי של תמונת המצב (State of the Project)

- **מצב הנתונים הנוכחי:**
  - **Firestore (חי ופעיל):** חלומות (`dreams`), משימות (`tasks`), אינבוקס (`inbox`), ויומן כושר (`fitness_logs`) כבר נשמרים ומנוהלים ישירות ב-Firestore.
  - **Room (מקומי בלבד):** מחזיק **אך ורק את המחשבונים** (`cars`, `ai_subscriptions`, `pension_funds`) בקובץ הקיים `"my_calculators.db"`.
- **מסקנה ארכיטקטונית:**
  - אין צורך להעביר חלומות, משימות ואינבוקס מ-Room — הם כבר ב-Firestore, אך המבנה שלהם והחוקים סביבם דורשים ייצוב מיידי (Step 1.5).
  - **שלב 10 מתמקד בשני דברים:**
    1. העברת נתוני המחשבונים בלבד מ-Room ל-Firestore.
    2. מיגרציית סכמה (Schema migration) של המסמכים הקיימים ב-Firestore (`space` ← `ownerUid` + `forWhom` + `isPrivate`, המרת `Double` לאגורות, ותאריכים ל-ISO `yyyy-MM-dd`).

---

## עקרונות המפתח של מודל המרחבים (Household-First Space Model)

1. **שייכות למשק הבית כברירת מחדל:**
   - כל פריט משותף נשמר תחת הנתיב ההיררכי: `households/{householdId}/{collection}/{itemId}`.
   - שני בני הזוג החברים במשק הבית רשאים לקרוא את כל הפריטים הללו כברירת מחדל.
2. **אוסף נפרד לחלוטין לפריטים פרטיים:**
   - פריטים שסומנו עם `isPrivate = true` נשמרים אך ורק בנתיב `users/{uid}/private/{collection}/{itemId}` (ולא בתוך אוסף ה-household).
   - **רציונל:** חוקי אבטחה ב-Firestore אינם מסננים שאילתות (Rules are not filters). שאילתה על אוסף ה-household שעלולה לפגוש מסמך פרטי תידחה במלואה (`PERMISSION_DENIED`).
3. **שדות מזהה ושיוך:**
   - לכל פריט יש `ownerUid` (מזהה היוצר) ושדה `forWhom`: `ME` / `PARTNER` / `BOTH`.
4. **תוויות מחושבות לפי צופה (Viewer-Computed Labels):**
   - אם `item.ownerUid == viewer.uid`:
     - `ME` ← **"שלי"**
     - `PARTNER` ← שמו/שמה הפרטי של בן/בת הזוג (במקום "שלו/שלה")
     - `BOTH` ← **"של שנינו"**
   - אם `item.ownerUid != viewer.uid` (בן/בת הזוג צופה):
     - `ME` של היוצר מוצג עם שמו/שמה הפרטי של בן/בת הזוג
     - `PARTNER` של היוצר מוצג כ-**"שלי"** (פריט שבן/בת הזוג יצר/ה עבורי נספר ומסומן כ"שלי")
     - `BOTH` מוצג כ-**"של שנינו"**
5. **הפחתת עומס קוגניטיבי:** תצוגת ברירת המחדל מציגה **"שלי + של שנינו"** (`mine + both`), ולחיצה על צ'יפ **"הצג הכל"** מציגה את כל פריטי הבית.
6. **צבעי `SpaceBadge`:** נקבעים לפי `forWhom` (שלי / בן-בת הזוג / של שנינו).
7. **שם קובץ Room DB:** נשמר בקפדנות כ-`"my_calculators.db"`.

---

## טבלת מיפוי המיגרציה ב-Step 10

| סוג נתונים | מיקום נוכחי | יעד ב-Step 10 | שיוך `forWhom` | פרטיות (`isPrivate`) | זכויות צפייה |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **מחשבון רכבים (Cars)** | Room מקומי | `households/{hid}/cars` | `BOTH` ("של שנינו") | `false` | שני בני הזוג |
| **מנויי AI (AI Subs)** | Room מקומי | `households/{hid}/ai_subscriptions` | `ME` ("שלי") | `false` | שני בני הזוג (מופיע ב"שלי") |
| **פנסיה וחיסכון (Pensions)** | Room מקומי | `households/{hid}/pensions` | `ME` ("שלי") | `false` | שני בני הזוג (מופיע ב"שלי") |
| **חלומות (Dreams)** | Firestore קיים (`/dreams`) | `households/{hid}/dreams` (שדרוג סכמה) | `BOTH` ("של שנינו") | `false` | שני בני הזוג |
| **משימות (Tasks)** | Firestore קיים (`/tasks`) | `households/{hid}/tasks` (שדרוג סכמה) | `ME` ("שלי") | `false` | שני בני הזוג (מופיע ב"שלי") |
| **אינבוקס (Inbox)** | Firestore קיים (`/inbox`) | `households/{hid}/inbox` (שדרוג סכמה) | `ME` ("שלי") | `false` | שני בני הזוג (מופיע ב"שלי") |
| **כושר (Fitness)** | Firestore קיים (`/fitness_logs`) | `users/{uid}/fitness` (נשאר אישי) | `ME` ("שלי") | `false` | המשתמש בלבד |

---

## סדר שלבי הביצוע המדויק (Step-by-Step Execution Plan)

### שלב 0: בדיקות מאפיינות ותיעוד התנהגות קיימת — הושלם (100% ירוק) ✅
- בדיקות המאפיינות ב-`CharacterizationTests.kt` רצות ועוברות בהצלחה (17/17 בדיקות ירוקות).
- מסמך `docs/KNOWN_ISSUES.md` מתעד את ההתנהגויות הנעולות תוך הפניה לשמות הבדיקות (כולל התנהגות עתידית מיועדת ל-1.4: "הזן צריכה").

### שלב 1: מסמך חוקי הפיתוח (`docs/RULES.md`), תכונות עתידיות (`docs/ROADMAP.md`), ו-Namespace — הושלם ✅
- `docs/RULES.md` נוצר ומעגן את עקרונות הפיתוח, מודל המרחבים, שם בסיס הנתונים Room `"my_calculators.db"`, ואיסור מחיקת מידע.
- מסמך תכונות עתידיות `docs/ROADMAP.md` עודכן עם 5 התיקונים של המשתמש, וסומן כ-LOCKED.
- עדכון Namespace וחבילות ל-`com.menash.myself` הושלם ונבדק בהצלחה בקומפילציה ובבדיקות.

---

### שלב 2 (1.5): ייצוב ותיקון שכבת Firebase (Stabilize Firebase) — הושלם (100% ירוק) ✅
1. **השבתת הרצה אוטומטית של MigrationManager:**
   - `AUTO_MIGRATE_ENABLED` הוגדר כ-`const val = false`.
   - מסמכי `calculator_sync` ישנים נשמרים ללא מחיקה; פונקציית בדיקה ייעודית מפיקה את רשימתם.
2. **מבנה נתונים משותף תחת משק הבית (Data Layout):**
   - מעבר לנתיב היררכי: `households/{hid}/{collection}/{itemId}` (עבור `tasks`, `dreams`, `inbox`).
   - חוקי האבטחה בודקים חברות באמצעות `get(/databases/$(database)/documents/households/$(hid)).data.memberUids`.
3. **תיחום מלא של כל השאילתות (Scoped Queries Only):**
   - כל שאילתות Firestore מתוחמות לנתיב ה-household או ל-`users/{uid}/...`.
   - נתוני כושר מתוחמים ב-`users/{uid}/fitness/{logId}`.
4. **זרימת הזמנות והצטרפות למשק בית (Join Flow):**
   - מסמך הזמנה ייעודי ב-`invites/{emailLower}`.
   - חוק העדכון למשק הבית דורש קיום הזמנה תואמת: `exists(/databases/$(database)/documents/invites/$(request.auth.token.email.lower())) && get(...).data.householdId == householdId`.
   - חברים שאינם בעלים אינם מורשים לשנות את `memberUids` (מלבד הסרת עצמם - יציאה).
   - הצטרפות לקוח (Client join): מתבצעת ישירות באמצעות `FieldValue.arrayUnion(uid)` ו-`partnerUid = uid` **ללא קריאה מוקדמת** (שכן משתמש שאינו חבר אינו מורשה לקרוא).
   - מחיקת ההזמנה מתבצעת לאחר הצטרפות מוצלחת על ידי המצטרף או הבעלים.
5. **אי-בליעת שגיאות Firestore (Never Swallow Errors):**
   - חשיפת `errorFlow` בממשק והצגת הודעת שגיאה ברורה עם קוד השגיאה.
6. **עבודה חופשית במחשבונים ללא התחברות (Offline Room Calculators):**
   - המחשבונים עובדים מקומית מול Room ללא צורך בחשבון מחובר.
7. **הרחבת אירועי לוח שנה חוזרים:**
   - קריאת `CalendarContract.Instances` במקום `Events`.
8. **בדיקות מקיפות באמולטור לחוקי אבטחה (`firestore.test.js`):**
   - הורצו 10/10 בדיקות ירוקות באמולטור, כולל תרחישים שליליים:
     - זר ללא הזמנה נדחה.
     - מוזמן לבית 1 אינו יכול להצטרף לבית 2.
     - שותף אינו יכול להסיר את הבעלים או להוסיף משתמש שלישי.
     - הצטרפות דרך `arrayUnion` מצליחה ללא קריאה מוקדמת, וההזמנה נמחקת.
     - יצירה ועדכון של פריטים דורשים `userId == request.auth.uid` ואי-שינוי ה-`userId`.
9. **חילוץ פענוח קמ/ליטר ובדיקת אפיון מדויקת (KNOWN_ISSUES 1.4):**
   - חולץ וננעל בבדיקה ייעודית.
10. **ניקוי תלויות מיותרות:**
    - הוסרו `firebase.ai` ורכיבי App Check הלא מאותחלים.
    - נוסף `firestore.indexes.json` עם אינדקס מורכב עבור `households/{hid}/inbox` (`status` ASC + `createdAt` DESC) ומקושר ב-`firebase.json`.

---

### שלב 3: שכבת ה-Domain ובדיקות יחידה טהורות (Pure Kotlin + Unit Tests) — הושלם (100% ירוק) ✅
- **`core/domain/model/` (פרימיטיבים ומודלים טהורים):**
  - `Money`: ייצוג מדויק באגורות (`Long`), מניעת שגיאות עיגול, אופרטורים חשבוניים (`+`, `-`, `*`, `/`), המרות שקלים, ועיצוב עברי תקני (`₪1,250` / `-₪500`).
  - `ForWhom`: טיפוס `ME`, `PARTNER`, `BOTH` עם פונקציית `labelForViewer(isOwner, partnerName)` לפי חוקי התצוגה המותאמת, וסינון עומס קוגניטיבי (`mine + both` כברירת מחדל, והצגת הכל בבחירה).
  - `DateRange`: טווח תאריכים מבוסס `java.time.LocalDate` עם בדיקת הכלה וספירת ימים/חודשים.
- **`feature/*/domain` (חישובים ייעודיים כפונקציות טהורות נטולות אנדרואיד):**
  - `CarCostCalculator.kt`: חישוב עלות 5-שנתית, תחימת עלות חודשית לאפס (KNOWN_ISSUES § 1.1), חישוב אנרגיה לדלק וחשמל, פענוח קמ/ל (KNOWN_ISSUES § 1.4), והשוואת בסיס עם קיטום כלפי מטה (KNOWN_ISSUES § 1.2).
  - `AiSubscriptionCalculator.kt`: חישוב עלות מנויים בדולר ובשקלים, סינון מנויים לא פעילים, וסיכום חודשי/שנתי.
  - `PensionSavingsCalculator.kt`: חישוב רווח, אחוז תשואה עם הגנה מחלוקה באפס (KNOWN_ISSUES § 2.1), וסיכום תיק עם זיהוי דמי הניהול הגבוהים ביותר.
  - `DreamSavingsCalculator.kt`: חישוב יתרת חיסכון, אחוז התקדמות, וחודשים נדרשים להשגת היעד.
  - `TaskScheduler.kt`: סינון DO NOW עם התאמת אנרגיה ומצב ריק כשאין התאמה, תג ברירת מחדל "שוטף" למשימות ללא תיוג, הסתרת "חמש לפני 11" אחרי 11:00, וסינון חלונות זמן לעוגנים.
- **בדיקות יחידה טהורות (Unit Tests):**
  - נוצרו 8 קבצי בדיקות חדשים ב-`app/src/test/java/com/menash/myself/domain/`:
    `MoneyTest`, `ForWhomTest`, `DateRangeTest`, `CarCostCalculatorTest`, `AiSubscriptionCalculatorTest`, `PensionSavingsCalculatorTest`, `DreamSavingsCalculatorTest`, `TaskSchedulerTest`.
  - כל הבדיקות עברו בהצלחה (100% ירוק ב-`gradle :app:testDebugUnitTest`).

---

### שלב 4: ממשקי נתונים ב-`core/data` וחיבור Room בלבד (השלב הבא)
- **`core/data/repository/` (ממשקים בלבד):** `TaskRepository`, `DreamRepository`, `InboxRepository`, `FitnessRepository`, `HouseholdRepository`, `CalculatorRepository`.
- **`core/data/local/`:**
  - ישויות Room קפואות לחלוטין ללא שינוי מבנה או שמות.
  - מימושי Repository המבוססים על Room בלבד עבור המחשבונים, המבצעים המרה מפורשת באגורות (`Long`) ו-`java.time` בגבול השכבה (Repository boundary).
  - סריאליזציית Moshi אחידה: שחזור = החלפה מלאה (עם אישור וגיבוי מקדים); ייבוא = בדיקת שורות קפדנית ודחיית שורות שגויות עם פירוט מלא.
  - מטמון מקומי ב-DataStore (לשער בנק ישראל ונתוני Hebcal).
- פיצ'רים תלויים אך ורק בממשקים ב-`core/data`.

### שלב 4: הקמת `core/designsystem` (עיצוב מונחה הפחתת עומס)
- **טוקנים:** `Spacing.kt` (4, 8, 16, 24, 32 dp), `Type.kt`, `ColorTokens.kt`.
- **צבעי `SpaceBadge`:** צבעים מובחנים לפי `forWhom` (שלי / בן/בת הזוג / של שנינו).
- **רכיבים משותפים:** `AppCard`, `ListRow`, `MoneyText`, `StatusChip`, `SpaceBadge`, `EmptyState`, `LoadingState`, `ErrorState`, `OfflineIndicator`, `CaptureSheet`.

### שלב 5: מכולת תלויות ידנית (`core/di/AppContainer.kt`)
- `AppContainer` יחיד המחבר את מימושי הנתונים לממשקים ומזריק אותם ל-ViewModels (שומר על מקור אמת יחיד).

### שלב 6: ארגון הפיצ'רים ופיצול קבצים (Feature Packages & Screen Splitting)
- הקפדה על פחות מ-300 שורות לקובץ ופחות מ-80 שורות לקומפוננטה.
- `feature/calculators/`: פיצול מסכי רכב, AI ופנסיה.
- `feature/dreams/`, `feature/tasks/`, `feature/fitness/`, `feature/calendar/`.
- `feature/importcatalog/`: קטלוג קבצים נתמכים, תבניות CSV, ואשף מיפוי מדויק עם דו"ח שגיאות לכל שורה שגויה.
- `feature/settings/`: הגדרות שער, בחירת עיר ל-Hebcal מתוך רשימת ערים (ללא ברירת מחדל של ירושלים), העדפות, ופרטי חשבון.
- `feature/household/`: ניהול משק הבית וזרימת הזמנה לפי מייל Google.

### שלב 7: ניווט 5 טאבים ואינטראקציה (Navigation & Interactions)
- **Bottom Navigation Bar (M3):** בית, חלומות, משימות, מחשבונים, עוד.
- תצוגת ברירת מחדל: "שלי + של שנינו" עם צ'יפ "הצג הכל".
- כפתור לכידה מהירה (+ FAB) נגיש תמיד.
- שחזור פריטים של `BOTH` מציג אזהרה מפורשת.
- Snackbar עם Undo לכל פעולה הפיכה; דיאלוג אישור לפעולות הרסניות.

### שלב 8: מקורות מידע חיצוניים רשמיים ומטמון DataStore (`core/external`)
- **בנק ישראל (BOI SDMX CSV):** שאיבת שער דולר יציג רשמי ללא מפתח API; שמירה ב-DataStore מקומי; בדיקת עדכון יומית אחרי 16:00; תמיכה בעקיפה ידנית מסומנת.
- **לוח עברי וזמני שבת (Hebcal REST API):** תאריך עברי, חגים, וזמני שבת לפי העיר שנבחרה בהגדרות; שמירה ב-DataStore מקומי; קרדיט רשמי "נתונים: Hebcal.com".

### שלב 9: פאזה סופית: מיגרציית סכמה ב-Firestore והעברת מחשבונים מ-Room
- מתבצעת רק לאחר שכל הרה-פקטורינג הסתיים ועבר את כל הבדיקות.
- **סדר ביצוע מחייב:**
  1. **התחברות (Sign in):** המשתמש מחובר בחשבון Google.
  2. **יצירת/הצטרפות למשק בית (Create/Join Household):** וידוא קיום מסמך הבית.
  3. **העברת מחשבונים מ-Room:** העברת נתוני `cars`, `ai_subscriptions`, `pension_funds` אל `households/{hid}/...`.
  4. **מיגרציית סכמה למסמכי Firestore הקיימים:**
     - `tasks`, `dreams`, `inbox`: הסבת `space` ("mine"/"shared") ל-`ownerUid`, `forWhom` (`ME`/`PARTNER`/`BOTH`), ו-`isPrivate = false`.
     - המרת ערכים כספיים מ-`Double` ל-`Long` באגורות.
     - המרת תאריכים למחרוזות תקניות בפורמט ISO `yyyy-MM-dd`.
  5. **קביעת בעלות:** ה-`ownerUid` נקבע כמשתמש המחובר המריץ, `isPrivate = false`.
- **אידמפוטנטיות ובטיחות:** יצירת מזהים יציבים (Stable IDs) ודגל `migratedAt` המונע כפילויות בכל הרצה חוזרת.
- **אימות כפול:** גיבוי מקומי מלא של בסיס הנתונים Room לפני המיגרציה, ובדיקת כמויות שורות מלאה מול מסמכי Firestore.
- **הסרת Room:** רק לאחר אימות סופי ומוחלט של כל הנתונים ב-Firestore, ניתוק Room והסרתו מהפרויקט.

---

## ניתוח סיכונים ותוכנית בקרה (Risks & Mitigations)

| סיכון | רמת סיכון | בקרה ומניעה |
| :--- | :--- | :--- |
| **חסימת שאילתות Firestore בגלל חוקי אבטחה** | קריטית | שלב 1.5 מייצב את המבנה ההיררכי ומפריד פריטים פרטיים לאוסף נפרד. |
| **אובדן נתוני מחשבונים מקומיים** | גבוהה | ישויות Room קפואות עד שלב 9; גיבוי מלא לפני המיגרציה; קובץ DB נשאר `"my_calculators.db"`. |
| **אי-זיהוי או דריסת נתונים קיימים ב-Firestore** | גבוהה | תיקון התוכנית מכיר בכך שחלומות/משימות כבר ב-Firestore ודורשים שדרוג סכמה ולא ייבוא מחדש. |
| **שינוי לא רצוי בתוצאות החישובים** | גבוהה | בדיקות מאפיינות (שלב 0) עברו בהצלחה ומופעלות בכל צעד. |
| **חסימת עדכוני APK** | גבוהה | שמירה מדויקת על `applicationId = "com.aistudio.mycalculators.qvxmzt"`. |
