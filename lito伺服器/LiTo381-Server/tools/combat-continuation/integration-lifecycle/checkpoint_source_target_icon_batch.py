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
latest=read(D/'source-target-icon-batch-build-current.json');repro=read(D/'source-target-icon-batch-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_target_icon_batch_contract_scenarios']==317
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
inherited_counts={k:v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_target_icon_batch_contract_scenarios'}
assert inherited_counts==parent_counts and sum(inherited_counts.values())==8881
assert build['inherited_same_SHA_scenarios']==8881
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['PASS independent target icon Batch inverse: 20 original methods,8 call ports,1 transient Batch sidecar,1 borrowed-owner completion sibling','PASS385 inherited runtime no-init coverage plus5 explicit target icon Batch owners','PASS317 target-icon Batch actual-original-accepted','captures=26, original-batches=25, completions=44, target-memory-frames=23, original-failures=2','PASS7 pair admission test-only']:assert marker in log,marker
with zipfile.ZipFile(build['base']['path'])as parent,zipfile.ZipFile(latest['runtime']['path'])as candidate:
 for entry in ['local/lito381/server/'+name+'.class'for name in ['NativeSourcePhysicalNpc$Commit','NativeSourcePhysicalNpc$Output','NativeSourcePhysicalAudience','NativeSourcePhysicalAudience$Audience','NativeSourcePhysicalAudience$Receiver','NativeAreaMaterialAction','NativeSourceRawDamage','NativeSourceRawDamage$Output','NativeSourceRawAudience','NativeSourceRawAudience$Proof','NativeSourceFirstPlayers','NativeSourceFirstPlayers$Snapshot','NativeSourceDollAudience','NativeSourceEquipmentOutput','NativeSourceEquipmentOutput$Viewer','NativeSourceEquipmentFinalOutput','NativeSourceEquipmentFinalOutput$First','NativeSourceDollDefense$Snapshot','NativeSourceDollDefense$Proof','NativeSourceDollPassive','NativeSourceDollPassive$Choice','NativePlayerCommit','NativePlayerCommit$DefenseVisual','NativeSourceDollAudience','NativeSourceDollDetachedOutput','NativeSourceEquipmentProcLife','NativeSourceEquipmentOutput$Detached','NativeSourceEquipmentOutput$Scope','NativeSourceEquipmentOutput$Link','NativeSourceEquipmentOutput$Output','NativeProcHealingPlayer','NativeProcHealingPlayer$Target','NativeLightAction']]:
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
for name in ['NativeSourceTargetIconBatch.java', 'SourceTargetIconBatchMethods.java', 'SourceTargetIconBatchDelta.java', 'SourceTargetIconBatchLoad.java', 'SourceTargetIconBatchPlan.java', 'build_source_target_icon_batch.py', 'reproduce_source_target_icon_batch.py', 'verify_source_target_icon_batch.py', 'checkpoint_source_target_icon_batch.py']:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-target-icons-original-Batch-borrowed-role-and-receipt-lifecycle-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['The two existing dedicated68/full ICONS1 task enqueues still call original deferScoped with original admission and original lambda wrappers. A transient Object on the original Batch indexes only those wrapper identities and original Tasks; target tasks claim once in original traversal or all original rescue branches. Existing graphics/generic tasks keep original caster/target policy; original cube rescue delegates unchanged.', 'At original paired output capture, borrow the existing target sourceItemOwner without creating or renewing it. A complete original NativeRolePublication.completion sibling retains original target action-lock check, Hub/application, Lease and original TLS scope/restore; only publicationOwner is replaced with a read-only existing-owner lookup. Target final frame keeps all original role/accepted/item guards and exact accepted target proof. No global bypass, owner recreation, new queue/worker/Frame/Context or status authority.', '20 original methods independently invert;8 call ports,1 transient Batch-owned sidecar,1 complete original borrowed-owner completion sibling,2 new runtime classes.385 inherited no-init plus5 explicit owners;317 new plus8881 inherited same-SHA checks and byte-identical frozen replay.26 original accepted captures,25 original batches,44 callbacks,23 target memory frames,2 original failures and6 actual delayed-receipt cases checked;20 protected SQL class bytes and7 separate forced-tie checks retained.', 'Actual receipt cases hold a third actor action lock and prove no early send/callback, original pending ownership, normal/caster-retired/target-scene-ABA/Main/STOPPED/DRAINING outcomes, once-only release and terminal sidecar cleanup. Missing/retired/replaced target item owner and replaced application reject target icons without initializing missing roles. Original failed caster completion stays false; original exceptions and callback scope restore survive target rescue.', 'Room-registry replacement/external lock-origin review, other output producers/OwnCharStatus, Source fruit item path, automatic Chinese aliases/source reload/actual MySQL, all materials/Source/backend/Main/memory/foreground/game remain open. No deployment, formal overwrite, new baseline or Source/player SQL/schema/WAL writes.']
v['program_checks']={'JVM_inherited_no_initialization_checks': 385, 'additional_target_icon_Batch_owners_explicitly_verified': 5, 'original_methods_independently_inverted': 20, 'original_changed_owners': 3, 'original_call_ports': 8, 'new_transient_Batch_sidecar_fields': 1, 'complete_original_borrowed_owner_completion_siblings': 1, 'new_runtime_class_entries': 2, 'pure_target_icon_batch_contract_scenarios': 317, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 8881, 'test_only_forced_tie_fixture_checks': 7, 'test_only_fixture_is_not_candidate_SHA': True, 'actual_original_accepted_captures': 26, 'actual_original_Batches': 25, 'actual_original_callbacks': 44, 'actual_target_memory_frames': 23, 'actual_original_failure_cases': 2, 'actual_delayed_target_receipt_cases': 6, 'original_admission_wrappers_callbacks_exceptions_cube_rescue_retained': True, 'existing_target_item_owner_read_only_no_initialization': True, 'original_target_role_application_and_final_frame_guards_retained': True, 'original_enclosing_role_scope_restored_before_callbacks': True, 'terminal_Batch_sidecar_released': True, 'original_failed_caster_completion_promoted': False, 'new_global_TLS_queue_worker_Frame_Context_or_actor_registry': False, 'byte_identical_frozen_rebuild': True, 'new_SELECTs': 0, 'new_connection_ownership': False, 'room_registry_and_external_lock_origins_qualified': False, 'bound_actual_server_game_or_Main_shutdown_cases': 0, 'full_producers_Source_backend_Main_memory_game_qualified': False}
v['historical_parent_Source_freshness']=parent_Source_freshness
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_target_icon_batch_contract','target_icon_batch_port_census','Source_icons1_output_contract','icons1_output_port_census','Source_icons1_epoch_contract','Source_icons1_epoch_fields','icons1_epoch_port_census','Source_icons1_brave_origin_contract','icons1_brave_origin_port_census','Source_icons1_inputs_contract','icons1_inputs_port_census','Source_icons1_contract','Source_icons1_oracle','icons1_port_census','Source_proc68_icon_contract','Source_proc68_icon_oracle','proc68_icon_port_census','Source_proc68_graphics_contract','Source_proc68_graphics_oracle','proc68_graphics_port_census','Source_context_role_frame_contract','context_role_frame_port_census','Source_batch_role_frame_contract','batch_role_frame_port_census','Source_batch_terminal_contract','batch_terminal_port_census','Source_controlled_role_frame_contract','controlled_role_frame_port_census','Source_controlled_audience_contract','controlled_audience_port_census','Source_poison_audience_contract','poison_audience_port_census','Source_element_audience_contract','element_audience_port_census','Source_element_transport_contract','element_transport_port_census','Source_physical_transport_contract','physical_transport_port_census','Source_physical_audience_contract','physical_audience_port_census','Source_raw_audience_contract','raw_audience_port_census','Source_equipment_final_output_contract','equipment_final_output_port_census','Source_barrier_targets_contract','Source_barrier_target_oracle','Source_barrier_skill_ids','barrier_skill_matrix','barrier_targets_port_census','Source_skill_defaults_contract','Source_skill_config_oracle','skill_defaults_port_census','Source_doll_defense_audience_contract','doll_defense_audience_port_census','Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_target_icon_batch_wired=True,Source_icons1_scalar_publication_versions_wired=True,Source_icons1_producer_receiver_output_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'presentation':'The two existing dedicated68/full ICONS1 task enqueues still call original deferScoped with original admission and original lambda wrappers. A transient Object on the original Batch indexes only those wrapper identities and original Tasks; target tasks claim once in original traversal or all original rescue branches. Existing graphics/generic tasks keep original caster/target policy; original cube rescue delegates unchanged. At original paired output capture, borrow the existing target sourceItemOwner without creating or renewing it. A complete original NativeRolePublication.completion sibling retains original target action-lock check, Hub/application, Lease and original TLS scope/restore; only publicationOwner is replaced with a read-only existing-owner lookup. Target final frame keeps all original role/accepted/item guards and exact accepted target proof. No global bypass, owner recreation, new queue/worker/Frame/Context or status authority. Room-registry replacement/external lock-origin review, other output producers/OwnCharStatus, Source fruit item path, automatic Chinese aliases/source reload/actual MySQL, all materials/Source/backend/Main/memory/foreground/game remain open. No deployment, formal overwrite, new baseline or Source/player SQL/schema/WAL writes.'}
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
block='## 本次已更新的程式\n\n將既有68專用圖示與完整ICONS1的兩個task，在原deferScoped成功入列後，以wrapper身分附在原Batch的transient sidecar。原輸出包裝與入列鎖檢查保留；原遍歷與三個rescue分支只執行各目標task一次。其他graphics／generic輸出與cube rescue保持原規則。\n\n目標圖示在原成對鎖內借用既有sourceItemOwner，不初始化或更新owner。完整複製原NativeRolePublication.completion，保留目標action lock檢查、Hub／application／Lease與原TLS scope還原，只將publicationOwner改為唯讀既有owner。最終frame保留原角色／accepted／item guard與目標proof；未取消全域角色檢查。\n\n20原方法inverse、8 call ports／1 Batch transient sidecar／1完整借用owner completion sibling、385繼承no-init＋5 explicit owners、317新增＋8881同SHA繼承與同位元重建通過。26 capture／25 Batch／44 callback／23目標memory frames／2原例外與6個實際延遲receipt案例；20 SQL reader及7個獨立強制tie檢查保留。\n\n實際receipt持有第三角色鎖，核對先不送包、待全部鎖放開後投遞、原pending計數與一次釋放；涵蓋正常、施法者退場、目標scene ABA、Main、STOPPED及DRAINING。缺失／退休／替換目標item owner及application替換均擋下目標圖示。原施法者失敗callback仍為false；原例外、scope還原與terminal sidecar清除保留。\n\nroom registry／外部鎖來源、其他producer／OwnCharStatus、果實入口、中文alias／來源重載／MySQL及完整材料技能／Source／後端／Main／memory／實機仍未完成；未部署或寫入資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-27 target icons original Batch／receipt：候選`{v['runtime']['sha256']}`，兩個目標圖示task以原Batch sidecar執行一次，借用既有目標item owner與原角色發布scope，保留原guard及callback結果。20原方法inverse、317新＋8881繼承、25 Batch／44 callback／23目標memory frames／6 actual receipts及同位元重建通過；room registry／其他producer／OwnCharStatus／Source／Main／memory／MySQL／實機未完成。未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-target-icons-original-Batch-borrowed-owner-and-receipt',full_goal_achieved=False)
continuation['source_target_icon_batch_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
