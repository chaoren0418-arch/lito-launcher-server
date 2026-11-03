"""Checkpoint original natural NPC output Task proof-only transport Context without new discovery."""
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
latest=read(D/'source-target-negative-reserve-build-current.json');repro=read(D/'source-target-negative-reserve-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_target_negative_reserve_contract_scenarios']==453
assert build['test_only_forced_tie_fixture_checks']==7
assert build['test_only_forced_tie_fixture']['sha256']!=latest['runtime']['sha256']
parent_build=read(Path(old['build']['path']))
# Historical Source references are checked against their immutable frozen copies.
# Current-stage originals above remain strictly verified. Never rewrite Source or old manifests.
parent_Source_freshness=[];historical_parent=copy.deepcopy(parent_build)
for ref in historical_parent['Source_frozen_references']:
 verify(ref['frozen']);original=ref['original'];observed=pin(Path(original['path']))
 if observed['sha256']!=original['sha256']:
  assert Path(original['path']).name=='L1MerchantInstance.java'
  assert original['sha256']=='fe0f06858acb7f075bb1a9edabc0e7ab5dcd79c0100d96f34527307931197006'
  assert observed['sha256']=='3ac6085c273fbd63decc05f232ac92bea461132f6a8069ed44d50303e62707dc'
  assert any(x['original']==observed and x['frozen']['sha256']==observed['sha256'] for x in build['Source_frozen_references'])
  parent_Source_freshness.append(dict(original_path=original['path'],historical_expected_sha256=original['sha256'],historical_frozen=ref['frozen'],current_original=observed,meaning='External merchant delivery notice now calls NpcItemNotifier.send. Historical snapshot retained; no merchant behavior qualification inferred.'))
 ref['original']=ref['frozen']
historical_parent['Source_inputs']=[x['frozen'] for x in historical_parent['Source_frozen_references']]
verify(historical_parent)
parent_counts={k:v for k,v in parent_build.items()if k.endswith('_contract_scenarios')}
inherited_counts={k:v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_target_negative_reserve_contract_scenarios'}
assert inherited_counts==parent_counts and sum(inherited_counts.values())==12580
assert build['inherited_same_SHA_scenarios']==12580
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['PASS independent target-negative reserve inverse: 24 original methods,1 original caller port,1 complete reserve sibling','PASS396 inherited runtime no-init coverage plus3 explicit target-negative reserve owners','PASS453 target-negative peer reserve actual-original-fence','original-fence-waits=42, original-receipts=15, constructor-rollbacks=1, original-caller-casts=32','PASS7 pair admission test-only']:assert marker in log,marker
with zipfile.ZipFile(build['base']['path'])as parent,zipfile.ZipFile(latest['runtime']['path'])as candidate:
 for entry in ['local/lito381/server/'+name+'.class'for name in ['NativeSourcePhysicalNpc$Commit','NativeSourcePhysicalNpc$Output','NativeSourcePhysicalAudience','NativeSourcePhysicalAudience$Audience','NativeSourcePhysicalAudience$Receiver','NativeAreaMaterialAction','NativeSourceRawDamage','NativeSourceRawDamage$Output','NativeSourceRawAudience','NativeSourceRawAudience$Proof','NativeSourceFirstPlayers','NativeSourceFirstPlayers$Snapshot','NativeSourceDollAudience','NativeSourceEquipmentOutput','NativeSourceEquipmentOutput$Viewer','NativeSourceEquipmentFinalOutput','NativeSourceEquipmentFinalOutput$First','NativeSourceDollDefense$Snapshot','NativeSourceDollDefense$Proof','NativeSourceDollPassive','NativeSourceDollPassive$Choice','NativePlayerCommit$DefenseVisual','NativeSourceDollAudience','NativeSourceDollDetachedOutput','NativeSourceEquipmentOutput$Detached','NativeSourceEquipmentOutput$Scope','NativeSourceEquipmentOutput$Link','NativeSourceEquipmentOutput$Output','NativeProcHealingPlayer','NativeProcHealingPlayer$Target','NativeLightAction']]:
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
for name in ['NativeSourceTargetNegativeReserve.java', 'SourceTargetNegativeReserveMethods.java', 'SourceTargetNegativeReserveDelta.java', 'SourceTargetNegativeReserveLoad.java', 'SourceTargetNegativeReservePlan.java', 'build_source_target_negative_reserve.py', 'reproduce_source_target_negative_reserve.py', 'verify_source_target_negative_reserve.py', 'checkpoint_source_target_negative_reserve.py']:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-material-peer-reserve-first-life-app-fence-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Only the original material173/174 peer reservation call now forwards its existing first Context to a fieldless adapter. Accepted BATTLE first Proof peer/lifetime/status/actor/master identities must remain current; missing/closed/foreign proof returns null without creating a lifetime. WORLD/unloaded paths retain complete original reserve behavior.', 'One complete original NativePlayerVitalCompletions.reserve sibling replaces the initial NativePlayerLifetime.of lookup with the first Proof lifetime. Original no-app processes condition is retained with a readonly first-proof/application-identity guard. After the original app.fence wait, the same proof is checked before original packet-scope lookup, app handlers and Receipt construction. Rejection branches to the original null/monitorexit/return path; original constructor Throwable rollback, pending map, Frame, close/double-release semantics and application-closing packet/worker rules remain.', '24 original methods independently invert,1 original caller port,1 complete reserve sibling,2 original owners,0 added fields and1 fieldless helper.396 inherited no-init plus3 explicit owners,453 new plus12580 inherited same-SHA checks and byte-identical replay.42 direct actual original app.fence waits plus32 actual original material caller fence waits cover PC/controlled peers, current/DRAINING/STOPPED/Main, missing/replaced life/status/roles/actor/attachment/proof/app and original app-closing packet scopes.15 explicit original Receipts validate exact registration/close/double-release counters;1 injected isolated constructor failure verifies original app-handler rollback and exact exception propagation. Original pending static map is restored in finally.', 'The selected peer reservation gap is wired; full material costs, probability/status mutation/output, other material/producer reservations, full Source/default SkillUse/control semantics, backend/Main/memory, Chinese MySQL and game cursor qualification remain open. Original native reserve remains unchanged for all other callers.20 SQL reader classes and57 direct player lock methods stay unchanged. No deployment, formal overwrite, new baseline or Source/player SQL/schema/WAL writes.']
v['program_checks']={'JVM_inherited_no_initialization_checks': 396, 'additional_target_negative_reserve_owners_explicitly_verified': 3, 'original_methods_independently_inverted': 24, 'original_changed_owners': 2, 'original_caller_ports': 1, 'complete_original_reserve_siblings': 1, 'new_original_fields': 0, 'new_runtime_class_entries': 1, 'pure_target_negative_reserve_contract_scenarios': 453, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 12580, 'test_only_forced_tie_fixture_checks': 7, 'test_only_fixture_is_not_candidate_SHA': True, 'actual_original_direct_app_fence_waits': 42, 'actual_original_caller_app_fence_waits': 32, 'actual_original_receipts': 15, 'original_constructor_Throwable_rollbacks': 1, 'first_Context_lifetime_and_same_application_required': True, 'original_closing_packet_worker_rule_retained': True, 'original_pending_Frame_Receipt_close_rollback_retained': True, 'missing_waited_status_or_lifetime_never_initialized': True, 'candidate_direct_lock_inventory_methods': 57, 'new_global_actor_registry_TLS_queue_domain_or_worker': False, 'byte_identical_frozen_rebuild': True, 'new_SELECTs': 0, 'new_connection_ownership': False, 'full_material_cost_RNG_status_IO_and_all_reserves_qualified': False, 'bound_actual_server_game_or_Main_shutdown_cases': 0, 'full_producers_Source_backend_Main_memory_game_qualified': False}
v['historical_parent_Source_freshness']=parent_Source_freshness
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_target_negative_reserve_contract','target_negative_reserve_port_census','Source_target_negative_life_contract','target_negative_life_port_census','Source_proc_scalar_extras_contract','proc_scalar_extras_port_census','Source_proc68_first_queue_contract','proc68_first_queue_port_census','Source_proc_scalar_skills_contract','Source_proc_scalar_icon_oracle','proc_scalar_skills_port_census','Source_proc_timed_refresh_contract','Source_proc_timed_refresh_oracle','proc_timed_refresh_port_census','Source_target_icon_batch_contract','target_icon_batch_port_census','Source_icons1_output_contract','icons1_output_port_census','Source_icons1_epoch_contract','Source_icons1_epoch_fields','icons1_epoch_port_census','Source_icons1_brave_origin_contract','icons1_brave_origin_port_census','Source_icons1_inputs_contract','icons1_inputs_port_census','Source_icons1_contract','Source_icons1_oracle','icons1_port_census','Source_proc68_icon_contract','Source_proc68_icon_oracle','proc68_icon_port_census','Source_proc68_graphics_contract','Source_proc68_graphics_oracle','proc68_graphics_port_census','Source_context_role_frame_contract','context_role_frame_port_census','Source_batch_role_frame_contract','batch_role_frame_port_census','Source_batch_terminal_contract','batch_terminal_port_census','Source_controlled_role_frame_contract','controlled_role_frame_port_census','Source_controlled_audience_contract','controlled_audience_port_census','Source_poison_audience_contract','poison_audience_port_census','Source_element_audience_contract','element_audience_port_census','Source_element_transport_contract','element_transport_port_census','Source_physical_transport_contract','physical_transport_port_census','Source_physical_audience_contract','physical_audience_port_census','Source_raw_audience_contract','raw_audience_port_census','Source_equipment_final_output_contract','equipment_final_output_port_census','Source_barrier_targets_contract','Source_barrier_target_oracle','Source_barrier_skill_ids','barrier_skill_matrix','barrier_targets_port_census','Source_skill_defaults_contract','Source_skill_config_oracle','skill_defaults_port_census','Source_doll_defense_audience_contract','doll_defense_audience_port_census','Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_material_173_174_first_peer_receipt_reserve_wired=True,Source_material_173_174_first_context_life_wired=True,Source_eight_custom_PC_scalar_Proc_paths_wired=True,Source_custom68_first_status_queue_wired=True,Source_four_custom_PC_scalar_Proc_paths_wired=True,Source_custom68_timer_refresh_and_actual_ability_guard_wired=True,Source_target_icon_batch_wired=True,Source_icons1_scalar_publication_versions_wired=True,Source_icons1_producer_receiver_output_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'paired-lifecycle':next(group['work']for group in old['remaining_work_groups']if group['id']=='paired-lifecycle').replace('Original receipt reserve still uses a creating lifetime lookup; reservation waits and complete material cost/status/output qualification remain open.','Selected material173/174 peer reservation now uses first Context life through the original app.fence; other reserves and complete material cost/status/output qualification remain open.')+' New same-SHA453 checks cover42 direct plus32 actual original caller fence waits,15 original receipt counters and1 constructor rollback. Original reserve remains unchanged for other callers.'}
for group in v['remaining_work_groups']:
 if group['id']in updates:group['work']=updates[group['id']]
v['remaining']=[x['work']for x in v['remaining_work_groups']];v['historical_remaining_preserved_in']=previous['manifest'];assert len(v['remaining_work_groups'])==12
for key in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[key]is False,key
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),system_goal=v['goal_reset_receipt'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
text=work.read_text('utf-8');text=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{v['runtime']['sha256']}`",text)
text=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",text)
text=re.sub(r'\d+個特定HP消費端直接呼叫點',f"{inv['observations']['current_direct_HP_call_sites']}個特定HP消費端直接呼叫點",text)
text=text.replace('更新：2026-10-25。','更新：2026-10-27。')
artifact_paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']}
text=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:artifact_paths[m[1]],text)
a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
block='## 本次已更新的程式\n\n材料173／174的原peer receipt預留呼叫帶入既有第一次Context Proof，BATTLE使用已保留的角色／主人／actor／status與lifetime，缺失、退休或替換資料直接拒絕，不補建lifetime。WORLD／未載入Source控制保留原reserve。\n\n完整原reserve副本保留no-app processes條件、application fence、packet／worker關閉規則、原Receipt／Frame／pending與handlers／例外回滾。等待原app.fence後，在packet scope／計數器／Receipt constructor之前重新檢查第一次proof與同一application；拒絕走原null／monitorexit／return分支。原生reserve與其他呼叫端未變。\n\n24原方法inverse、1 caller port、1完整reserve副本、0原欄位新增、1無欄位helper；396繼承no-init＋3 explicit owner、453新增＋12580同SHA繼承及同位元重建通過。42直接fence waits＋32原材料呼叫端fence waits，15個原Receipt登記／釋放／重複close檢查、1次constructor Throwable原handlers回滾。20 SQL reader bytes、57 direct lock inventory與7個獨立測試SHA強制tie維持。\n\n此選定peer reserve缺口已接線；材料實際成本／機率／狀態／IO、其他材料與生產端reserve、完整Source／外控、中文MySQL、後端／Main／memory與實機仍待完成。未部署、未建立新基準或寫入Source／玩家資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-27 material peer reserve first life/app fence：候選`{v['runtime']['sha256']}`，24原方法inverse、1 caller／1完整reserve副本、453新增＋12580繼承、42直接＋32原caller fence waits、15原Receipt／1原constructor rollback及同位元重建通過。其他reserve、材料實際成本／狀態／IO、完整Source／Main／memory／MySQL／實機仍未完成，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-material-peer-reserve-first-life-app-fence',full_goal_achieved=False)
continuation['source_target_negative_reserve_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
