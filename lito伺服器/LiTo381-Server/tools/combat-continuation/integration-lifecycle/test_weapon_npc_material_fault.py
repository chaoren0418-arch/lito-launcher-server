"""Only the previously qualified memory observation hook is retargeted; native fixtures run sequentially."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
build=read(D/'weapon-target-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];seed=read(Path(build['manifest']['path']))
qualified=read(Path(read(C/'weapon-break-lifetime/build/replay/current.json')['receipt']['path']));assert qualified['status'].startswith('passed');basis=next(r for r in qualified['results'] if r['probe']=='WeaponBreakFaultProbe');observer=next(p for p in basis['compiled_inputs'] if p['path'].endswith('GameStateStore.class'));assert sha(Path(observer['path']))==observer['sha256']
with zipfile.ZipFile(jar) as new,zipfile.ZipFile(qualified['runtime']['path']) as old:assert new.read('local/lito381/server/GameStateStore.class')==old.read('local/lito381/server/GameStateStore.class')
run=D/'weapon-NPC-material-fault'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
numeric=read(Path(read(D/'npc-weapon-numeric-current.json')['manifest']['path']));oracle=next(Path(p['path']) for p in numeric['inputs'] if p['path'].endswith('SourceNpcDmgModeOracle.java'))
for p in [Path(__file__),R/'RetargetWeaponMemoryObserver.java',R/'WeaponBreakNpcMaterialFaultProbe.java',R/'WeaponBreakNpcEpochProbe.java',R/'WeaponBreakCompanionSemanticsProbe.java',R/'BattleTamedLifecycleProbe.java',R/'NpcWeaponBreakHpProbe.java',C/'return-nature-material/ReturnNatureMaterialProbe.java',oracle]:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');tools=run/'tools';tools.mkdir();tcp=';'.join(map(str,[tools,*[p['path'] for p in seed['asm']]]));log=run/'test.log';results=[];v=dict(status='running',runtime=build['runtime'],inputs=[pin(p) for p in inputs.iterdir()],original_observer=observer,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=240)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',tools,inputs/'RetargetWeaponMemoryObserver.java']);adapted=run/'observer';invoke([J/'java.exe','-Xverify:all','-cp',tcp,'RetargetWeaponMemoryObserver',observer['path'],adapted])
  classes=run/'classes';F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[adapted,classes,C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']))
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*[p for p in sorted(inputs.glob('*.java')) if p.name!='RetargetWeaponMemoryObserver.java']])
  for flag in [False,True]:
   branch=run/str(flag).lower();branch.mkdir();invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.WeaponBreakNpcMaterialFaultProbe',S/'config/server.properties',branch/'runtime',branch/'observations.json',jar,'after','NPC','normal','fault',str(flag).lower()]);result=read(branch/'observations.json');assert result['cases']==16 and result['actual_memory_faults']==16 and result['partial_commits']==0;results.append(dict(flag=flag,observations=result,receipt=pin(branch/'observations.json')))
 v.update(status='passed-NPC-companion-material-memory-fault-rollback-and-immediate-retry',observations=dict(cases=32,checks=sum(r['observations']['checks'] for r in results),actual_memory_faults=32,partial_commits=0),results=results,adapted_observer=pin(adapted/'local/lito381/server/GameStateStore.class'),production_PC_overlays=0)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'weapon-NPC-material-fault-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
