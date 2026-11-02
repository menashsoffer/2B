#!/usr/bin/env python3
"""List methods removed by Kover annotation filters (unfiltered XML first)."""
import sys
import xml.etree.ElementTree as ET


def methods(path):
    root = ET.parse(path).getroot()
    return {(c.attrib['name'], m.attrib['name'], m.get('desc', ''))
            for c in root.iter('class') for m in c.findall('method')}


if len(sys.argv) != 3:
    sys.exit('usage: coverage-exclusions.py WITHOUT.xml WITH.xml')
before, after = map(methods, sys.argv[1:])
for cls, name, desc in sorted(before - after):
    print(f'{cls}.{name}{desc}')
print(f'Removed methods: {len(before - after)}')
