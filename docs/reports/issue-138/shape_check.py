"""Compare non-accepted ClientSession/Protocol/GameHub method results with docs/combat-merge/b09-shape-diffs.md."""
import json, re, sys
from pathlib import Path
r = Path.cwd()
res = json.loads((r / 'scratch/issue-138' / sys.argv[1] / 'b-approved.json').read_text(encoding='utf8'))
doc = (r / 'docs/combat-merge/b09-shape-diffs.md').read_text(encoding='utf8')
listed = set(re.findall(r'^### `([^`]+)`', doc, re.M))
listed |= {m for m in re.findall(r'^\| `(lambda\$[^`]+)`', doc, re.M)}
listed |= {'sourceApplicationRun()V'}
ok = re.compile(r'^(SAME|DEVIATION|ORPHAN|LAMBDA|VISIBILITY)')
for c in res['classes']:
    if c['status'] not in ('DIFF', 'ONLY_LEFT', 'ONLY_RIGHT'):
        continue
    bad = [m for m in c.get('methodResults', []) if not ok.match(m['status'])]
    names = {m['method'] for m in bad}
    print(c['name'], c['status'], 'bad', len(bad),
          'in-shape-list', len(names & listed), 'NOT-in-list', sorted(names - listed)[:20])
print('shape-list entries', len(listed))
