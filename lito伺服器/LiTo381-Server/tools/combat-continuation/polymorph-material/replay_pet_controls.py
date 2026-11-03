"""Run already-qualified frozen pet consumers on the new combined SHA."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,os,zipfile
R=Path(__file__).resolve().parent;C=R.parent;S=C.parents[1];B=S/'tools/resource-database/build';P=C/'pet-controls'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];assert read(P/'build/qualification-current.json')['status']=='passed-targeted-pet-controls-full-release-open'
D=R/'build/pet-replay';D.mkdir(exist_ok=True);run=D/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir()
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2'
deps=[C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=b['runtime'],driver=pin(Path(__file__)),results=[],formal_deployment_changed=False,full_goal_achieved=False)
try:
 for name,cls,cases in [('load','PetControlLoad',3),('capacity','PetCapacity',56),('collar','PetCollar',98),('taming','PetTaming',42),('zombie','PetZombie',42)]:
  basis=P/'build'/name/'result.json';old=read(basis);assert old['status'].startswith('passed-');assert old['runtime']==b['base'];classes=P/'build'/name/'classes'
  inputs=[pin(p)for p in classes.rglob('*.class')];assert inputs
  assert all(Path(x['path']).name not in ['ClientSession.class','NativePetControls.class','CombatRules.class','BattleCompanion.class','ClassicWorldMonsterPopulation.class']for x in inputs)
  dest=run/name;dest.mkdir();log=dest/'test.log';cp=';'.join(map(str,[classes,*deps]));observation=dest/'observations.json'
  args=[S/'config/server.properties',dest,jar]if name=='load' else[S/'config/server.properties',dest/'runtime',observation,jar]
  with log.open('w',encoding='utf-8')as out:subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.'+cls+'Probe',*args])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=240)
  result=read(observation);assert result['cases']==cases
  if name=='capacity':assert result['decisions']==3584
  for key in ['locked_writes','locked_writes_observed']:
   if key in result:assert result[key]==0
  v['results'].append(dict(test=name,basis=pin(basis),compiled_inputs=inputs,results=result,observations=pin(observation),log=pin(log)));print('PASS',name,cases,flush=True)
 # Pure planner is reused by exact helper class identity, not re-executed.
 with zipfile.ZipFile(Path(b['base']['path']))as a,zipfile.ZipFile(jar)as z:
  key='local/lito381/server/NativePetControls.class';assert a.read(key)==z.read(key)
 v.update(status='passed',pure_plan_basis=pin(P/'build/plan/result.json'))
except BaseException as error:
 v.update(status='failed',error=str(error))
 if 'log'in globals()and log.exists():print(log.read_text('utf-8',errors='replace')[-5000:])
 raise
finally:
 (run/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');(D/'current.json').write_text(json.dumps(dict(status=v['status'],runtime=b['runtime'],receipt=pin(run/'result.json')),indent=2),encoding='utf-8')
