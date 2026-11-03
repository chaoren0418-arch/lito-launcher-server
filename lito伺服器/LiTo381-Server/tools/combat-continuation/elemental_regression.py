"""Apply the existing sealed regression workflow to the exact elemental candidate."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent
source=R/'lease_regression.py'
text=source.read_text('utf-8')
assert text.count("D=R/'build/lease-regression'")==1
assert text.count("R/'invisibility-leases/build/build.json'")==2
text=text.replace("D=R/'build/lease-regression'","D=R/'build/elemental-regression'")
text=text.replace("R/'invisibility-leases/build/build.json'","R/'elemental-lifetime/build/build.json'")
text=text.replace('lease-candidate-under-test','elemental-candidate-under-test')
text=text.replace('PASS exact lease candidate:','PASS exact elemental candidate:')
D=R/'build/elemental-regression';D.mkdir(exist_ok=True)
frozen=D/'driver.py';frozen.write_text(text,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(D/'driver-adaptation.json').write_text(json.dumps(dict(original=dict(path=str(source),sha256=sha(source)),
    adapted=dict(path=str(frozen),sha256=sha(frozen)),changes='candidate build receipt, evidence destination, status labels only'),indent=2),encoding='utf-8')
exec(compile(text,str(frozen),'exec'),{'__file__':str(R/'elemental_regression.py'),'__name__':'__main__'})
