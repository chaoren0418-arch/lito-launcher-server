"""Extract per-test failure headline (first 2 lines after '<<< FAILURE!/ERROR!') from a GitHub Actions mvn log."""
import re, sys
from pathlib import Path
lines = Path(sys.argv[1]).read_text(encoding='utf-8-sig', errors='replace').splitlines()
strip = re.compile(r'^.*?\d{4}-\d\d-\d\dT[\d:.]+Z ')
lines = [strip.sub('', l) for l in lines]
out = []
for i, l in enumerate(lines):
    m = re.match(r'\[ERROR\] local\.lito381\.server\.(\S+) -- Time elapsed: .* <<< (FAILURE|ERROR)!', l)
    if m:
        msg = lines[i + 1][:400] if i + 1 < len(lines) else ''
        at = next((x.strip() for x in lines[i + 2:i + 40] if 'local.lito381.server' in x and 'Test' not in x.split('(')[0].split('.')[-2:][0]), '')
        tat = next((x.strip() for x in lines[i + 2:i + 60] if re.search(r'Test\.java:\d+', x)), '')
        out.append(f'{m.group(1)} [{m.group(2)}]\n    {msg}\n    main@ {at[:200]}\n    test@ {tat[:200]}')
print('\n'.join(out))
print('count', len(out))
