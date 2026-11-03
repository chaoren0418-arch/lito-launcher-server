"""One-time, copy-first migration of the accepted 0.6.35 runtime. No deletion."""
from pathlib import Path
import hashlib,json,re,shutil
L=Path(__file__).resolve().parent.parent
W=L/'runtime/shared-resource-migration-20261011'
A=L/'app'
def copy(s,d):
    d.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(s,d)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def remap(t):
    pairs=[(str(W/'compat-baseline-035'),str(A/'compat')),(str(W/'server-gm-034'),str(A/'server')),(str(W),str(A))]
    for old,new in pairs:
        for x,y in [(old.replace('\\','\\\\'),new.replace('\\','\\\\')),(old,new),(old.replace('\\','/'),new.replace('\\','/'))]:t=t.replace(x,y)
    return t.replace("'server-gm-034'","'server'").replace("'compat-baseline-035/ddraw.dll'","'compat/ddraw.dll'")
pins=json.loads((W/'launcher-build-inputs-0.6.35.json').read_text('utf-8'))
for name in pins:
    p=Path(name)
    if not p.is_relative_to(W):continue
    rel=p.relative_to(W)
    if rel.parts[0]=='compat-baseline-035':rel=Path('compat')/rel.name
    if rel.parts[0]=='server-gm-034':rel=Path('server')/rel.name
    copy(p,A/rel)
shutil.copytree(W/'server-gm-034/lib',A/'server/lib',dirs_exist_ok=True)
copy(W/'server-gm-034/test_formal.py',A/'server/test_formal.py')
for p in (W/'compat-baseline-035').iterdir():
    if p.suffix in {'.cpp','.h','.inc','.def','.cmd','.json'}:copy(p,L/'native'/p.name)
for p in W.iterdir():
    if p.suffix in {'.cpp','.h','.def','.cmd'}:copy(p,L/'native/shared-resources'/p.name)
for p in (W/'launcher-src-0.6.35').iterdir():
    if p.is_file():
        d=L/'src'/p.name;copy(p,d)
        if p.suffix in {'.cs','.py','.csproj'}:d.write_text(remap(d.read_text('utf-8-sig')),encoding='utf-8')
shutil.copytree(W/'server-profiles',L/'server-profiles',dirs_exist_ok=True)
# Only the current helper carries runtime pins; reseal after relocation.
helper=L/'src/development_startup.py'
t=helper.read_text('utf-8');start=t.index('PINS = {');end=t.index('\nBIN_SHA = ',start)
newpins={}
for name in pins:
    p=Path(remap(name));newpins[str(p)]=sha(p)
t=t[:start]+'PINS = {\n'+''.join(f'    Path({p!r}): {v!r},\n' for p,v in newpins.items())+'}\n'+t[end:]
helper.write_text(t,encoding='utf-8')
cs=L/'src/DevelopmentStartup.cs';t=cs.read_text('utf-8')
t=re.sub(r'private const string EntrySha = "[A-F0-9]+";',f'private const string EntrySha = "{sha(helper)}";',t)
cs.write_text(t,encoding='utf-8')
(A/'runtime-pins.json').write_text(json.dumps(newpins,ensure_ascii=False,indent=2),encoding='utf-8')
# Preserve patch sources for the shipped JAR, rather than rebuilding unrelated server changes.
for part in ['repair-032','repair-033','repair-034']:
    for p in (W/part).rglob('*'):
        if p.is_file() and p.suffix in {'.java','.py','.json','.md'} and not any(x in p.parts for x in ['classes','test-classes','__pycache__']):
            copy(p,L/'server-patches'/part/p.relative_to(W/part))
(L/'docs').mkdir(exist_ok=True)
copy(L.parent.parent/'launcher-cleanup-before.json',L/'docs/cleanup-before.json')
print('Copied current runtime, source, native source, and pinned server into clean layout.')
