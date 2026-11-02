#!/usr/bin/env python3
"""Audit methods removed by annotation filters; fail closed on ambiguous mappings."""
import collections
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

if len(sys.argv) != 3:
    sys.exit('usage: coverage-exclusions.py WITHOUT.xml WITH.xml')
root = pathlib.Path(__file__).resolve().parent.parent / 'app/src/main/java'

def methods(path):
    doc = ET.parse(path).getroot()
    return {(c.attrib['name'], m.attrib['name'], m.get('desc', ''), c.get('sourcefilename', ''))
            for c in doc.iter('class') for m in c.findall('method')}

def source_functions(path):
    text = path.read_text(encoding='utf-8')
    # Restrict annotation scan to the declaration immediately preceding the function.
    pattern = r'((?:@[\w.]+(?:\([^\n]*\))?\s*)+)(?:(?:private|public|internal|protected|inline|suspend|tailrec|operator|infix|override|open|final)\s+)*fun\s+(?:<[^>]+>\s*)?(?:[\w.<>?]+\.)?(`[^`]+`|\w+)\s*\('
    found = {m.group(2).strip('`') for m in re.finditer(pattern, text)
             if re.search(r'@(?:[\w.]+\.)?(?:Composable|Preview)\b', m.group(1))}
    # Kotlin property accessors can themselves carry @Composable.
    getter = r'\bval\s+(?:[\w.<>?]+\.)?(\w+)\s*:[^\n]+\n\s*((?:@[\w.]+(?:\([^\n]*\))?\s*)+)get\s*\('
    for m in re.finditer(getter, text):
        if re.search(r'@(?:[\w.]+\.)?(?:Composable|Preview)\b', m.group(2)):
            found.add('get' + m.group(1)[0].upper() + m.group(1)[1:])
    return found

removed = methods(sys.argv[1]) - methods(sys.argv[2])
counts = collections.Counter()
unmapped = []
groups = {1: [], 2: [], 4: []}
for cls, method, desc, filename in sorted(removed):
    path = root / cls.rsplit('/', 1)[0] / filename
    annotated = source_functions(path) if path.is_file() else set()
    pieces = cls.rsplit('/', 1)[-1].split('$')[1:] + [method.split('$')[0].split('-')[0]]
    candidates = [name for name in pieces if name in annotated]
    label = f'{cls}.{method}{desc}'
    if len(set(candidates)) != 1:
        unmapped.append((label, candidates, str(path)))
    else:
        counts[(str(path.relative_to(root)), candidates[0])] += 1
    money = 'com/menash/myself/feature/money/'
    if cls.startswith(money):
        tail = cls[len(money):]
        group = 1 if tail.startswith('domain/') else 2 if tail.startswith('importer/') else 4 if not tail.startswith('data/') else None
        if group:
            groups[group].append(label)
print(f'Total removed JVM methods: {len(removed)}')
print('Removed counts by enclosing annotated source function:')
for (file, function), count in sorted(counts.items()):
    print(f'{file}:{function}: {count}')
for group, entries in groups.items():
    print(f'G{group}: {len(entries)}')
    for entry in entries:
        print('  ' + entry)
print(f'Unmapped or ambiguous: {len(unmapped)}')
for entry, candidates, path in unmapped:
    print(f'  {entry} candidates={candidates} source={path}')
if unmapped:
    sys.exit(1)
