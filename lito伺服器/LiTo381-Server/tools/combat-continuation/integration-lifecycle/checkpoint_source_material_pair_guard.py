"""Checkpoint shared material pair first identities and two native interface ABI repairs."""
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
  if set(v)=={'original','frozen','meaning'} and v['meaning'].startswith('Historical mutable replay-script reference'):
   original=v['original'];frozen=v['frozen']
   assert Path(original['path'])==R/'reproduce_source_weapon_player_gap.py'
   assert original['sha256']==frozen['sha256'] and sha(Path(original['path']))!=original['sha256']
   assert Path(frozen['path']).parent.name=='editable-input-snapshot'
   verify(frozen)
   return
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
latest=read(D/'source-material-pair-guard-build-current.json');repro=read(D/'source-material-pair-guard-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_material_pair_guard_contract_scenarios']==150 and build['pure_weapon_final_frame_contract_scenarios']==540
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
inherited_counts={k:v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_material_pair_guard_contract_scenarios'}
assert inherited_counts==parent_counts and sum(inherited_counts.values())==15025
assert build['inherited_same_SHA_scenarios']==15025
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['PASS independent material pair inverse: 2539 complete original methods,2 explicit Task identity ports,2 interface opcodes,1 complete original Task sibling,0 fields','PASS401 inherited runtime no-init coverage plus5 explicit shared material pair owners','PASS150 remaining material actual-ingress','original-attempts=30, original-pair-waits=30','PASS540 source-equipped weapon','original-casts=39, encrypted-memory-frames=299, original-pair-waits=10, original-final-viewer-waits=16, detached-original-final-waits=13','PASS7 pair admission test-only']:assert marker in log,marker
with zipfile.ZipFile(build['base']['path'])as parent,zipfile.ZipFile(latest['runtime']['path'])as candidate:
 for entry in ['local/lito381/server/'+name+'.class'for name in ['NativeSourcePhysicalNpc$Commit','NativeSourcePhysicalNpc$Output','NativeSourcePhysicalAudience','NativeSourcePhysicalAudience$Audience','NativeSourcePhysicalAudience$Receiver','NativeAreaMaterialAction','NativeSourceRawDamage','NativeSourceRawDamage$Output','NativeSourceRawAudience','NativeSourceRawAudience$Proof','NativeSourceFirstPlayers','NativeSourceFirstPlayers$Snapshot','NativeSourceDollAudience','NativeSourceEquipmentOutput','NativeSourceEquipmentOutput$Viewer','NativeSourceEquipmentFinalOutput','NativeSourceEquipmentFinalOutput$First','NativeSourceDollDefense$Snapshot','NativeSourceDollDefense$Proof','NativeSourceDollPassive','NativeSourceDollPassive$Choice','NativePlayerCommit$DefenseVisual','NativeSourceDollAudience','NativeSourceDollDetachedOutput','NativeSourceEquipmentOutput$Detached','NativeSourceEquipmentOutput$Scope','NativeSourceEquipmentOutput$Link','NativeSourceEquipmentOutput$Output','NativeProcHealingPlayer','NativeProcHealingPlayer$Target']]:
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
for name in ['SourceInvocationAbiCensus.java','audit_source_invocation_abi.py','NativeSourceMaterialPairGuard.java','SourceMaterialPairGuardMethods.java','SourceMaterialPairGuardDelta.java','SourceMaterialPairGuardLoad.java','SourceRemainingMaterialPairPlan.java','probe_source_material_pair_guard.py','build_source_material_pair_guard.py','reproduce_source_material_pair_guard.py','reproduce_source_material_pair_gap.py','checkpoint_source_material_pair_guard.py','verify_source_material_pair_guard.py','NativeSourceWeaponFinalFrame.java', 'SourceWeaponFinalFrameMethods.java', 'SourceWeaponFinalFrameDelta.java', 'SourceWeaponFinalFrameLoad.java', 'SourceWeaponOverlayPlan.java', 'SourceWeaponRollbackPlan.java', 'WeaponStateFault.java', 'SourceWeaponStateFaultMethods.java', 'run_source_weapon_overlay_checks.py', 'probe_source_weapon_final_frame.py', 'build_source_weapon_final_frame.py', 'reproduce_source_weapon_final_frame.py', 'checkpoint_source_weapon_final_frame.py', 'verify_source_weapon_final_frame.py', 'audit_source_weapon_next.py']:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-shared-material-pair-first-role-life-status-and-interface-ABI-remaining-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Shared commitPair now carries first role/life/status values in a bounded typed original Task. Before both original after-lock identity checks, the Task validates exact first status containers and first hub lifetime, then the original pair proof and exact original bindings; it never recaptures a stale Binding or initializes a replacement lifetime after waiting.', 'Earlier173/174 and27 Context proofs are borrowed as values. Canonical accepted Frame/corpse proof retains precedence. Initial WORLD/unloaded/null-target fallback retains the exact original Task via a complete original sibling. Original DRAINING identity and receipt admission, self-pair, transfer, checked exceptions, contention and terminal cleanup remain intact.', 'Two injected skillId getters in nativeMaterialTargetNegativeBody used invokevirtual against the TargetNegativeStatusSpell interface. Both now use invokeinterface, at the Source-none request and LOS ports; the rest of the original body is retained.', '150 new plus15025 inherited ordinary same-SHA checks and byte-identical frozen replay. Actual native173/174 ingress executes30 original target-lock waits, including replacement role, null/replaced status, life replacement, missing/foreign lifetime, Main, STOPPED, closed first proof and controls unloaded after admission. Normal paths pay original MP50/gem1 and MP10/gem5 exactly once; invalid paths charge and emit nothing. Probability, complete status semantics and final viewer delivery of173/174 remain unqualified.', '2539 complete original methods independently invert across3 owners,2 explicit original Task identity ports,2 interface opcodes,1 complete original Task sibling,0 original field changes and2 added classes.401 inherited no-init plus5 explicit owners. Existing material27 final receiver and Source weapon overlay/rollback checks are revalidated separately without merging test-only fault SHA counts.', 'Remaining materials/proc/default consumers, NPC/HP output, full Source/external controls, formal0.6.71 isolated composition, actual backend/Main/memory, Chinese MySQL and game cursor acceptance remain open. No deployment, formal overwrite, new baseline, Source/player SQL/schema/WAL writes.']
v['program_checks']={'JVM_inherited_no_initialization_checks': 401, 'additional_shared_material_pair_owners_explicitly_verified': 5, 'original_methods_independently_inverted': 2539, 'original_changed_owners': 3, 'explicit_Task_identity_ports': 2, 'interface_ABI_opcode_repairs': 2, 'complete_original_Task_siblings': 1, 'new_original_fields': 0, 'new_runtime_class_entries': 2, 'pure_material_pair_guard_contract_scenarios': 150, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 15025, 'test_only_forced_tie_fixture_checks': 7, 'test_only_fixture_is_not_candidate_SHA': True, 'actual_original_material_attempts': 30, 'actual_original_pair_waits': 30, 'original_cost_and_probability_and_status_bodies_retained': True, 'earlier_proof_values_without_late_lifetime_creation': True, 'original_accepted_corpse_precedence_retained': True, 'accepted_DRAINING_preserved': True, 'original_WORLD_unloaded_Task_identity_retained': True, 'candidate_direct_lock_inventory_methods': 57, 'new_global_actor_registry_TLS_queue_domain_or_worker': False, 'byte_identical_frozen_rebuild': True, 'new_SELECTs': 0, 'new_connection_ownership': False, 'bound_actual_server_game_or_Main_shutdown_cases': 0, 'full_producers_Source_backend_Main_memory_game_qualified': False}
v['historical_parent_Source_freshness']=parent_Source_freshness
abi_index=read(D/'source-invocation-ABI-census-current.json');verify(abi_index);assert abi_index['runtime']==latest['runtime'];abi=read(Path(abi_index['manifest']['path']));verify(abi);assert abi['mismatched_call_sites']==0;v['internal_invocation_ABI_census']=abi_index['manifest']
material_gap=read(D/'source-material-pair-gap-current.json');verify(material_gap);verify(read(Path(material_gap['manifest']['path'])));v['shared_material_pair_original_gap']=material_gap['manifest']
gap=read(D/'source-weapon-counter-gap-current.json');verify(gap);verify(read(Path(gap['manifest']['path'])));v['counter_gap_reproduction']=gap['manifest']
next_weapon=read(D/'source-weapon-next-reference-current.json');verify(next_weapon);assert next_weapon['runtime']==latest['runtime'];verify(read(Path(next_weapon['manifest']['path'])));v['remaining_weapon_PC_reference_audit']=next_weapon['manifest']
late_gap=read(D/'source-weapon-final-viewer-gap-current.json');verify(late_gap);verify(read(Path(late_gap['manifest']['path'])));v['historical_weapon_final_viewer_failure']=late_gap['manifest']
overlay_index=read(D/'source-weapon-overlay-checks-current.json');verify(overlay_index);assert overlay_index['runtime']==latest['runtime'];overlay=read(Path(overlay_index['manifest']['path']));verify(overlay);assert overlay['status'].startswith('passed-current-SHA') and overlay['ordinary_same_SHA_checks']==540 and overlay['test_only_rollback_checks']==38 and overlay['test_only_rollback_attempts']==4 and overlay['test_only_original_state_methods_inverted']==445;assert overlay['test_only_fault_runtime']['sha256']!=latest['runtime']['sha256'];v['source_weapon_overlay_and_separate_save_rollback']=overlay_index['manifest']
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_material_pair_guard_contract','material_pair_guard_port_census','Source_weapon_final_frame_contract','weapon_final_frame_port_census','Source_weapon_player_counter_contract','Source_weapon_durability_oracle','Source_weapon_Earth_expectation_correction','weapon_player_counter_port_census','Source_weapon_counter_magic_contract','Source_weapon_counter_magic_oracle','weapon_counter_magic_port_census','Source_weapon_target_life_contract','weapon_target_life_port_census','Source_target_negative_reserve_contract','target_negative_reserve_port_census','Source_target_negative_life_contract','target_negative_life_port_census','Source_proc_scalar_extras_contract','proc_scalar_extras_port_census','Source_proc68_first_queue_contract','proc68_first_queue_port_census','Source_proc_scalar_skills_contract','Source_proc_scalar_icon_oracle','proc_scalar_skills_port_census','Source_proc_timed_refresh_contract','Source_proc_timed_refresh_oracle','proc_timed_refresh_port_census','Source_target_icon_batch_contract','target_icon_batch_port_census','Source_icons1_output_contract','icons1_output_port_census','Source_icons1_epoch_contract','Source_icons1_epoch_fields','icons1_epoch_port_census','Source_icons1_brave_origin_contract','icons1_brave_origin_port_census','Source_icons1_inputs_contract','icons1_inputs_port_census','Source_icons1_contract','Source_icons1_oracle','icons1_port_census','Source_proc68_icon_contract','Source_proc68_icon_oracle','proc68_icon_port_census','Source_proc68_graphics_contract','Source_proc68_graphics_oracle','proc68_graphics_port_census','Source_context_role_frame_contract','context_role_frame_port_census','Source_batch_role_frame_contract','batch_role_frame_port_census','Source_batch_terminal_contract','batch_terminal_port_census','Source_controlled_role_frame_contract','controlled_role_frame_port_census','Source_controlled_audience_contract','controlled_audience_port_census','Source_poison_audience_contract','poison_audience_port_census','Source_element_audience_contract','element_audience_port_census','Source_element_transport_contract','element_transport_port_census','Source_physical_transport_contract','physical_transport_port_census','Source_physical_audience_contract','physical_audience_port_census','Source_raw_audience_contract','raw_audience_port_census','Source_equipment_final_output_contract','equipment_final_output_port_census','Source_barrier_targets_contract','Source_barrier_target_oracle','Source_barrier_skill_ids','barrier_skill_matrix','barrier_targets_port_census','Source_skill_defaults_contract','Source_skill_config_oracle','skill_defaults_port_census','Source_doll_defense_audience_contract','doll_defense_audience_port_census','Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_shared_material_pair_first_Task_identity_wired=True,Source_material_173_174_native_interface_ABI_repaired=True,Source_material_27_detached_output_final_role_life_wired=True,Source_material_27_source_weapon_overlay_and_original_save_rollback_checked=True,Source_material_27_PC_dynamic_counter_exception_wired=True,Source_material_27_controlled_Earth_bind_gate_corrected=True,Source_material_27_after_pair_first_binding_wired=True,Source_material_27_controlled_counter_magic_wired=True,Source_material_27_first_target_context_wired=True,Source_material_173_174_first_peer_receipt_reserve_wired=True,Source_material_173_174_first_context_life_wired=True,Source_eight_custom_PC_scalar_Proc_paths_wired=True,Source_custom68_first_status_queue_wired=True,Source_four_custom_PC_scalar_Proc_paths_wired=True,Source_custom68_timer_refresh_and_actual_ability_guard_wired=True,Source_target_icon_batch_wired=True,Source_icons1_scalar_publication_versions_wired=True,Source_icons1_producer_receiver_output_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={key:next(g['work']for g in old['remaining_work_groups']if g['id']==key)+' Shared pair typed Task now validates first role/life/status and existing hub lifetime before both original post-lock Binding checks. Actual173/174 ingress30 pair waits, exact original costs and no-cost/no-output stale rejection pass on current SHA; two erroneous interface opcodes corrected. Full remaining material semantics, Ctrl/client cursor, final output, backend/Main/memory and Source remain open.'for key in ['paired-lifecycle','materials']}
for group in v['remaining_work_groups']:
 if group['id']in updates:group['work']=updates[group['id']]
v['remaining']=[x['work']for x in v['remaining_work_groups']];v['historical_remaining_preserved_in']=previous['manifest']
v['inherited_test_coverage_adjustments']=copy.deepcopy(old.get('inherited_test_coverage_adjustments',[]));assert len(v['remaining_work_groups'])==12
for key in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[key]is False,key
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),system_goal=v['goal_reset_receipt'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
text=work.read_text('utf-8');text=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{v['runtime']['sha256']}`",text)
text=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",text)
text=re.sub(r'\d+個特定HP消費端直接呼叫點',f"{inv['observations']['current_direct_HP_call_sites']}個特定HP消費端直接呼叫點",text)
text=text.replace('更新：2026-10-25。','更新：2026-10-27。')
artifact_paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']}
text=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:artifact_paths[m[1]],text)
a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
block='## 本次已更新的程式\n\n共用commitPair以既有Task攜帶首次角色、life與status容器，在鎖後兩個原Binding核對點先驗證；沿用首次173／174／27證據與既有lifetime，不在等待後重新擷取或建立身分。接受後屍體Frame優先、DRAINING身分、原receipt admission／transfer／exception／close與WORLD／初始未載入後備保留。沒有新增TLS、全域索引、佇列或worker。\n\n修正原生173／174本體在Source-none與LOS轉接的兩個介面呼叫opcode。150新增＋15025繼承同SHA檢查及同位元重建；2539完整原方法inverse、2 Task身分點、2介面opcode、1完整原Task副本、0原欄位變更、2新類別；401繼承no-init＋5 explicit owners。\n\n30次173／174原生入口pair等待涵蓋角色切換、status清除或相同life容器替換、生命變更、lifetime遺失或替換、Main、STOPPED、proof關閉與中途卸載。正常173成本MP50／材料1、174成本MP10／材料5，失效路徑無成本與封包。此處不宣告173／174完整機率、狀態與最終接收者語意。\n\n材料27原最終送包／Source裝備覆蓋與獨立錯誤注入回滾另於現行SHA重驗；其餘材料／proc／NPC／外控、Source裝備實例身分、物理消費端、正式隔離合成、中文MySQL、完整後端／Main／memory與實機仍未完成。未部署、未建立正式基準、未寫入Source或玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-27 shared material pair：候選`{v['runtime']['sha256']}`；150新增＋15025繼承同SHA與同位元重建。173／174原生入口30次pair等待，首次角色／life／status守衛、兩個介面opcode修正，2539完整原方法inverse、0原欄位新增。其餘Source／材料／後端／MySQL／實機未完成，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-shared-material-pair-first-role-life-status-and-native-interface-ABI',full_goal_achieved=False)
continuation['source_material_pair_guard_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
