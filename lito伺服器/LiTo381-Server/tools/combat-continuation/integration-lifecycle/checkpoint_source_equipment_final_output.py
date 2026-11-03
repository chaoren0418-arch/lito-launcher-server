"""Checkpoint weak first equipment viewers and actual detached original final receiver transport."""
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
latest=read(D/'source-equipment-final-output-build-current.json');repro=read(D/'source-equipment-final-output-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_equipment_final_output_contract_scenarios']==81
assert build['test_only_forced_tie_fixture_checks']==7
assert build['test_only_forced_tie_fixture']['sha256']!=latest['runtime']['sha256']
parent_build=read(Path(old['build']['path']));verify(parent_build)
inherited_counts={k:v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_equipment_final_output_contract_scenarios'}
parent_counts={k:v for k,v in parent_build.items()if k.endswith('_contract_scenarios')}
assert sum(parent_counts.values())==5403 and sum(inherited_counts.values())==5404
assert inherited_counts.keys()==parent_counts.keys()
assert [(k,parent_counts[k],v)for k,v in inherited_counts.items()if v!=parent_counts[k]]==[('pure_equipment_output_contract_scenarios',62,63)]
plan_ref=lambda m:next(x for x in m['inputs']if Path(x['path']).name=='SourceEquipmentOutputPlan.java')
assert plan_ref(build)['sha256']==plan_ref(parent_build)['sha256']
# The unchanged inherited plan checks each declared Viewer field. The new final
# transient marker adds one schema assertion, not a new full lifecycle case.
count_reconciliation=dict(parent_baseline_contract_assertions=5403,reexecuted_inherited_contract_assertions=5404,additional_inherited_Viewer_field_assertions=1,unchanged_equipment_plan=dict(parent=plan_ref(parent_build),candidate=plan_ref(build)),frozen_build_inherited_same_SHA_scenarios_is_parent_baseline=True)
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 2514 original methods,4 data ports,1 exact Viewer ctor and1 complete original synchronized final frame','PASS349','PASS81 equipment final-output original-seal','PASS7 pair admission test-only','memory-frames=6, original-route-parity=4, receiver-waits=5, original-captures=1']:assert marker in log,marker
with zipfile.ZipFile(build['base']['path'])as parent,zipfile.ZipFile(latest['runtime']['path'])as candidate:
 for entry in ['local/lito381/server/'+name+'.class'for name in ['NativeSourceDollDefense$Snapshot','NativeSourceDollDefense$Proof','NativeSourceDollPassive','NativeSourceDollPassive$Choice','NativePlayerCommit','NativePlayerCommit$DefenseVisual','NativeSourceDollAudience','NativeSourceDollDetachedOutput','NativeSourceEquipmentProcLife','NativeSourceEquipmentOutput$Detached','NativeSourceEquipmentOutput$Scope','NativeSourceEquipmentOutput$Link','NativeSourceEquipmentOutput$Output','NativeProcHealingPlayer','NativeProcHealingPlayer$Target','NativeLightAction']]:
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
for name in ['NativeSourceEquipmentFinalOutput.java','SourceEquipmentFinalOutputMethods.java','SourceEquipmentFinalOutputDelta.java','SourceEquipmentFinalOutputLoad.java','SourceEquipmentFinalOutputPlan.java','build_source_equipment_final_output.py','reproduce_source_equipment_final_output.py','verify_source_equipment_final_output.py','NativeSourceBarrierTargets.java','SourceBarrierTargetsMethods.java','SourceBarrierTargetsDelta.java','SourceBarrierTargetsLoad.java','SourceBarrierTargetsPlan.java','build_source_barrier_targets.py','reproduce_source_barrier_targets.py','verify_source_barrier_targets.py','NativeSourceSkillDefaults.java','SourceSkillDefaultsMethods.java','SourceSkillDefaultsDelta.java','SourceSkillDefaultsLoad.java','SourceSkillDefaultsPlan.java','build_source_skill_defaults.py','reproduce_source_skill_defaults.py','verify_source_skill_defaults.py','NativeSourceDollDefenseAudience.java','SourceDollDefenseAudienceMethods.java','SourceDollDefenseAudienceDelta.java','SourceDollDefenseAudienceLoad.java','SourceDollDefenseAudiencePlan.java','build_source_doll_defense_audience.py','reproduce_source_doll_defense_audience.py','verify_source_doll_defense_audience.py','NativeSourceDollAudience.java','SourceDollAudienceMethods.java','SourceDollAudienceDelta.java','SourceDollAudienceLoad.java','SourceDollAudiencePlan.java','build_source_doll_audience.py','reproduce_source_doll_audience.py','verify_source_doll_audience.py','NativeSourceDollDetachedOutput.java','SourceDollDetachedOutputMethods.java','SourceDollDetachedOutputDelta.java','SourceDollDetachedOutputLoad.java','SourceDollDetachedOutputPlan.java','build_source_doll_detached_output.py','reproduce_source_doll_detached_output.py','verify_source_doll_detached_output.py','NativeSourceManualScope.java','SourceManualActionScopeMethods.java','SourceManualActionScopeDelta.java','SourceManualActionScopeLoad.java','SourceManualActionScopePlan.java','SourceManualActionLockCensus.java','build_source_manual_action_scope.py','reproduce_source_manual_action_scope.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_manual_action_scope.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-equipment-weak-first-viewer-detached-original-final-receiver-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Original equipment-proc Hub discovery captures observer and weak status-container identity before its existing rawLife/binding reads. One invocation-local First value feeds the original Viewer construction through an exact delegating ctor; one final transient noncanonical field keeps only a weak marker. Viewer current adds exact observer mode and, for marked discoveries, original weak container/nullable raw-life checks before the complete old validation body. A dead observer acquires no new actor life. Old record schema/equality/hash remain and old constructors stay unmarked.', 'The original send factory still uses completionFrame(null), but its Runnable now captures Output and calls one complete original synchronized final-frame sibling with explicit proof at the original equipment guard. Actual transport constructs no new Context and does not reload/re-sample Source. All other final guards, cipher/socket/log/inventory body and original Frame/Context restoration are retained. HP/self/HPBAR drops unrelated caster proof as before; graphics keeps its original actor rules. Existing Batch capture/closure/clear and original accepted healing leaf/seal stay unchanged.', 'Independent inverse of2514 original methods,4 data ports,1 exact Viewer ctor and1 complete original synchronized final body;349 no-init classes.81 new candidate checks plus5404 inherited same-SHA assertions (5403 parent baseline plus1 field-loop assertion),7 separate test-only forced-tie checks. Six encrypted memory frames,4 original self/static-frame parity frames,5 original final receiver waits and1 original capture/Batch run pass. Tests retain same role owner/raw life when replacing a status container so weak identity has independent effect. Frozen replay byte identical;20 SQL consumer classes and original accepted ProcLife/Detached/Scope/Link/Output/HealingPlayer/Target/LightAction/Commit/defense helpers retained.', 'These fixtures use detached actual accepted/HP leaf/audience/queue/output methods, not a real Source Context, complete proc HP backend, observer mode ABA or normal role/memory transition, Main shutdown/server/database/game. Seed participant life policy is retained; unmarked manual/legacy values do not gain qualified weak state. Full skill/proc/control/producer semantics and concentrated actual acceptance remain open. No deployment, formal overwrite, new baseline or Source/player SQL/schema/WAL writes.']
v['program_checks']=dict(JVM_no_initialization_classes=349,original_methods_independently_inverted=2514,original_data_ports=4,new_exact_delegating_Viewer_ctors=1,complete_original_synchronized_final_frame_siblings=1,existing_changed_class_entries=3,new_fieldless_helper_entries=1,new_immutable_first_value_entries=1,new_final_transient_noncanonical_Viewer_fields=1,pure_equipment_final_output_contract_scenarios=81,inherited_and_revalidated_same_SHA_pure_contract_scenarios=5404,inherited_parent_baseline_contract_assertions=5403,additional_inherited_Viewer_field_assertions=1,test_only_forced_tie_fixture_checks=7,test_only_fixture_is_not_candidate_SHA=True,original_encrypted_memory_frames=6,original_self_static_frame_cipher_parity_frames=4,actual_original_receiver_monitor_waits=5,actual_original_capture_Batch_runs=1,first_observer_and_weak_container_before_original_life_binding=True,observer_discovery_creates_no_actor_life=True,status_replacement_same_role_owner_life_rejected=True,old_Viewer_constructor_unmarked_and_record_identity_retained=True,original_completionFrame_null_Batch_and_cleanup_retained=True,actual_output_path_constructs_new_Context=False,new_Runnable_captures_only_Output=True,accepted_target_HP_has_no_new_caster_alive_or_role_dependency=True,original_graphics_actor_policy_retained=True,original_other_final_guards_cipher_socket_log_inventory_body_retained=True,new_registry_TLS_queue_workers_SQL_HP_RNG_or_lifetime_authority=False,byte_identical_frozen_rebuild=True,new_SELECTs=0,new_connection_ownership=False,bound_actual_server_game_or_Main_shutdown_cases=0,full_observer_ABA_role_memory_Source_backend_Main_game_qualified=False)
v['inherited_count_reconciliation']=count_reconciliation
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_equipment_final_output_contract','equipment_final_output_port_census','Source_barrier_targets_contract','Source_barrier_target_oracle','Source_barrier_skill_ids','barrier_skill_matrix','barrier_targets_port_census','Source_skill_defaults_contract','Source_skill_config_oracle','skill_defaults_port_census','Source_doll_defense_audience_contract','doll_defense_audience_port_census','Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_equipment_weak_first_viewer_and_detached_original_final_receiver_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'presentation':'Original equipment proc output uses weak first discovered status/observer/life proof and exact packet checks at its complete original final synchronized frame. Actual send no longer constructs a new Context; original completionFrame(null), Batch capture/closure and accepted HP/caster-graphics separation remain. Complete full observer-mode ABA, seed participant/normal role and memory transitions, other HPBAR/digits/kiringku/proc_spr/suppression/RO login/resource/producers and actual Source/backend/Main/MySQL/game acceptance.'}
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
block='## 本次已更新的程式\n\n裝備觸發治療輸出在原Hub觀眾發現點、原rawLife／binding前，保存observer與狀態容器WeakReference。原Viewer新增1個final transient非canonical欄位，原record identity／hash與舊constructor保留；current在原body前重查模式及已標記的原容器／nullable life。觀戰者不因此建立actor life。\n\n原send仍沿用completionFrame(null)與既有Batch，factory改為只捕捉Output；實際傳送不再new Context或重新取樣Source，透過完整原synchronized final frame，在原equipment guard位置傳入明確proof。其餘guard／cipher／socket／log／inventory與原Frame／Context還原保留。已接受target HP不新增caster存活／角色依賴，gfx沿用原actor規則。\n\n2514原方法／4port／1Viewer ctor／1完整final frame inverse、349 no-init、81新＋5404同SHA繼承檢查（含1項新增Viewer欄位巡檢）與同位元重建通過。6加密記憶體IO、4原self/static路徑比對、5接收鎖等待、1原capture/Batch；同owner／life但換容器仍拒絕。7強制tie測試另列，非候選SHA。20 SQL reader與原accepted／ProcLife／HealingPlayer／Target／Scope／Output／LightAction／Commit保留。\n\n尚未執行完整observer模式ABA、seed participant／正常角色與記憶體轉換、完整Proc後端／Main關閉／Source Context／MySQL／遊戲驗收；未部署或寫入資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 equipment final output：候選`{v['runtime']['sha256']}`，原viewer沿用weak container／observer／life，傳送不再new Context；原同步frame與已接受HP／caster分離保留。2514原方法／4port／1ctor／1final frame inverse、349 no-init、81新＋5404同SHA繼承檢查（含1項新增Viewer欄位巡檢）、6加密IO／4原路徑比對／5接收鎖等待／1原capture與同位元重建通過。完整角色／observer ABA／Main／Source／MySQL／實機仍開放；未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-equipment-weak-first-viewer-detached-original-final-receiver-program-only',full_goal_achieved=False)
continuation['source_equipment_final_output_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
