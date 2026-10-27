# חוקי הפיתוח והארכיטקטורה (Development Rules) — "המחשבונים שלי" (Myself)

מסמך זה מהווה חוזה מחייב לפיתוח ולתחזוקת האפליקציה. יש לקרוא מסמך זה לפני כל שינוי עתידי בקוד.

---

## 1. כללי יסוד
1. **כשלא בטוחים — שואלים:** לעולם אין לנחש כוונת משתמש, התנהגות עסקית או נוסחאות חישוב.
2. **איסור מוחלט על מחיקת נתוני משתמש:** אף פעולה, מיגרציה או שחזור אינם מוחקים נתונים ללא גיבוי מקומי מקדים ואישור מפורש מהמשתמש.
3. **מבנה אחיד:** כל פיצ'ר חדש נבנה באותו מבנה בדיוק (`ui/`, `viewmodel/`, `domain/`).

---

## 2. ארכיטקטורת חבילות וקבצים (Feature-Based Architecture)
- **חבילה ו-Namespace:** `com.menash.myself`.
- **מזהה אפליקציה (Application ID):** נשאר קבוע (`com.aistudio.mycalculators.qvxmzt`) כדי להבטיח עדכונים שוטפים של ה-APK המותקן על המכשיר.
- **חלוקת מודולים:**
  - `core/`: תשתית משותפת (נתונים, מודלים, עיצוב, שירותים חיצוניים, הזרקת תלויות).
  - `feature/`: מודולי תכונה עצמאיים:
    - `feature/home`: מסך "מה מול העיניים".
    - `feature/capture`: לכידה מהירה (Quick Capture) ואינבוקס.
    - `feature/dreams`: חלומות, רשימות ורעיונות אפליקציה.
    - `feature/tasks`: משימות, Top 3, Five Before 11.
    - `feature/fitness`: מעקב תנועה וכושר יומי ושבועי.
    - `feature/calendar`: יומן אירועים (קריאה בלבד מיומן המכשיר).
    - `feature/importcatalog`: קטלוג קבצים להעלאה, תבניות CSV ואשף מיפוי.
    - `feature/settings`: הגדרות שער, בחירת עיר ל-Hebcal, ופרטי חשבון.
    - `feature/household`: ניהול משק הבית ושותפים.
    - `feature/calculators/`: מחשבונים פיננסיים (`car`, `ai`, `pension`, `hub`).
- **מבנה פנימי של כל פיצ'ר:**
  - `ui/`: מסכים וקומפוננטות UI.
  - `viewmodel/`: ניהול מצב עם `UiState` מבוסס `StateFlow`, ואירועים נכנסים (`Events`).
  - `domain/`: לוגיקה עסקית וחישובים כפונקציות טהורות (Pure Kotlin בלבד — ללא שום ייבוא של ספריות Android).
- **גבולות תלויות (Dependency Boundaries):**
  - פיצ'רים תלויים אך ורק בממשקי Repository המוגדרים ב-`core/data/repository`.
  - **איסור מוחלט:** אף פיצ'ר אינו מייבא ישירות את `core/firebase` או ספריות Firestore ישירות.
  - מכולת תלויות ידנית פשוטה (`AppContainer`) — ללא שימוש בפריימוורקים כבדים.

---

## 3. איכות קוד וחישובים
1. **גודל קבצים וקומפוננטות:**
   - מקסימום כ-300 שורות לקובץ קוד.
   - מקסימום כ-80 שורות לפונקציית Composable.
   - מסכים מורכבים מפוצלים לקומפוננטות משנה ייעודיות.
2. **לוגיקת חישוב בשכבת ה-Domain:**
   - כל חישוב (עלות בעלות 5 שנים, המרות מנויי AI, תשואת פנסיה ודמי ניהול, התקדמות חיסכון לחלום) מתבצע כפונקציה טהורה ומכוסה בבדיקות יחידה מקיפות.
3. **ייצוג כספים ותאריכים:**
   - **כסף:** מיוצג תמיד כ-`Long` באגורות (או `BigDecimal`), לעולם לא כ-`Double`.
   - **תאריכים:** `java.time` עם אזור זמן `ZoneId.of("Asia/Jerusalem")`.
   - ישויות Room שומרות על טיפוסיהן המקוריים עד שלב המיגרציה, וההמרה לאגורות מתבצעת בגבול ה-Repository.
4. **עברית ושפות:**
   - כל טקסט משתמש מוגדר ב-`res/values/strings.xml` (עברית).
   - אין מחרוזות מוקלדות ישירות בקוד (Hardcoded Strings) ואין מספרי קסם (Magic Numbers).
5. **סריאליזציה, גיבוי וייבוא:**
   - שימוש בלעדי בספריית **Moshi** לגיבוי, שחזור וייבוא.
   - **שחזור = החלפה (REPLACE):** מחליף את הנתונים רק לאחר אישור מפורש של המשתמש וביצוע גיבוי אוטומטי מקדים.
   - **ייבוא קפדני:** לעולם אין להמציא ערכי ברירת מחדל לשדות חסרים או לא תקינים. שורה שגויה נדחית ומוצגת בדו"ח שגיאות מפורט לפי שורה.
6. **מבנה Firestore:**
   - לכל מסמך יש שדה `schemaVersion`. שינויי מבנה נתונים עוברים מיגרציות מפורשות ללא Fallback הרסני.

---

## 4. מודל משק הבית והמרחבים (Space Model)
1. **שייכות למשק הבית:** כל פריט משויך למשק הבית (`householdId`), ושני בני הזוג רשאים לקרוא את כל הפריטים כברירת מחדל.
2. **שדות מזהה ושיוך:** לכל פריט יש `ownerUid` ושדה `forWhom`:
   - `ME` (מיועד לי)
   - `PARTNER` (מיועד לבן/בת הזוג)
   - `BOTH` (מיועד לשנינו)
3. **תוויות מחושבות לפי צופה (Viewer-Computed Labels):**
   - עבור היוצר: `ME` ← "שלי", `PARTNER` ← שמו/שמה הפרטי של בן/בת הזוג (במקום "שלה"/"שלו" גנרי), `BOTH` ← "של שנינו".
   - עבור בן/בת הזוג: `ME` של היוצר מוצג עם שמו/שמה הפרטי של היוצר (בן/בת הזוג של הצופה), ו-`PARTNER` מוצג כ-"שלי" (פריט שבן/בת הזוג יצר/ה עבורי נספר ומסומן כ"שלי").
4. **הפחתת עומס קוגניטיבי:**
   - תצוגת ברירת המחדל מציגה "שלי + של שנינו" (`mine + both`).
   - כפתור/צ'יפ "הצג הכל" חושף בלחיצה אחת את כל פריטי הבית.
5. **פריטים פרטיים באוסף נפרד (`users/{uid}/private`):**
   - כבוי כברירת מחדל (`isPrivate = false`).
   - פריט שסומן במפורש כ-`isPrivate = true` נשמר אך ורק באוסף נפרד `users/{uid}/private/...` ולא בתוך אוסף ה-household.
   - הרציונל: חוקי Firestore אינם פילטרים; ערבוב פריטים פרטיים באוסף ה-household גורר דחייה מוחלטת של שאילתות על האוסף המשותף.
6. **חוקי אבטחה ב-Firestore (Default-Deny):**
   - `users/{uid}/private/...`: נגיש לקריאה ולכתיבה אך ורק ל-`ownerUid` (`request.auth.uid == uid`).
   - `households/{householdId}/...`: פריטי הבית המשותפים נגישים לחברי משק הבית בלבד.
   - הצטרפות למשק בית מותנית באימות אימייל: `resource.data.partnerEmail == request.auth.token.email.lower()`, ועדכון מוגבל אך ורק להוספת ה-UID של המצטרף למערך `members` (`diff.affectedKeys().hasOnly(['members'])`). כתובות נשמרות באותיות קטנות.
7. **צבעי `SpaceBadge`:** נצבעים לפי `forWhom` (שלי / בן-בת הזוג / של שנינו), ולא לפי צבע אדם קבוע.
8. **גיבוי ושחזור מתוחם:** שחזור פריטים המסומנים כ-`BOTH` מציג אזהרה ברורה שהפעולה משפיעה על שני בני הזוג.
9. **שם קובץ Room DB:** נשמר ללא שינוי כ-`"my_calculators.db"` כדי להבטיח רציפות נתונים מלאה במכשיר המשתמש.

---

## 5. ממשק משתמש ומערכת עיצוב (Design System & Cognitive Load)
1. **עיצוב מבוסס טוקנים (`core/designsystem`):**
   - מרווחים אחידים: רשת מודולרית של 4dp, 8dp, 16dp, 24dp, 32dp.
   - רכיבים משותפים: `AppCard`, `ListRow`, `MoneyText` (עם פסיקי אלפים וסמל ₪ מיושר RTL), `StatusChip`, `SpaceBadge`, `EmptyState`, `CaptureSheet`.
   - אף מסך אינו מגדיר סגנונות משל עצמו — הכל נגזר ממערכת העיצוב.
2. **ניווט ב-5 לשוניות (Bottom Navigation Bar):**
   - בית, חלומות, משימות, מחשבונים, עוד.
   - כפתור לכידה מהירה (`+` FAB) זמין וצף תמיד בכל מסך.
3. **ניהול עומס קוגניטיבי:**
   - פעולה עיקרית אחת לכל מסך.
   - חשיפה הדרגתית (Progressive Disclosure): פרטים מלאים נפתחים בלחיצה ולא מעמיסים על הרשימה הראשית.
4. **משוב וביטול פעולות:**
   - הודעת Snackbar עם כפתור "ביטול" (Undo) לכל פעולה הפיכה.
   - דיאלוג אישור (Confirmation Dialog) אך ורק לפעולות הרסניות (כגון מחיקה או שחזור דורס).
5. **4 מצבי מסך אחידים בכל פיצ'ר:**
   - טעינה (Loading), ריק (Empty), שגיאה (Error), ואופליין (Offline).
6. **RTL ונגישות:**
   - תמיכה מלאה ב-RTL (אייקוני כיוון הפוכים אוטומטית `AutoMirrored`, יישור מספרים וסמלי מטבע).
   - יעד מגע מינימלי של 48dp לכל אלמנט אינטראקטיבי.
   - תיאורי תוכן (`contentDescription`) מלאים לכל אייקון.
   - תמיכה בהגדלת גופנים מערכתית וניגודיות צבעים תקנית.

---

## 6. נתונים חיצוניים ממקורות רשמיים בלבד (External Verified Sources)
1. **מקורות רשמיים בלבד:** כל נתון אוטומטי חייב להגיע ממקור רשמי ומתועד. חל איסור מוחלט על גרידת אתרים (Web Scraping).
2. **ללא מפתחות מוטבעים באפליקציה:** אין לשמור API Keys בתוך קוד האפליקציה (מפתח ב-APK חשוף לחלוטין). שירותים הדורשים מפתח ימתינו לבקאנד. כל מקור נתונים חדש מוצע לאישור המשתמש לפני הוספתו.
3. **מטמון מקומי ב-DataStore:** כל נתון חיצוני נשמר ב-DataStore מקומי (ולא ב-Firestore).
4. **ייחוס מקור וזמן עדכון:** כל ערך אוטומטי מציג את מקורו ואת זמן העדכון האחרון. במקרה של כשל תקשורת, מוצג הערך השמור האחרון עם סימון "לא עודכן לאחרונה" — לעולם אין ליפול בשקט לערך מומצא או מקודד מראש.
5. **בנק ישראל (BOI SDMX CSV):**
   - כתובת מאומתת (נקראה ב-2026-10-27, HTTP 200, והחזירה נתונים): `https://edge.boi.org.il/FusionEdgeServer/sdmx/v2/data/dataflow/BOI.STATISTICS/EXR/1.0/RER_USD_ILS?c%5BDATA_TYPE%5D=OF00&lastNObservations=1&format=csv`. סדרה `RER_USD_ILS`, פילטר `DATA_TYPE=OF00`. השורה שנקראה: `TIME_PERIOD=2026-10-26`, `OBS_VALUE=3.078`. אותה קריאה אל `edge.boi.gov.il` החזירה את אותה שורה.
   - לוח פרסום מאומת מול `https://www.boi.org.il/roles/markets/reprate/`: חישוב פעם ביום, בימי העבודה במטבע חוץ בלבד. אין שערים יציגים בימי שבת וראשון ובימי שבתון. פרסום זמן קצר אחרי 15:15 (בימי ו', חול המועד וערבי חג — אחרי 12:15). המועד אינו מובטח. יום ו' הוא יום פרסום. חלון `startPeriod=2026-09-01&endPeriod=2026-10-26` על אותה סדרה החזיר 38 תצפיות, כולן ימי שני–שישי, ואף לא אחת ביום ראשון. לוח האפליקציה: ימי עסקים שני–שישי. שער שאוחזר לפני 15:30 (שני–חמישי) או לפני 12:30 (שישי) נחשב לא עדכני אחרי השעה הזו. הסנכרון רץ ב-15:45 בימים שני–חמישי וב-12:45 ביום שישי, ומדלג על שבת ועל ראשון. חגים וערבי חג אינם מטופלים (ראו `docs/BACKLOG_STEP8.md`). אם המידע אינו עדכני בעת פתיחת האפליקציה, מתבצע רענון.
   - דריסה ידנית אפשרית אך ורק כבחירה מפורשת של המשתמש ומסומנת בבירור (אינה נדרסת אוטומטית בעת סנכרון).
   - תמיכה בשליפת שער היסטורי לפי תאריך מבוקש (לצורך הפקדות והשקעות בעתיד).
6. **לוח עברי וזמני שבת (Hebcal REST API):**
   - כתובת מאומתת: `https://www.hebcal.com/hebcal?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=now&month=x&ss=on&mf=on&c=on&geo=geoname&geonameid={geonameid}`.
   - תאריך עברי, חגים וזמני כניסת/יציאת שבת לפי עיר הנבחרת פעם אחת בהגדרות (ללא בקשת הרשאת מיקום, ללא ברירת מחדל של ירושלים — מוצג "בחר עיר" עד לבחירת עיר).
   - הצגת קרדיט רשמי: "נתונים: Hebcal.com" (רישיון CC BY 4.0).
   - מטמון שבועי ב-DataStore.
   - בדיקות יחידה למודולים אלו משתמשות בדגימות תגובה מוקלטות (ללא קריאות רשת חיות בבדיקות).

---

## 7. בניית מקומית (Local build)
- **JDK:** OpenJDK 21.0.12.1 (המינימום של AGP 9.1 הוא 17). **Gradle:** 9.3.1 דרך ה-wrapper. **Android SDK:** `platforms;android-36.1` (תואם `compileSdk` 36 עם `minorApiLevel = 1`), Build-Tools `36.0.0` (ברירת המחדל של AGP 9.1; במכונה הזו הותקן גם `36.1.0`).
- **קבצים ששיבוט נקי צריך ואינם ב-git:** `app/google-services.json` (הפלאגין מייצר ממנו את `default_web_client_id`) ו-`debug.keystore` בשורש הפרויקט (ה-debug signing הקיים, `debugConfig`, לא משתנה).
- **כלל:** בנייה עם `google-services.json` מזויף מיועדת לבדיקות יחידה בלבד. אסור להתקין אותה על טלפון.

---

## 8. התקנה והפצה (Install and distribution)

דחיפה ל-`main` מפעילה את `.github/workflows/distribute.yml`, וגם הפעלה ידנית (workflow_dispatch). הודעת קומיט שמכילה `[skip ci]` מדלגת על הריצה. כך מגיעה גרסה לטלפונים, בלי Android Studio ובלי כבל:

1. אם סוד חסר, הריצה נעצרת מיד ומדפיסה את **שם** הסוד. אין build ואין העלאה.
2. `app/google-services.json` נכתב מהסוד `GOOGLE_SERVICES_JSON` — הקובץ האמיתי, לא הקובץ המזויף של בדיקות היחידה.
3. `./gradlew testDebugUnitTest`. כישלון בדיקות עוצר את ההפצה.
4. `./gradlew assembleRelease` בונה APK חתום. `versionCode` מגיע ממשתנה הסביבה `VERSION_CODE`; ב-CI זה `github.run_number`, כדי שכל בנייה תהיה חדשה והטלפון יקבל עדכון במקום.
5. Firebase CLI מעלה את `app/build/outputs/apk/release/app-release.apk` לקבוצת הבודקים שה-alias שלה `family`. הערות השחרור הן הודעת הקומיט.

**שמות הסודות** (Settings → Secrets and variables → Actions). הערכים אינם במסמך הזה ואינם ב-git:

- `KEYSTORE_BASE64` — ה-keystore של ה-release ב-base64
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD` — for PKCS12 the key password equals the store password (`KEY_PASSWORD` = `KEYSTORE_PASSWORD`)
- `GOOGLE_SERVICES_JSON` — JSON גולמי, לא base64
- `FIREBASE_SERVICE_ACCOUNT` — מפתח JSON של service account עם תפקיד Firebase App Distribution Admin
- `FIREBASE_APP_ID` — מזהה אפליקציית Android ב-Firebase (`1:…:android:…`)

ב-Firebase Console חייבת להיות קבוצת בודקים עם alias `family`, ובה מנש ואשתו.

תיעוד ששימש את הזרימה:

- `https://firebase.google.com/docs/app-distribution/android/distribute-cli` — `firebase appdistribution:distribute` עם `--app`, `--groups`, `--release-notes-file`
- `https://firebase.google.com/docs/app-distribution/authenticate-service-account` — `GOOGLE_APPLICATION_CREDENTIALS` הוא **נתיב** לקובץ ה-JSON, לא תוכן ה-JSON

טביעות האצבע הציבוריות של מפתח ה-release (להוספה באפליקציית Android ב-Firebase):

- SHA-1: `1B:A8:5C:43:31:B2:F0:83:88:E6:80:26:13:6A:88:FB:11:71:FF:A7`
- SHA-256: `B5:D3:22:D7:27:DB:1B:E9:BD:A4:FC:B6:93:9E:8E:80:6F:33:49:56:5D:B0:24:C9:8A:53:92:5D:62:E0:1E:22`

ה-keystore וסיסמאותיו **אינם בריפו**. הגיבוי שלהם נשמר מחוץ ל-git. איבוד ה-keystore אומר שאי אפשר לעדכן את האפליקציה המותקנת במקום, לעולם. בנייה מקומית נשארת על `debug.keystore` (gitignore) ועל `debugConfig`. אין סיסמת release בקוד.

---

## 9. Crashlytics (release only)

Crashlytics is a no-cost Firebase product on the Spark plan and on Blaze. No billing change and no plan upgrade.

- Pricing: `https://firebase.google.com/docs/projects/billing/firebase-pricing-plans` (Crashlytics is listed under no-cost products, including production use on Spark).
- Product page: `https://firebase.google.com/products/crashlytics` ("There's no cost to using Crashlytics.").
- SDK setup: `https://firebase.google.com/docs/crashlytics/android/get-started`. The SDK is `firebase-crashlytics` from the existing BoM (`firebaseBom` in `gradle/libs.versions.toml`), so it has no version of its own. The Gradle plugin is `com.google.firebase.crashlytics` `3.0.8` (`firebaseCrashlyticsGradle`).

Collection is on for release and off for debug:

- `app/src/release/AndroidManifest.xml` sets `firebase_crashlytics_collection_enabled` to `true`.
- `app/src/debug/AndroidManifest.xml` sets it to `false`.
- Doc: `https://firebase.google.com/docs/crashlytics/android/customize-crash-reports#enable-opt-in-reporting`.
- `CrashlyticsNonFatal.record` returns immediately when `BuildConfig.DEBUG` is true, so a debug non-fatal is not buffered and uploaded later by a release process. The SDK can still store automatic debug crashes locally while collection is disabled, and send that buffer if a later launch enables collection on the same install. Debug and release share application id `com.aistudio.mycalculators.qvxmzt`.

Non-fatals are recorded from `DefaultErrorReporter.reportError(action, throwable)` and from `FirestoreErrors.report`. Fatals are recorded by the Crashlytics handler that `FirebaseInitProvider` installs before `Application.onCreate` (`eagerInDefaultApp` on `CrashlyticsRegistrar`, then `CrashlyticsCore.onPreExecute`). `MyselfApp.onCreate` wraps that handler. Both paths pass `CrashlyticsNonFatal.sanitized`: the same exception class and stack frames, with the message removed at every cause and suppressed throwable. A `FirebaseFirestoreException` copy keeps `code.name` as its message (`NOT_FOUND`, and the other code names). That name is not personal data. A cycle in the cause chain is not followed again. If sanitizing throws, the wrapper still forwards a message-less `RuntimeException` that keeps the original stack, so Crashlytics still records the fatal. If no previous handler is installed, the wrapper calls `Process.killProcess` and `exitProcess(10)` instead of returning. The original throwable is written with `Log.e` only when `BuildConfig.DEBUG` is true. There are no custom keys. `lastTechnicalError` is unchanged and stays on the device.

Handler order: `https://github.com/firebase/firebase-android-sdk/blob/master/firebase-common/src/main/java/com/google/firebase/provider/FirebaseInitProvider.java` (`onCreate` is called before `Application.onCreate`), `https://github.com/firebase/firebase-android-sdk/blob/master/firebase-crashlytics/src/main/java/com/google/firebase/crashlytics/CrashlyticsRegistrar.java`, `https://github.com/firebase/firebase-android-sdk/blob/master/firebase-crashlytics/src/main/java/com/google/firebase/crashlytics/internal/common/CrashlyticsCore.java`, `https://developer.android.com/reference/android/app/Application#onCreate()`.

R8, minify, and mapping upload (`https://firebase.google.com/docs/crashlytics/android/get-deobfuscated-reports`):

- Release `isMinifyEnabled` is still `false`. R8 does not write `app/build/outputs/mapping/release/mapping.txt`, so there is nothing to deobfuscate and the upload is a no-op. Stack traces are readable without a mapping file.
- When minify is turned on, the Crashlytics Gradle plugin uploads that mapping file during the release build if `mappingFileUploadEnabled` is true. Release sets that flag only when `app/google-services.json` is present. Debug sets it to `false`.
- The real `google-services.json` has to be on disk before `assembleRelease`. CI already writes it from the `GOOGLE_SERVICES_JSON` secret before the build. Do not commit the file, and do not upload mappings with the unit-test fake: the upload is stored against the Firebase Android app id inside that json.
- The upload task needs outbound network from the build. No new GitHub secret is required while `GOOGLE_SERVICES_JSON` is the real Android app config. `.github/workflows` is intentionally unchanged.
- A checkout with no `google-services.json` still compiles: `missingGoogleServicesStrategy` is `WARN` and `googleServices.missing.passthrough=true`. Mapping upload stays off in that case.

Firebase console, for the existing Android app (the same one App Distribution uses):

- If the Crashlytics page asks to enable Crashlytics, enable it. The first report from a release build finishes setup. No test-crash button is shipped in the app.
- Do not add user ids or custom keys.
- Google Analytics is not a dependency of this change. Breadcrumb logs stay off until Analytics is added on purpose.
