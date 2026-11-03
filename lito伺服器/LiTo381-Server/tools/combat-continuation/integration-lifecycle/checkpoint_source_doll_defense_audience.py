"""Checkpoint first defense audience proofs; retain original full-room Snapshot/Calc/RNG/HP/accepted/send paths."""
from pathlib import Path
from datetime import datetime,timezone
import copy,csv,hashlib,json,re,shutil,subprocess,zipfile
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];C=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
cache={}
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:
   p=Path(v['path']);key=str(p);actual=cache.get(key)
   if actual is None:actual=sha(p);cache[key]=actual
   assert actual==v['sha256'],key
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
index=D/'source-program-update-progress-current.json';previous=read(index);verify(previous);old=read(Path(previous['manifest']['path']));verify(old)
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';proof=read(proofpath);audit=read(auditpath)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
latest=read(D/'source-doll-defense-audience-build-current.json');repro=read(D/'source-doll-defense-audience-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_doll_defense_audience_contract_scenarios']==123
assert build['test_only_forced_tie_fixture_checks']==7
assert build['test_only_forced_tie_fixture']['sha256']!=latest['runtime']['sha256']
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_doll_defense_audience_contract_scenarios')==3927
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 17 original methods,3 data ports and1 exact Receiver ctor','PASS345','PASS123 defense first-audience original-Snapshot','PASS7 pair admission test-only','snapshots=3, enhanced-visuals=3, encrypted-memory-frames=8, original-route-parity=6, receiver-waits=6, accepted-seals=2']:assert marker in log,marker
with zipfile.ZipFile(build['base']['path'])as parent,zipfile.ZipFile(latest['runtime']['path'])as candidate:
 for entry in ['local/lito381/server/'+name+'.class'for name in ['NativeSourceDollDefense$Snapshot','NativeSourceDollDefense$Proof','NativeSourceDollPassive','NativeSourceDollPassive$Choice','NativePlayerCommit','NativePlayerCommit$DefenseVisual','ClientSession','NativeSourceDollAudience','NativeSourceDollDetachedOutput']]:
  assert parent.read(entry)==candidate.read(entry),entry
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert subprocess.run(['powershell','-NoProfile','-Command',"@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"],capture_output=True,text=True,check=True).stdout.strip()=='0'
anchors=[R/'current.json',D/'source-kiringku-progress-current.json',D/'competition-only-product-scope-current.json',D/'source-equipment-mysql-reload-readonly-origin-current.json',D/'source-raw-producer-build-current.json',D/'source-arena-poison-build-current.json',S.parent.parent/'lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar']
anchor_refs=[pin(p)for p in anchors]
with zipfile.ZipFile(old['readonly_SQL_consumers_byte_retained']['base']['path'])as sql,zipfile.ZipFile(latest['runtime']['path'])as jar:
 entries=old['readonly_SQL_consumers_byte_retained']['entries'];assert len(entries)==20
 for name in entries:assert sql.read(name)==jar.read(name),name
run=D/'source-completion-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'editable-input-snapshot';inputs.mkdir(parents=True)
current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';work=S/'docs/maintenance/source-completion-work-current.zh-TW.md';old_index=pin(index)
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md'),(work,'previous-work.md')]:shutil.copyfile(p,run/name)
frozen_index=pin(run/'previous-program-progress.json')
for ref in old['editable_inputs']:
 name=Path(ref['path']).name;shutil.copyfile(R/name,inputs/name)
for name in ['NativeSourceDollDefenseAudience.java','SourceDollDefenseAudienceMethods.java','SourceDollDefenseAudienceDelta.java','SourceDollDefenseAudienceLoad.java','SourceDollDefenseAudiencePlan.java','build_source_doll_defense_audience.py','reproduce_source_doll_defense_audience.py','verify_source_doll_defense_audience.py','NativeSourceDollAudience.java','SourceDollAudienceMethods.java','SourceDollAudienceDelta.java','SourceDollAudienceLoad.java','SourceDollAudiencePlan.java','build_source_doll_audience.py','reproduce_source_doll_audience.py','verify_source_doll_audience.py','NativeSourceDollDetachedOutput.java','SourceDollDetachedOutputMethods.java','SourceDollDetachedOutputDelta.java','SourceDollDetachedOutputLoad.java','SourceDollDetachedOutputPlan.java','build_source_doll_detached_output.py','reproduce_source_doll_detached_output.py','verify_source_doll_detached_output.py','NativeSourceManualScope.java','SourceManualActionScopeMethods.java','SourceManualActionScopeDelta.java','SourceManualActionScopeLoad.java','SourceManualActionScopePlan.java','SourceManualActionLockCensus.java','build_source_manual_action_scope.py','reproduce_source_manual_action_scope.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_manual_action_scope.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-defense-first-audience-existing-accepted-seal-original-final-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['The original full-room defense Snapshot Receiver factory borrows original Context first role/life/observer metadata through the existing first-audience helper. Receiver gains one final transient noncanonical proof value. The original output and final-frame predicates check first life/observer and exact captured packet bytes, without new actor/Hub/action/status/inventory locks, capture authority or HP/cost/RNG policy.', 'Only the original defender, with the same original binding and first life as its own stamp, may use the existing Proof.acceptedLife. That token remains written exclusively by the byte-retained original accepted handler. Already accepted fatal self output remains allowed; a later revival and other receivers cannot borrow the defender death seal. Dead original observers retain eligibility under original proc_spr/range rules. Old unmarked/unloaded fallback and original record components/equality/hash remain.', 'Independent inverse of17 original methods,3 data ports and1 exact delegating Receiver constructor;345 no-init classes.123 new candidate checks plus3927 inherited same-SHA checks,7 separate test-only forced-tie checks. Three original full-room Snapshots and enhanced visual ctors seal an item/summon under the original actor lock. Eight encrypted memory frames,6 original unmarked route parity frames,6 original receiver-monitor waits and2 original accepted fatal handler seals pass. Frozen replay byte identical;20 SQL consumer classes and original Snapshot/passive/Proof/Commit/DefenseVisual/ClientSession/earlier helpers byte retained.', 'These fixtures use preconstructed Context metadata and original enhanced visual/accepted/send methods. They do not execute complete original draw/Calc/HP/zero-delta/backend/Main shutdown or real Source Context/server/database/game. Other producers, complete Source/material/proc/math/controls/Main/memory/formal composition and concentrated actual acceptance remain open. No deployment, formal overwrite, new baseline or Source/player SQL/schema/WAL writes.']
v['program_checks']=dict(JVM_no_initialization_classes=345,original_methods_independently_inverted=17,original_data_ports=3,new_exact_delegating_Receiver_ctors=1,existing_changed_class_entries=2,new_fieldless_helper_entries=1,new_final_transient_noncanonical_Receiver_fields=1,pure_doll_defense_audience_contract_scenarios=123,inherited_and_revalidated_same_SHA_pure_contract_scenarios=3927,test_only_forced_tie_fixture_checks=7,test_only_fixture_is_not_candidate_SHA=True,actual_original_full_room_Snapshots=3,actual_original_enhanced_DefenseVisual_constructors=3,original_encrypted_memory_frames=8,original_unmarked_route_cipher_parity_frames=6,actual_original_receiver_monitor_waits=6,original_accepted_fatal_handler_seals=2,first_observer_life_and_binding_borrowed_from_earlier_Context=True,only_original_defender_may_borrow_its_existing_accepted_death_seal=True,original_accepted_seal_writer_unchanged=True,original_full_room_Snapshot_Proof_passive_Commit_Visual_ClientSession_byte_retained=True,original_record_schema_equality_hash_and_old_ctor_fallback_retained=True,no_new_dead_observer_HP_veto=True,exact_original_packet_bytes_checked_before_cipher=True,new_registry_TLS_queue_workers_SQL_HP_or_RNG_authority=False,byte_identical_frozen_rebuild=True,unloaded_original_fallback=True,new_SELECTs=0,new_connection_ownership=False,bound_actual_server_game_or_Main_shutdown_cases=0,complete_original_draw_Calc_zero_delta_HP_Source_Main_game_qualified=False)
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_doll_defense_audience_contract','doll_defense_audience_port_census','Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_defense_first_audience_original_accepted_and_final_receiver_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'presentation':'Original defense Snapshot receivers now borrow first role/life/observer; original output/final-frame predicates check it and exact bytes. Only the original defender may use its original accepted death seal, preserving fatal self while rejecting later revival/foreign receiver inheritance. Full-room doll/passive maps and original Calc/RNG/HP/accepted/send/finally remain. Complete actual draw/Calc/zero-delta/whole accepted continuation and backend, other HPBAR/digits/kiringku/proc_spr/suppression/RO login/resources, Source Context/Main/memory/MySQL/game remain open.'}
for group in v['remaining_work_groups']:
 if group['id']in updates:group['work']=updates[group['id']]
v['remaining']=[x['work']for x in v['remaining_work_groups']];v['historical_remaining_preserved_in']=previous['manifest'];assert len(v['remaining_work_groups'])==12
for key in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[key]is False,key
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),system_goal=v['goal_reset_receipt'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
text=work.read_text('utf-8');text=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{v['runtime']['sha256']}`",text)
text=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",text)
artifact_paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']}
text=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:artifact_paths[m[1]],text)
a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
block='## 本次已更新的程式\n\n娃娃防禦Snapshot的原Receiver factory已借用原Context首次角色／生命／observer證明，新增1個final transient非canonical欄位；原output／final-frame predicate重查原生命／observer與精確封包位元組。只有原防禦者、同binding與stamp原生命可沿用既有Proof.acceptedLife；寫入者仍是未改動的原accepted handler。致死後自收保留，後續復活與其他接收者不得借用該死亡seal；死亡observer仍遵循原proc_spr／距離規則。原整份dolls／passive choice map、Calc／RNG／HP／accepted／send／finally保留。\n\n17原方法／3data port／1精確delegating ctor inverse、345 no-init、123新＋3927同SHA繼承及同位元重建通過。3原Snapshot／3原增強visual constructor在原actor lock封存item／summon，8加密記憶體IO、6原unmarked路徑比對、6接收鎖等待與2原accepted致死seal通過。7強制tie測試另列，非候選SHA；20 SQL reader與原Snapshot／Proof／passive／Commit／DefenseVisual／ClientSession位元保留。\n\n夾具未執行完整原draw／Calc／HP／zero-delta／後端／Main關閉，未建立真實Source Context或啟動伺服器／MySQL／遊戲；其餘技能／Source／外控與集中實機驗收仍開放。未部署、覆寫正式或寫入資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 first defense audience：候選`{v['runtime']['sha256']}`，防禦原Receiver沿用首次角色／生命／observer與精確封包；原defender既有accepted死亡seal保留。17原方法／3port／1ctor inverse、345 no-init、123新＋3927同SHA繼承、3Snapshot／3visual ctor／8加密IO／6原路徑比對／6接收鎖等待／2致死seal與同位元重建通過。完整Calc／HP／Source／Main／MySQL／實機仍開放；未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='original-defense-first-audience-existing-accepted-seal-and-final-receiver-program-only',full_goal_achieved=False)
continuation['source_doll_defense_audience_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
