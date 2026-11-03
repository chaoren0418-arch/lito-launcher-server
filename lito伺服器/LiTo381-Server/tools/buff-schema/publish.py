"""Publish only the manually accepted BUFF reader and native selector candidate."""
from pathlib import Path
import hashlib, json, re, shutil, subprocess, sys, zipfile

R = Path(__file__).resolve().parent
S = R.parents[1]
L = S.parent.parent / 'lito登入器/LiTo381-Launcher'
B = R / 'build'
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
old_jar = 'c02aaae8d54a27e30460be2e3c47709ee64d35f007a8be27ac74a327268b9539'
new_jar = '75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371'
new_dll = '0135e6e76594eb98695b85e9b451669adaedccfa6a13fafc49bf934d9c761de4'
jar = L / 'app/server/LiTo381-SharedResources-candidate.jar'
assert sha(jar) == old_jar
assert sha(B / 'candidate.jar') == new_jar
assert sha(B / 'native/ddraw.dll') == new_dll
emulation = json.loads((B / 'native-emulation.json').read_text('utf-8'))
assert emulation['passed'] and emulation['dll_sha256'] == new_dll
run = 'buff-schema-native-20261018-d'
terminal = json.loads((L / 'app/runs' / run / 'terminal.json').read_text('utf-8'))
result = json.loads((B / 'staging/runs' / run / 'result.json').read_text('utf-8'))
assert terminal['cleanup_verified'] and not terminal['cleanup_errors']
assert result['accepted'] and result['cleanup_verified']

# Small rollback package: only files replaced by this publication; shared .NET
# runtime and game resources remain in place. The 0.6.66 baseline is untouched.
paths = [jar, L/'app/compat/ddraw.dll', L/'native/ddraw.dll',
         L/'app/server/Start-Shared-Formal.py', S/'deployment/release-inputs.json',
         L/'app/runtime-pins.json', L/'tools/build_current.py']
paths += [L/'src'/n for n in ['development_startup.py', 'DevelopmentStartup.cs',
          'FormalStandardSources.cs', 'LiTo381Launcher.csproj', 'MainForm.cs']]
paths += [p for p in (L/'release/current').iterdir() if p.is_file() and
          (p.name.startswith('LiTo381Launcher.') or p.name == 'development_startup.py')]
backup = L / 'backups/pre-0.6.69-buff-pages-20261018.zip'
entries = []
with zipfile.ZipFile(backup, 'x', zipfile.ZIP_DEFLATED) as z:
    for i, p in enumerate(paths):
        name = f'files/{i:02d}/{p.name}'
        z.write(p, name)
        entries.append({'path': str(p), 'entry': name, 'sha256': sha(p)})
    z.writestr('manifest.json', json.dumps(entries, ensure_ascii=False, indent=2))
with zipfile.ZipFile(backup) as z:
    for entry in entries:
        assert hashlib.sha256(z.read(entry['entry'])).hexdigest() == entry['sha256']

shutil.copyfile(B/'candidate.jar', jar)
for dest in [L/'app/compat/ddraw.dll', L/'native/ddraw.dll']:
    shutil.copyfile(B/'native/ddraw.dll', dest)
p = L/'app/server/Start-Shared-Formal.py'
t = p.read_text('utf-8')
assert t.count(old_jar) == 1
p.write_text(t.replace(old_jar, new_jar), encoding='utf-8')
p = S/'deployment/release-inputs.json'
data = json.loads(p.read_text('utf-8-sig'))
assert data['server_sha256'] == old_jar
data['server_sha256'] = new_jar
p.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
for p in [L/'src/LiTo381Launcher.csproj', L/'src/MainForm.cs', L/'tools/build_current.py']:
    t = p.read_text('utf-8')
    assert '0.6.68' in t
    p.write_text(t.replace('0.6.68', '0.6.69'), encoding='utf-8')
subprocess.run([sys.executable, '-B', '-X', 'utf8', str(L/'tools/build_current.py')], check=True)
receipt = {'version': '0.6.69', 'manual_accepted_run': run,
           'jar_sha256': sha(jar), 'dll_sha256': sha(L/'app/compat/ddraw.dll'),
           'rollback': str(backup), 'rollback_sha256': sha(backup),
           'scope': 'BUFF schema reads and four-slot character paging only',
           'source_database_mutations': False, 'combat_candidates_included': False}
(B/'publication.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(receipt, ensure_ascii=False))
