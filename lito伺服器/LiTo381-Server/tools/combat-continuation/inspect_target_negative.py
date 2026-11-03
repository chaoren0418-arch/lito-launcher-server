"""Inspect actual remaining status roots, without inferring target scope from names."""
from pathlib import Path
import json,subprocess,hashlib
R=Path(__file__).resolve().parent;D=R/'build/target-negative-audit';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'decay-material/build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
inspector=R/'build/ground-material-audit/DumpClassMethod.java';cp=';'.join(map(str,[D,*[p['path']for p in read(R/'build/integration-regression/preparation.json')['asm']]]));J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
subprocess.run([str(J/'javac.exe'),'-g:none','-encoding','UTF-8','-cp',cp,'-d',str(D),str(inspector)],check=True)
outputs=[]
for name in ['useTargetNegativeStatusSpell','useWindShackleSpell','useClassicWorldBlindSpell','useReturnToNatureSpell']:
 p=D/(name+'.txt')
 with p.open('w',encoding='utf-8')as out:subprocess.run([str(J/'java.exe'),'-Xmx128m','-cp',cp,'DumpClassMethod',str(jar),name,'ClientSession'],stdout=out,check=True)
 assert p.stat().st_size>0
 outputs.append(pin(p))
(D/'inputs.json').write_text(json.dumps(dict(runtime=b['runtime'],inspector=pin(inspector),outputs=outputs,scope='static inspection only; no candidate mutation or behavioral qualification',findings=['Wind Shackle enumerates visible players, world NPCs, quest and time-attack actors; do not adapt as a single-target pair cast','TargetNegativeStatusKind has an enum argument; preserve kind-specific target/effect behavior and inspect private enum boundary before adapting'],formal_deployment_changed=False),indent=2),encoding='utf-8')
print('Inspected four original roots; no runtime changes')
