# בדיקות קצה לקצה (E2E) — Maestro

בדיקות שמריצות את האפליקציה האמיתית על אמולטור Android, כמו משתמש: מתחברים, פותחים את טאב הכסף, משתפים קובץ בנק.

## איך זה בנוי
- **build type `e2e`** (`app/build.gradle.kts`): עותק של debug עם `applicationIdSuffix = ".e2e"` ו-`FIREBASE_EMULATOR_HOST = "10.0.2.2"`.
  ב-debug וב-release הערך ריק, ולכן הן מדברות עם פרויקט Firebase האמיתי כמו קודם.
- **חיבור לאמולטורים**: `MyselfApp` (Auth, פורט 9099) ו-`FirestoreManager` (Firestore, פורט 8085, כמו `firebase.json`).
- **`app/src/e2e`** (קיים רק ב-build הזה):
  - `E2eActivity`: נפתחת ע"י `myself-e2e://signin?email=...&share=...`. יוצרת/מחברת משתמש בדיקה באימייל+סיסמה מול אמולטור ה-Auth
    (אין Google Sign-In בבדיקות), ועם `share` משתפת קובץ לדוגמה מ-`assets/e2e/` דרך אותו ACTION_SEND ששיתוף אמיתי עובר.
  - הרשאת http לאמולטורים (network security config).
  - `assets/e2e/hapoalim_oct.xlsx`: קובץ סינתטי (אותו קובץ כמו בטסטים של הייבוא), בלי נתונים אמיתיים.
- **testTags**: ב-build עם `DEBUG` (debug ו-e2e) ה-testTags של Compose נחשפים כ-resource id, ו-Maestro מוצא אותם ב-`id:`.
- **תרחישים**: `e2e/maestro/*.yaml`. כל תרחיש מתחיל ב-`clearState` ומשתמש חדש (אימייל עם חותמת זמן), כך שהם לא תלויים זה בזה.

## הגנות
- release לא מכיל שום דבר מ-`src/e2e`. ה-workflow בודק את זה בכל ריצה (manifest ממוזג + `FIREBASE_EMULATOR_HOST` ריק ב-BuildConfig של release).
- הסיסמה והמשתמשים קיימים רק באמולטור המקומי.

## CI (`.github/workflows/e2e.yml`)
- PR ל-main שנוגע בקוד האפליקציה: תרחישים עם tag `smoke`.
- כל לילה וריצה ידנית (בלי tags): כל התרחישים.
- תוצרים: `e2e-report.xml`, צילומי מסך, logcat ולוג האמולטורים, ב-artifact `e2e-output`.

## הרצה מקומית
```
firebase emulators:start --only auth,firestore --project demo-no-project
./gradlew installE2e            # אמולטור Android פתוח (10.0.2.2 = המחשב)
maestro test e2e/maestro
```
`app/google-services.json` מקומי צריך client גם ל-`com.aistudio.mycalculators.qvxmzt.e2e` (ה-workflow מייצר קובץ מזויף כזה).

## תרחיש חדש
קובץ YAML ב-`e2e/maestro/`, מתחיל ב-`launchApp: clearState: true` ו-`openLink: myself-e2e://signin?email=...`.
מחפשים לפי `id:` (testTag) ולא לפי טקסט כשאפשר. tag `smoke` רק לתרחישים קצרים וקריטיים.

## סיפורים בדיוניים

אפשר להוסיף לקישור `seed=<name>&today=2026-11-03`. אחרי יצירת משק הבית נכתבים נתוני הסיפור דרך מאגרי האפליקציה. מזהים קבועים מאפשרים לנסות שוב אם הזריעה נקטעה; הכנסת ההכנסה מסמנת זריעה שהושלמה.

| שם | נתונים |
| --- | --- |
| `plan_only` | הכנסה 20,000, קבועות 6,000, שוטפות 5,000 ותקופתיות 1,200 לשנה; ללא תנועות |
| `mid_month` | התוכנית ותנועות שוטפות 3,200 והכנסה 20,000 בחודש הנוכחי |
| `ready_to_close` | התוכנית ותנועות בחודש הקודם, כולל חריגה בתקציב |
| `history` | תנועות בששת החודשים הקודמים, ארבעה חודשים סגורים |
| `no_current_budget` | ללא תקציב שוטף; תנועות 1,012 בקבועות ו-6,120 ללא קטגוריה |
