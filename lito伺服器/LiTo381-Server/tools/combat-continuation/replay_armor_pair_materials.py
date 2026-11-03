"""Replay frozen water-life and protection probes against the armor composition."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,os,shutil
R=Path(__file__).resolve().parent;S=R.parents[1];B=S/'tools/resource-database/build';A=R/'armor-state';D=A/'build/pair-replay';D.mkdir(exist_ok=True)
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
build=read(A/'build/build.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256']
run=D/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir();shutil.copyfile(__file__,run/Path(__file__).name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2'
deps=[R/'elemental-material/build/lifecycle/classes',R/'elemental-material/build/boundaries/classes',R/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=build['runtime'],driver=pin(run/Path(__file__).name),basis=[],results=[],formal_deployment_changed=False,full_goal_achieved=False)
log=None
try:
 for directory,prefix,material_cases in [('water-life-material','WaterLife',64),('protection-material','Protection',104)]:
  source=R/directory
  for receipt in [source/'build/tests/result.json',source/'build/boundaries/result.json']:
   assert read(receipt)['status']=='passed';v['basis'].append(pin(receipt))
  for name,count in [('Material',material_cases),('Fault',8),('Lifecycle',56)]:
   overlays=[source/('build/tests/classes' if name=='Material' else 'build/boundaries/classes')]
   if name=='Fault':overlays.append(source/'build/boundaries/memory-observer')
   inputs=[pin(p) for directory_path in overlays for p in directory_path.rglob('*.class')]
   dest=run/(prefix+name);dest.mkdir();log=dest/'probe.log';cp=';'.join(map(str,[*overlays,*deps]))
   with log.open('w',encoding='utf-8')as out:
    subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.'+prefix+name+'Probe',S/'config/server.properties',dest/'runtime',dest/'observations.json',jar,'after'])),cwd=S,env=env,check=True,stdout=out,stderr=subprocess.STDOUT,timeout=180)
   result=read(dest/'observations.json');assert result['cases']==count and result.get('locked_writes',result.get('locked_write_calls'))==0
   if name=='Material':
    assert result['Source_window_bypasses']==0
    assert (dest/'goldens.txt').read_bytes()==(source/'build/tests/after/goldens.txt').read_bytes()
   if name=='Fault':assert result['actual_memory_faults']==8 and result['failed_material_casts_with_partial_commit']==0
   v['results'].append(dict(probe=prefix+name,results=result,observations=pin(dest/'observations.json'),log=pin(log),compiled_inputs=inputs));print('PASS',prefix+name,count,flush=True)
 v['status']='passed'
except BaseException as error:
 v.update(status='failed',error=str(error))
 if log and log.exists():print(log.read_text('utf-8',errors='replace')[-5500:])
 raise
finally:
 (run/'result.json').write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8')
 (D/'current.json').write_text(json.dumps(dict(status=v['status'],runtime=build['runtime'],receipt=pin(run/'result.json')),indent=2),encoding='utf-8')
