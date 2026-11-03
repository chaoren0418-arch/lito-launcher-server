"""Inspect existing Source poison scopes before planning material changes."""
from pathlib import Path
import json,subprocess,hashlib
R=Path(__file__).resolve().parent;D=R/'build/poison-material-audit';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'resurrection-lifetime/build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
inspector=R/'build/ground-material-audit/DumpClassMethod.java';cp=';'.join(map(str,[D,*[p['path']for p in read(R/'build/integration-regression/preparation.json')['asm']]]));J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
subprocess.run([str(J/'javac.exe'),'-g:none','-encoding','UTF-8','-cp',cp,'-d',str(D),str(inspector)],check=True)
outputs=[]
for name in ['usePoisonSpell','useClassicWorldPoisonSpell','useParalysisSpell','useClassicWorldParalysisSpell','sourcePoisonBody$usePoisonSpell','sourcePoisonBody$useClassicWorldPoisonSpell','sourcePoisonBody$useParalysisSpell','sourcePoisonBody$useClassicWorldParalysisSpell']:
 p=D/(name+'.txt')
 with p.open('w',encoding='utf-8')as out:subprocess.run([str(J/'java.exe'),'-Xmx128m','-cp',cp,'DumpClassMethod',str(jar),name,'ClientSession'],stdout=out,check=True)
 text=p.read_text('utf-8');assert text.strip(),name
 outputs.append(dict(**pin(p),legacy_material_calls=text.count('CombatDatabase.consumeMaterial('),saves=text.count('GameStateStore.saveEconomyState('),source_scope_calls=text.count('CombatPoisonLifecycle')))
(D/'inputs.json').write_text(json.dumps(dict(runtime=b['runtime'],inspector=pin(inspector),outputs=outputs,scope='static Source scope/material interaction only; no transformation or equivalence claim',formal_deployment_changed=False),indent=2),encoding='utf-8')
print(json.dumps([{'name':Path(x['path']).name,'legacy_material_calls':x['legacy_material_calls'],'saves':x['saves']} for x in outputs]))
