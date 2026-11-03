"""Frozen original skill27 player/policy regressions on the NPC-epoch candidate."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,os,zipfile,shutil
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build';W=C/'weapon-break-material';P=C/'pet-world-policy';T=C/'weapon-break-lifetime'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p));write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
b=read(D/'weapon-target-build-current.json');jar=Path(b['runtime']['path']);assert sha(jar)==b['runtime']['sha256'];run=D/'weapon-player-and-policy-replay'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name)
qualified_index=read(T/'build/replay/current.json');assert qualified_index['status'].startswith('passed');qualified_path=Path(qualified_index['receipt']['path']);assert sha(qualified_path)==qualified_index['receipt']['sha256'];qualified=read(qualified_path);qualified_rows={r['probe']:r for r in qualified['results']};previous={}
if (D/'weapon-player-replay-current.json').exists():
 index=read(D/'weapon-player-replay-current.json');path=Path(index['manifest']['path']);assert sha(path)==index['manifest']['sha256'];prior_run=read(path)
 if prior_run['runtime']==b['runtime']:previous={r['name']:r for r in prior_run['results']}
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
common=[C/'elemental-material/build/lifecycle/classes',C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip']
specs=[('WeaponBreakMaterialProbe',[W/'build/tests/classes'],96,W/'build/tests/result.json'),('WeaponBreakFaultProbe',[W/'build/boundaries/classes',W/'build/boundaries/memory-observer'],8,W/'build/boundaries/result.json'),('WeaponBreakLifecycleProbe',[W/'build/boundaries/classes'],56,W/'build/boundaries/result.json'),('WeaponBreakTargetFaultProbe',[T/'build/target-fault/classes',T/'build/target-fault/memory-observer'],8,T/'build/target-fault/result.json'),('WorldMaterialPolicyProbe',[P/'build/tests/classes'],32,P/'build/tests/result.json')]
v=dict(status='running',runtime=b['runtime'],results=[],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,full_goal_achieved=False)
try:
 for name,overlays,count,receipt in specs:
  prior=read(receipt)
  if name in qualified_rows:prior=qualified;receipt=qualified_path
  assert prior['status'].startswith('passed');source_jar=Path(prior['runtime']['path']);assert sha(source_jar)==prior['runtime']['sha256']
  with zipfile.ZipFile(source_jar) as old,zipfile.ZipFile(jar) as new:assert old.read('local/lito381/server/GameStateStore.class')==new.read('local/lito381/server/GameStateStore.class')
  dest=run/name;dest.mkdir();inputs=[pin(p) for folder in overlays for p in folder.rglob('*.class')];assert inputs and all(not p['path'].endswith('ClientSession.class') for p in inputs)
  if name in qualified_rows:assert inputs==qualified_rows[name]['compiled_inputs'],'qualified fixture inputs changed: '+name
  adapter=None
  if name=='WeaponBreakLifecycleProbe':
   seed=read(Path(read(D/'build-current.json')['manifest']['path']));tool=dest/'tools';tool.mkdir();tcp=';'.join(map(str,[tool,*[p['path'] for p in seed['asm']]]));source=dest/'AdaptWeaponLifeFixture.java';shutil.copyfile(R/source.name,source);adapted=dest/'fixture';original=overlays[0]/'local/lito381/server/WeaponBreakLifecycleProbe.class'
   with (dest/'adapter.log').open('w',encoding='utf-8') as output:
    for args in [[J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tool,source],[J/'java.exe','-Xverify:all','-cp',tcp,'AdaptWeaponLifeFixture',original,adapted]]:subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=output,stderr=subprocess.STDOUT,check=True,timeout=90)
   overlays=[adapted,*overlays];adapter=dict(source=pin(source),original=pin(original),adapted=pin(adapted/'local/lito381/server/WeaponBreakLifecycleProbe.class'),log=pin(dest/'adapter.log'),parent_diagnostic=pin(D/'legacy-weapon-life-diagnostic/diagnostic.log'),change='one fixture branch pending==1 becomes pending>0; parent already has queued=true,pending=2; actual lock wait, no early mutation, role/scene retirement, cost/effect, I/O and zero terminal receipts remain unchanged')
  old=previous.get(name)
  if old and old['compiled_inputs']==inputs:
   for reference in [old['output'],old['log']]:assert sha(Path(reference['path']))==reference['sha256']
   assert old['observations']['cases']==count
   if name=='WeaponBreakMaterialProbe':assert Path(old['output']['path']).with_name('goldens.txt').read_bytes()==(W/'build/tests/after/goldens.txt').read_bytes()
   v['results'].append(old);write(run/'manifest.json',v);print('REUSE '+name+' exact candidate, compiled inputs, observations/log and success goldens',flush=True);continue
  cp=';'.join(map(str,[*overlays,*common,jar,S/'lib/*']));log=dest/'test.log';print('RUN '+name,flush=True)
  with log.open('w',encoding='utf-8') as out:subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.'+name,S/'config/server.properties',dest/'runtime',dest/'observations.json',jar,'after'])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=240)
  result=read(dest/'observations.json');assert result['cases']==count and result.get('locked_write_calls',result.get('locked_writes',-1))==0
  if name=='WeaponBreakMaterialProbe':assert (dest/'goldens.txt').read_bytes()==(W/'build/tests/after/goldens.txt').read_bytes()
  if name in ['WeaponBreakFaultProbe','WeaponBreakTargetFaultProbe']:assert result['actual_memory_faults']==8 and result['failed_material_casts_with_partial_commit']==0
  if name=='WorldMaterialPolicyProbe':assert result['locked_policy_queries']==0 and result['npc_replaced_during_policy_read']==4
  v['results'].append(dict(name=name,basis=pin(receipt),compiled_inputs=inputs,fixture_adapter=adapter,observations=result,output=pin(dest/'observations.json'),log=pin(log)));write(run/'manifest.json',v);print('PASS '+name+' cases='+str(count),flush=True)
 v.update(status='passed-preserved-player-weapon-and-memory-fault-and-world-policy',cases=200,checks=sum(r['observations']['checks'] for r in v['results']))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-6000:]);raise
finally:write(run/'manifest.json',v);write(D/'weapon-player-replay-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'],checks=v['checks'])))
