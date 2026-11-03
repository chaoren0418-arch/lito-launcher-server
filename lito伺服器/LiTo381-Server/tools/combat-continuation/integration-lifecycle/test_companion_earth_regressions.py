"""Run unchanged qualified negative fixture bodies against one candidate; preserve old wire goldens."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib,json,os,shutil,subprocess,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];C=R.parent;D=R/'build';B=S/'tools/resource-database/build';N=C/'earth-bind-material'
read=lambda p:json.loads(p.read_text('utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'companion-earth-scene-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256']
previous={};previous_manifest=None
prior_index=D/'companion-earth-regressions-current.json'
if prior_index.exists():
 prior=read(prior_index);previous_manifest=prior['manifest'];assert sha(Path(previous_manifest['path']))==previous_manifest['sha256'];prior=read(Path(previous_manifest['path']))
 if prior['runtime']==build['runtime']:
  for ref in prior['inputs']:assert sha(Path(ref['path']))==ref['sha256']
  previous={row['test']:row for row in prior['results']}
run=D/'companion-earth-regressions'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);classes=run/'classes'
sources=[*N.glob('EarthBind*Probe.java'),B/'NativeCastMaterialProbe.java',B/'NativeLightCastProbe.java',B/'NativeLightStoppedProbe.java',B/'SourceActionPairProbe.java',B/'de-native-skill-consumption-development/reference/SourceSkillConsumptionReference.java',B/'de-native-skill-consumption-development/source-inputs/L1SkillId.java']
for p in [*sources,Path(__file__),B/'ObserveDeItemTransaction.java']:shutil.copyfile(p,inputs/p.name)
life=inputs/'EarthBindLifecycleProbe.java';adapter=R/'LegacyPairLifecycleAdapter.py';shutil.copyfile(adapter,inputs/adapter.name);namespace={};exec(compile(adapter.read_text('utf-8'),str(adapter),'exec'),namespace);life.write_text(namespace['adapt_earth'](life.read_text('utf-8')),'utf-8');diagnostic=read(D/'negative-receipt-diagnostic-current.json');assert sha(Path(diagnostic['manifest']['path']))==diagnostic['manifest']['sha256']
hook=inputs/'DeItemTransactionProbe.java';hook.write_text('package local.lito381.server; public final class DeItemTransactionProbe {public static void beforeSave(){EarthBindFaultProbe.beforeSave();}public static void beforeFlush()throws Exception{EarthBindFaultProbe.beforeFlush();}}','utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2'
cp=';'.join(map(str,[classes,C/'elemental-material/build/lifecycle/classes',C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
v=dict(status='running',runtime=build['runtime'],basis=build['manifest'],inputs=[pin(p) for p in inputs.iterdir()],results=[],full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,production_PC_overlays=0)
v['fixture_adapter']=dict(original=pin(N/'EarthBindLifecycleProbe.java'),adapted=pin(life),adapter=pin(inputs/adapter.name),diagnostic=diagnostic['manifest'],existing_pair_boundary=pin(B/'NativePairMaterialBoundaryProbe.java'),existing_single_target_guard=pin(R/'NativeWeaponBreakTargetLifecycle.java'),only_change='waiting receipt ownership >0 replaces private count1; replace obsolete MP-from-retired-pair assertion with exact zero stale batch, committed MP/material once and one independent native MP publication. Original lock waits, retirement, STOPPED, accepted effects, socket failure and zero terminal receipts retained.')
v['fixture_adapter']['raw_STOPPED_wait']='Hold the peer lock in mode4 and require the actual NativePairedCastSafety.run stack before shutdown; holding the caster lock may stop a raw packet in its earlier policy/admission gate, before a material cast owns any receipt.'
log=run/'compile.log'
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*[inputs/p.name for p in sources],hook])
  seed=read(Path(build['manifest']['path']));parent=Path(seed['base']['path']);assert sha(parent)==seed['base']['sha256']
  asm=read(Path(read(D/'summon-retirement-build-current.json')['manifest']['path']))['asm'];tcp=';'.join(map(str,[run/'tools',*[a['path'] for a in asm]]))
  for a in asm:assert sha(Path(a['path']))==a['sha256']
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',run/'tools',inputs/'ObserveDeItemTransaction.java'])
  overlay=run/'memory-observer';invoke([J/'java.exe','-Xverify:all','-cp',tcp,'ObserveDeItemTransaction',parent,overlay])
  with zipfile.ZipFile(parent) as old,zipfile.ZipFile(jar) as new:assert old.read('local/lito381/server/GameStateStore.class')==new.read('local/lito381/server/GameStateStore.class')
 specs=[('Material',112,'tests'),('WorldNpc',32,'world-npcs'),('Fault',8,None),('Lifecycle',56,None)]
 for test,cases,golden in specs:
  if test in previous:
   row=previous[test]
   for key in ['log','receipt','qualified_goldens','actual_goldens']:
    if key in row:assert sha(Path(row[key]['path']))==row[key]['sha256']
   assert row['observations']['cases']==cases;row=dict(row,reused_same_SHA_manifest=previous_manifest);v['results'].append(row);write(run/'manifest.json',v);print('REUSE '+test+' cases='+str(cases),flush=True);continue
  branch=run/test;branch.mkdir();log=branch/'test.log';print('RUN '+test,flush=True)
  with log.open('w',encoding='utf-8') as out:
   runtimecp=str(classes)+';'+str(overlay)+';'+cp.removeprefix(str(classes)+';') if test=='Fault' else cp
   subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',runtimecp,'local.lito381.server.EarthBind'+test+'Probe',S/'config/server.properties',branch/'runtime',branch/'observations.json',jar,'after'])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=600)
  obs=read(branch/'observations.json');assert obs['cases']==cases and obs.get('locked_writes',obs.get('locked_write_calls'))==0
  row=dict(test=test,observations=obs,log=pin(log),receipt=pin(branch/'observations.json'))
  if golden:
   reference=N/'build'/golden/'after/goldens.txt';assert (branch/'goldens.txt').read_bytes()==reference.read_bytes();row.update(stable_wire_equal=True,qualified_goldens=pin(reference),actual_goldens=pin(branch/'goldens.txt'));assert obs['Source_window_bypasses']==0
  if test=='Fault':assert obs['actual_memory_faults']==8 and obs['failed_material_casts_with_partial_commit']==0
  v['results'].append(row);write(run/'manifest.json',v);print('PASS '+test+' cases='+str(cases),flush=True)
 v.update(status='passed-same-SHA-208-Earth-Bind-player-NPC-BATTLE-fault-and-lifecycle-scope',cases=sum(r['observations']['cases'] for r in v['results']),checks=sum(r['observations']['checks'] for r in v['results']))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4000:]);raise
finally:write(run/'manifest.json',v);write(D/'companion-earth-regressions-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
