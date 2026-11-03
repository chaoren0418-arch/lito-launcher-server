from pathlib import Path
import json,subprocess,hashlib
R=Path(__file__).resolve().parent;D=R/'build/water-life-audit';D.mkdir(exist_ok=True)
jar=R/'summon-ring/build/candidate-summon-ring.jar'
assert hashlib.sha256(jar.read_bytes()).hexdigest()=='68f0f3381731e076d8d726938cf869d9e59f2c6f98347380611188f583aaeaa3'
asm=json.loads((R/'build/integration-regression/preparation.json').read_text('utf-8'))['asm']
cp=';'.join(map(str,[D,*[p['path'] for p in asm]]));J=Path('C:/Program Files/Microsoft/jdk-17.0.17-hotspot/bin')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(D),str(R/'DumpMethod.java')],check=True)
with (D/'method.txt').open('w',encoding='utf-8') as out:
    subprocess.run([str(J/'java.exe'),'-Xmx128m','-cp',cp,'DumpMethod',str(jar),'useWaterLifeSpell'],stdout=out,check=True)
print('Wrote exact candidate Water Life handler inspection')
