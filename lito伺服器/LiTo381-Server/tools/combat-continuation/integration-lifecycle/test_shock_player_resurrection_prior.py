"""Replay original PC/ordinary NPC/fault bodies unchanged, sequentially, on the exact new candidate."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,sys
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
name='shock-player-resurrection-originals-lifecycle-only' if '--lifecycle-only'in sys.argv else 'shock-player-resurrection-originals';expected=48 if '--lifecycle-only'in sys.argv else 368
build=read(D/'shock-player-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];seed=read(Path(build['manifest']['path']));run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
originals=[C/'resurrection-ingress'/name for name in ['ResurrectionMaterialProbe.java','ResurrectionLifecycleProbe.java']]+[C/'resurrection-material/ResurrectionAnswerLifetimeProbe.java',R/'ResurrectionLifecycleContinuationProbe.java']
adaptation=read(D/'shock-player-resurrection-pair-receipt-adaptation.json');assert sha(Path(adaptation['adapted']['path']))==adaptation['adapted']['sha256'];assert Path(adaptation['adapted']['path']).read_text('utf-8').replace('ResurrectionLifecycleContinuationProbe','ResurrectionLifecycleProbe').replace('NativePlayerVitalCompletions.pending(life)==(world||mode==0||mode==1?2:1)','NativePlayerVitalCompletions.pending(life)==1').replace('boolean accepted=mode==4&&world;','boolean accepted=mode==4;').replace('"actual target producer reserves and waits for ordered player lock mode="+mode+" world="+world+" queued="+lock.hasQueuedThread(worker)+" pending="+NativePlayerVitalCompletions.pending(life)','"actual target producer reserves and waits for ordered player lock"')==Path(adaptation['original']['path']).read_text('utf-8')
for p in [Path(__file__),*originals,D/'shock-player-resurrection-pair-receipt-adaptation.json']:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';F=B/'baseline-PC-ingress-efd4-v2';overlay=run/'memory-observer';cp=';'.join(map(str,[classes,C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');log=run/'compile.log';v=dict(status='running',runtime=build['runtime'],inputs=[pin(p)for p in inputs.iterdir()],original_bodies=[dict(original=pin(p),frozen=pin(inputs/p.name))for p in originals],results=[],pair_receipt_adaptation=pin(inputs/'shock-player-resurrection-pair-receipt-adaptation.json'),full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args,out=out):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=900)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*sorted(inputs.glob('*.java'))])
 for label,entry,cases in ([('lifecycle','ResurrectionLifecycleContinuationProbe',48)] if '--lifecycle-only'in sys.argv else [('material','ResurrectionMaterialProbe',256),('lifecycle','ResurrectionLifecycleContinuationProbe',48),('answers','ResurrectionAnswerLifetimeProbe',64)]):
  branch=run/label;branch.mkdir();log=branch/'test.log';print('RUN '+label,flush=True)
  with log.open('w',encoding='utf-8')as out:invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.'+entry,S/'config/server.properties',branch/'runtime',branch/'observations.json',jar,'after'],out)
  observations=read(branch/'observations.json');assert observations['cases']==cases and observations.get('locked_write_calls',observations.get('locked_writes'))==0
  if label=='material':assert observations['Source_window_bypasses']==0
  if label=='answers':assert observations['stale_native_acceptances']==0 and observations['stale_direct_acceptances']==0
  row=dict(label=label,observations=observations,receipt=pin(branch/'observations.json'),log=pin(log))
  if label=='material':
   golden=C/'resurrection-ingress/build/tests/after/goldens.txt';actual=branch/'goldens.txt';assert actual.read_bytes()==golden.read_bytes(),'original resurrection wire changed';row.update(golden=pin(golden),actual=pin(actual),original_wire_equal=True)
  v['results'].append(row);write(run/'manifest.json',v);print('PASS '+label+' cases='+str(cases),flush=True)
 v.update(status='passed-original-resurrection-lifecycle-only' if '--lifecycle-only'in sys.argv else 'passed-368-original-resurrection-material-lifecycle-answer-role-and-wire',cases=sum(r['observations']['cases']for r in v['results']),checks=sum(r['observations']['checks']for r in v['results']));assert v['cases']==expected
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:write(run/'manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
