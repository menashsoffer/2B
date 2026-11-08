"""Builds app/src/test/resources/import/paamonim_plan.xls from a real Paamonim רישומון.

The real file is the input only and never enters the repository. The output keeps the
layout of the two sheets the plan import reads ("שיקוף" and "רשימת תקציבים"): cell
positions, the Paamonim line names and the template labels. Everything personal is replaced:
  - every amount is a random number (fixed seed); no amount equals the original at its cell
  - names of people and creditors, and custom line names, are replaced with generic labels
  - formulas are stored as their values; totals are recomputed from the fake numbers
Small integers (0..36) and the year are kept: they are flags, counts and row indexes.
Run: python3 tools/import-fixtures/make_paamonim_fixture.py <real-file.xls> <names.json>  (needs xlrd 2 and xlwt)
"""
import json, os, random, sys
import xlrd, xlwt
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "test", "resources", "import", "paamonim_plan.xls")
random.seed(2025)
src = xlrd.open_workbook(sys.argv[1])
# Personal text to replace (people, creditors, custom line names) comes from a local JSON map
# {"original": "replacement"} that stays outside the repository, like the source file.
REPL = json.load(open(sys.argv[2], encoding="utf-8"))
def fake(lo, hi, step=10): return float(random.randrange(lo // step, hi // step) * step)
def read(name):
    s = src.sheet_by_name(name)
    g = {}
    for r in range(s.nrows):
        for c in range(s.ncols):
            t, v = s.cell_type(r, c), s.cell_value(r, c)
            if t == xlrd.XL_CELL_TEXT and v.strip(): g[(r, c)] = REPL.get(v.strip(), v)
            elif t == xlrd.XL_CELL_NUMBER:
                keep = v == 0 or v == 2025 or (float(v).is_integer() and 0 < abs(v) <= 36)
                f = fake(50, 900)
                while abs(f - v) < 0.01: f += 10
                g[(r, c)] = float(v) if keep else f
    return g
B, H = read("רשימת תקציבים"), read("שיקוף")
num = lambda g, r, c: g.get((r, c)) if isinstance(g.get((r, c)), float) else 0.0
# One fake budget per line, shared by both sheets so they agree.
budget = {}
for r in range(2, 47):
    name = B.get((r, 1))
    if isinstance(name, str) and num(B, r, 2): budget[name] = B[(r, 2)]
income = {}
for r in range(49, 60):
    name = B.get((r, 1))
    if isinstance(name, str) and num(B, r, 2):
        v = fake(6000, 14000, 100)
        while abs(v - src.sheet_by_name('רשימת תקציבים').cell_value(r, 2)) < 0.01: v += 100
        income[name] = B[(r, 2)] = v
for r in range(4, 50):
    if H.get((r, 3)) in budget: H[(r, 6)] = budget[H[(r, 3)]]
    if H.get((r, 8)) in income:
        H[(r, 10)] = H[(r, 11)] = income[H[(r, 8)]]
debt_monthly = num(H, 29, 10)
for c in range(2, 14):  # totals in the budget list, months C..N
    exp = sum(num(B, r, c) for r in range(2, 47)); inc = sum(num(B, r, c) for r in range(49, 60))
    B[(47, c)], B[(60, c)], B[(62, c)] = exp, inc, inc - exp
    if c == 2: B[(63, c)] = debt_monthly
    B[(64, c)] = inc - exp - num(B, 63, c)
sumcol = lambda c, r0, r1: sum(num(H, r, c) for r in range(r0, r1 + 1))
H[(15, 10)], H[(15, 11)] = sumcol(10, 4, 14), sumcol(11, 4, 14)
H[(49, 5)], H[(49, 6)] = sumcol(5, 4, 48), sumcol(6, 4, 48)
for row, (r0, r1) in {21: (4, 11), 22: (15, 34), 23: (37, 48)}.items():
    H[(row, 9)], H[(row, 10)] = sumcol(5, r0, r1), sumcol(6, r0, r1)
H[(24, 9)], H[(24, 10)] = H[(49, 5)], H[(49, 6)]
H[(30, 9)] = num(H, 28, 9) + num(H, 29, 9)
H[(35, 8)], H[(35, 9)] = H[(15, 10)], H[(49, 5)]
H[(35, 10)] = H[(35, 8)] - H[(35, 9)]; H[(35, 11)] = H[(35, 10)] - debt_monthly
H[(36, 8)] = H[(35, 11)] * 12
out = xlwt.Workbook(encoding="utf-8")
for name, g in (("שיקוף", H), ("רשימת תקציבים", B)):
    ws = out.add_sheet(name, cell_overwrite_ok=True)
    for (r, c), v in sorted(g.items()): ws.write(r, c, v)
out.save(OUT)
print("written", OUT)
