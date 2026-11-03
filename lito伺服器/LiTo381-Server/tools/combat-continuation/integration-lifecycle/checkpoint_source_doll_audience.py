"""Checkpoint first doll audience identity and exact accepted target death seal; no deployment or actual services."""
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
latest=read(D/'source-doll-audience-build-current.json');repro=read(D/'source-doll-audience-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_doll_audience_contract_scenarios']==40
assert build['test_only_forced_tie_fixture_checks']==7
assert build['test_only_forced_tie_fixture']['sha256']!=latest['runtime']['sha256']
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_doll_audience_contract_scenarios')==3887
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 31 original methods,6 original data ports and1 exact Receiver ctor','PASS344','PASS40 doll first-audience original-discovery','PASS7 pair admission test-only','original-Views=2, original-Snapshots=2, encrypted-memory-frames=2, final-receiver-waits=3, accepted-seals=1']:assert marker in log,marker
with zipfile.ZipFile(build['base']['path'])as parent,zipfile.ZipFile(latest['runtime']['path'])as candidate:
 for entry in ['local/lito381/server/NativeSourceDollDefense.class','local/lito381/server/NativeSourceDollDefense$Snapshot.class']:
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
for name in ['NativeSourceDollAudience.java','SourceDollAudienceMethods.java','SourceDollAudienceDelta.java','SourceDollAudienceLoad.java','SourceDollAudiencePlan.java','build_source_doll_audience.py','reproduce_source_doll_audience.py','verify_source_doll_audience.py','NativeSourceDollDetachedOutput.java','SourceDollDetachedOutputMethods.java','SourceDollDetachedOutputDelta.java','SourceDollDetachedOutputLoad.java','SourceDollDetachedOutputPlan.java','build_source_doll_detached_output.py','reproduce_source_doll_detached_output.py','verify_source_doll_detached_output.py','NativeSourceManualScope.java','SourceManualActionScopeMethods.java','SourceManualActionScopeDelta.java','SourceManualActionScopeLoad.java','SourceManualActionScopePlan.java','SourceManualActionLockCensus.java','build_source_manual_action_scope.py','reproduce_source_manual_action_scope.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_manual_action_scope.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-doll-original-first-audience-life-observer-accepted-seal-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['The original Context under-Hub first-player capture stores its first observer flag on the existing Member before original life/binding reads. Two original View List.copyOf branches and the original Hub roomMembers port carry an immutable first Audience. The later original Snapshot keeps the selected original Receiver even if later Context construction or binding reads would see another role or life. Membership, duplicates, order and original caller dispatch remain intact.', 'One final transient noncanonical Object field on Receiver carries only original Hub/life/observer. Existing output.current checks it at the original synchronized final receiver monitor, before cipher/socket. Already dead observers remain eligible under the original display/range policies. Only a matching accepted Frame/event/target/binding/original life may borrow its existing sealed target death token; later revivals and foreign events cannot inherit that seal. No new HP admission or caster-liveness rules.', 'Independent inverse of31 original methods,6 exact data ports and1 exact delegating Receiver constructor;344 no-init classes.40 new candidate checks plus3887 inherited same-SHA checks,7 separate test-only forced-tie checks. Two original View Context branches,2 original Snapshots,2 actual encrypted memory frames,3 final receiver-monitor waits and1 existing accepted death seal are exercised. Original record components/equality/hash and old constructor fallback preserved. Frozen replay byte identical;20 SQL consumer classes and earlier detached output helper byte retained.', 'Fixtures use preconstructed Context metadata and original Hub/list/accepted-seal methods; they construct no real Source Context, do no HP backend mutation and start no actual server/Main/database/game. Entire original no-Context View branch across actual Source Context construction, other producer/first-player origins, all skills/math/controls/Main/memory and concentrated actual acceptance remain open. No deployment, formal overwrite, baseline or Source/player SQL/schema/WAL writes.']
v['program_checks']=dict(JVM_no_initialization_classes=344,original_methods_independently_inverted=31,original_data_ports=6,new_exact_delegating_Receiver_ctors=1,existing_changed_class_entries=4,new_fieldless_helper_entries=1,new_bounded_immutable_value_entries=2,new_final_transient_noncanonical_Receiver_fields=1,pure_doll_audience_contract_scenarios=40,inherited_and_revalidated_same_SHA_pure_contract_scenarios=3887,test_only_forced_tie_fixture_checks=7,test_only_fixture_is_not_candidate_SHA=True,actual_original_View_Context_branch_calls=2,actual_original_Snapshot_calls=2,original_encrypted_memory_frames=2,actual_original_receiver_monitor_waits=3,existing_accepted_target_death_seals=1,first_observer_before_original_life_and_binding=True,later_Context_and_binding_do_not_replace_original_receiver=True,original_record_components_equality_hash_and_old_ctor_fallback_retained=True,no_new_dead_observer_HP_veto=True,exact_existing_accepted_target_seal_distinct_from_later_revival=True,queued_Context_Frame_View_or_audience_map=False,new_registry_TLS_queue_workers_or_SQL=False,original_detached_output_helper_byte_retained=True,byte_identical_frozen_rebuild=True,WORLD_and_unloaded_original_fallback=True,new_SELECTs=0,new_connection_ownership=False,bound_actual_server_game_or_Main_shutdown_cases=0,whole_original_no_Context_View_branch_and_real_Source_Context_HP_Main_game_qualified=False)
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_doll_original_first_audience_and_exact_accepted_target_seal_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'presentation':'Doll original Context/Hub audience now carries first role/life/observer into existing detached output and original final receiver monitor; later Snapshot Context/binding reads cannot adopt a new role/life. Exact accepted target death seal preserves fatal-target output and rejects later revival. Dead observers retain original eligibility. Complete whole original no-Context View branch across real Source Context construction, other audience/producer origins, full HPBAR/digits/kiringku/proc_spr/suppression/RO login/resource routes, and actual backend/Main/MySQL/game acceptance remain open.'}
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
block='## 本次已更新的程式\n\n娃娃觀眾的角色／生命／observer證明已從原Context Hub下首次發現，以及原View的Hub／兩個List.copyOf分支，沿用到原Snapshot與最終接收者。後續Context建立或binding讀取不再採用較新的角色／生命。既有Receiver增加1個final transient非canonical proof欄位，原record components／equals／hash及舊constructor行為保留。原本死亡的observer仍可依原顯示／距離規則收取；已接受致死事件只在同一target／binding／原生命與event相符時沿用既有死亡seal，後續復活不會繼承。\n\n31原方法／6精確data port／1精確delegating ctor獨立inverse、344 no-init、40新＋3887同SHA繼承、同位元重建通過。2原View Context分支、2原Snapshot、2加密記憶體IO、3原接收鎖等待及1既有accepted死亡seal通過；7強制tie測試另列，非候選SHA。前輪不捕捉Context的輸出helper與20 SQL reader class位元保留。\n\n夾具使用預先建構的Context metadata，未建立真實Source Context，未跑完整HP／Main關閉／伺服器／MySQL／遊戲；整段no-Context View分支跨真實重載、其他來源與完整Source語意仍開放。未部署、覆寫正式或寫入資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 first doll audience：候選`{v['runtime']['sha256']}`，原觀眾角色／生命／observer證明沿用至Snapshot與最終同步接收者；同事件既有accepted死亡seal與後續復活分開處理。31原方法／6port／1ctor inverse、344 no-init、40新＋3887同SHA繼承、2View／2Snapshot／2加密IO／3接收鎖等待／1accepted seal與同位元重建通過。完整Source Context／HP／Main／MySQL／實機仍開放；未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='original-doll-first-audience-life-observer-exact-accepted-target-seal-program-only',full_goal_achieved=False)
continuation['source_doll_audience_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
