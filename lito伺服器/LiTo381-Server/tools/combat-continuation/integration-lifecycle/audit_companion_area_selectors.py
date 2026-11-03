"""Read-only census of the next area selectors; this is not behavior qualification."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil
R=Path(__file__).resolve().parent;D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
b=read(D/'companion-resurrection-scene-build-current.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
seed=read(Path(read(D/'summon-retirement-build-current.json')['manifest']['path']));run=D/'companion-area-selector-audit'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'SelectedMethods.java']:shutil.copyfile(p,inputs/p.name)
source=Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model/skill/L1SkillUse.java');frozen=inputs/'L1SkillUse.java';shutil.copyfile(source,frozen)
for ref in seed['asm']:assert sha(Path(ref['path']))==ref['sha256']
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');tools=run/'tools';cp=';'.join([str(tools),*[ref['path'] for ref in seed['asm']]])
subprocess.run([str(J/'javac.exe'),'-g:none','-encoding','UTF-8','-cp',cp,'-d',str(tools),str(inputs/'SelectedMethods.java')],check=True,timeout=60)
ports=run/'ports.txt';selectors=['NativeAreaMaterialAction','NativeAreaMaterialAction$Context','ClientSession:patienceTargets,phantasmTargets,useClassicWorldMassSlowSpell,useClassicWorldSleepSpell,nativeAreaWorldSlowBody,nativeAreaWorldSleepBody,nativeMaterialAreaSilenceBody,nativeMaterialAreaWindBody']
subprocess.run([str(J/'java.exe'),'-Xms16m','-Xmx256m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-cp',cp,'SelectedMethods',str(jar),str(ports),*selectors],check=True,timeout=60)
rows=[];cls=method=''
for line in ports.read_text('utf-8').splitlines():
 if line.startswith('CLASS '):cls=line[6:]
 if line.startswith('METHOD '):method=line
 if any(s in line for s in ['.classicWorldCompanionSnapshots(','.controlledCompanionSnapshots(','.classicWorldMembers(','.worldMembers(','.companions(','.areaSnapshots(','.owners(','.phantasms(','.phantasmTargets(','.patienceTargets(']):rows.append(dict(owner=cls,method=method,port=line))
v=dict(status='located-readonly-area-primary-secondary-scene-selectors-behavior-open',runtime=b['runtime'],basis=b['manifest'],inputs=[pin(p) for p in inputs.iterdir()],asm=seed['asm'],Source=dict(original=pin(source),frozen=pin(frozen)),ports=pin(ports),selector_calls=rows,limits=['Context actor-scene capture does not establish that every primary/secondary body enumerates remote owners.','NativeArea worldMembers filters actual player membership; inspect each companion-only iterator before changing it. Genuine player/BATTLE policies must stay distinct.','Sleep phantasms delegates to private phantasmTargets and patienceTargets; inspect both selectors and range/eligibility/ownership before extending companions.','No gameplay rewrite or new behavioral cases claimed; full 34 material skills, Source controls and final same-SHA regression remain open.'],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
assert sha(source)==sha(frozen);write(run/'manifest.json',v);write(D/'companion-area-selector-audit-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],selector_calls=len(rows),runtime=v['runtime']['sha256'])))
