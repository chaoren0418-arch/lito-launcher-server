"""Required candidate checks, strictly sequential; each receipt is frozen under the exact candidate."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,sys,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build';read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
build=read(D/'shock-companion-build-current.json');assert sha(Path(build['runtime']['path']))==build['runtime']['sha256'];run=D/'shock-companion-sequential-verification'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name)
specs=[('reproduce_shock_companion.py',[],'shock-companion-reproduction',None),('test_shock_companion_admission.py',[],'shock-companion-admission-after',4),('test_shock_companion_boundaries.py',[],'shock-companion-boundaries',276),('test_shock_companion_publication.py',[],'shock-companion-publication-after',60),('test_shock_companion_controls.py',[],'shock-companion-controls',96),('test_shock_companion_death_bags.py',[],'shock-companion-death-bags',24)]
for kind in ['gap','actors','life','publication','fault','originals','gates','area']:specs.append(('test_shock_prior_regressions.py',[kind],'shock-prior-negative-'+kind+'-adapter',None))

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
 v.update(status='passed-sequential-shock-companion-required-checks',cases=sum(r['cases']or 0 for r in v['results']))
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:write(run/'manifest.json',v);write(D/'shock-companion-sequential-verification-current.json',dict(status=v['status'],runtime=build['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
