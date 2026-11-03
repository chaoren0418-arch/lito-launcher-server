"""Replay original PC/ordinary NPC/fault bodies unchanged, sequentially, on the exact new candidate."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'controlled-negative-material-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];seed=read(Path(build['manifest']['path']));run=D/'controlled-negative-original-regressions'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
originals=[C/'target-negative-material'/name for name in ['TargetNegativeMaterialProbe.java','TargetNegativeWorldNpcProbe.java','TargetNegativeFaultProbe.java']]
for p in [Path(__file__),*originals,B/'ObserveDeItemTransaction.java']:shutil.copyfile(p,inputs/p.name)
(inputs/'DeItemTransactionProbe.java').write_text('package local.lito381.server;public class DeItemTransactionProbe {public static void beforeSave(){TargetNegativeFaultProbe.beforeSave();}public static void beforeFlush()throws Exception{TargetNegativeFaultProbe.beforeFlush();}}\n','utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';F=B/'baseline-PC-ingress-efd4-v2';overlay=run/'memory-observer';cp=';'.join(map(str,[classes,C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');log=run/'compile.log';v=dict(status='running',runtime=build['runtime'],inputs=[pin(p)for p in inputs.iterdir()],original_bodies=[dict(original=pin(p),frozen=pin(inputs/p.name))for p in originals],results=[],full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args,out=out):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=900)
  tcp=';'.join(map(str,[run/'tools',*[p['path']for p in seed['asm']]]));invoke([J/'javac.exe','-g:none','-cp',tcp,'-d',run/'tools',inputs/'ObserveDeItemTransaction.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'ObserveDeItemTransaction',jar,overlay]);invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*[p for p in sorted(inputs.glob('*.java'))if p.name!='ObserveDeItemTransaction.java']])
 for label,entry,cases in [('PC','TargetNegativeMaterialProbe',224),('WORLD-NPC','TargetNegativeWorldNpcProbe',64),('PC-fault','TargetNegativeFaultProbe',16)]:
  branch=run/label;branch.mkdir();log=branch/'test.log';print('RUN '+label,flush=True)
  with log.open('w',encoding='utf-8')as out:invoke([J/'java.exe','-Xverify:all','-cp',str(classes)+';'+str(overlay)+';'+cp.removeprefix(str(classes)+';')if label=='PC-fault'else cp,'local.lito381.server.'+entry,S/'config/server.properties',branch/'runtime',branch/'observations.json',jar,'after'],out)
  observations=read(branch/'observations.json');assert observations['cases']==cases and observations.get('locked_write_calls',observations.get('locked_writes'))==0
  if label=='PC-fault':assert observations['actual_memory_faults']==16 and observations['failed_material_casts_with_partial_commit']==0
  else:assert observations['Source_window_bypasses']==0
  row=dict(label=label,observations=observations,receipt=pin(branch/'observations.json'),log=pin(log))
  if label=='PC':
   golden=C/'target-negative-material/build/tests/after/goldens.txt';actual=branch/'goldens.txt';assert actual.read_bytes()==golden.read_bytes(),'original PC material/effect packet frames changed';row.update(golden=pin(golden),actual=pin(actual),original_wire_equal=True)
  v['results'].append(row);write(run/'manifest.json',v);print('PASS '+label+' cases='+str(cases),flush=True)
 v.update(status='passed-304-original-PC-world-NPC-memory-fault-and-original-PC-wire',cases=sum(r['observations']['cases']for r in v['results']),checks=sum(r['observations']['checks']for r in v['results']));assert v['cases']==304
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:write(run/'manifest.json',v);write(D/'controlled-negative-original-regressions-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
