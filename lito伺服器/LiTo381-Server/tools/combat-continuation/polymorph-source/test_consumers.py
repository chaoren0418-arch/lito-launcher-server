"""Reuse the frozen native skill67 probes on the shared-data candidate; no recompilation."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,os,zipfile
R=Path(__file__).resolve().parent;P=R.parent/'polymorph-material';S=R.parents[2];B=S/'tools/resource-database/build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];D=R/'build/consumers';D.mkdir(exist_ok=True);run=D/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir()
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2'
deps=[R.parent/'elemental-material/build/lifecycle/classes',R.parent/'elemental-material/build/boundaries/classes',R.parent/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=b['runtime'],driver=pin(Path(__file__)),results=[],formal_deployment_changed=False,full_goal_achieved=False)
try:
 for name,folder,cases in [('Material','tests',128),('Fault','boundaries',8),('Lifecycle','boundaries',56),('Menu','menu',80)]:
  receipt=P/'build'/folder/'result.json';basis=read(receipt);assert basis['status']=='passed';old=Path(basis['runtime']['path']);assert sha(old)==basis['runtime']['sha256']
  overlays=[P/'build'/folder/'classes']
  if name=='Fault':
   overlays.append(P/'build/boundaries/memory-observer')
   with zipfile.ZipFile(old)as a,zipfile.ZipFile(jar)as z:assert a.read('local/lito381/server/GameStateStore.class')==z.read('local/lito381/server/GameStateStore.class')
  inputs=[pin(p)for path in overlays for p in path.rglob('*.class')];assert inputs
  dest=run/name;dest.mkdir();log=dest/'test.log';cp=';'.join(map(str,[*overlays,*deps]))
  with log.open('w',encoding='utf-8')as out:subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.Polymorph'+name+'Probe',S/'config/server.properties',dest/'runtime',dest/'observations.json',jar,'after'])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=240)
  obs=read(dest/'observations.json');assert obs['cases']==cases
  for key in ['locked_writes','locked_write_calls','Source_window_bypasses','failed_material_casts_with_partial_commit']:
   if key in obs:assert obs[key]==0
  if name=='Material':assert (dest/'goldens.txt').read_bytes()==(P/'build/tests/after/goldens.txt').read_bytes()
  v['results'].append(dict(probe=name,results=obs,basis=pin(receipt),compiled_inputs=inputs,observation=pin(dest/'observations.json'),log=pin(log)));print('PASS',name,cases,flush=True)
 v['status']='passed'
except BaseException as error:
 v.update(status='failed',error=str(error))
 if 'log'in globals()and log.exists():print(log.read_text('utf-8',errors='replace')[-6000:])
 raise
finally:
 (run/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');(D/'current.json').write_text(json.dumps(dict(status=v['status'],runtime=b['runtime'],receipt=pin(run/'result.json')),indent=2),encoding='utf-8')
