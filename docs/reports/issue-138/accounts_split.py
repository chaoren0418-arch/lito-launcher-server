"""#138: LegacyMySqlRepository moved out of Accounts.java; let named source audits read both files."""
import re, sys
from pathlib import Path
root = Path(__file__).resolve().parents[2] / 'lito伺服器/LiTo381-Server/src/test/java/local/lito381/server'
for name in sys.argv[1:]:
    p = root / f'{name}.java'
    t = p.read_text(encoding='utf-8')
    out, pos, n = [], 0, 0
    for m in re.finditer(r'(Files|LocalResources)\.readString\(\s*Path\.of\([^;]*?Accounts\.java"\)\s*,\s*StandardCharsets\.UTF_8\)', t, re.S):
        reader = m.group(1)
        out.append(t[pos:m.end()])
        out.append('\n                // #138 B-land: LegacyMySqlRepository now lives in its own file.\n'
                   f'                + {reader}.readString(Path.of(\n'
                   '                        "src/main/java/local/lito381/server/LegacyMySqlRepository.java"),\n'
                   '                        StandardCharsets.UTF_8)')
        pos = m.end(); n += 1
    out.append(t[pos:])
    p.write_text(''.join(out), encoding='utf-8', newline='')
    print(name, n)
