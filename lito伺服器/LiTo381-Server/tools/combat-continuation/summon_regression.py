"""Apply the sealed regression workflow to the composed Source summon candidate."""
from pathlib import Path
import hashlib,json,subprocess,sys
R=Path(__file__).resolve().parent
subprocess.run([sys.executable,str(R/'summon-ring/test.py')],check=True)
source=R/'lease_regression.py';text=source.read_text('utf-8')
assert text.count("D=R/'build/lease-regression'")==1
assert text.count("R/'invisibility-leases/build/build.json'")==2
text=text.replace("D=R/'build/lease-regression'","D=R/'build/summon-regression'")
text=text.replace("R/'invisibility-leases/build/build.json'","R/'summon-ring/build/build.json'")
text=text.replace('lease-candidate-under-test','summon-candidate-under-test').replace('PASS exact lease candidate:','PASS exact summon candidate:')
D=R/'build/summon-regression';D.mkdir(exist_ok=True);frozen=D/'driver.py';frozen.write_text(text,encoding='utf-8')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
(D/'driver-adaptation.json').write_text(json.dumps(dict(original=dict(path=str(source),sha256=sha(source)),adapted=dict(path=str(frozen),sha256=sha(frozen)),changes='candidate receipt, evidence destination, status labels'),indent=2),encoding='utf-8')
exec(compile(text,str(frozen),'exec'),{'__file__':str(R/'summon_regression.py'),'__name__':'__main__'})
