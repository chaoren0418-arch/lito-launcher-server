"""Seal already completed same-JAR evidence. Does not run, deploy, or mutate SQL."""
from pathlib import Path
from datetime import datetime,timezone
import json,hashlib
R=Path(__file__).resolve().parent;D=R/'build';ROOT=R.parent
read=lambda p:json.loads(p.read_text('utf8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def checked(ref):
 p=Path(ref['path']);assert sha(p)==ref['sha256'],str(p);return read(p)
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
b=read(D/'build.json');runtime=b['runtime'];assert runtime['sha256']=='13118eb96554c4652ae2db2a43b0c1f43c004210651601ca3c71accd76a41748';assert pin(Path(runtime['path']))==runtime
repro=read(R/'build-repro/build.json');assert repro['runtime']['sha256']==runtime['sha256'];assert sha(Path(repro['runtime']['path']))==runtime['sha256']
old=read(D/'qualification-current.json')
for ref in old['protected']:assert pin(Path(ref['path']))==ref
rows=read(D/'targeted/receipt.json');assert rows['status']=='passed' and rows['runtime']==runtime and len(rows['tests'])==22
counts=dict(cases=0,checks=0,socket_writes_under_locks=0,failed_material_partial_commits=0)
for row in rows['tests']+[dict(result=pin(D/'battle-lifecycle/result.json'))]:
 t=checked(row['result']);assert t['status'].startswith('passed') and t['runtime']==runtime
 values=[t['after']] if 'after'in t else [x['results']for x in t['results']if x['mode']=='after']
 assert values
 for v in values:
  counts['cases']+=v['cases'];counts['checks']+=v['checks']
  locked=v.get('locked_write_calls',v.get('locked_writes'));assert locked==0
  assert v.get('failed_material_casts_with_partial_commit',0)==0 and v.get('Source_window_bypasses',0)==0
assert counts['cases']==1688
composed=read(D/'composed-replay/current.json');assert composed['status']=='passed' and composed['runtime']==runtime
cm=checked(composed['receipt']);assert cm['status']=='passed' and cm['runtime']==runtime and len(cm['results'])==71
assert sum(x['results']['cases']for x in cm['results'])==5164
reg=ROOT/'build/call-nature-material-regression'/runtime['sha256'][:12]
refs={}
for key,name,count in [('existing_20','qualification-current.json',20),('affected_18','affected-current.json',18)]:
 p=reg/name;t=read(p);assert t['status'].startswith('passed');m=checked(t['manifest']);assert m['runtime']==runtime and len(m['results'])==count and m['status'].startswith('passed')
 refs[key]=dict(status=t['status'],receipt=pin(p),manifest=t['manifest'])
sql=checked(old['sql_startup_and_owned_cleanup']);assert sql['accepted'] is True and sql['cleanup_verified'] is True
assert any(runtime['path']in x['argv']for x in sql['instances'])
timing=read(D/'curse-clock-boundary/receipt.json');assert timing['status']=='passed-measured-paralysis-publication-clock-boundary' and timing['runtime']==runtime and timing['counts']['cases']==24
census=read(D/'material-census/receipt.json');assert census['runtime']==runtime and census['remaining_legacy_calls']==3
v=dict(old,updated_utc=datetime.now(timezone.utc).isoformat(),status='backend-regressions-passed-native-validation-deferred-by-user',same_sha_backend_regression_passed=True,targeted_replay=pin(D/'targeted/receipt.json'),battle_corpse_lifecycle=pin(D/'battle-lifecycle/result.json'),targeted_summary=counts,composed_replay=pin(D/'composed-replay/current.json'),composed_cases=5164,existing_regressions=refs['existing_20']['receipt'],affected_regressions=refs['affected_18']['receipt'],timing_investigation=pin(D/'curse-timing-investigation.json'),additional_publication_clock_cases=24,material_census=pin(D/'material-census/receipt.json'),prepared_native_entry=pin(D/'staging/native-entry-receipt.json'),sealing_driver=pin(Path(__file__)))
v['scope']=list(dict.fromkeys(old['scope']+['64 Quest/Time Attack queued corpse expiry/consume/revive/reset and role races']))
v['remaining']=['BATTLE type2 replacement, static NPC instance/AI isolation and full NPC factory remain outside verified WORLD replacement scope','remaining NPC/companion material and lifecycle consumers listed in current report','manual native batch validation deferred by user; no formal publication']
write(D/'qualification-current.json',v)
index=ROOT/'continuation-current.json';i=read(index)
i.update(time_utc=v['updated_utc'],status='same-sha-backend-qualified-full-objective-open',latest_candidate=runtime,latest_targeted_candidate=runtime,latest_candidate_full_regression_passed=True,targeted_candidate_full_regression_passed=True,latest_candidate_regression=refs,latest_20_plus_18_candidate=dict(runtime=runtime,regressions=refs),call_nature_candidate=pin(D/'qualification-current.json'),candidate_under_validation=runtime)
for key in ['latest_static_material_census','latest_targeted_material_census','remaining_material_census']:i[key]=pin(D/'material-census/receipt.json')
i['remaining_material_skill_ids']=[27,36,41,44]
i['remaining']=['NPC/static/companion consumers still listed in current report; static census is not proof of complete skill coverage','full class/target/Ctrl/area/trigger and state-lifetime qualification','final native UI and manual batch validation before formal publication']
i['next_material_stage']='Review remaining NPC/static/companion gaps; reuse qualified NPC WORLD replacement, area66/76/161/167 and69/165 implementations'
i['active_work'].update(battle_npc_followup_audit=pin(D/'battle-cancellation-audit/receipt.json'),updated_utc=v['updated_utc'],validation=pin(D/'qualification-current.json'),call_nature=pin(D/'qualification-current.json'),next='13118eb9 passed 1688 targeted,5164 composed,20+18 groups and SQL startup/owned cleanup. Reuse this candidate; no intermediate manual tests. Remaining NPC/static/companion scope stays open; formal unchanged.')
write(index,i)
controls=ROOT/'control-gaps.json';c=read(controls)
def repin(x):
 if isinstance(x,dict):
  if x.get('path')==str(index.resolve()) and 'sha256'in x:x['sha256']=sha(index)
  for value in x.values():repin(value)
 elif isinstance(x,list):
  for value in x:repin(value)
repin(c);write(controls,c)
print(json.dumps(dict(status=v['status'],runtime=runtime,targeted=counts,composed_cases=5164,existing_groups=20,affected_groups=18,formal_deployment_changed=False),ensure_ascii=False))
