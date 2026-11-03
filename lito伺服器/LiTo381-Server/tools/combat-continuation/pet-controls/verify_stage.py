"""Seal targeted pet-control evidence without approving the combat release."""
from pathlib import Path
import hashlib,json,zipfile
R=Path(__file__).resolve().parent;C=R.parent;S=C.parents[1];L=S.parent.parent/'lito登入器/LiTo381-Launcher'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
build=read(R/'build/build.json');runtime=build['runtime'];assert sha(Path(runtime['path']))==runtime['sha256']
repro=read(R/'build-repro/build.json');assert repro['runtime']['sha256']==runtime['sha256'] and sha(Path(repro['runtime']['path']))==runtime['sha256']
rows=[]
for folder,status,cases in [('plan','passed-pure-planning-only',18360),('load','passed-configuration-only',3),('capacity','passed-capacity-only',56),('collar','passed-collar-only',98),('taming','passed-taming-capacity-only',42),('zombie','passed-zombie-capacity-only',42)]:
 p=R/'build'/folder/'result.json';v=read(p);assert v['status']==status,(folder,v['status'])
 # Pure planning is linked by the build's exact helper source; native probes also pin runtime.
 if folder=='plan':
  # The planner ran before the native candidate existed. Reuse it only when
  # its exact helper bytes are the ones compiled into the candidate build.
  helper=[x for x in v['inputs'] if Path(x['path']).name=='NativePetControls.java'];assert len(helper)==1
  built=[x for x in build['inputs'] if Path(x['path']).name=='NativePetControls.java'];assert len(built)==1
  assert helper[0]['sha256']==built[0]['sha256']==sha(Path(built[0]['path']))
  observation=v['observations'];assert sha(Path(observation['path']))==observation['sha256'];result=read(Path(observation['path']))
 else:
  assert v['runtime']==runtime
  result=v.get('results',v.get('after',{}))
 assert result['cases']==cases,(folder,result)
 if folder=='capacity':assert result['decisions']==3584
 if folder=='collar':assert result['locked_writes']==0
 if folder in ('taming','zombie'):assert result['locked_writes_observed']==0
 rows.append(dict(test=folder,receipt=pin(p),results=result))
formal=L/'app/server/LiTo381-SharedResources-candidate.jar';dll=L/'app/compat/ddraw.dll'
assert sha(formal)=='75da69b0d2f7f9d394ee6bf2fdb396b153c75d9b938069b0999ed8dac4f50371'
assert sha(dll)=='0135e6e76594eb98695b85e9b451669adaedccfa6a13fafc49bf934d9c761de4'
retention=[]
with zipfile.ZipFile(C/'build/candidate-combat-buff.jar')as a,zipfile.ZipFile(Path(runtime['path']))as b:
 for name in ['SourceBuffRead','LegacyMySqlRepository']:
  key='local/lito381/server/'+name+'.class';assert a.read(key)==b.read(key);retention.append(dict(entry=key,sha256=hashlib.sha256(b.read(key)).hexdigest()))
parent=read(C/'earth-bind-material/build/composed-replay/current.json');assert parent['status']=='passed' and parent['runtime']==build['base'];assert sha(Path(parent['receipt']['path']))==parent['receipt']['sha256']
v=dict(status='passed-targeted-pet-controls-full-release-open',runtime=runtime,build=pin(R/'build/build.json'),reproduction=pin(R/'build-repro/build.json'),tests=rows,parent_composed_replay=pin(C/'earth-bind-material/build/composed-replay/current.json'),formal_protected=[pin(formal),pin(dll)],accepted_buff_byte_retention=retention,targeted_controls=['PrinceChaForPet','KnightChaForPet','ElfChaForPet','WizardChaForPet','DarkelfChaForPet','DragonKnightChaForPet','IllusionistChaForPet'],limitations=['No final 20+18 regression or SQL/native-GUI release qualification for this SHA','Taming/Create Zombie material transactions and role/entity lifecycle remain pending; tested native factories are WORLD only','Remaining twelve material skills and sixteen combat display controls are still open'],formal_deployment_changed=False,source_database_mutated=False,full_goal_achieved=False)
(R/'build/qualification-current.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');print(v['status'])
