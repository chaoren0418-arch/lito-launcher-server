from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'static-replacement-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];seed=read(Path(build['manifest']['path']));run=D/'static-respawn-metadata-audit'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for p in [Path(__file__),R/'StaticRespawnMetadataAuditProbe.java',R/'ObserveStaticRespawnMetadata.java']:shutil.copyfile(p,inputs/p.name)
sources=['model/skill/skillmode/CANCELLATION.java','model/Instance/L1NpcInstance.java','model/L1Spawn.java','model/spawn/NpcSpawnService.java','utils/L1SpawnUtil.java'];Source=[]
for name in sources:
 p=Path('D:/lineage開發/1-伺服器/src/com/lineage/server')/name;q=run/'Source'/p.name;q.parent.mkdir(exist_ok=True);shutil.copyfile(p,q);Source.append(dict(original=pin(p),frozen=pin(q)))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');tcp=';'.join(map(str,[run/'tools',*[r['path']for r in seed['asm']]]));env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'audit.log';v=dict(status='running-static-readonly-no-Hub',runtime=build['runtime'],Source=Source,inputs=[pin(p)for p in inputs.iterdir()],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 with log.open('w',encoding='utf-8')as out:
  def invoke(args):subprocess.run(list(map(str,args)),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
  invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-cp',tcp,'-d',run/'tools',inputs/'ObserveStaticRespawnMetadata.java']);invoke([J/'java.exe','-Xverify:all','-cp',tcp,'ObserveStaticRespawnMetadata',build['runtime']['path'],run/'observer']);cp=';'.join(map(str,[run/'classes',run/'observer',build['runtime']['path'],S/'lib/*']));invoke([J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',cp,'-d',run/'classes',inputs/'StaticRespawnMetadataAuditProbe.java']);invoke([J/'java.exe','-Xverify:all','-cp',cp,'local.lito381.server.StaticRespawnMetadataAuditProbe',S/'config/server.properties',run/'spawnlist_npc.tsv'])
 o=read(run/'observations.json');v.update(status='audited-static-spawnlist-respawn-metadata-not-native-lifecycle-qualification',observations=o,table=pin(run/'spawnlist_npc.tsv'))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-1800:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'static-respawn-metadata-audit-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],observations=v['observations'])))
