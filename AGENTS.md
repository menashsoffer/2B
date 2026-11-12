# הוראות לסוכנים

חוקי הפיתוח: [docs/RULES.md](docs/RULES.md). המבנה: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/STRUCTURE.md](docs/STRUCTURE.md).

## מפת הקוד (Graphify)
- להתמצאות כללית: [docs/graph/GRAPH_REPORT.md](docs/graph/GRAPH_REPORT.md) (בראשו ה-commit שממנו נבנה).
- לשאלות ממוקדות על הקוד: `pip install "graphifyy==0.9.107" && graphify update .` (כ-10 שניות, בלי API), ואז `graphify query "<שאלה>"`, `graphify affected "<סמל>"`, `graphify path "<A>" "<B>"`, `graphify explain "<סמל>"`.
- לפני שינוי בסמל מרכזי, להריץ `graphify affected` ולבדוק את מה שיוצא.
- אין לשמור את `graphify-out/` בריפו ואין לערוך ידנית את `docs/graph/GRAPH_REPORT.md`. ה-workflow `graphify.yml` מעדכן אותו אחרי מיזוג.
- אם `pip` חסום בסביבה, ממשיכים בלי הגרף.
