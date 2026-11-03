"""Seven learned-skill classes × three original-INT inputs × fourteen native targets × Ctrl/application flags."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,sys
R=Path(__file__).resolve().parent;C=R.parent;S=R.parents[2];B=S/'tools/resource-database/build';D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
build=read(D/'weapon-target-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];smoke='--smoke' in sys.argv;name='weapon-class-matrix'+('-smoke' if smoke else '');run=D/name/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
numeric=read(Path(read(D/'npc-weapon-numeric-current.json')['manifest']['path']));oracle=next(Path(p['path']) for p in numeric['inputs'] if p['path'].endswith('SourceNpcDmgModeOracle.java'))
for p in [Path(__file__),R/'WeaponBreakClassMatrixProbe.java',R/'WeaponBreakNpcEpochProbe.java',R/'BattleTamedLifecycleProbe.java',R/'NpcWeaponBreakHpProbe.java',C/'return-nature-material/ReturnNatureMaterialProbe.java',oracle]:shutil.copyfile(p,inputs/p.name)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';F=B/'baseline-PC-ingress-efd4-v2';cp=';'.join(map(str,[classes,C/'elemental-material/build/boundaries/classes',C/'build/tests/classes',B/'pc-native-signed-hp-development/signed-probe-fixtures',B/'pc-native-accept-development/fixtures',F,F/'fixture-support.zip',jar,S/'lib/*']));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 -Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64');log=run/'compile.log';results=[];v=dict(status='running',runtime=build['runtime'],inputs=[pin(p) for p in inputs.iterdir()],results=results,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8') as out:subprocess.run(list(map(str,[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',classes,*sorted(inputs.glob('*.java'))])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=90)
 for caster_type in ([0] if smoke else range(7)):
  for intelligence in ([10] if smoke else [10,18,25]):
   for ctrl in ([False] if smoke else [False,True]):
    branch=run/f'{caster_type}-{intelligence}-{ctrl}';branch.mkdir();log=branch/'test.log';print(f'RUN class={caster_type} originalInt={intelligence} Ctrl={ctrl}',flush=True)
    with log.open('w',encoding='utf-8') as out:subprocess.run(list(map(str,[J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.WeaponBreakClassMatrixProbe',S/'config/server.properties',branch/'runtime',branch/'observations.json',jar,'after','NPC','ctrl' if ctrl else 'normal','classes','true',caster_type,intelligence])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=240)
    result=read(branch/'observations.json');assert result['cases']==28 and result['class']==caster_type and result['originalInt']==intelligence;results.append(dict(caster_type=caster_type,originalInt=intelligence,ctrl=ctrl,observations=result,receipt=pin(branch/'observations.json'),log=pin(log)));write(run/'manifest.json',v)
 count=28 if smoke else 1176;assert sum(r['observations']['cases'] for r in results)==count;v.update(status='passed-skill27-class-originalINT-native-target-Ctrl-matrix',observations=dict(cases=count,checks=sum(r['observations']['checks'] for r in results)),results=results)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2500:]);raise
finally:v['last_log']=pin(log);write(run/'manifest.json',v);write(D/(name+'-current.json'),dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
