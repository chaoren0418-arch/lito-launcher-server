"""Static check: for a source-audit test, list string literals passed to contains()/indexOf() that are absent
from the concatenation of src/main sources the test reads (approximation: all of src/main)."""
import re, sys
from pathlib import Path
root = Path(__file__).resolve().parents[2] / 'lito伺服器/LiTo381-Server'
main = {p.name: p.read_text(encoding='utf-8', errors='replace') for p in (root / 'src/main/java/local/lito381/server').glob('*.java')}
for name in sys.argv[1:]:
    t = (root / f'src/test/java/local/lito381/server/{name}.java').read_text(encoding='utf-8')
    files = set(re.findall(r'(\w+)\.java"', t)) - {name}
    text = ''.join(main.get(f + '.java', '') for f in files)
    print('==', name, 'reads', sorted(files))
    for m in re.finditer(r'\.(?:contains|indexOf)\(\s*((?:"(?:[^"\\]|\\.)*"\s*\+?\s*)+)', t):
        lits = re.findall(r'"((?:[^"\\]|\\.)*)"', m.group(1))
        s = ''.join(lits).encode().decode('unicode_escape').encode('latin1').decode('utf-8', 'replace')
        if s and s not in text:
            print('   MISSING:', s[:150])
