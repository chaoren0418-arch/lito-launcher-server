"""List failing testcases from surefire XML reports: python sf_failures.py <surefire-dir>"""
import sys, re
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(sys.argv[1])
n = 0
for f in sorted(root.glob('TEST-*.xml')):
    t = ET.parse(f).getroot()
    for tc in t.iter('testcase'):
        for kind in ('failure', 'error'):
            e = tc.find(kind)
            if e is None:
                continue
            n += 1
            msg = (e.get('message') or '').replace('\n', ' ')[:300]
            body = e.text or ''
            at = [l.strip() for l in body.splitlines() if 'local.lito381.server' in l][:6]
            print(f"{tc.get('classname').split('.')[-1]}#{tc.get('name')} [{kind}] {e.get('type')}")
            print(f"    {msg}")
            for a in at:
                print(f"      {a[:180]}")
print('count', n)
