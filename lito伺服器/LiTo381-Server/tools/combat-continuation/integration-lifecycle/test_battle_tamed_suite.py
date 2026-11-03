"""Sequential scoped qualification of one immutable candidate; never a release gate claim."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,subprocess,shutil
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
build=read(D/'battle-tamed-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256']
run=D/'battle-tamed-suite'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name)
specs=[('test_battle_tamed.py',[],'battle-tamed-tests',24),('test_battle_pet_lifecycle.py',[],'battle-pet-lifecycle',80),('test_battle_tamed_release_faults.py',[],'battle-tamed-release-faults',24),('test_battle_tamed_progress_faults.py',[],'battle-tamed-progress-faults',24),('test_battle_tamed_gm.py',[],'battle-tamed-GM',12),('test_tamed.py',['--integrated'],'tamed-on-battle-tests',12),('test_tamed.py',['--integrated','--capture'],'tamed-on-battle-capture-tests',12)]
v=dict(status='running',runtime=build['runtime'],build=build['manifest'],driver=pin(run/Path(__file__).name),results=[],full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 for index,(name,args,receipt,count) in enumerate(specs):
  assert read(D/'battle-tamed-build-current.json')['runtime']==build['runtime'] and sha(jar)==build['runtime']['sha256']
  log=run/(receipt+'.log');print(f'RUN {index+1}/{len(specs)} {receipt}',flush=True)
  with log.open('w',encoding='utf-8') as output:subprocess.run(['python','-B','-X','utf8',str(R/name),*args],cwd=S,stdout=output,stderr=subprocess.STDOUT,check=True,timeout=300)
  current=read(D/(receipt+'-current.json'));assert current['status'].startswith('passed') and current['runtime']==build['runtime'];manifest=Path(current['manifest']['path']);assert sha(manifest)==current['manifest']['sha256'];m=read(manifest);assert m['observations']['cases']==count
  v['results'].append(dict(name=receipt,receipt=pin(D/(receipt+'-current.json')),manifest=current['manifest'],observations=m['observations'],log=pin(log)));write(run/'manifest.json',v);print(f'PASS {receipt} cases={count}',flush=True)
 assert sum(r['observations']['cases'] for r in v['results'])==188
 v.update(status='passed-scoped-BATTLE-tamed-GM-progression-and-WORLD-compatibility',cases=188,checks=sum(r['observations']['checks'] for r in v['results']))
except BaseException as error:v.update(status='failed',error=str(error));raise
finally:write(run/'manifest.json',v);write(D/'battle-tamed-suite-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'],checks=v['checks'])))
