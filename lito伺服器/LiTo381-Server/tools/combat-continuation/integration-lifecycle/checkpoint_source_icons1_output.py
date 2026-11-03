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
latest=read(D/'source-icons1-output-build-current.json');repro=read(D/'source-icons1-output-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_icons1_output_contract_scenarios']==100
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
inherited_counts={k:v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_icons1_output_contract_scenarios'}
assert inherited_counts==parent_counts and sum(inherited_counts.values())==8781
assert build['inherited_same_SHA_scenarios']==8781
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['PASS independent ICONS1 output inverse: 2489 original methods,1 dedicated enqueue port,1 full original final sibling','PASS382 inherited runtime no-init coverage plus5 explicit full ICONS1 output owners','PASS100 Source ICONS1 output actual-original-proc','captures=13, memory-frames=2, final-receiver-waits=11','PASS7 pair admission test-only']:assert marker in log,marker
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
for name in ['NativeSourceIcons1Output.java', 'SourceIcons1OutputMethods.java', 'SourceIcons1OutputDelta.java', 'SourceIcons1OutputLoad.java', 'SourceIcons1OutputPlan.java', 'build_source_icons1_output.py', 'reproduce_source_icons1_output.py', 'verify_source_icons1_output.py', 'checkpoint_source_icons1_output.py']:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-accepted-proc68-full-ICONS1-target-output-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['After the accepted original custom-duration skill68 output, preserve the dedicated task and append one complete Source ICONS1 task when all original scalar inputs exist. Reuse target-only accepted proof,17 weak publication cells, catalog-bound coexist policy and target-specific weak room patience value. No new owner field, HP authority, Frame, Context, actor registry, worker or SQL.', 'Complete original synchronized final receiver sibling validates exact receiver/packet, original target role/owner/life/scene/Hub, Main/STOPPED,17 scalar versions/container identity, policy/catalog identity, target patience and active expiry before original cipher/socket path. No17 status-monitor acquisition. DRAINING accepted. Skill68 dedicated lease is separate because68 has no ICONS1 slot.', '2489 original methods independently invert, one original enqueue port and one full final receiver sibling,382 inherited no-init plus5 explicit owners.100 new plus8781 inherited same-SHA checks,13 actual original captures,2 memory frames and11 forced final receiver waits; byte-identical frozen rebuild.20 SQL reader class bytes unchanged and7 separate forced-tie checks preserved.', 'Only the existing accepted custom68 path is wired. Actual whole Batch/caster-retirement delivery, full room-registry replacement and external lock-origin audit, other producers/OwnCharStatus, fruit item consumer, automatic Chinese aliases/source reload/actual MySQL/full Source/Main/memory/backend/game remain open. No deployment, formal overwrite, new baseline or Source/player SQL/schema/WAL writes.']
v['program_checks']={'JVM_inherited_no_initialization_checks': 382, 'additional_output_owners_explicitly_verified': 5, 'original_methods_independently_inverted': 2489, 'original_changed_owners': 2, 'original_enqueue_ports': 1, 'complete_original_final_receiver_siblings': 1, 'new_original_owner_fields': 0, 'new_runtime_class_entries': 3, 'pure_icons1_output_contract_scenarios': 100, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 8781, 'test_only_forced_tie_fixture_checks': 7, 'test_only_fixture_is_not_candidate_SHA': True, 'actual_original_accepted_captures': 13, 'actual_memory_socket_frames': 2, 'actual_final_receiver_monitor_waits': 11, 'weak_scalar_container_cells': 17, 'capture_requires_existing_target_action_lock': True, 'all_status_monitors_acquired_in_final_guard': False, 'whole_Batch_caster_retirement_delivery_qualified': False, 'room_registry_and_external_lock_origins_qualified': False, 'byte_identical_frozen_rebuild': True, 'new_SELECTs': 0, 'new_connection_ownership': False, 'accepted_custom68_full_ICONS1_output_wired': True, 'OwnCharStatus_and_other_producers_wired': False, 'bound_actual_server_game_or_Main_shutdown_cases': 0, 'full_producers_Source_backend_Main_memory_game_qualified': False}
v['historical_parent_Source_freshness']=parent_Source_freshness
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_icons1_output_contract','icons1_output_port_census','Source_icons1_epoch_contract','Source_icons1_epoch_fields','icons1_epoch_port_census','Source_icons1_brave_origin_contract','icons1_brave_origin_port_census','Source_icons1_inputs_contract','icons1_inputs_port_census','Source_icons1_contract','Source_icons1_oracle','icons1_port_census','Source_proc68_icon_contract','Source_proc68_icon_oracle','proc68_icon_port_census','Source_proc68_graphics_contract','Source_proc68_graphics_oracle','proc68_graphics_port_census','Source_context_role_frame_contract','context_role_frame_port_census','Source_batch_role_frame_contract','batch_role_frame_port_census','Source_batch_terminal_contract','batch_terminal_port_census','Source_controlled_role_frame_contract','controlled_role_frame_port_census','Source_controlled_audience_contract','controlled_audience_port_census','Source_poison_audience_contract','poison_audience_port_census','Source_element_audience_contract','element_audience_port_census','Source_element_transport_contract','element_transport_port_census','Source_physical_transport_contract','physical_transport_port_census','Source_physical_audience_contract','physical_audience_port_census','Source_raw_audience_contract','raw_audience_port_census','Source_equipment_final_output_contract','equipment_final_output_port_census','Source_barrier_targets_contract','Source_barrier_target_oracle','Source_barrier_skill_ids','barrier_skill_matrix','barrier_targets_port_census','Source_skill_defaults_contract','Source_skill_config_oracle','skill_defaults_port_census','Source_doll_defense_audience_contract','doll_defense_audience_port_census','Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_icons1_scalar_publication_versions_wired=True,Source_icons1_producer_receiver_output_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'presentation':'After the accepted original custom-duration skill68 output, preserve the dedicated task and append one complete Source ICONS1 task when all original scalar inputs exist. Reuse target-only accepted proof,17 weak publication cells, catalog-bound coexist policy and target-specific weak room patience value. No new owner field, HP authority, Frame, Context, actor registry, worker or SQL. Complete original synchronized final receiver sibling validates exact receiver/packet, original target role/owner/life/scene/Hub, Main/STOPPED,17 scalar versions/container identity, policy/catalog identity, target patience and active expiry before original cipher/socket path. No17 status-monitor acquisition. DRAINING accepted. Skill68 dedicated lease is separate because68 has no ICONS1 slot. Only the existing accepted custom68 path is wired. Actual whole Batch/caster-retirement delivery, full room-registry replacement and external lock-origin audit, other producers/OwnCharStatus, fruit item consumer, automatic Chinese aliases/source reload/actual MySQL/full Source/Main/memory/backend/game remain open. No deployment, formal overwrite, new baseline or Source/player SQL/schema/WAL writes.'}
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
block='## 本次已更新的程式\n\n原已接受的自訂秒數技能68完成原生狀態更新後，保留原專用圖示task，補入完整Source ICONS1 task。借用原目標proof、17個弱容器版本、來源綁定的共存policy與目標Patience值；原生資料不足時不初始化缺失狀態。\n\n完整複製原synchronized最終receiver，在原cipher之前驗證目標／角色／owner／life／scene／Hub、Main／STOPPED、容器版本與身分、catalog／policy、Patience與有效期限。DRAINING保留；ICONS1沒有68欄位，與68專用圖示lease分開。最終guard不取得17個status鎖。\n\n2489原方法inverse、1 enqueue port與1完整receiver sibling、382繼承no-init＋5 explicit owners、100新增＋8781同SHA繼承檢查及同位元重建通過。13次原生capture、2次記憶體socket frame、11次最終receiver等待，20 SQL reader與7個獨立強制tie檢查保留。\n\n完整Batch在施法者退場後的投遞、room registry替換及外部鎖來源、其他生產端與OwnCharStatus、果實物品入口、中文alias／來源熱重載／MySQL、完整Source／Main／memory／後端／實機仍待補齊。未部署或寫入資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-27 accepted proc68 full ICONS1：候選`{v['runtime']['sha256']}`，完整Source snapshot接原已接受目標與最終receiver。2489原方法inverse、100新＋8781同SHA繼承、13capture／2memory frames／11receiver waits與同位元重建通過。完整Batch／room registry／其他producer／OwnCharStatus／Source／Main／memory／MySQL／實機未完成；未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-accepted-custom68-full-ICONS1-target-output',full_goal_achieved=False)
continuation['source_icons1_output_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
