from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,sys
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
before='--before' in sys.argv;build=read(R/'current.json' if before else D/'weapon-target-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];name='weapon-MP-alias'+('-before' if before else '');run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
numeric=read(Path(read(D/'npc-weapon-numeric-current.json')['manifest']['path']));oracle=next(Path(p['path']) for p in numeric['inputs'] if p['path'].endswith('SourceNpcDmgModeOracle.java'))
for p in [Path(__file__),R/'WeaponBreakManaAliasProbe.java',R/'WeaponBreakNpcEpochProbe.java',R/'BattleTamedLifecycleProbe.java',R/'NpcWeaponBreakHpProbe.java',C/'return-nature-material/ReturnNatureMaterialProbe.java',oracle]:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');log=run/'test.log';results=[];v=dict(status='running',runtime=build['runtime'],inputs=[pin(p) for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=240)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*sorted(inputs.glob('*.java'))])
  for ctrl in [False,True]:
   branch=run/str(ctrl).lower();branch.mkdir();invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.WeaponBreakManaAliasProbe',S/'config/server.properties',branch/'runtime',branch/'observations.json',jar,'before' if before else 'after','NPC','ctrl' if ctrl else 'normal','alias']);result=read(branch/'observations.json');assert result['cases']==10 and result['missing_current_caster_MP']==(10 if before else 0);results.append(dict(ctrl=ctrl,observations=result,receipt=pin(branch/'observations.json')))
 v.update(status='observed-parent-MP-numeric-payload-alias' if before else 'passed-opcode-aware-MP-and-retired-NPC-output',observations=dict(cases=20,checks=sum(r['observations']['checks'] for r in results),missing_current_caster_MP=sum(r['observations']['missing_current_caster_MP'] for r in results)),results=results)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
