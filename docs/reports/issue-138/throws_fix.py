"""Given a javac log, for each 'unreported exception java.io.IOException' error, add `throws IOException`
to the enclosing method declaration (only when the error is not inside a lambda). Prints every change."""
import re, sys
from pathlib import Path
log = Path(sys.argv[1]).read_text(encoding='utf-8', errors='replace')
errs = sorted(set(re.findall(r'\[ERROR\] /(.+?\.java):\[(\d+),(\d+)\] unreported exception java\.io\.IOException', log)))
decl = re.compile(r'^\s*(?:(?:public|protected|private|static|final|synchronized)\s+)*[\w<>\[\],.? ]+\s+(\w+)\s*\(')
changed = []
for path, line, col in errs:
    p = Path(path)
    lines = p.read_text(encoding='utf-8').split('\n')
    i = int(line) - 1
    j = i
    while j >= 0:
        m = decl.match(lines[j])
        if m and m.group(1) not in ('if', 'for', 'while', 'switch', 'return', 'new', 'catch', 'synchronized'):
            break
        j -= 1
    head = lines[j]
    # find the ')' that closes the parameter list, then '{'
    k = head.find('{', head.find('('))
    if k < 0 or 'throws' in head[:k]:
        print('MANUAL', p.name, line, '|', head.strip()[:160]); continue
    seg = head[:k].rstrip()
    if not seg.endswith(')'):
        print('MANUAL', p.name, line, '|', head.strip()[:160]); continue
    # lambda check: text between decl and error must not contain '->' opened on the error line before col
    if '->' in lines[i][:int(col)] or (j != i and any('->' in l for l in lines[j + 1:i + 1])):
        print('LAMBDA?', p.name, line, 'method', decl.match(head).group(1))
    full = '\n'.join(lines)
    name = 'IOException' if re.search(r'import\s+java\.io\.(IOException|\*)\s*;', full) else 'java.io.IOException'
    lines[j] = seg + ' throws ' + name + head[len(seg):]
    p.write_text('\n'.join(lines), encoding='utf-8', newline='')
    changed.append((p.name, j + 1, decl.match(head).group(1)))
    print('THROWS', p.name, j + 1, decl.match(head).group(1))
