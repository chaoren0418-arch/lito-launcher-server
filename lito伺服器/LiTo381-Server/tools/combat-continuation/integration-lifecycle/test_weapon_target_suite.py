"""Pinned skill27 target/receipt regression, strictly sequential native processes."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib,subprocess,shutil,sys
R=Path(__file__).resolve().parent;S=R.parents[2];D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'));sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
build=read(D/'weapon-target-build-current.json');jar=Path(build['runtime']['path']);assert sha(jar)==build['runtime']['sha256']
run=D/'weapon-target-suite'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');run.mkdir(parents=True);shutil.copyfile(__file__,run/Path(__file__).name)
specs=[('review_source.py',['--weapon'],'on-weapon-source-drift-review',0),('test_weapon_npc_epoch.py',[],'weapon-NPC-epoch',96),('test_weapon_npc_epoch.py',['--ctrl'],'weapon-NPC-epoch-Ctrl',96),('test_weapon_npc_epoch.py',['--companions'],'weapon-companion-epoch',60),('test_weapon_npc_epoch.py',['--companions','--ctrl'],'weapon-companion-epoch-Ctrl',60),('replay_weapon_targets.py',[],'weapon-player-replay',200),('test_pair.py',['--weapon'],'on-weapon-pair-tests',288),('test_npc_physical_area_lifecycle.py',['--weapon'],'on-weapon-npc-physical-area-lifecycle',26),('test_pair_main.py',['--weapon'],'on-weapon-pair-Main',10),('test_skill73_main.py',['--weapon'],'on-weapon-skill73-Main',8)]
specs[5:5]=[('test_weapon_npc_epoch.py',['--battle-companions'],'weapon-BATTLE-companion-epoch',60),('test_weapon_npc_epoch.py',['--battle-companions','--ctrl'],'weapon-BATTLE-companion-epoch-Ctrl',60),('test_weapon_npc_epoch.py',['--companions','--semantics'],'weapon-companion-epoch-semantics',60),('test_weapon_npc_epoch.py',['--battle-companions','--semantics'],'weapon-BATTLE-companion-epoch-semantics',60)]
specs.append(('test_weapon_npc_material_fault.py',[],'weapon-NPC-material-fault',32))
specs.append(('test_weapon_mana_alias.py',[],'weapon-MP-alias',20))
specs.append(('test_weapon_class_matrix.py',[],'weapon-class-matrix',1176))
v=dict(status='running',runtime=build['runtime'],build=build['manifest'],driver=pin(run/Path(__file__).name),results=[],full_goal_achieved=False,same_SHA_full_regression_passed=False,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
def observations(name,m):
 if name=='review_source.py':return dict(cases=0,checks=None)
 if name=='test_pair.py':
  rows=[r['results'] for r in m['results'] if r['runtime']==build['runtime']];return dict(cases=sum(r['cases'] for r in rows),checks=sum(r['checks'] for r in rows) if all('checks' in r for r in rows) else None)
 if name in ['test_pair_main.py','test_skill73_main.py']:return dict(cases=len(m['results']),checks=None)
 if name=='replay_weapon_targets.py':return dict(cases=m['cases'],checks=m['checks'])
 return m['observations']
try:
 for i,(name,args,receipt,count) in enumerate(specs):
  assert read(D/'weapon-target-build-current.json')['runtime']==build['runtime'] and sha(jar)==build['runtime']['sha256'];index=D/(receipt+'-current.json');reuse=False
  if index.exists():
   old=read(index)
   if old['status'].startswith('passed') and old['runtime']==build['runtime']:
    path=Path(old['manifest']['path']);assert sha(path)==old['manifest']['sha256'];reuse=observations(name,read(path))['cases']==count
  log=run/(receipt+'.log');print(f'{"REUSE" if reuse else "RUN"} {i+1}/{len(specs)} {receipt}',flush=True)
  with log.open('w',encoding='utf-8') as out:
   if reuse:out.write('Reused exact runtime and hash-verified immutable receipt.\n')
   else:subprocess.run([sys.executable,'-B','-X','utf8',str(R/name),*args],cwd=S,stdout=out,stderr=subprocess.STDOUT,check=True,timeout=600)
  current=read(index);assert current['status'].startswith('passed') and current['runtime']==build['runtime'];path=Path(current['manifest']['path']);assert sha(path)==current['manifest']['sha256'];result=observations(name,read(path));assert result['cases']==count
  snapshots=run/'receipt-snapshots';snapshots.mkdir(exist_ok=True);snapshot=snapshots/index.name;shutil.copyfile(index,snapshot)
  v['results'].append(dict(name=receipt,receipt=pin(snapshot),manifest=current['manifest'],observations=result,log=pin(log)));write(run/'manifest.json',v);print(f'PASS {receipt} cases={count}',flush=True)
 assert sum(r['observations']['cases'] for r in v['results'])==2312
 v.update(status='passed-scoped-weapon-target-epoch-pair-Ctrl-area-and-Main',cases=2312,known_checks=sum(r['observations']['checks'] or 0 for r in v['results']),check_count_complete=False)
except BaseException as error:v.update(status='failed',error=str(error));print(log.read_text('utf-8',errors='replace')[-6500:]);raise
finally:write(run/'manifest.json',v);write(D/'weapon-target-suite-current.json',dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json')))
print(json.dumps(dict(status=v['status'],cases=v['cases'],known_checks=v['known_checks'])))
