"""Finish required native checks strictly sequentially; reuse only an exact candidate receipt."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,sys,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'blind-scene-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];run=D/'blind-scene-sequential-verification'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name)
specs=[('test_blind_scene.py',[],'blind-scene-after',144),('test_blind_scene.py',['--source-player'],'blind-source-player-after',64),('test_blind_npc_faults.py',[],'blind-NPC-material-faults',48),('test_blind_scene_regressions.py',[],'blind-scene-area-regressions-adapter',232)]
v=dict(status='running-native-fixtures-sequentially',runtime=build['runtime'],results=[],full_goal_achieved=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);log=run/'driver.log'
def verify(value):
 if isinstance(value,dict):
  if'path'in value and'sha256'in value:assert sha(Path(value['path']))==value['sha256'],value['path']
  for x in value.values():verify(x)
 elif isinstance(value,list):
  for x in value:verify(x)
try:
 for name,args,receipt,cases in specs:
  index=D/(receipt+'-current.json');reuse=False
  if index.exists():
   i=read(index);reuse=i['status'].startswith('passed')and i['runtime']==build['runtime']
   if reuse:verify(i);m=read(Path(i['manifest']['path']));verify(m)
  log=run/(receipt+'.log');print('RUN '+receipt,flush=True)
  with log.open('w',encoding='utf-8')as out:
   if reuse:out.write('Reused immutable recursively hash-verified receipt for exact runtime.\n')
   else:subprocess.run([sys.executable,'-B','-X','utf8',str(R/name),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=2200)
  i=read(index);assert i['status'].startswith('passed')and i['runtime']==build['runtime'];verify(i);m=read(Path(i['manifest']['path']));verify(m);observations=m.get('observations',m);assert observations['cases']==cases;frozen=run/index.name;shutil.copyfile(index,frozen);v['results'].append(dict(driver=pin(R/name),args=args,receipt=pin(frozen),manifest=i['manifest'],cases=cases,log=pin(log)));write(run/'manifest.json',v);print('PASS '+receipt+' cases='+str(cases),flush=True)
 v.update(status='passed-sequential-488-core-Source-player-NPC-fault-and-original-area-checks',cases=sum(r['cases']for r in v['results']));assert v['cases']==488
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:write(run/'manifest.json',v);write(D/'blind-scene-sequential-verification-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
