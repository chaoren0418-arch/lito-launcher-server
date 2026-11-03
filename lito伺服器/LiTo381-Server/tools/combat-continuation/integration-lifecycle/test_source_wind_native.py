"""Real Main/Hub/socket fixture runs sequentially by caller; no PC/NPC overlays."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,sys
R=Path(__file__).resolve().parent;S=R.parents[2];C=R.parent;D=R/'build';B=S/'tools/resource-database/build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind=sys.argv[1];before='--before'in sys.argv
spec={'state':('State',72),'boundary':('Boundary',696),'player-boundary':('PlayerBoundary',96),'io':('Io',144),'player-io':('PlayerIo',48),'faults':('Fault',48)}
suffix,expected=spec[kind];cls='SourceWind'+suffix+'Probe';source=R/'source167-fixtures'/(cls+'.java');versions=read(source.parent/'manifest.json');version=next(x for x in versions['rows']if x['versioned']['path']==str(source.resolve()))
for k in ['original','versioned']:assert sha(Path(version[k]['path']))==version[k]['sha256']
inverse=source.read_text('utf-8')
for old,new in reversed(version['changes']):assert new in inverse;inverse=inverse.replace(new,old)
assert inverse==Path(version['original']['path']).read_text('utf-8')
build=read(D/('source-mass-slow-pipeline-build-current.json'if before else'source-wind-target-build-current.json'));jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256']
name='source167-'+kind+('-before'if before else'-after');run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
shared=[R/n for n in ['ShockPlayerAdmissionProbe.java','ShockCompanionAdmissionProbe.java','BlindSceneProbe.java','FreezeAcceptedStageProbe.java','SourceMaterialRangePairProbe.java','SourceMaterialRangeProbe.java','CompanionPlacementProbe.java']]+[C/'return-nature-material/ReturnNatureMaterialProbe.java']
for p in [Path(__file__),source,*shared]:shutil.copyfile(p,inputs/p.name)
fault=kind=='faults';overlay=run/'memory-observer'
if fault:
 shutil.copyfile(B/'ObserveDeItemTransaction.java',inputs/'ObserveDeItemTransaction.java');(inputs/'DeItemTransactionProbe.java').write_text('package local.lito381.server;public class DeItemTransactionProbe {public static void beforeSave(){'+cls+'.beforeSave();}public static void beforeFlush()throws Exception{'+cls+'.beforeFlush();}}\n','utf-8')
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,*([overlay]if fault else[]),C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');log=run/'test.log';v=dict(status='running',runtime=build['runtime'],versioned_Source_fixture=version,inputs=[pin(p)for p in inputs.iterdir()],production_PC_or_NPC_overlays=0,full_goal_achieved=False,qualified_for_release=False,all_target_intent_policy_qualified=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=900)
  if fault:
   bm=read(Path(build['manifest']['path']));tcp=';'.join(map(str,[run/'tools',*[x['path']for x in bm['asm']]]));invoke([J/'javac.exe','-g:none','-cp',tcp,'-d',run/'tools',inputs/'ObserveDeItemTransaction.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'ObserveDeItemTransaction',jar,overlay]);assert [p.name for p in overlay.rglob('*.class')]==['GameStateStore.class'];v['memory_observation_overlay']=pin(overlay/'local/lito381/server/GameStateStore.class')
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*[p for p in sorted(inputs.glob('*.java'))if p.name!='ObserveDeItemTransaction.java']]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.'+cls,S/'config/server.properties',run/'runtime',run/'observations.json',jar,'before'if before else'after'])
 o=read(run/'observations.json');assert o['cases']==expected;assert o.get('observation_only',False)==before
 text=log.read_text('utf-8');assert 'Vital completion failed'not in text and 'role publication capture requires actual mutation lock'not in text
 v.update(status='observed-Source167-old-controlled-status-mutation'if before else'passed-native-Source167-'+kind,observations=o)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
