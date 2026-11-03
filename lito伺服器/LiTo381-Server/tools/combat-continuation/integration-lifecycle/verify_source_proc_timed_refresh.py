"""Verify scoped current program evidence; report six legacy aliases separately without blessing them."""
from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;D=R/'build'
read=lambda p:json.loads(p.read_text('utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
cache={};count=0
def verify(v):
 global count
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:
   p=Path(v['path']);key=str(p);actual=cache.get(key)
   if actual is None:actual=sha(p);cache[key]=actual
   assert actual==v['sha256'],key;count+=1
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
idx=read(D/'source-program-update-progress-current.json');verify(idx);m=read(Path(idx['manifest']['path']));verify(m);run=Path(idx['manifest']['path']).parent
assert m['program_checks']['pure_proc_timed_refresh_contract_scenarios']==746
assert m['program_checks']['inherited_and_revalidated_same_SHA_pure_contract_scenarios']==9198
assert m['program_checks']['test_only_forced_tie_fixture_checks']==7
assert m['test_only_forced_tie_fixture']['sha256']!=idx['runtime']['sha256']
for name in ['source-proc-timed-refresh-build','source-proc-timed-refresh-reproduction','source-completion-inventory','source-next-producer-contracts']:
 index=read(D/(name+'-current.json'));verify(index);assert index['runtime']==idx['runtime'];verify(read(Path(index['manifest']['path'])))
proof=read(D/'Source-lifecycle-progress-verified-current.json')
for key in ['protected','live_helpers','experimental_live_helpers','docs','source_program_update_progress']:verify(proof[key])
continuation=read(R.parent/'continuation-current.json');verify(continuation['source_proc_timed_refresh_progress']);verify(read(run/'post-checkpoint-receipt.json'))
prev=read(Path(m['previous_program_progress']['manifest']['path']));legacy_path=Path(m['previous_program_progress']['manifest']['path']).parent/'legacy-current-alias-audit.json'
while not legacy_path.exists():
 ref=prev['previous_program_progress']['manifest'];verify(ref);prev=read(Path(ref['path']));legacy_path=Path(ref['path']).parent/'legacy-current-alias-audit.json'
legacy=read(legacy_path);assert len(legacy['references'])==6
for ref in legacy['references']:
 ref['observed_sha256']=sha(Path(ref['expected']['path']));ref['matches']=ref['observed_sha256']==ref['expected']['sha256'];assert not ref['matches'],ref['key']
legacy['status']='historical-aliases-observed-without-rewriting-qualified-baseline';legacy['full_qualified']=False
lp=run/'legacy-current-alias-audit.json';lp.write_text(json.dumps(legacy,ensure_ascii=False,indent=2)+'\n','utf-8')
for key in ['full_goal_achieved','qualified_for_release','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','formal_deployment_changed','Source_or_player_SQL_or_WAL_written']:assert m[key]is False,key
assert len(m['remaining_work_groups'])==12
v=dict(status='passed-scoped-program-checks-with-unresolved-legacy-aliases',runtime=idx['runtime'],progress=idx['manifest'],references_checked=count,unique_files_checked=len(cache),legacy_alias_audit=pin(lp),unresolved_legacy_aliases=6,program_checks=m['program_checks'],remaining_work_group_count=12,full_goal_achieved=False,qualified_for_release=False,new_game_runtime_cases=0,new_SQL_connections=0,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False)
out=run/'final-consistency.json';out.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8');print(json.dumps(dict(status=v['status'],runtime=v['runtime'],references_checked=count,unique_files_checked=len(cache),final_consistency=pin(out))))
