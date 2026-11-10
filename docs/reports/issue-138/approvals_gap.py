"""List approval lines that appear in B batch reports (#62-#70) but not in docs/combat-merge shared lists."""
import re
from pathlib import Path
r = Path.cwd()
cm = r / 'docs/combat-merge'
shared = {}
for f in cm.glob('*.txt'):
    for line in f.read_text(encoding='utf-8-sig').splitlines():
        k = line.split(' #')[0].strip()
        if k and not k.startswith('#'):
            shared.setdefault(' '.join(k.split()), []).append(f.name)
pat = re.compile(r'^(local/lito381/server/\S+(?:\s+[\w-]+(?:\s+\w+)?)?)\s+# approved: (https://\S+)')
for n in range(62, 71):
    p = r / f'docs/reports/issue-{n}.md'
    if not p.exists():
        continue
    for line in p.read_text(encoding='utf-8').splitlines():
        m = pat.match(line.strip().strip('`'))
        if m:
            key = ' '.join(m.group(1).split())
            if key not in shared:
                print(f'#{n} MISSING: {line.strip()}')
