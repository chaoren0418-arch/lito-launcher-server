from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,shutil,subprocess,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
write=lambda p,v:p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
b=read(D/'companion-proc-scene-build-current.json');verify(b);run=D/'companion-proc-regressions'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name);v=dict(status='running',runtime=b['runtime'],basis=b['manifest'],results=[],full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);log=run/'runner.log';log.write_text('Sequential same-SHA native verification.\n','utf-8')
try:
 for name,receipt,cases in [('test_companion_proc_scene.py','companion-proc-scene-after',6),('test_companion_proc_legacy.py','companion-proc-legacy',887),('test_companion_proc_gates.py','companion-proc-gates',352)]:
  source=R/name;log=run/(name+'.log');index=D/(receipt+'-current.json');print('RUN '+name,flush=True)
  with log.open('w',encoding='utf-8') as out:
   if index.exists() and read(index)['status'].startswith('passed') and read(index)['runtime']==b['runtime']:out.write('Reused immutable hash-verified receipt for exact runtime.\n')
   else:subprocess.run([sys.executable,'-B','-X','utf8',source],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=1800)
  i=read(index);verify(i);assert i['runtime']==b['runtime'] and i['status'].startswith('passed');m=read(Path(i['manifest']['path']));verify(m);o=m.get('observations',dict(cases=m.get('cases'),checks=m.get('checks',m.get('known_checks'))));assert o['cases']==cases;frozen=run/index.name;shutil.copyfile(index,frozen);v['results'].append(dict(source=pin(source),receipt=pin(frozen),manifest=i['manifest'],observations=dict(cases=cases,checks=o.get('checks')),log=pin(log)));write(run/'manifest.json',v);print('PASS '+name+' cases='+str(cases),flush=True)
 v.update(status='passed-same-SHA-1245-Proc-original-AI-common-Ref-pair-area-and-Main-scope',cases=sum(x['observations']['cases'] for x in v['results']),known_checks=sum(x['observations']['checks'] or 0 for x in v['results']),check_count_complete=False);assert v['cases']==1245
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-2400:]);raise
finally:write(run/'manifest.json',v);write(D/'companion-proc-regressions-current.json',dict(status=v['status'],runtime=b['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'])))
