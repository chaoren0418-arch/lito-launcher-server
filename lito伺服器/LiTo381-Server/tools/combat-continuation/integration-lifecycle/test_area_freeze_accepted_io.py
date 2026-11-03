"""Versioned PC80 projection of the original raw50 worker-held stage fixture; no PC overlays."""
from pathlib import Path
import hashlib,json,subprocess,shutil,os
from datetime import datetime,timezone
R=Path(__file__).resolve().parent;S=R.parents[2];C=R.parent;D=R/'build';B=S/'tools/resource-database/build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'area-freeze-accepted-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];run=D/'area-freeze-accepted-io'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
original=R/'FreezeAcceptedIoProbe.java';text=original.read_text('utf-8');changes=[('FreezeAcceptedIoProbe', 'AreaFreezeAcceptedIoProbe', 2), ('selectedSkill=50', 'selectedSkill=80', 1), ('ctrl<2', 'ctrl<1', 1), ('before?8:32', 'before?4:16', 2), ('var timed=(BattleTimedStatus)get(target,"timedStatus");', 'for(var m:members)if(m!=pc&&m!=target)set(m,"positionX",x+11);var timed=(BattleTimedStatus)get(target,"timedStatus");', 1), (' static boolean observe(', ' static void send(ClientSession pc,long id)throws Exception{call(pc,"handle",new Class[]{byte[].class},new Protocol.Writer().c(Protocol.C_USE_SPELL).c(9).c(7).bytes());}\n static boolean observe(', 1)]
for old,new,count in changes:
 assert text.count(old)==count;text=text.replace(old,new)
inverse=text
for old,new,count in reversed(changes):
 assert inverse.count(new)==count;inverse=inverse.replace(new,old)
assert inverse==original.read_text('utf-8');(inputs/'AreaFreezeAcceptedIoProbe.java').write_text(text,'utf-8')
shutil.copyfile(R/'FreezeAcceptedStageProbe.java',inputs/'FreezeAcceptedStageProbe.java')
for p in [Path(__file__),R/'ShockPlayerAdmissionProbe.java',R/'ShockCompanionAdmissionProbe.java',R/'BlindSceneProbe.java',C/'return-nature-material/ReturnNatureMaterialProbe.java']:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],versioned_fixture_inverse_verified=True,original_fixture=pin(original),inputs=[pin(p)for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=900)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*sorted(inputs.glob('*.java'))]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.AreaFreezeAcceptedIoProbe',S/'config/server.properties',run/'runtime',run/'observations.json',jar,'after'])
 obs=read(run/'observations.json');assert obs['cases']==16 and obs['actual_native_frame_monitor_waits']==16 and obs['stale_freeze_frames']==0;v.update(status='passed-scoped-PC80-accepted-mutation-stage',observations=obs)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'area-freeze-accepted-io-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
