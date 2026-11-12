#!/usr/bin/env python3
"""Check that every covered class belongs to exactly one reporting group."""
import sys
import xml.etree.ElementTree as ET

if len(sys.argv) != 2:
    sys.exit('usage: coverage-groups.py REPORT.xml')
classes = [c.attrib['name'].replace('/', '.') for c in ET.parse(sys.argv[1]).iter('class')]
base = 'com.menash.myself.feature.'
money = base + 'money.'
def groups(name):
    domain = name.startswith(money + 'domain.')
    importer = name.startswith(money + 'importer.')
    data = name.startswith(money + 'data.')
    ui = name.startswith(base) and '.ui.' in name[len(base):]
    presentation = name.startswith(money) and not (domain or importer or data or ui)
    other = name.startswith(base) and not name.startswith(money) and not ui
    rest = not name.startswith(base) or ui
    return [i for i, match in enumerate((domain, importer, data, presentation, other, rest), 1) if match]
counts = [0] * 6
unassigned, overlaps = [], []
for name in classes:
    matches = groups(name)
    if not matches:
        unassigned.append(name)
    if len(matches) > 1:
        overlaps.append(name)
    for group in matches:
        counts[group - 1] += 1
for i, count in enumerate(counts, 1):
    print(f'G{i}: {count}')
print(f'Total: {len(classes)}; unassigned: {len(unassigned)}; overlapping: {len(overlaps)}')
if unassigned or overlaps:
    print('Unassigned:', *unassigned, sep='\n')
    print('Overlapping:', *overlaps, sep='\n')
    sys.exit(1)
