"""Read the pinned candidate handler; no source or deployment changes."""
from pathlib import Path
import json,subprocess,hashlib
R=Path(__file__).resolve().parent;D=R/'build/protection-audit';D.mkdir(exist_ok=True)
jar=R/'water-life-material/build/candidate-water-life-material.jar'
assert hashlib.sha256(jar.read_bytes()).hexdigest()=='c83d82a84676a984a3d5e63e41a52f4e564359b3ba93c0ff6923e2748276fed7'
asm=json.loads((R/'build/integration-regression/preparation.json').read_text('utf-8'))['asm']
cp=';'.join(map(str,[D,*[p['path'] for p in asm]]));J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(D),str(R/'DumpMethod.java')],check=True)
with (D/'method.txt').open('w',encoding='utf-8') as out:
    subprocess.run([str(J/'java.exe'),'-Xmx128m','-cp',cp,'DumpMethod',str(jar),'useDamageProtectionSpell'],stdout=out,check=True)
print('Wrote exact candidate damage protection handler inspection')
