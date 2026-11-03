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
latest=read(D/'source-weapon-counter-magic-build-current.json');repro=read(D/'source-weapon-counter-magic-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_weapon_counter_magic_contract_scenarios']==506
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
inherited_counts={k:v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_weapon_counter_magic_contract_scenarios'}
assert inherited_counts==parent_counts and sum(inherited_counts.values())==13586
assert build['inherited_same_SHA_scenarios']==13586
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['PASS independent Source weapon Counter Magic inverse: 6 original methods,1 selected successful-stack call,0 field/sibling changes','PASS398 inherited runtime no-init coverage plus2 explicit weapon Counter Magic owners','PASS506 weapon full material Counter Magic','original-casts=8, encrypted-memory-frames=56, actual-Source-query-waits=12','PASS7 pair admission test-only']:assert marker in log,marker
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
for name in ['NativeSourceWeaponCounterMagic.java','SourceWeaponCounterMagicMethods.java','SourceWeaponCounterMagicDelta.java','SourceWeaponCounterMagicLoad.java','SourceWeaponFullPathPlan.java','build_source_weapon_counter_magic.py','reproduce_source_weapon_counter_magic.py','verify_source_weapon_counter_magic.py','checkpoint_source_weapon_counter_magic.py','reproduce_source_weapon_counter_gap.py','audit_source_weapon_next.py','SourceWeaponPlayerFullPathPlan.java','reproduce_source_weapon_player_gap.py']:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-weapon-controlled-Counter-Magic-full-material-life-remaining-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Actual BATTLE material27 controlled-NPC successful-stack path now consumes Counter Magic unless the existing Source exception set contains27, emits the original Counter Magic effect to the original audience, and suppresses the weapon stack. Failed probability and Earth Bind do not consume the counter. WORLD/unmarked original fallback stays unchanged.', 'Exactly one original successful addWeaponBreakStack call is substituted by a fieldless helper. The complete original cost/RNG/material/reuse/MP/failure-message/output body and every other method independently invert; first Context/proof/life/master/actor are checked without constructing replacement role or status.', 'Actual NativeLightAction.dispatch on nonempty strict Source NPC proxy data uses real RuntimeInventory, GameStateStore(persistent=false, ephemeral=true), canonical statuses, original native pair/reservation/material/MP/admission/status/packet path and original cipher into memory sockets. No live repository, database, server or client is initialized. A manual ScheduledThreadPoolExecutor holds genuine cooldown tasks without starting a worker; tasks are cancelled and released per fixture.', '506 new ordinary checks plus13586 inherited same-SHA checks and byte-identical frozen replay.8 full original material casts cover success/counter/dynamic exception/miss/Earth Bind/MP/material/reuse;56 actual encrypted memory frames are decrypted and compared to original weapon/counter/action/MP/failure packets.12 actual original Source-query waits invalidate source/owner role/status/life, actor life/status/attachment, Main/STOPPED and missing/foreign hub lifetime before any material, MP, stack or frame mutation. An independent executable oracle uses the exact frozen Source Counter Magic method and104 fixed exceptions.', 'The real PC weapon branch, remaining material/Source consumers, all other damage producers and lifecycles, formal0.6.71 composition, actual same-SHA backend/Main/memory, Chinese MySQL equipped-character readonly validation and client cursor acceptance remain open. No deployment, formal overwrite, new baseline or Source/player SQL/schema/WAL writes.']
v['program_checks']={'JVM_inherited_no_initialization_checks': 398, 'additional_weapon_counter_magic_owners_explicitly_verified': 2, 'original_methods_independently_inverted': 6, 'original_changed_owners': 1, 'selected_old_call_sites': 1, 'new_original_fields': 0, 'new_runtime_class_entries': 1, 'pure_weapon_counter_magic_contract_scenarios': 506, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 13586, 'test_only_forced_tie_fixture_checks': 7, 'test_only_fixture_is_not_candidate_SHA': True, 'actual_original_material_casts': 8, 'original_encrypted_memory_frames': 56, 'actual_original_Source_query_waits': 12, 'independent_exact_Source_counter_consumer_oracle': True, 'existing_Source_counter_exception_set_retained': True, 'original_Source_stats_query_outside_actor_monitor': True, 'first_life_role_actor_status_query_revalidation': True, 'original_material_MP_reuse_failure_and_packet_bodies_retained': True, 'strict_repository_proxy_no_SQL': True, 'GameStateStore_nonpersistent_and_no_state_file': True, 'manual_cooldown_tasks_no_threads_and_terminal_cleanup': True, 'candidate_direct_lock_inventory_methods': 57, 'new_global_actor_registry_TLS_queue_domain_or_worker': False, 'byte_identical_frozen_rebuild': True, 'new_SELECTs': 0, 'new_connection_ownership': False, 'full_actual_PC_weapon_all_material_and_Source_qualified': False, 'bound_actual_server_game_or_Main_shutdown_cases': 0, 'full_producers_Source_backend_Main_memory_game_qualified': False}
v['historical_parent_Source_freshness']=parent_Source_freshness
gap=read(D/'source-weapon-counter-gap-current.json');verify(gap);verify(read(Path(gap['manifest']['path'])));v['counter_gap_reproduction']=gap['manifest']
next_weapon=read(D/'source-weapon-next-reference-current.json');verify(next_weapon);assert next_weapon['runtime']==latest['runtime'];verify(read(Path(next_weapon['manifest']['path'])));v['remaining_weapon_PC_reference_audit']=next_weapon['manifest']
player_gap=read(D/'source-weapon-player-gap-current.json');verify(player_gap);assert player_gap['runtime']==latest['runtime'];verify(read(Path(player_gap['manifest']['path'])));v['remaining_weapon_PC_dynamic_exception_failure']=player_gap['manifest']
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_weapon_counter_magic_contract','Source_weapon_counter_magic_oracle','weapon_counter_magic_port_census','Source_weapon_target_life_contract','weapon_target_life_port_census','Source_target_negative_reserve_contract','target_negative_reserve_port_census','Source_target_negative_life_contract','target_negative_life_port_census','Source_proc_scalar_extras_contract','proc_scalar_extras_port_census','Source_proc68_first_queue_contract','proc68_first_queue_port_census','Source_proc_scalar_skills_contract','Source_proc_scalar_icon_oracle','proc_scalar_skills_port_census','Source_proc_timed_refresh_contract','Source_proc_timed_refresh_oracle','proc_timed_refresh_port_census','Source_target_icon_batch_contract','target_icon_batch_port_census','Source_icons1_output_contract','icons1_output_port_census','Source_icons1_epoch_contract','Source_icons1_epoch_fields','icons1_epoch_port_census','Source_icons1_brave_origin_contract','icons1_brave_origin_port_census','Source_icons1_inputs_contract','icons1_inputs_port_census','Source_icons1_contract','Source_icons1_oracle','icons1_port_census','Source_proc68_icon_contract','Source_proc68_icon_oracle','proc68_icon_port_census','Source_proc68_graphics_contract','Source_proc68_graphics_oracle','proc68_graphics_port_census','Source_context_role_frame_contract','context_role_frame_port_census','Source_batch_role_frame_contract','batch_role_frame_port_census','Source_batch_terminal_contract','batch_terminal_port_census','Source_controlled_role_frame_contract','controlled_role_frame_port_census','Source_controlled_audience_contract','controlled_audience_port_census','Source_poison_audience_contract','poison_audience_port_census','Source_element_audience_contract','element_audience_port_census','Source_element_transport_contract','element_transport_port_census','Source_physical_transport_contract','physical_transport_port_census','Source_physical_audience_contract','physical_audience_port_census','Source_raw_audience_contract','raw_audience_port_census','Source_equipment_final_output_contract','equipment_final_output_port_census','Source_barrier_targets_contract','Source_barrier_target_oracle','Source_barrier_skill_ids','barrier_skill_matrix','barrier_targets_port_census','Source_skill_defaults_contract','Source_skill_config_oracle','skill_defaults_port_census','Source_doll_defense_audience_contract','doll_defense_audience_port_census','Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_material_27_controlled_counter_magic_wired=True,Source_material_27_first_target_context_wired=True,Source_material_173_174_first_peer_receipt_reserve_wired=True,Source_material_173_174_first_context_life_wired=True,Source_eight_custom_PC_scalar_Proc_paths_wired=True,Source_custom68_first_status_queue_wired=True,Source_four_custom_PC_scalar_Proc_paths_wired=True,Source_custom68_timer_refresh_and_actual_ability_guard_wired=True,Source_target_icon_batch_wired=True,Source_icons1_scalar_publication_versions_wired=True,Source_icons1_producer_receiver_output_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'paired-lifecycle':next(group['work']for group in old['remaining_work_groups']if group['id']=='paired-lifecycle')+' Material27 nonempty full-path Source query now has12 actual readonly-query waits rejecting stale first role/status/lifetime/actor/Main before costs or IO. Actual backend/Main/memory remains open.', 'materials':next(group['work']for group in old['remaining_work_groups']if group['id']=='materials')+' Material27 controlled Counter Magic/dynamic exception is corrected at the original successful-stack call.8 full native casts and56 memory cipher frames qualify this controlled branch and preserve original costs; PC weapon branch and other materials remain open. Two actual PC casts reproduce a confirmed dynamic Counter Magic exception failure on this candidate; the original receipt/material/MP path completes but still consumes the bypassed ward. The exact failure is frozen as the next program fix.'}
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
block='## 本次已更新的程式\n\n材料27對受控NPC成功命中後，依固定Source先消耗魔法屏障並取消壞物層數；既有Source外控例外保留屏障。未命中或地面束縛保留屏障。僅替換原成功加層呼叫，原WORLD／unmarked後備與完整材料／MP／機率／冷卻／封包本體維持。\n\n完整NativeLightAction入口使用非空嚴格記憶體Source資料proxy、真RuntimeInventory、明確persistent=false的GameStateStore、原生狀態與加密記憶體socket。8次完整材料施法、56個原生加密封包、12次原Source查詢等待，驗證屏障／例外／未命中／束縛／資源不足／冷卻及角色／status／life／actor／Main失效後不扣資源、不改層數、不送包。真實MySQL與遊戲尚未執行。\n\n6原方法inverse、1原成功呼叫點、0欄位變更、1無欄位helper；398繼承no-init＋2 explicit owner；506新增＋13586同SHA繼承檢查與同位元重建通過。獨立oracle執行固定Source原屏障方法與104項固定例外。手動冷卻排程不建立worker，所有暫存task與receipt在隔離案例結束清除。20 SQL reader bytes、57 direct player lock inventory與7個獨立測試SHA強制tie維持。\n\nPC壞物分支另有2次完整施法重現魔屏外控例外仍被消耗，已凍結失敗證據，列為下一修正。其餘材料／傷害生產端、完整Source／外控、正式隔離合成、中文MySQL、後端／Main／memory與實機仍待完成；未部署、未建立新基準或寫入Source／玩家資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-27 material27 controlled Counter Magic：候選`{v['runtime']['sha256']}`；6原方法inverse、1原成功呼叫點、506新增＋13586同SHA繼承及同位元重建通過。8次完整材料施法、56個加密記憶體封包、12次原Source查詢等待；原成本／MP／機率／外控例外與失效拒絕保留。PC分支／其餘Source／後端／MySQL／實機未完成，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-weapon-controlled-Counter-Magic-full-material-query-life',full_goal_achieved=False)
continuation['source_weapon_counter_magic_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
