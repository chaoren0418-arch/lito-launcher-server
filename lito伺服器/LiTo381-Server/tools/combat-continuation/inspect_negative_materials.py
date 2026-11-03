"""Inspect the remaining single-target negative families from the composed immutable JAR."""
from pathlib import Path
import json,subprocess,hashlib
R=Path(__file__).resolve().parent;D=R/'build/negative-material-audit';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
b=read(R/'protection-material/build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
cp=';'.join(map(str,[D,*[p['path'] for p in read(R/'build/integration-regression/preparation.json')['asm']]]));J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(D),str(R/'DumpMethod.java')],check=True)
for name in ['useEntangleSpell','useWeaknessSpell','useEraseMagicSpell','useArmorBreakSpell']:
 with (D/(name+'.txt')).open('w',encoding='utf-8') as out:
  subprocess.run([str(J/'java.exe'),'-Xmx128m','-cp',cp,'DumpMethod',str(jar),name],stdout=out,check=True)
 print(name,flush=True)
(D/'inputs.json').write_text(json.dumps(dict(runtime=b['runtime'],inspector=dict(path=str(R/'DumpMethod.java'),sha256=sha(R/'DumpMethod.java')),scope='read-only census; not qualification'),indent=2),encoding='utf-8')
