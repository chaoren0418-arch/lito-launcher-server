"""Actual candidate classes with Source-backed visual fixture versions; no PC/NPC overlays."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,sys
R=Path(__file__).resolve().parent;S=R.parents[2];C=R.parent;D=R/'build';B=S/'tools/resource-database/build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
kind=sys.argv[1]
specs={
 'controlled':('SourceMassSlowControlledProbe',24), 'mixed':('SourceMassSlowMixedProbe',24), 'state':('SourceMassSlowStateProbe',72),
 'ctrl':('SourceMassSlowCtrlAreaProbe',12), 'faults':('SourceMassSlowFaultProbe',48), 'io':('SourceMassSlowIoProbe',144),
 'player-io':('SourceMassSlowPlayerIoProbe',48), 'player-boundary':('SourceMassSlowPlayerBoundaryProbe',96), 'boundary':('SourceMassSlowBoundaryProbe',696),
 'own':('SourceMassSlowOwnPrimaryProbe',64),
 'legacy-scene':('CompanionAreaSceneProbe',16), 'legacy-lifecycle':('CompanionAreaLifecycleProbe',64), 'legacy-publication':('CompanionAreaPublicationProbe',32),
 'legacy-secondary':('CompanionAreaSecondaryProbe',16), 'legacy-secondary-retirement':('CompanionAreaSecondaryRetirementProbe',16),
 'battle44-boundary':('BattleControlledCancellationBoundaryProbe',420), 'battle44-publication':('BattleControlledCancellationPublicationProbe',84),
 'battle44-io':('BattleControlledCancellationIoProbe',72), 'battle44-faults':('BattleControlledCancellationFaultProbe',24),
}
cls,expected=specs[kind];version_folder=R/('source76-pipeline-legacy-fixtures'if kind.startswith('legacy-')else'source76-pipeline-fixtures')
source=version_folder/(cls+'.java');version=None
if source.exists():
 basis=read(version_folder/'manifest.json');version=next(row for row in basis['rows']if Path(row['versioned']['path']).name==source.name)
 for key in ['original','versioned']:assert sha(Path(version[key]['path']))==version[key]['sha256']
 inverse=source.read_text('utf-8')
 for old,new in reversed(version['changes']):assert inverse.count(new)==1;inverse=inverse.replace(new,old)
 assert inverse==Path(version['original']['path']).read_text('utf-8')
else:source=R/(cls+'.java')
build=read(D/'source-wind-target-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];seed=read(Path(build['manifest']['path']))
run=D/('source167-retained-native-'+kind)/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
shared=[R/n for n in ['ShockPlayerAdmissionProbe.java','ShockCompanionAdmissionProbe.java','BlindSceneProbe.java','FreezeAcceptedStageProbe.java','SourceMaterialRangePairProbe.java','SourceMaterialRangeProbe.java','CompanionPlacementProbe.java']]+[C/'return-nature-material/ReturnNatureMaterialProbe.java']
for p in [Path(__file__),source,*shared]:shutil.copyfile(p,inputs/p.name)
fault=kind in ['faults','battle44-faults'];overlay=run/'memory-observer';classes=run/'classes';J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');F=B/'baseline-PC-ingress-efd4-v2'
if fault:
 shutil.copyfile(B/'ObserveDeItemTransaction.java',inputs/'ObserveDeItemTransaction.java')
 (inputs/'DeItemTransactionProbe.java').write_text('package local.lito381.server;public class DeItemTransactionProbe {public static void beforeSave(){'+cls+'.beforeSave();}public static void beforeFlush()throws Exception{'+cls+'.beforeFlush();}}\n','utf-8')
cp=';'.join(map(str,[classes,*([overlay]if fault else[]),C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']))
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');log=run/'test.log'
v=dict(status='running',runtime=build['runtime'],fixture=pin(source),versioned_Source_visual_fixture=version,inputs=[pin(p)for p in inputs.iterdir()],production_PC_or_NPC_overlays=0,full_goal_achieved=False,qualified_for_release=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=900)
  if fault:
   tcp=';'.join(map(str,[run/'tools',*[x['path']for x in seed['asm']]]));invoke([J/'javac.exe','-g:none','-cp',tcp,'-d',run/'tools',inputs/'ObserveDeItemTransaction.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'ObserveDeItemTransaction',jar,overlay]);assert [p.name for p in overlay.rglob('*.class')]==['GameStateStore.class'];v['memory_observation_overlay']=pin(overlay/'local/lito381/server/GameStateStore.class')
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*[p for p in sorted(inputs.glob('*.java'))if p.name!='ObserveDeItemTransaction.java']]);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.'+cls,S/'config/server.properties',run/'runtime',run/'observations.json',jar,'after'])
 o=read(run/'observations.json');assert o['cases']==expected and not o.get('observation_only',False)
 text=log.read_text('utf-8');assert 'Vital completion failed'not in text and 'role publication capture requires actual mutation lock'not in text
 v.update(status='passed-native-Source76-pipeline-'+kind,observations=o)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/('source167-retained-native-'+kind+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
