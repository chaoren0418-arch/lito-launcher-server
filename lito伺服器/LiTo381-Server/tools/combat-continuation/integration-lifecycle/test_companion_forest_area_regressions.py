"""Replay original area fixture bodies on one candidate, with original material wire goldens."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,os
R=Path(__file__).resolve().parent;S=R.parents[2];C=R.parent;D=R/'build';B=S/'tools/resource-database/build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'companion-forest-area-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256']
previous={};prior_index=D/'companion-forest-area-regressions-current.json'
if prior_index.exists():
 prior=read(prior_index);assert sha(Path(prior['manifest']['path']))==prior['manifest']['sha256'];prior=read(Path(prior['manifest']['path']))
 if prior['runtime']==build['runtime']:
  for ref in prior['inputs']:assert sha(Path(ref['path']))==ref['sha256']
  previous={r['test']:r for r in prior['results']}
run=D/'companion-forest-area-regressions'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True);classes=run/'classes'
sources=[*[C/'area-sleep-slow'/n for n in ['AreaSleepSlowProbe.java','AreaSleepSlowNpcProbe.java','AreaSleepSlowCompanionProbe.java','AreaSleepSlowLifecycleProbe.java','AreaBattleNpcProbe.java']],*[C/'area-material'/n for n in ['AreaMaterialProbe.java','AreaNpcProbe.java','AreaLifecycleProbe.java']],*[B/n for n in ['NativeCastMaterialProbe.java','NativeLightCastProbe.java','NativeLightStoppedProbe.java','SourceActionPairProbe.java']],B/'de-native-skill-consumption-development/reference/SourceSkillConsumptionReference.java',B/'de-native-skill-consumption-development/source-inputs/L1SkillId.java']
for p in [*sources,R/'CandidateAreaOriginProbe.java',Path(__file__)]:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2'
cp=';'.join(map(str,[classes,C/'cancellation-npc-material/build/tests/classes',C/'cancellation-probability/build/boundaries/classes',C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
specs=[('AreaMaterial','AreaMaterialProbe',144,'area-material'),('AreaNpc','AreaNpcProbe',64,None),('AreaLifecycle','AreaLifecycleProbe',96,None)]
v=dict(status='running',runtime=build['runtime'],basis=build['manifest'],inputs=[pin(p) for p in inputs.iterdir()],fixture_sources=[dict(original=pin(p),frozen=pin(inputs/p.name)) for p in sources],results=[],fixture_bodies_changed=False,full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);log=run/'compile.log'
try:
 with log.open('w',encoding='utf-8') as out:subprocess.run(list(map(str,[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*sorted(inputs.glob('*.java'))])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
 v['compile_log']=pin(log)
 log=run/'class-origin.log'
 with log.open('w',encoding='utf-8') as out:subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.CandidateAreaOriginProbe',jar])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=60)
 v['class_origin']=pin(log)
 for label,cls,cases,golden in specs:
  if label in previous:
   row=previous[label]
   for key in ['log','receipt','qualified_goldens','actual_goldens']:
    if key in row:assert sha(Path(row[key]['path']))==row[key]['sha256']
   assert row['observations']['cases']==cases;v['results'].append(row);print('REUSE '+label+' cases='+str(cases),flush=True);continue
  branch=run/label;branch.mkdir();log=branch/'test.log';print('RUN '+label,flush=True)
  with log.open('w',encoding='utf-8') as out:subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.'+cls,S/'config/server.properties',branch/'runtime',branch/'observations.json',jar,'after'])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=600)
  obs=read(branch/'observations.json');assert obs['cases']==cases and obs['locked_write_calls']==0
  text=log.read_text('utf-8');assert 'Vital completion failed' not in text and 'role publication capture requires actual mutation lock' not in text
  row=dict(test=label,observations=obs,log=pin(log),receipt=pin(branch/'observations.json'))
  if golden:
   reference=C/golden/'build/tests/after/self-material-goldens.txt';actual=branch/'self-material-goldens.txt';assert actual.read_bytes()==reference.read_bytes(),'original Source material frames changed';row.update(stable_wire_equal=True,qualified_goldens=pin(reference),actual_goldens=pin(actual))
  v['results'].append(row);write(run/'manifest.json',v);print('PASS '+label+' cases='+str(cases),flush=True)
 v.update(status='passed-same-SHA-304-existing-area-player-NPC-BATTLE-and-lifecycle-scope',cases=sum(r['observations']['cases'] for r in v['results']),checks=sum(r['observations']['checks'] for r in v['results']));assert v['cases']==304
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2200:]);raise
finally:write(run/'manifest.json',v);write(D/'companion-forest-area-regressions-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
