"""Checkpoint original Raw first-audience life and observer final-receiver proof."""
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
latest=read(D/'source-raw-audience-build-current.json');repro=read(D/'source-raw-audience-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_raw_audience_contract_scenarios']==77
assert build['test_only_forced_tie_fixture_checks']==7
assert build['test_only_forced_tie_fixture']['sha256']!=latest['runtime']['sha256']
parent_build=read(Path(old['build']['path']));verify(parent_build)
parent_counts={k:v for k,v in parent_build.items()if k.endswith('_contract_scenarios')}
inherited_counts={k:v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_raw_audience_contract_scenarios'}
assert inherited_counts==parent_counts and sum(inherited_counts.values())==5485
assert build['inherited_same_SHA_scenarios']==5485
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 44 original methods,2 data ports,1 exact Output ctor','PASS351','PASS77 raw first-audience original-capture','PASS7 pair admission test-only','memory-frames=4, receiver-waits=4, original-captures=10']:assert marker in log,marker
with zipfile.ZipFile(build['base']['path'])as parent,zipfile.ZipFile(latest['runtime']['path'])as candidate:
 for entry in ['local/lito381/server/'+name+'.class'for name in ['ClientSession','NativeSourceEquipmentOutput','NativeSourceEquipmentOutput$Viewer','NativeSourceEquipmentFinalOutput','NativeSourceEquipmentFinalOutput$First','NativeSourceDollDefense$Snapshot','NativeSourceDollDefense$Proof','NativeSourceDollPassive','NativeSourceDollPassive$Choice','NativePlayerCommit','NativePlayerCommit$DefenseVisual','NativeSourceDollAudience','NativeSourceDollDetachedOutput','NativeSourceEquipmentProcLife','NativeSourceEquipmentOutput$Detached','NativeSourceEquipmentOutput$Scope','NativeSourceEquipmentOutput$Link','NativeSourceEquipmentOutput$Output','NativeProcHealingPlayer','NativeProcHealingPlayer$Target','NativeLightAction']]:
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
for name in ['NativeSourceRawAudience.java','SourceRawAudienceMethods.java','SourceRawAudienceDelta.java','SourceRawAudienceLoad.java','SourceRawAudiencePlan.java','build_source_raw_audience.py','reproduce_source_raw_audience.py','verify_source_raw_audience.py','NativeSourceEquipmentFinalOutput.java','SourceEquipmentFinalOutputMethods.java','SourceEquipmentFinalOutputDelta.java','SourceEquipmentFinalOutputLoad.java','SourceEquipmentFinalOutputPlan.java','build_source_equipment_final_output.py','reproduce_source_equipment_final_output.py','verify_source_equipment_final_output.py','NativeSourceBarrierTargets.java','SourceBarrierTargetsMethods.java','SourceBarrierTargetsDelta.java','SourceBarrierTargetsLoad.java','SourceBarrierTargetsPlan.java','build_source_barrier_targets.py','reproduce_source_barrier_targets.py','verify_source_barrier_targets.py','NativeSourceSkillDefaults.java','SourceSkillDefaultsMethods.java','SourceSkillDefaultsDelta.java','SourceSkillDefaultsLoad.java','SourceSkillDefaultsPlan.java','build_source_skill_defaults.py','reproduce_source_skill_defaults.py','verify_source_skill_defaults.py','NativeSourceDollDefenseAudience.java','SourceDollDefenseAudienceMethods.java','SourceDollDefenseAudienceDelta.java','SourceDollDefenseAudienceLoad.java','SourceDollDefenseAudiencePlan.java','build_source_doll_defense_audience.py','reproduce_source_doll_defense_audience.py','verify_source_doll_defense_audience.py','NativeSourceDollAudience.java','SourceDollAudienceMethods.java','SourceDollAudienceDelta.java','SourceDollAudienceLoad.java','SourceDollAudiencePlan.java','build_source_doll_audience.py','reproduce_source_doll_audience.py','verify_source_doll_audience.py','NativeSourceDollDetachedOutput.java','SourceDollDetachedOutputMethods.java','SourceDollDetachedOutputDelta.java','SourceDollDetachedOutputLoad.java','SourceDollDetachedOutputPlan.java','build_source_doll_detached_output.py','reproduce_source_doll_detached_output.py','verify_source_doll_detached_output.py','NativeSourceManualScope.java','SourceManualActionScopeMethods.java','SourceManualActionScopeDelta.java','SourceManualActionScopeLoad.java','SourceManualActionScopePlan.java','SourceManualActionLockCensus.java','build_source_manual_action_scope.py','reproduce_source_manual_action_scope.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_manual_action_scope.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-Raw-first-audience-life-observer-final-receiver-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['At the original Raw captureOutput allocation, a final transient noncanonical Output proof freezes receiver life and observer from the existing sealed first-player snapshot. Published missing/unfinished members fail closed. Exact original accepted target proof alone may borrow death life through the original helper; no later binding/token/observer sampling or mutable Member is retained.', 'The original allowFrame receives a prefix for exact receiver/bytes/mode/frozen life. Complete original guard body, Batch task/capture/cleanup, producer/HP/cost/RNG/rollback, final synchronized receiver/cipher and original unmarked ctor remain. Record components/equality/hash are unchanged. Raw DamageCommitPlan.none/digit distinctions stay unchanged.', 'Independent inverse44 original methods/2 ports/1 exact delegating Output ctor;351 no-init classes.77 new checks plus5485 inherited same-SHA checks,7 separate test-only forced-tie checks. Four encrypted memory frames,4 actual original receiver-monitor waits and10 original captures cover first audience, life replacement, mode, actual scene-role roundtrip, approved fatal self, missing first proof, DRAINING/STOPPED/Main boolean fences and TLS restoration. Frozen replay byte identical;20 SQL consumers and ClientSession, all original equipment output, accepted participant and producer helpers unchanged.', 'These detached actual capture/Batch/final-frame tests are not full original producer/HP/backend/Main shutdown/memory/Source/role-transition/MySQL/game qualification. Full earlier source and primary-secondary proof, other NPC/controlled audiences and all skill/control/producer work remain open; no deployment, formal overwrite, new baseline or Source/player SQL/schema/WAL writes.']
v['program_checks']=dict(JVM_no_initialization_classes=351,original_methods_independently_inverted=44,original_data_ports=2,new_exact_delegating_Output_ctors=1,existing_changed_class_entries=2,new_fieldless_helper_entries=1,new_immutable_first_audience_proof_entries=1,new_final_transient_noncanonical_Output_fields=1,pure_raw_audience_contract_scenarios=77,inherited_and_revalidated_same_SHA_pure_contract_scenarios=5485,test_only_forced_tie_fixture_checks=7,test_only_fixture_is_not_candidate_SHA=True,original_encrypted_memory_frames=4,actual_original_receiver_monitor_waits=4,actual_original_capture_Batch_runs=10,original_first_snapshot_life_and_observer_reused=True,exact_original_accepted_target_death_seal_only=True,no_late_token_binding_or_observer_capture=True,original_Output_ctor_unmarked_and_record_identity_retained=True,complete_original_guard_body_Batch_scope_cleanup_and_final_receiver_retained=True,original_raw_producer_HP_cost_RNG_rollback_and_digit_distinctions_retained=True,new_registry_TLS_queue_workers_SQL_HP_RNG_or_lifetime_authority=False,byte_identical_frozen_rebuild=True,new_SELECTs=0,new_connection_ownership=False,bound_actual_server_game_or_Main_shutdown_cases=0,full_producers_Source_backend_Main_memory_game_qualified=False)
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_raw_audience_contract','raw_audience_port_census','Source_equipment_final_output_contract','equipment_final_output_port_census','Source_barrier_targets_contract','Source_barrier_target_oracle','Source_barrier_skill_ids','barrier_skill_matrix','barrier_targets_port_census','Source_skill_defaults_contract','Source_skill_config_oracle','skill_defaults_port_census','Source_doll_defense_audience_contract','doll_defense_audience_port_census','Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_Raw_first_audience_life_observer_final_receiver_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'hp-producers':'Original Raw capture/Batch outputs now freeze existing first receiver life/observer and recheck exact bytes at the original final guard. Only the same original accepted event target may borrow approved death life. Actual same-role revival/mode/final wait and scene ABA, fatal self and DRAINING pass; all earlier source/primary-secondary proofs, complete wand/reflect/feedback/poison/cube/ground/controlled/bot/NPC producers/HP/cost/RNG/rollback, whole accepted continuations, other NPC/controlled audience paths, Main/IO/memory and Source/backend/game remain open.'}
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
block='## 本次已更新的程式\n\nRaw共用輸出在原captureOutput建構點，沿用已封存的首次觀眾快照，固定life與observer。Output新增1個final transient非canonical欄位，保留record identity／hash與舊constructor；缺少已發布成員或未完成snapshot時拒絕採用較晚身分。僅原accepted helper可把同一事件目標的原life推進至已核准death life，不重新取樣token／binding／observer，也不保留mutable Member／Context／Frame。\n\n原allowFrame前新增接收者／原位元／模式／life檢查；完整原guard、Batch queue／task／scope／cleanup、同步接收鎖／cipher及原生產端HP／成本／機率／回滾與raw數字語意保留。\n\n44原方法／2port／1Output ctor inverse、351 no-init、77新＋5485同SHA繼承及同位元重建通過。4加密記憶體IO、4原接收鎖等待、10原capture/Batch涵蓋同角色復活、模式、實際scene往返角色退休、已接受致死自用HP及DRAINING。7強制tie測試另列，非候選SHA。20 SQL reader、ClientSession與原裝備／accepted／HP生產端未變更。\n\n完整生產端、正常角色／記憶體生命週期、Source Context／HP後端／Main關閉／MySQL／遊戲驗收仍未完成。未部署或寫入資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 Raw audience：候選`{v['runtime']['sha256']}`，原captureOutput固定首次receiver life／observer，僅借原accepted目標death seal；原final guard／Batch／同步接收與HP／cost／RNG保留。44原方法／2port／1ctor inverse、351 no-init、77新＋5485同SHA繼承、4加密IO／4接收鎖等待／10原capture與同位元重建通過。完整Source／生產端／Main／memory／MySQL／實機仍開放；未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-Raw-first-audience-life-observer-final-receiver-program-only',full_goal_achieved=False)
continuation['source_raw_audience_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
