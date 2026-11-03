from pathlib import Path
import json,subprocess,hashlib
R=Path(__file__).resolve().parent;D=R/'build/summon-audit';D.mkdir(exist_ok=True)
jar=R/'elemental-lifetime/build/candidate-elemental-lifetime.jar'
assert hashlib.sha256(jar.read_bytes()).hexdigest()=='b0362026f3ec4ec105a63ed1f1da87cdb1192f69e546f86f16f88d220b6d582c'
asm=json.loads((R/'build/integration-regression/preparation.json').read_text('utf-8'))['asm']
cp=';'.join(map(str,[D,*[p['path'] for p in asm]]));J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(D),str(R/'DumpMethod.java')],check=True)
with (D/'summon-method.txt').open('w',encoding='utf-8') as out:
    subprocess.run([str(J/'java.exe'),'-Xmx128m','-cp',cp,'DumpMethod',str(jar),'useSummonSpell'],stdout=out,check=True)
print('Wrote exact candidate Summon Monster handler inspection')
