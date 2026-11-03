"""Sequential exact-runtime qualification; Source and release completion are separate gates."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
build=read(D/'source-mass-slow-target-build-current.json');verify(build);runtime=build['runtime']
run=D/'source-mass-slow-qualification'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name)
groups=[]
for suffix,script,n in [('controlled-after','controlled',24),('mixed','mixed',24),('state','state',72),('Ctrl-area','ctrl_area',12),('material-faults','faults',48),('io','io',144),('player-io','player_io',48),('player-boundary','player_boundary',96),('boundary','boundary',696)]:
 groups.append(('source-mass-slow-'+suffix,'test_source_mass_slow_'+script+'.py',[],n))
for kind,n in [('scene',16),('lifecycle',64),('publication',32),('secondary',16),('secondary-retirement',16)]:groups.append(('source76-legacy-'+kind,'test_source_mass_slow_legacy.py',[kind],n))
for kind,n in [('boundary',420),('publication',84),('io',72),('faults',24)]:groups.append(('source76-battle44-retained-'+kind,'test_source_mass_slow_battle44_retained.py',[kind],n))
for kind,n in {'194-stage':16,'194-boundary':64,'194-completion':64,'194-duration':8,'194-io':32,'194-relations':16,'194-shape':8,'194-range':8,'194-range-boundary':16,'npc':126,'pc50-stage':16,'pc50-pre':64,'pc50-post':64,'pc50-duration':8,'pc50-io':32,'pc50-relations':16,'pc80-stage':8,'pc80-pre':36,'pc80-post':48,'pc80-duration':4,'pc80-io':16,'pc80-producer':12,'pc80-action':4,'hp':26,'hp-main':2,'shock-mutation':64,'shock-publication':64,'shock-death':8,'range':24,'screen-reject':24,'screen-boundary':192,'range-pair':24,'npc-range':144,'gates':352,'area':232}.items():
 groups.append(('mass-slow-prior-prior-retained-'+kind,'test_source_mass_slow_retained.py',[kind],n))
assert sum(g[3]for g in groups)==3750
v=dict(status='running',runtime=runtime,results=[],full_goal_achieved=False,qualified_for_release=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False);log=run/'runner.log';log.write_text('Native Main/Hub/socket groups run strictly sequentially.\n','utf-8')
try:
 for name,script,args,expected in groups:
  assert read(D/'source-mass-slow-target-build-current.json')['runtime']==runtime
  index=D/(name+'-current.json');reused=False
  if index.exists():
   i=read(index)
   if i['status'].startswith('passed')and i.get('runtime')==runtime:
    verify(i);m=read(Path(i['manifest']['path']));verify(m);reused=True
    for row in m.get('inputs',[]):
     path=R/Path(row['path']).name
     if path.exists()and path.name.startswith('SourceMassSlow')and sha(path)!=row['sha256']:reused=False
  branch=run/(name+'.log');print(('REUSE 'if reused else'RUN ')+name,flush=True)
  if reused:branch.write_text('Reused hash-verified immutable result for this exact runtime.\n','utf-8')
  else:
   with branch.open('w',encoding='utf-8')as out:subprocess.run([sys.executable,'-B','-X','utf8',str(R/script),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=1800)
  i=read(index);verify(i);assert i['status'].startswith('passed')and i['runtime']==runtime
  m=read(Path(i['manifest']['path']));verify(m);o=m.get('observations',m);assert o['cases']==expected
  frozen=run/(name+'-current.json');shutil.copyfile(index,frozen)
  v['results'].append(dict(name=name,cases=expected,known_checks=o.get('checks',o.get('known_checks'))or 0,receipt=pin(frozen),manifest=i['manifest'],log=pin(branch),reused=reused));write(run/'manifest.json',v);print('PASS '+name+' cases='+str(expected),flush=True)
 v.update(status='passed-scoped-3750-exact-SHA-Source76-and-retained-native-cases',native_cases=sum(x['cases']for x in v['results']),known_checks=sum(x['known_checks']for x in v['results']),check_count_complete=False)
except BaseException as error:
 v.update(status='failed',error=str(error));print(branch.read_text('utf-8',errors='replace')[-3500:]);raise
finally:
 write(run/'manifest.json',v);write(D/'source-mass-slow-qualification-current.json',dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],native_cases=v['native_cases'],known_checks=v['known_checks'])))
