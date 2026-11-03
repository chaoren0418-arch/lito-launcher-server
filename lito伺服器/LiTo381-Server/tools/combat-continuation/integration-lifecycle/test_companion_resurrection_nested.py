from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,sys
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
before='--before' in sys.argv;stopped='--stopped' in sys.argv;name='companion-resurrection-nested-before' if before else 'companion-resurrection-nested-after';name+=('-stopped' if stopped else '');build=read(D/'companion-resurrection-scene-build-current.json');
if before:build=dict(runtime=dict(path=str(D/'companion-resurrection-scene-builds/20261021T002602251795Z/candidate-companion-resurrection-scene-unqualified.jar'),sha256='37b3442d38949b1c0e2fdf779938c603fd8cb08d56d16b4fef7aaf3917daf48b'))
jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'CompanionResurrectionNestedProbe.java',R/'CompanionPlacementProbe.java',C/'return-nature-material/ReturnNatureMaterialProbe.java']:shutil.copyfile(p,inputs/p.name)
portal=inputs/'CompanionPlacementProbe.java';text=portal.read_text('utf-8');assert text.count('companion(pc,x+1,y,false)')==1;portal.write_text(text.replace('companion(pc,x+1,y,false)','companion(pc,x+1,y,true)'),'utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],inputs=[pin(p) for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=600)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*sorted(inputs.glob('*.java'))]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.CompanionResurrectionNestedProbe',S/'config/server.properties',run/'runtime',run/'observations.json',jar,'before' if before else 'stopped' if stopped else 'after'])
 result=read(run/'observations.json');assert result['cases']==16 and result['new_scene_ghost_hits']==0 and result['old_scene_unresolved_targets']==0;v.update(status='observed-nested-foreign-target-scope-bypasses-corpse-capture' if before else 'passed-nested-independent-corpse-capture-and-outer-context-restoration',observations=result)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
