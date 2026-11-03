from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,os,zipfile
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
parent=read(R/'current.json');base=Path(parent['runtime']['path']);assert sha(base)==parent['runtime']['sha256']=='3bda9d5db34bb6ee95bce7dece848007344095c30cd1801d6537efa06b4f05df'
run=D/'companion-landing-builds'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'inputs';inputs.mkdir(parents=True)
for name in ['build_companion_landing.py','NativeCompanionLanding.java']:shutil.copyfile(R/name,inputs/name)
source=[]
for relative in ['utils/CheckUtil.java','utils/Teleportation.java','model/L1Location.java']:
 p=Path('D:/lineage開發/1-伺服器/src/com/lineage/server')/relative;q=inputs/relative;q.parent.mkdir(exist_ok=True);shutil.copyfile(p,q);source.append(dict(original=pin(p),frozen=pin(q)))
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');classes=run/'classes';env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m -Dfile.encoding=UTF-8');log=run/'build.log';v=dict(status='building',base=parent['runtime'],inputs=[pin(p) for p in inputs.iterdir() if p.is_file()],Source=source,full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False,production_dispatch_wired=False)
try:
 with log.open('w',encoding='utf-8') as out:subprocess.run(list(map(str,[J/'javac.exe','-g:none','-encoding','UTF-8','-implicit:none','-sourcepath','','-cp',';'.join(map(str,[base,S/'lib/*'])),'-d',classes,inputs/'NativeCompanionLanding.java'])),cwd=S,env=env,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=180)
 with zipfile.ZipFile(base) as z:entries={n:z.read(n) for n in z.namelist()};old=dict(entries)
 for p in classes.rglob('*.class'):entries[p.relative_to(classes).as_posix()]=p.read_bytes()
 changed=sorted(n for n in old if old[n]!=entries[n]);fresh=sorted(set(entries)-set(old));assert changed==[] and fresh==['local/lito381/server/NativeCompanionLanding$Terrain.class','local/lito381/server/NativeCompanionLanding.class'];jar=run/'candidate-companion-landing-component-unqualified.jar'
 with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
  for n,data in sorted(entries.items()):z.writestr(zipfile.ZipInfo(n,(1980,1,1,0,0,0)),data)
 v.update(status='built-unwired-Source-NPC-landing-search-component',runtime=pin(jar),changed=changed,fresh=fresh,new_workers_schedulers_ThreadLocals_registries_fields=0)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-4000:]);raise
finally:v['log']=pin(log);write(run/'manifest.json',v);write(D/'companion-landing-build-current.json',dict(status=v['status'],manifest=pin(run/'manifest.json'),**({'runtime':v['runtime']} if 'runtime' in v else {})))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'])))
