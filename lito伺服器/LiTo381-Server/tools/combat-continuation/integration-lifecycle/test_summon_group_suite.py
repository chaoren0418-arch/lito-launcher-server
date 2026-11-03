from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
build=read(D/'summon-group-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256'];run=D/'summon-group-suite'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name)
specs=[('review_source.py',['--summon-group'],'on-summon-group-source-drift-review',0),('test_summon_owner.py',['--summon-group'],'on-summon-group-summon-owner-tests',100),('test_tamed_scene_close.py',['--summon-group'],'on-summon-group-tamed-scene-close',2),('test_tamed.py',['--summon-group','--integrated'],'on-summon-group-tamed-on-battle-tests',12),('test_tamed.py',['--summon-group','--integrated','--capture'],'on-summon-group-tamed-on-battle-capture-tests',12),('test_battle_tamed.py',['--summon-group'],'on-summon-group-battle-tamed-tests',24),('test_battle_tamed_gm.py',['--summon-group'],'on-summon-group-battle-tamed-GM',12),('test_battle_tamed_release_faults.py',['--summon-group'],'on-summon-group-battle-tamed-release-faults',24),('test_battle_tamed_progress_faults.py',['--summon-group'],'on-summon-group-battle-tamed-progress-faults',24),('test_battle_pet_lifecycle.py',['--summon-group'],'on-summon-group-battle-pet-lifecycle',88),('test_pair.py',['--summon-group'],'on-summon-group-pair-tests',288),('test_npc_physical_area_lifecycle.py',['--summon-group'],'on-summon-group-npc-physical-area-lifecycle',26),('test_pair_main.py',['--summon-group'],'on-summon-group-pair-Main',10),('test_skill73_main.py',['--summon-group'],'on-summon-group-skill73-Main',8)]
specs[1:1]=[('test_create_zombie.py',['--summon-group'],'on-summon-group-create-zombie-lifecycle',56),('test_create_zombie_world.py',['--summon-group'],'on-summon-group-create-zombie-world',28),('test_create_zombie_world_faults.py',['--summon-group'],'on-summon-group-create-zombie-world-faults',28),('test_create_zombie_gm.py',['--summon-group'],'on-summon-group-create-zombie-GM-lifecycle',28)]
specs[1:1]=[('test_summon_group.py',['--after'],'summon-group-command-after',100),('test_summon_group_lifetime.py',[],'summon-group-lifetime',64),('test_create_zombie_gm_policy.py',['--after','--summon-group'],'on-summon-group-create-zombie-GM-policy-after',8)]
v=dict(status='running',runtime=build['runtime'],build=build['manifest'],results=[],full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 for name,args,receipt,count in specs:
  log=run/(receipt+'.log');print('RUN '+receipt,flush=True)
  index=D/(receipt+'-current.json');reuse=False
  if index.exists():
   prior=read(index);reuse=prior['status'].startswith('passed') and prior['runtime']==build['runtime']
   if reuse:assert sha(Path(prior['manifest']['path']))==prior['manifest']['sha256']
  with log.open('w',encoding='utf-8') as out:
   if reuse:out.write('Reused hash-verified immutable receipt for this exact candidate.\n')
   else:subprocess.run([sys.executable,'-B','-X','utf8',str(R/name),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=600)
  index=D/(receipt+'-current.json');current=read(index);assert current['status'].startswith('passed') and current['runtime']==build['runtime'];path=Path(current['manifest']['path']);assert sha(path)==current['manifest']['sha256'];m=read(path)
  result=dict(cases=0,checks=None) if name=='review_source.py' else dict(cases=len(m['results']),checks=None) if name in ['test_pair_main.py','test_skill73_main.py'] else dict(cases=sum(row['results']['cases'] for row in m['results'] if row['runtime']==build['runtime']),checks=sum(row['results']['checks'] for row in m['results'] if row['runtime']==build['runtime'])) if name=='test_pair.py' else m['observations'];assert result['cases']==count
  snapshots=run/'receipt-snapshots';snapshots.mkdir(exist_ok=True);snapshot=snapshots/index.name;shutil.copyfile(index,snapshot);v['results'].append(dict(name=receipt,receipt=pin(snapshot),manifest=current['manifest'],observations=result,log=pin(log)));write(run/'manifest.json',v);print('PASS '+receipt+' cases='+str(count),flush=True)
 assert sum(r['observations']['cases'] for r in v['results'])==942;v.update(status='passed-scoped-Source-group-command-and-zombie-TAMED-lifecycle-pair-Ctrl-area-Main',cases=942,known_checks=sum(r['observations']['checks'] or 0 for r in v['results']),check_count_complete=False)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-3500:]);raise
finally:write(run/'manifest.json',v);write(D/'summon-group-suite-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'],known_checks=v['known_checks'])))
