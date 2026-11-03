"""Preserve the exact shared compiled fixture before the next layer recompiles it."""
from pathlib import Path
import json,hashlib,zipfile
R=Path(__file__).resolve().parent;D=R/'build/tests';source=R.parent/'decay-material/build/boundaries/classes'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
result=json.loads((D/'result.json').read_text('utf-8'));assert result['status']=='passed'
manifest=D/'dependency-freeze.json';archive=D/'decay-probe-dependencies.zip'
if manifest.exists():
 data=json.loads(manifest.read_text('utf-8'));assert sha(archive)==data['archive']['sha256'];print('Existing dependency freeze verified');raise SystemExit(0)
files=sorted(source.rglob('*.class'));assert files
assert all(p.stat().st_mtime_ns<=(D/'test.log').stat().st_mtime_ns for p in files),'dependency rebuilt after passing test'
with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED)as z:
 for p in files:z.writestr(p.relative_to(source).as_posix(),p.read_bytes())
with zipfile.ZipFile(archive)as z:
 assert all(z.read(p.relative_to(source).as_posix())==p.read_bytes()for p in files)
manifest.write_text(json.dumps(dict(status='byte-equal-freeze-before-recompilation',test_receipt=pin(D/'result.json'),test_log=pin(D/'test.log'),archive=pin(archive),classes=[pin(p)for p in files],note='Captured after completed test while all dependency class modification times still precede that test log'),indent=2),encoding='utf-8')
print('Frozen',len(files),'compiled dependency classes')
