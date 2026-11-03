"""Audit already-present resurrection transaction routes before planning more changes."""
from pathlib import Path
import json,subprocess,hashlib
R=Path(__file__).resolve().parent;D=R/'build/resurrection-material-audit';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'target-negative-material/build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
inspector=R/'build/ground-material-audit/DumpClassMethod.java';cp=';'.join(map(str,[D,*[p['path']for p in read(R/'build/integration-regression/preparation.json')['asm']]]));J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
subprocess.run([str(J/'javac.exe'),'-g:none','-encoding','UTF-8','-cp',cp,'-d',str(D),str(inspector)],check=True)
outputs=[]
for owner,name in [('ClientSession',n) for n in ['useResurrectionSpell','useClassicWorldResurrectionSpell','commitResurrectionCast','commitClassicWorldResurrectionCast','answerResurrection','useCallOfNatureSpell']]+[('Protocol','askAnswer')]:
 p=D/(name+'.txt')
 with p.open('w',encoding='utf-8')as out:subprocess.run([str(J/'java.exe'),'-Xmx128m','-cp',cp,'DumpClassMethod',str(jar),name,owner],stdout=out,check=True)
 outputs.append(dict(**pin(p),method_found=p.stat().st_size>0))
(D/'inputs.json').write_text(json.dumps(dict(runtime=b['runtime'],inspector=pin(inspector),outputs=outputs,findings=[
 'The two resurrection commit methods use RuntimeInventory.removeCatalog directly, so a CombatDatabase.consumeMaterial-only census does not cover their costs.',
 'The two outer roots acquire ordered PC monitors, save economy state after commit, and retain the resurrection request/answer flow; a single-target negative-spell adapter is not sufficient.',
 'Cast commit MP subtraction has no NativeSkillConsumption.boundMp call; signed consumption boundary requires behavioral evidence before changing it.',
 'No behavior change or qualification is claimed by this static audit; greater resurrection75 remains pending.'
],scope='static inspection only; no behavioral equivalence or candidate modification claimed',formal_deployment_changed=False),indent=2),encoding='utf-8')
print(json.dumps([{'name':Path(x['path']).name,'method_found':x['method_found']}for x in outputs]))
