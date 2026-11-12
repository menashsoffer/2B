# מפת הקוד (Graphify)

[Graphify](https://github.com/Graphify-Labs/graphify) קורא את הקוד (Kotlin, JS, Python) ובונה ממנו גרף: מחלקות, פונקציות, קבצים, מי קורא למי ומה תלוי במה. הבנייה מקומית (tree-sitter), בלי LLM ובלי מפתח API.

## מה יש בריפו
- [GRAPH_REPORT.md](GRAPH_REPORT.md): הדוח. צמתים מרכזיים (מה שהכול עובר דרכו), קהילות (אשכולות קוד שקשורים זה לזה), קשרים מפתיעים, ושאלות מוצעות. בראש הדוח רשום מאיזה commit הוא נבנה.
- `.github/workflows/graphify.yml`: אחרי כל מיזוג קוד ל-main בונה את הגרף מחדש, מעדכן את הדוח כאן ב-commit של הבוט (`docs: refresh code graph report [skip ci]`), ומעלה את `graph.json` ו-`graph.html` כ-artifact של הריצה (30 יום). אפשר גם להריץ ידנית (Run workflow).

`graphify-out/` עצמה לא נשמרת בריפו (`.gitignore`): `graph.json` שוקל כ-11MB ונכתב מחדש בכל בנייה.

## שימוש מקומי
```bash
pip install "graphifyy==0.9.107"   # פייתון 3.10+
graphify update .                   # כ-10 שניות, יוצר graphify-out/
graphify query "איך נסגר חודש?"     # תת-גרף ממוקד לשאלה
graphify affected "MoneyViewModel"  # מה יושפע משינוי
graphify path "ImportViewModel" "FirestoreMoneyRepository"
graphify explain "closeChecks"
```
לפתוח את `graphify-out/graph.html` בדפדפן לגרף אינטראקטיבי. הגרף גדול מ-5,000 צמתים, ולכן ה-HTML מציג קהילות ולא כל צומת.

## מגבלות ידועות
- קבצי XML (משאבים, מניפסט) ו-Gradle לא מנותחים.
- המנתח של tree-sitter ל-Kotlin לא מכיר חלק מהתחביר החדש, ובכ-10 קבצים הוא מדלג על אזור (כתוב בפלט כ-"syntax errors"). הקוד מתקמפל כרגיל. הקבצים האלה מופיעים בגרף חלקית.
- שמות הקהילות הם שם הצומת המרכזי בכל אחת (בלי LLM).
- הגרף מתאר מבנה. הוא לא מחליף קריאה של הקוד לפני שינוי.
