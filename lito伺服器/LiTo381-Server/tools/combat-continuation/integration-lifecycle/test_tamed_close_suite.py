from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
build=read(D/'tamed-close-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];run=D/'tamed-close-suite'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name)
specs=[('review_source.py',['--tamed-close'],'on-close-source-drift-review',0),('test_tamed_scene_close.py',[],'tamed-scene-close',2),('test_tamed.py',['--tamed-close','--integrated'],'on-close-tamed-on-battle-tests',12),('test_tamed.py',['--tamed-close','--integrated','--capture'],'on-close-tamed-on-battle-capture-tests',12),('test_pair_main.py',['--tamed-close'],'on-close-pair-Main',10),('test_skill73_main.py',['--tamed-close'],'on-close-skill73-Main',8)]
v=dict(status='running',runtime=build['runtime'],build=build['manifest'],results=[],full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 for name,args,receipt,count in specs:
  log=run/(receipt+'.log');print('RUN '+receipt,flush=True)
  with log.open('w',encoding='utf-8') as out:subprocess.run([sys.executable,'-B','-X','utf8',str(R/name),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=300)
  index=D/(receipt+'-current.json');current=read(index);assert current['status'].startswith('passed') and current['runtime']==build['runtime'];path=Path(current['manifest']['path']);assert sha(path)==current['manifest']['sha256'];m=read(path)
  result=dict(cases=0,checks=None) if name=='review_source.py' else dict(cases=len(m['results']),checks=None) if name in ['test_pair_main.py','test_skill73_main.py'] else m['observations'];assert result['cases']==count
  snapshots=run/'receipt-snapshots';snapshots.mkdir(exist_ok=True);snapshot=snapshots/index.name;shutil.copyfile(index,snapshot);v['results'].append(dict(name=receipt,receipt=pin(snapshot),manifest=current['manifest'],observations=result,log=pin(log)));write(run/'manifest.json',v);print('PASS '+receipt+' cases='+str(count),flush=True)
 assert sum(r['observations']['cases'] for r in v['results'])==44;v.update(status='passed-scoped-TAMED-scene-close-normal-capture-release-and-Main',cases=44,known_checks=sum(r['observations']['checks'] or 0 for r in v['results']),check_count_complete=False)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:write(run/'manifest.json',v);write(D/'tamed-close-suite-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'],known_checks=v['known_checks'])))
