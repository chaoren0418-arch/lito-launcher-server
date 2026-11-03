"""Strictly sequential affected-path replay; preserve parent receipts and native process bounds."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
build=read(D/'static-motion-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256']
phase_only='--phase-only' in sys.argv;suite='static-motion-phase-suite' if phase_only else 'static-motion-suite';run=D/suite/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name)
specs=[('test_static_motion.py',[],'static-motion-tests',8),('test_static_motion_lifecycle.py',[],'static-motion-lifecycle-tests',24),('test_battle_tamed.py',['--static'],'on-static-battle-tamed-tests',24),('test_battle_pet_lifecycle.py',['--static'],'on-static-battle-pet-lifecycle',80),('test_battle_tamed_release_faults.py',['--static'],'on-static-battle-tamed-release-faults',24),('test_battle_tamed_progress_faults.py',['--static'],'on-static-battle-tamed-progress-faults',24),('test_battle_tamed_gm.py',['--static'],'on-static-battle-tamed-GM',12),('test_tamed.py',['--static','--integrated'],'on-static-tamed-on-battle-tests',12),('test_tamed.py',['--static','--integrated','--capture'],'on-static-tamed-on-battle-capture-tests',12),('test_npc_weapon_hp.py',['--static','--actions'],'on-static-npc-action-HP',176),('test_npc_action_lifecycle.py',['--static'],'on-static-npc-action-lifecycle',36),('test_npc_physical_area_lifecycle.py',['--static'],'on-static-npc-physical-area-lifecycle',26)]
specs[2:2]=[('review_source.py',['--static'],'on-static-source-drift-review',0),('test_pair.py',['--static'],'on-static-pair-tests',288),('test_pair_main.py',['--static'],'on-static-pair-Main',10),('test_skill73_main.py',['--static'],'on-static-skill73-Main',8)]
if phase_only:specs=[s for s in specs if s[0] in ['test_static_motion.py','test_static_motion_lifecycle.py','review_source.py','test_pair_main.py','test_skill73_main.py']]
v=dict(status='running',runtime=build['runtime'],build=build['manifest'],driver=pin(run/Path(__file__).name),results=[],full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
try:
 for index,(name,args,receipt,count) in enumerate(specs):
  assert read(D/'static-motion-build-current.json')['runtime']==build['runtime'] and sha(jar)==build['runtime']['sha256']
  log=run/(receipt+'.log');print(f'RUN {index+1}/{len(specs)} {receipt}',flush=True)
  with log.open('w',encoding='utf-8') as output:subprocess.run([sys.executable,'-B','-X','utf8',str(R/name),*args],cwd=S,stdout=output,stderr=subprocess.STDOUT,check=True,timeout=300)
  current=read(D/(receipt+'-current.json'));assert current['status'].startswith('passed') and current['runtime']==build['runtime'];manifest=Path(current['manifest']['path']);assert sha(manifest)==current['manifest']['sha256'];m=read(manifest)
  if name=='review_source.py':observations=dict(cases=0,checks=None)
  elif name=='test_pair.py':
   selected=[r['results'] for r in m['results'] if r['runtime']==build['runtime']];observations=dict(cases=sum(r['cases'] for r in selected),checks=sum(r['checks'] for r in selected) if all('checks' in r for r in selected) else None)
  elif name in ['test_pair_main.py','test_skill73_main.py']:observations=dict(cases=len(m['results']),checks=None)
  else:observations=m['observations']
  assert observations['cases']==count
  snapshots=run/'receipt-snapshots';snapshots.mkdir(exist_ok=True);snapshot=snapshots/(receipt+'-current.json');shutil.copyfile(D/(receipt+'-current.json'),snapshot)
  v['results'].append(dict(name=receipt,receipt=pin(snapshot),manifest=current['manifest'],observations=observations,log=pin(log)));write(run/'manifest.json',v);print(f'PASS {receipt} cases={count}',flush=True)
 cases=50 if phase_only else 764;assert sum(r['observations']['cases'] for r in v['results'])==cases
 v.update(status='passed-scoped-static-motion-rest-phase-and-Main' if phase_only else 'passed-scoped-static-motion-paired-Main-and-inherited-NPC-TAMED-lifecycle',cases=cases,kernel_cases=294917,known_checks=sum(r['observations']['checks'] or 0 for r in v['results']),check_count_complete=False)
except BaseException as error:v.update(status='failed',error=str(error));raise
finally:write(run/'manifest.json',v);write(D/(suite+'-current.json'),dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'],kernel_cases=v['kernel_cases'],known_checks=v['known_checks'])))
