from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,sys
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
before='--before'in sys.argv;player='--source-player'in sys.argv;lifecycle='--lifecycle'in sys.argv;publication='--publication'in sys.argv;conditions='--conditions'in sys.argv;assert sum([player,lifecycle,publication,conditions])<=1
additional='aliases'if '--aliases'in sys.argv else'npc'if '--source-npc'in sys.argv else None
assert not additional or not any([player,lifecycle,publication,conditions])
entry='BlindSourcePlayerProbe'if player else'BlindSceneLifecycleProbe'if lifecycle else'BlindScenePublicationProbe'if publication else'BlindSceneConditionsProbe'if conditions else'BlindSceneProbe';name=('blind-source-player'if player else'blind-scene-lifecycle'if lifecycle else'blind-scene-publication'if publication else'blind-scene-conditions'if conditions else'blind-scene')+('-before'if before else'-after')
if additional:
 entry='BlindAdditionalProbe';name='blind-'+('player-alias'if additional=='aliases'else'Source-npc')+('-before'if before else'-after')
build=read(D/('companion-wind-build-current.json' if before else 'blind-scene-build-current.json'));jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
files=[Path(__file__),R/'BlindSceneProbe.java',C/'return-nature-material/ReturnNatureMaterialProbe.java']
if player:files += [R/'BlindSourcePlayerProbe.java',C/'blind-material/BlindMaterialProbe.java']
if lifecycle or publication:files += [R/(entry+'.java'),R/'CompanionPlacementProbe.java']
if conditions:files += [R/(entry+'.java')]
if additional:files += [R/(entry+'.java'),C/'blind-material/BlindMaterialProbe.java',C/'blind-material/BlindWorldNpcProbe.java']
for p in files:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],inputs=[pin(p)for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=900)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*sorted(inputs.glob('*.java'))]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.'+entry,S/'config/server.properties',run/'runtime',run/'observations.json',jar,'before'if before else'after',*([additional]if additional else[])])
 observations=read(run/'observations.json')
 if additional:assert observations['cases']==(80 if additional=='aliases'else 16) and observations['old_wrong_Source_effects']==(4 if before and additional=='npc'else 0)
 elif conditions:assert not before and observations['cases']==96 and observations['actual_kinds']==3 and observations['conditions']==16
 elif lifecycle or publication:assert not before and observations['cases']==(64 if lifecycle else 32) and observations['new_scene_ghost_hits']==0 and observations['old_scene_unresolved_targets']==0
 elif player:assert observations['cases']==64 and observations['old_wrong_Source_effects']==(16 if before else 0)
 else:assert observations['cases']==144 and observations['actual_kinds']==3 and observations['old_missing_targets']==(132 if before else 0)
 v.update(status=('observed-ordinary-NPC-Source-target-range-gap'if before else'passed-'+additional+'-Source-primary-target-and-shared40-lease')if additional else'passed-96-actual-NPC-Source-target-conditions-raw44-alias40-and-resets'if conditions else'passed-64-real-selected-owner-pair-48-lock-waits-actor-life-scene-material-STOPPED'if lifecycle else'passed-32-post-commit-actor-life-pose-same-ID-role-publication'if publication else('observed-16-wrong-Source-player103-effects'if before else'passed-64-Source-player-target-selection')if player else('observed-132-missing-legal-controlled-NPC-blind-targets'if before else'passed-144-two-skills-actual-kinds-native-relations-normal-Ctrl'),observations=observations)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-5000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
