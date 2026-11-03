"""Required candidate checks, strictly sequential; each receipt is frozen under the exact candidate."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,sys,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'shock-player-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];run=D/'shock-player-sequential-verification'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name)
specs=[('reproduce_shock_player.py',[],'shock-player-reproduction',None),('test_shock_player_admission.py',[],'shock-player-admission-after',8),('test_shock_player_boundaries.py',[],'shock-player-boundaries',200),('test_shock_player_controls.py',[],'shock-player-controls',64),('test_shock_player_publication.py',[],'shock-player-publication',64),('test_shock_player_death_receipt.py',[],'shock-player-death-receipt',8),('test_shock_player_close.py',[],'shock-player-close',4),('test_shock_player_hp_enqueue_fault.py',[],'shock-player-hp-enqueue-fault',4),('test_shock_player_resurrection_prior.py',[],'shock-player-resurrection-originals',368),('test_shock_player_hp_prior.py',[],'shock-player-hp-originals',26),('test_shock_player_hp_main.py',[],'shock-player-hp-main',2)]
for kind in ['companion-admission','companion-boundaries','companion-publication','companion-controls','companion-death-bags','gap','actors','life','publication','fault','originals','gates','area']:specs.append(('test_shock_player_prior.py',[kind],'shock-player-prior-'+kind+'-adapter',None))

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
   if reuse:out.write('Reused recursively hash-verified exact candidate receipt.\n')
   else:subprocess.run([sys.executable,'-B','-X','utf8',str(R/name),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=2200)
  i=read(index);assert i['status'].startswith('passed')and i['runtime']==build['runtime'];verify(i);m=read(Path(i['manifest']['path']));verify(m);o=m.get('observations',m)
  if cases is not None:assert o.get('scenarios',o['cases'])==cases
  frozen=run/index.name;shutil.copyfile(index,frozen);v['results'].append(dict(driver=pin(R/name),args=args,receipt=pin(frozen),manifest=i['manifest'],cases=o.get('cases'),log=pin(log)));write(run/'manifest.json',v);print('PASS '+receipt,flush=True)
 v.update(status='passed-sequential-shock-player-required-checks',cases=sum(r['cases']or 0 for r in v['results']))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:write(run/'manifest.json',v);write(D/'shock-player-sequential-verification-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
