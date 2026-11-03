from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,os,zipfile
R=Path(__file__).resolve().parent;C=R.parent;S=C.parents[1];B=S/'tools/resource-database/build';W=C/'cancellation-material'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(R/'build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];assert sha(R/'build-repro/candidate-cancellation-speed-immunity.jar')==sha(jar)
D=R/'build/material-replay';D.mkdir(exist_ok=True);run=D/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir()
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2'
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
common=[C/'elemental-material/build/lifecycle/classes',C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip']
v=dict(status='running',runtime=b['runtime'],formal_deployment_changed=False,full_goal_achieved=False,results=[])
try:
 for name,overlays,cases in [
  ('CancellationMaterialProbe',[W/'build/tests/classes'],96),
  ('CancellationFaultProbe',[W/'build/boundaries/classes',W/'build/boundaries/memory-observer'],8),
  ('CancellationLifecycleProbe',[W/'build/boundaries/classes'],56),
  ('PoisonScopeConsumerProbe',[C/'earth-bind-material/build/consumer-completion/classes',C/'poison-publication/build/boundaries/classes'],8),
  ('PoisonScopeCurseConsumerProbe',[C/'poison-publication/build/curse-consumer/classes',C/'poison-publication/build/timers/classes',C/'poison-publication/build/boundaries/classes'],24),
  ('PoisonScopeClockProbe',[C/'poison-publication/build/clock-audit/classes',C/'poison-publication/build/boundaries/classes'],8),
  ('PoisonScopeTimerProbe',[C/'poison-publication/build/timers/classes',C/'poison-publication/build/boundaries/classes'],8)]:
  dest=run/name;dest.mkdir();inputs=[pin(x)for d in overlays for x in d.rglob('*.class')];assert inputs and all(Path(x['path']).name!='ClientSession.class'for x in inputs)
  cp=';'.join(map(str,[*overlays,*common,jar,S/'lib/*']))
  with (dest/'test.log').open('w',encoding='utf-8')as out:subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.'+name,S/'config/server.properties',dest/'runtime',dest/'observations.json',jar,'after'])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  row=read(dest/'observations.json');assert row['cases']==cases
  assert row.get('locked_write_calls',row.get('locked_writes',-1))==0
  if name=='CancellationMaterialProbe':assert (dest/'goldens.txt').read_bytes()==(W/'build/tests/after/goldens.txt').read_bytes()
  if name=='CancellationFaultProbe':assert row['actual_memory_faults']==8 and row['failed_material_casts_with_partial_commit']==0
  for key in ('early_warning_clocks','early_first_ticks'):
   if key in row:assert row[key]==0
  v['results'].append(dict(probe=name,observations=pin(dest/'observations.json'),results=row,compiled_inputs=inputs));print(name,row,flush=True)
 v['status']='passed-player-cancellation-material-and-lifetime-only'
except BaseException as e:
 v.update(status='failed',error=str(e));print((dest/'test.log').read_text('utf-8',errors='replace')[-4000:]);raise
finally:
 (run/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');(D/'current.json').write_text(json.dumps(dict(status=v['status'],runtime=b['runtime'],receipt=pin(run/'result.json')),indent=2),encoding='utf-8')
