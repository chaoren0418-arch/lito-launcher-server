"""B-land: copy staged B sources into src/main; split LegacyMySqlRepository out of Accounts.java."""
import shutil
from pathlib import Path
root = Path(__file__).resolve().parents[2]
srv = root / 'lito伺服器/LiTo381-Server'
st = srv / 'merge-inputs/50635f7e/staged/local/lito381/server'
dst = srv / 'src/main/java/local/lito381/server'

acc = dst / 'Accounts.java'
data = acc.read_bytes()
marker = b'\nfinal class LegacyMySqlRepository implements AccountRepository {'
i = data.index(marker)
assert data.count(marker) == 1
head = data[:i].rstrip(b'\r\n') + (b'\r\n' if b'\r\n' in data[:i] else b'\n')
acc.write_bytes(head)
print('Accounts.java truncated', len(data), '->', len(head))

n_over = n_new = 0
for p in sorted(st.glob('*.java')):
    t = dst / p.name
    if t.exists():
        n_over += 1
    else:
        n_new += 1
    shutil.copyfile(p, t)
print('overwritten', n_over, 'new', n_new)
