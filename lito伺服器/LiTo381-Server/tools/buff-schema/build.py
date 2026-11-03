from pathlib import Path
import hashlib, json, shutil, subprocess, zipfile

R = Path(__file__).resolve().parent
S = R.parents[1]
L = S.parent.parent / 'lito登入器/LiTo381-Launcher'
B = R / 'build'
C = B / 'classes'
C.mkdir(parents=True, exist_ok=True)
J = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
base = R / 'inputs/formal-0.6.68.jar'
expected = 'c02aaae8d54a27e30460be2e3c47709ee64d35f007a8be27ac74a327268b9539'
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
if not base.exists():
    formal = L / 'app/server/LiTo381-SharedResources-candidate.jar'
    assert sha(formal) == expected
    base.parent.mkdir(exist_ok=True)
    shutil.copyfile(formal, base)
assert sha(base) == expected
asm = Path.home() / '.m2/repository/org/ow2/asm'
cp = ';'.join(str(asm / f'{a}/9.7/{a}-9.7.jar') for a in ['asm', 'asm-tree'])
source = S / 'src/main/java/local/lito381/server/SourceBuffRead.java'
def run(*args): subprocess.run(list(map(str, args)), check=True)
run(J/'javac.exe', '-encoding', 'UTF-8', '-implicit:none', '-cp', base, '-d', C, source)
run(J/'javac.exe', '-encoding', 'UTF-8', '-cp', cp, '-d', B, R/'PatchBuffRead.java')
candidate = B / 'candidate.jar'
run(J/'java.exe', '-cp', str(B)+';'+cp, 'PatchBuffRead', base, C, candidate)
with zipfile.ZipFile(base) as old, zipfile.ZipFile(candidate) as new:
    assert set(old.namelist()).issubset(new.namelist())
    changed = [n for n in new.namelist() if n not in old.namelist() or new.read(n) != old.read(n)]
    assert set(changed) == {'local/lito381/server/LegacyMySqlRepository.class', 'local/lito381/server/SourceBuffRead.class'}, changed
receipt = {'base_sha256': sha(base), 'candidate_sha256': sha(candidate), 'changed_entries': changed,
           'inputs': {str(p): sha(p) for p in [Path(__file__), R/'PatchBuffRead.java', source]},
           'scope': 'BUFF read and schema validation only; no combat candidate integration'}
(B/'build.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(receipt, ensure_ascii=False))
