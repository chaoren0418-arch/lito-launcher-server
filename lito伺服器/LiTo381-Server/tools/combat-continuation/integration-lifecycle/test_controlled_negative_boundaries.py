from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,sys
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
profile='fault'if'--fault'in sys.argv else'publication'if'--publication'in sys.argv else'lifecycle';entry='ControlledNegative'+profile.title()+'Probe';build=read(D/'controlled-negative-material-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];seed=read(Path(build['manifest']['path']));name='controlled-negative-'+profile;run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
files=[Path(__file__),R/(entry+'.java'),R/'ControlledNegativeActorProbe.java',R/'BlindSceneProbe.java',C/'return-nature-material/ReturnNatureMaterialProbe.java']
if profile!='fault':files.append(R/'CompanionPlacementProbe.java')
else:files.append(B/'ObserveDeItemTransaction.java')
for p in files:shutil.copyfile(p,inputs/p.name)
if profile=='fault':(inputs/'DeItemTransactionProbe.java').write_text('package local.lito381.server;public class DeItemTransactionProbe {public static void beforeSave(){ControlledNegativeFaultProbe.beforeSave();}public static void beforeFlush()throws Exception{ControlledNegativeFaultProbe.beforeFlush();}}\n','utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';F=B/'baseline-PC-ingress-efd4-v2';overlay=run/'memory-observer';cp=';'.join(map(str,[classes,overlay,C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],inputs=[pin(p)for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=900)
  if profile=='fault':
   tcp=';'.join(map(str,[run/'tools',*[p['path']for p in seed['asm']]]));invoke([J/'javac.exe','-g:none','-cp',tcp,'-d',run/'tools',inputs/'ObserveDeItemTransaction.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'ObserveDeItemTransaction',jar,overlay]);assert [p.name for p in overlay.rglob('*.class')]==['GameStateStore.class']
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*[p for p in sorted(inputs.glob('*.java'))if p.name!='ObserveDeItemTransaction.java']]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.'+entry,S/'config/server.properties',run/'runtime',run/'observations.json',jar,'after'])
 observations=read(run/'observations.json')
 if profile=='fault':assert observations['cases']==48 and observations['actual_memory_faults']==48 and observations['failed_material_partial_commits']==0
 else:assert observations['scenarios']==(64 if profile=='lifecycle'else 32) and observations['cases']>=observations['scenarios'] and observations['new_scene_ghost_hits']==observations['old_scene_unresolved_targets']==0
 v.update(status='passed-controlled-negative-'+profile+'-canonical-material-and-paired-actor-lifetime',observations=observations)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
