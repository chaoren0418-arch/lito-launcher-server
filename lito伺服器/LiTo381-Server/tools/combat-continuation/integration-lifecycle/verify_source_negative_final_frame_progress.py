"""Verify the common program checkpoint while preserving the six unresolved qualified aliases."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;D=R/'build'
read=lambda p:json.loads(Path(p).read_text('utf-8'))
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
pin=lambda p:dict(path=str(Path(p).resolve()),sha256=sha(p))
cache={};count=0
def verify(v):
 global count
 if isinstance(v,dict):
  if set(v)=={'original','frozen','meaning'}and v['meaning'].startswith('Historical mutable replay-script reference'):
   a,b=v['original'],v['frozen'];assert Path(a['path'])==R/'reproduce_source_weapon_player_gap.py'and a['sha256']==b['sha256']and sha(a['path'])!=a['sha256'];assert Path(b['path']).parent.name=='editable-input-snapshot';verify(b);return
  if 'path'in v and 'sha256'in v:
   p=v['path'];actual=cache.get(p)
   if actual is None:actual=sha(p);cache[p]=actual
   assert actual==v['sha256'],p;count+=1
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
idx=read(D/'source-program-update-progress-current.json');verify(idx);m=read(idx['manifest']['path']);verify(m);run=Path(idx['manifest']['path']).parent
assert idx['runtime']['sha256']=='50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729'
checks=m['program_checks'];assert checks['negative_final_viewer_waits']==240 and checks['negative_final_viewer_assertions']==2280 and checks['negative_final_self_waits']==60 and checks['negative_final_self_assertions']==480 and checks['negative_final_cleanup_assertions']==17
assert checks['negative_pair_first_assertions']==1878 and checks['negative_pair_first_waits']==150
assert checks['pure_mass_teleport_exit_contract_scenarios']==193 and checks['inherited_and_revalidated_same_SHA_pure_contract_scenarios']==15910 and checks['ordinary_same_SHA_checks']==16103
assert checks['controlled_negative_final_assertions']==695 and checks['controlled_negative_final_monitor_waits']==78 and checks['controlled_NPC_pair_assertions']==456 and checks['controlled_NPC_pair_waits']==60 and checks['PC_pair_assertions']==312 and checks['PC_pair_waits']==42
assert checks['candidate_direct_lock_inventory_methods']==58 and checks['test_only_forced_tie_fixture_checks']==7
assert m['test_only_forced_tie_fixture']['sha256']!=idx['runtime']['sha256']
for name in ['source-negative-final-frame-build','source-negative-final-frame-common-reproduction','source-completion-inventory','source-next-producer-contracts','source-invocation-ABI-census','source-weapon-next-reference','source-weapon-overlay-checks']:
 ref=read(D/(name+'-current.json'));verify(ref);assert ref['runtime']['sha256']==idx['runtime']['sha256'];verify(read(ref['manifest']['path']))
proof=read(D/'Source-lifecycle-progress-verified-current.json')
for k in ['protected','live_helpers','experimental_live_helpers','docs','source_program_update_progress']:verify(proof[k])
continuation=read(R.parent/'continuation-current.json');verify(continuation['source_negative_final_frame_progress']);verify(read(run/'post-checkpoint-receipt.json'))
prev_ref=m['previous_program_progress']['manifest'];legacy_path=Path(prev_ref['path']).parent/'legacy-current-alias-audit.json';prev=read(prev_ref['path'])
while not legacy_path.exists():
 prev_ref=prev['previous_program_progress']['manifest'];verify(prev_ref);prev=read(prev_ref['path']);legacy_path=Path(prev_ref['path']).parent/'legacy-current-alias-audit.json'
legacy=read(legacy_path);assert len(legacy['references'])==6
for ref in legacy['references']:
 ref['observed_sha256']=sha(ref['expected']['path']);ref['matches']=ref['observed_sha256']==ref['expected']['sha256'];assert not ref['matches'],ref['key']
legacy['status']='historical-aliases-observed-without-rewriting-qualified-baseline';legacy['full_qualified']=False
lp=run/'legacy-current-alias-audit.json';lp.write_text(json.dumps(legacy,ensure_ascii=False,indent=2)+'\n','utf-8')
for k in ['full_goal_achieved','qualified_for_release','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','formal_deployment_changed','Source_or_player_SQL_or_WAL_written']:assert m[k]is False,k
assert len(m['remaining_work_groups'])==12
v=dict(status='passed-scoped-program-checks-with-unresolved-legacy-aliases',runtime=idx['runtime'],progress=idx['manifest'],references_checked=count,unique_files_checked=len(cache),legacy_alias_audit=pin(lp),unresolved_legacy_aliases=6,program_checks=checks,remaining_work_group_count=12,historical_replay_script_aliases_resolved_through_identical_frozen_copies=len(m['historical_weapon_PC_gap_input_aliases']),full_goal_achieved=False,qualified_for_release=False,new_game_runtime_cases=0,new_SQL_connections=0,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
out=run/'final-consistency.json';out.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],references_checked=count,unique_files_checked=len(cache),final_consistency=pin(out)),ensure_ascii=False))
