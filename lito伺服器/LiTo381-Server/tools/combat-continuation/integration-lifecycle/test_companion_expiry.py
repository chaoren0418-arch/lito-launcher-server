from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,sys
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
before='--before' in sys.argv;name='companion-expiry-after';build=read(D/'companion-expiry-build-current.json');
if before:build=dict(runtime=read(Path(build['manifest']['path']))['base'])
name='companion-expiry-before' if before else name;jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'CompanionExpiryProbe.java',R/'CompanionPlacementProbe.java',C/'return-nature-material/ReturnNatureMaterialProbe.java']:shutil.copyfile(p,inputs/p.name)
hook=inputs/'DeItemTransactionProbe.java';hook.write_text('package local.lito381.server; public final class DeItemTransactionProbe {public static void beforeSave(){CompanionExpiryProbe.beforeSave();}public static void beforeFlush()throws Exception{CompanionExpiryProbe.beforeFlush();}}',encoding='utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,run/'observer',C/'return-nature-material/build/boundaries/memory-observer',C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],observer=pin(C/'return-nature-material/build/boundaries/memory-observer/local/lito381/server/GameStateStore.class'),inputs=[pin(p) for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=240)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*sorted(inputs.glob('*.java'))]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.CompanionExpiryProbe',S/'config/server.properties',run/'runtime',run/'observations.json',jar,'before' if before else 'after'])
 result=read(run/'observations.json');assert result['cases']==(8 if before else 24) and result['owner_flush_faults']==(0 if before else 2) and result['observed_skipped_due_actors']==(8 if before else 0);v.update(status='observed-active-offscene-summon-expiry-gap' if before else 'passed-actual-world-clock-offscene-expiry-and-stale-capture-lifetimes',observations=result)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
