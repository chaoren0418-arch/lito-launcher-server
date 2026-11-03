"""Replay qualified skill170 probes against the composed skill68 candidate, without repeating the baseline."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,os
R=Path(__file__).resolve().parent;S=R.parents[1];B=S/'tools/resource-database/build';W=R/'water-life-material';P=R/'protection-material';D=P/'build/water-life-replay';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
b=read(P/'build/build.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256']
receipts=[W/'build/tests/result.json',W/'build/boundaries/result.json',P/'build/tests/result.json',P/'build/boundaries/result.json']
assert all(read(p)['status']=='passed' for p in receipts)
run=D/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir();J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2'
deps=[R/'elemental-material/build/lifecycle/classes',R/'elemental-material/build/boundaries/classes',R/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
rows=[];v=dict(status='running',runtime=b['runtime'],basis=[pin(p) for p in receipts],results=rows,formal_deployment_changed=False,full_goal_achieved=False)
try:
 for name,count in [('Material',64),('Fault',8),('Lifecycle',56)]:
  path=W/('build/tests/classes' if name=='Material' else 'build/boundaries/classes');overlays=[path]
  if name=='Fault':overlays.append(W/'build/boundaries/memory-observer')
  inputs=[pin(p) for directory in overlays for p in directory.rglob('*.class')]
  dest=run/name;dest.mkdir();log=dest/'probe.log';cp=';'.join(map(str,[*overlays,*deps]))
  with log.open('w',encoding='utf-8') as out:
   subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.WaterLife'+name+'Probe',S/'config/server.properties',dest/'runtime',dest/'observations.json',jar,'after'])),cwd=S,env=env,check=True,stdout=out,stderr=subprocess.STDOUT,timeout=180)
  result=read(dest/'observations.json');assert result['cases']==count and result.get('locked_writes',result.get('locked_write_calls'))==0
  if name=='Material':
   assert result['Source_window_bypasses']==0
   assert (dest/'goldens.txt').read_bytes()==(W/'build/tests/after/goldens.txt').read_bytes()
  if name=='Fault':assert result['actual_memory_faults']==8 and result['failed_material_casts_with_partial_commit']==0
  rows.append(dict(probe=name,results=result,observations=pin(dest/'observations.json'),log=pin(log),compiled_inputs=inputs));print('PASS',name,count,flush=True)
 v['status']='passed'
except BaseException as e:
 v.update(status='failed',error=str(e));print(log.read_text('utf-8',errors='replace')[-5500:]);raise
finally:
 (run/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8');(D/'current.json').write_text(json.dumps(dict(status=v['status'],runtime=b['runtime'],receipt=pin(run/'result.json')),indent=2),encoding='utf-8')
