"""Builds the synthetic statement files used by the importer tests.

Every name, number and amount here is made up. Layouts follow
docs/money/IMPORT.md (headers, title rows, footers) as checked against real exports.
Run: python3 tools/import-fixtures/make_fixtures.py  (needs openpyxl and LibreOffice for .xls)
"""
import datetime as dt
import os
import shutil
import subprocess
import sys
import tempfile

from openpyxl import Workbook

OUT = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "test", "resources", "import")
D = dt.datetime


def save(wb, name):
    os.makedirs(OUT, exist_ok=True)
    wb.save(os.path.join(OUT, name))


def bank_rows(start, rows):
    """rows: (date, operation, details, ref, debit, credit, beneficiary); adds the running balance."""
    out, balance = [], start
    for date, op, details, ref, debit, credit, beneficiary in rows:
        balance = round(balance - (debit or 0) + (credit or 0), 2)
        out.append([date, op, details, ref, debit, credit, balance, date, beneficiary, None])
    return out


HAPOALIM_HEADERS = ["תאריך", "הפעולה", "פרטים", "אסמכתא", "חובה", "זכות", 'יתרה בש"ח', "תאריך ערך", "לטובת", "עבור"]
OCT = [
    (D(2026, 10, 1), "משכורת", "מזהה 012345678", 11001, None, 15000, None),
    (D(2026, 10, 2), "מקס איט פיננסי", None, 11002, 632.30, None, None),
    (D(2026, 10, 5), "משיכה מבנקט", "סניף 612", 11003, 500, None, None),
    (D(2026, 10, 7), "העברה ב-bit", None, 11004, 120, None, "דנה לוי"),
    (D(2026, 10, 10), "הוראת קבע", "ועד בית", 11005, 250, None, None),
    (D(2026, 10, 12), "ישראכרט", None, 11006, 800, None, None),
    (D(2026, 10, 15), "העברה לחשבון אחר", "חשבון 987654321", 11007, 2000, None, "וואן זירו"),
    (D(2026, 10, 15), "ריבית", None, 11008, 3.20, None, None),
    (D(2026, 10, 20), "ביטוח לאומי", "קצבת ילדים", 11009, None, 173, None),
]
LATE = [
    (D(2026, 10, 22), "שופרסל", None, 11010, 340, None, None),
    (D(2026, 10, 25), "משכורת", "מזהה 012345678", 11011, None, 15000, None),
]


def hapoalim(name, rows, start=5000.0, extra=None):
    wb = Workbook()
    ws = wb.active
    ws.title = "תנועות"
    ws.append([])
    ws.append([])
    ws.append(["תנועות בחשבון"])
    ws.append(["מספר חשבון: 12-345-678901", None, "תאריך הפקה: 29/10/2026"])
    ws.append(HAPOALIM_HEADERS)
    for r in bank_rows(start, rows):
        ws.append(r)
    for r in extra or []:
        ws.append(r)
    wb.create_sheet("גיליון2")
    wb.create_sheet("גיליון3")
    save(wb, name)


def balance_after(start, rows):
    return bank_rows(start, rows)[-1][6]


MAX_HEADERS = ["תאריך עסקה", "שם בית העסק", "קטגוריה", "4 ספרות אחרונות של כרטיס האשראי", "סוג עסקה",
               "סכום חיוב", "מטבע חיוב", "סכום עסקה מקורי", "מטבע עסקה מקורי", "תאריך חיוב", "הערות", "תיוגים",
               "מועדון הנחות", "מפתח דיסקונט", "אופן ביצוע ההעסקה", 'שער המרה ממטבע מקור/התחשבנות לש"ח']


def max_row(date, merchant, category, kind, charge, original=None, currency="₪", notes=None, charge_date="02-11-2026", last4="1234"):
    return [date, merchant, category, last4, kind, charge, "₪", original if original is not None else charge, currency,
            charge_date, notes, None, None, None, "טלפוני", None]


MAX_ROWS = [
    max_row("05-10-2026", "שופרסל דיל 123", "מזון וצריכה", "רגילה", 254.30),
    max_row("06-10-2026", "קפה גרג", "מסעדות, קפה וברים", "רגילה", 12),
    max_row("06-10-2026", "קפה גרג", "מסעדות, קפה וברים", "רגילה", 12),
    max_row("08-10-2026", "AMAZON MKTPLACE", "אופנה", "רגילה", 84.10, 22.50, "$"),
    max_row("10-10-2026", "ספוטיפיי", "פנאי, בידור וספורט", "הוראת קבע", 19.90),
    max_row("12-10-2026", "מחסני חשמל", "חשמל ומחשבים", "תשלומים", 400, 1200, "₪", "תשלום 2 מתוך 3"),
    max_row("14-10-2026", "זארה", "אופנה", "זיכוי", 150),
]


def max_file(name, rows, total_text):
    wb = Workbook()
    ws = wb.active
    ws.title = "עסקאות במועד החיוב"
    ws.append(["רות כהן", None, "ת.ז. 012345678"])
    ws.append(["כרטיס זהב - 1234"])
    ws.append(["11/2026"])
    ws.append(MAX_HEADERS)
    for r in rows:
        ws.append(r)
    ws.append([])
    ws.append(["סך הכל"])
    ws.append([total_text])
    save(wb, name)


CAL_HEADERS = ["תאריך\nעסקה", "שם בית עסק", 'סכום\nבש"ח', "מועד\nחיוב", "סוג\nעסקה", "מזהה כרטיס\nבארנק דיגיטלי", "הערות"]
NOV10 = D(2026, 11, 10)


def cal_file(name, rows, total):
    wb = Workbook()
    ws = wb.active
    ws.title = "פירוט עסקאות וזיכויים"
    ws.append(["פירוט עסקאות לכרטיס המסתיים ב-5678 של ישראל ישראלי, בנק הפועלים"])
    ws.append(CAL_HEADERS)
    for r in rows:
        ws.append(r)
    ws.append(['סה"כ עסקאות בגיליון זה', None, total, None, None, None, 'הסכום לא כולל עסקאות במט"ח ועסקאות בקליטה'])
    ws.append(["המידע נכון לתאריך הפקת הקובץ"])
    path = os.path.join(OUT, name)
    wb.save(path)


CAL_OCT = [
    [D(2026, 10, 3), "רמי לוי", 412.60, NOV10, "רגילה", None, None],
    [D(2026, 10, 4), "פז דלק", 200, None, "רגילה", None, "עסקה בקליטה"],
    [D(2026, 10, 5), "NETFLIX.COM", None, NOV10, "רגילה", None, "סכום העסקה הוא 15.49 $"],
    [D(2026, 10, 6), "עיריית תל אביב", 380, NOV10, "הוראת קבע", None, None],
    [D(2026, 10, 7), "דמי כרטיס", 9.90, NOV10, "דמי חבר", None, None],
    [D(2026, 10, 8), "שופרסל", 60, NOV10, "עסקת זיכוי", None, None],
]
CAL_NEXT = [
    [D(2026, 10, 3), "רמי לוי", 412.60, NOV10, "רגילה", None, None],
    [D(2026, 10, 4), "פז דלק", 212.40, NOV10, "רגילה", None, None],
    [D(2026, 10, 5), "NETFLIX.COM", 57.80, NOV10, "רגילה", None, "סכום העסקה הוא 15.49 $"],
    [D(2026, 10, 12), "סופר פארם", 89.90, NOV10, "רגילה", None, None],
]


def reversed_hebrew(text):
    return "‭" + text[::-1] + "‬"


OZ_HEADERS = ["תאריך תנועה", "תאריך ערך", "סוג פעולה", "תיאור", "סכום פעולה", "מטבע", "חיוב/זיכוי", "יתרה", "אסמכתא"]


def oz_file(name, currency, rows, start):
    wb = Workbook()
    ws = wb.active
    ws.title = "2026-10-01_2026-10-31_ALL"
    ws.append(OZ_HEADERS)
    balance = start
    for when, kind, desc, amount, ref in rows:
        balance = round(balance + amount, 2)
        ws.append([when, when.replace(hour=0, minute=0), kind, desc, amount, currency,
                   "חיוב" if amount < 0 else "זיכוי", balance, ref])
    tmp = tempfile.mkdtemp()
    src = os.path.join(tmp, name.replace(".xls", ".xlsx"))
    wb.save(src)
    subprocess.run(["soffice", "--headless", "--convert-to", "xls:MS Excel 97", "--outdir", tmp, src],
                   check=True, capture_output=True)
    shutil.copy(os.path.join(tmp, name), os.path.join(OUT, name))
    shutil.rmtree(tmp)


OZ_ILS = [
    (D(2026, 10, 1, 9, 15), "העברות", reversed_hebrew("העברה מחשבון"), 2000, "OZ-1001"),
    (D(2026, 10, 3, 16, 40), "ניירות ערך", "VOO " + reversed_hebrew("קניה"), -1500, "OZ-1002"),
    (D(2026, 10, 5, 10, 0), "המרת מט״ח", reversed_hebrew("המרה לדולר"), -300, "OZ-1003"),
    (D(2026, 10, 31, 23, 0), "אחר", reversed_hebrew("ריבית רבעונית"), 1.25, "OZ-1004"),
    (D(2026, 10, 31, 23, 5), "אחר", reversed_hebrew("דמי מנוי"), -9.90, "OZ-1005"),
]
OZ_USD = [
    (D(2026, 10, 6, 17, 30), "ניירות ערך", "VOO " + reversed_hebrew("קניה"), -100, "OZ-2001"),
    (D(2026, 10, 31, 23, 0), "אחר", reversed_hebrew("עמלת משמורת"), -1, "OZ-2002"),
]


def encrypted_copy(source, name):
    """Marks a real .xls as password protected: its CODEPAGE record becomes FILEPASS (same length)."""
    data = bytearray(open(os.path.join(OUT, source), "rb").read())
    at = data.find(bytes([0x42, 0x00, 0x02, 0x00]))
    data[at:at + 2] = bytes([0x2F, 0x00])
    open(os.path.join(OUT, name), "wb").write(bytes(data))


def main():
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(OUT)
    hapoalim("hapoalim_oct.xlsx", OCT)
    hapoalim("hapoalim_overlap.xlsx", OCT[5:] + LATE, start=balance_after(5000.0, OCT[:5]))
    hapoalim("hapoalim_broken.xlsx", OCT[:3], extra=[
        ["לא תאריך", "שורה שבורה", None, 1, 10, None, None, None, None, None],
    ] + [r for r in bank_rows(balance_after(5000.0, OCT[:4]), OCT[4:])])
    max_file("max_nov.xlsx", MAX_ROWS, "₪632.30")
    max_file("max_mismatch.xlsx", MAX_ROWS, "₪700.00")
    max_file("max_empty.xlsx", [], "₪0.00")
    cal_file("cal_nov.xlsm", CAL_OCT, 742.50)
    cal_file("cal_next.xlsm", CAL_NEXT, 714.90)
    oz_file("onezero_ils.xls", "ILS", OZ_ILS, 10000)
    oz_file("onezero_usd.xls", "USD", OZ_USD, 500)
    encrypted_copy("onezero_ils.xls", "encrypted.xls")
    wb = Workbook()
    wb.active.append(["שם", "טלפון", "כתובת"])
    wb.active.append(["משהו", "050", "רחוב"])
    save(wb, "unknown.xlsx")
    open(os.path.join(OUT, "not_excel.xlsx"), "wb").write(b"<html><body>not a spreadsheet</body></html>")
    open(os.path.join(OUT, "corrupt.xlsx"), "wb").write(b"PK\x03\x04" + b"\x00" * 64)


if __name__ == "__main__":
    sys.exit(main())
