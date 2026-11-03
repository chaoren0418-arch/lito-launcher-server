from pathlib import Path
import json,subprocess,hashlib
R=Path(__file__).resolve().parent;D=R/'build/elemental-audit';D.mkdir(exist_ok=True)
jar=R/'invisibility-leases/build/candidate-invisibility-leases.jar';assert hashlib.sha256(jar.read_bytes()).hexdigest()=='d4bc643fca4ab18ea4b6b84e9f685bbc80bd26d806b98970a6a0d98d28ae3a81'
asm=json.loads((R/'build/integration-regression/preparation.json').read_text('utf-8'))['asm'];cp=';'.join(map(str,[D,*[p['path'] for p in asm]]));J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(D),str(R/'DumpMethod.java')],check=True)
with (D/'elemental-method.txt').open('w',encoding='utf-8') as out:subprocess.run([str(J/'java.exe'),'-Xmx128m','-cp',cp,'DumpMethod',str(jar),'useElementalSummonSpell'],stdout=out,check=True)
print('Wrote exact candidate elemental handler inspection')
