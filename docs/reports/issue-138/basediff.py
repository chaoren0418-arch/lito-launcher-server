"""Per-method status of the B-land Maven JAR vs reference/base-0.6.71.jar (from r2/full.json).

Usage: py -I scratch/issue-138/basediff.py <SimpleClassName> [method-substring] [--all]
  Without --all only non-SAME methods are printed. Class status ONLY_LEFT = not in 0.6.71 at all.
"""
import json, sys
from pathlib import Path

d = json.loads((Path(__file__).parent / 'r2/full.json').read_text(encoding='utf8'))
cls, sub = sys.argv[1], (sys.argv[2] if len(sys.argv) > 2 and not sys.argv[2].startswith('--') else '')
show_all = '--all' in sys.argv
for c in d['classes']:
    if c['name'].split('/')[-1] != cls:
        continue
    print(c['name'], c['status'])
    for m in c.get('methodResults') or []:
        if sub and sub not in m['method']:
            continue
        if show_all or m['status'] not in ('SAME',):
            print(f"  {m['status']:<14} {m['method']}  {m.get('detail','')[:160]}")
