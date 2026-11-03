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
latest=read(D/'source-weapon-player-counter-build-current.json');repro=read(D/'source-weapon-player-counter-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_weapon_player_counter_contract_scenarios']==385
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
inherited_counts={k:v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_weapon_player_counter_contract_scenarios'}
assert parent_counts['pure_weapon_counter_magic_contract_scenarios']==506
expected_counts=dict(parent_counts,pure_weapon_counter_magic_contract_scenarios=514)
assert inherited_counts==expected_counts and sum(inherited_counts.values())==14100
assert build['inherited_same_SHA_scenarios']==14100
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['PASS independent Source weapon PC counter inverse: 2536 original methods,4 selected PC-counter/controlled-Earth/after-pair-binding calls,0 field/sibling changes','PASS399 inherited runtime no-init coverage plus4 explicit weapon PC counter owners','PASS385 PC weapon full material','original-casts=11, encrypted-memory-frames=123, original-pair-waits=10','PASS514 weapon full material','original-casts=8, encrypted-memory-frames=60, actual-Source-query-waits=12','PASS7 pair admission test-only']:assert marker in log,marker
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
for name in ['NativeSourceWeaponPlayerCounter.java', 'SourceWeaponPlayerCounterMethods.java', 'SourceWeaponPlayerCounterDelta.java', 'SourceWeaponPlayerCounterLoad.java', 'SourceWeaponPlayerFullPathPlan.java', 'SourceWeaponFullPathPlan.java', 'build_source_weapon_player_counter.py', 'reproduce_source_weapon_player_counter.py', 'verify_source_weapon_player_counter.py', 'checkpoint_source_weapon_player_counter.py', 'probe_source_weapon_player_counter.py', 'reproduce_source_weapon_player_gap.py', 'audit_source_weapon_next.py']:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-PC-weapon-counter-controlled-Earth-and-first-pair-durability-remaining-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['BATTLE material27 PC targets now honor the existing Source Counter Magic exception set at the original successful consume call, using the existing first Context peer and timed status. Unmarked/WORLD calls retain the complete original consume path.', 'Controlled NPC weapon27 probability now honors the fixed Source Earth Bind exception, matching both PC_PC and PC_NPC gates in L1MagicPc. The previous controlled helper and Earth fixture expectation were incorrect; original historical evidence stays frozen and is explicitly superseded by the new Source27-Earth-expectation-correction receipt. Corrected inherited controlled-branch checks rise from506 to514 because four Counter Magic frames now actually occur.', 'Both Binding.matches calls in the original NativeLightAction after-pair continuation now borrow the original weapon Context bindings and validate its first proof. Missing/replaced source or target status/life/role after the original pair wait is rejected before Binding.capture, original costs or durability mutation. Other skills and unmarked scopes keep original Binding.matches.', 'Across three original owners,2536 original methods independently invert after removing four reviewed call ports; no fields or sibling methods are added.385 new checks plus14100 revalidated inherited/corrected ordinary same-SHA checks and byte-identical frozen replay.11 actual native PC casts use a real equipped RuntimeInventory weapon and original deterministic damage roll, exact frozen Source L1Inventory.receiveDamage oracle, original nonpersistent economy, packet/encryption flow and source/target/Hub-unlocked memory writes.123 native memory frames and10 actual original target-action-lock waits cover dynamic counter exceptions, miss/Earth, resources/reuse, durability cap/observed over-cap, no weapon and stale status/role/lifetime/Main. The corrected controlled branch adds8 full casts,60 native frames and12 actual Source-query waits.', 'Real Source-equipped weapon overlay, original failure rollback and full accepted/final receiver paths, all remaining material skills and Source consumers/producers, formal0.6.71 isolated composition, same-SHA backend/Main/memory, Chinese MySQL equipped-character readonly validation and game cursor acceptance remain open. No deployment, formal overwrite, new baseline or Source/player SQL/schema/WAL writes.']
v['program_checks']={'JVM_inherited_no_initialization_checks': 399, 'additional_weapon_player_counter_owners_explicitly_verified': 4, 'original_methods_independently_inverted': 2536, 'original_changed_owners': 3, 'selected_old_call_sites': 4, 'new_original_fields': 0, 'new_runtime_class_entries': 1, 'pure_weapon_player_counter_contract_scenarios': 385, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 14100, 'previous_controlled_counter_checks': 506, 'corrected_controlled_counter_checks': 514, 'inherited_count_adjustment_explicitly_Source_backed': True, 'test_only_forced_tie_fixture_checks': 7, 'test_only_fixture_is_not_candidate_SHA': True, 'actual_original_PC_material_casts': 11, 'PC_original_encrypted_memory_frames': 123, 'actual_original_PC_pair_waits': 10, 'corrected_controlled_material_casts': 8, 'corrected_controlled_memory_frames': 60, 'actual_original_Source_query_waits': 12, 'independent_exact_Source_durability_oracle': True, 'existing_Source_counter_exception_set_retained': True, 'Source27_Earth_Bind_exception_both_PC_and_controlled': True, 'first_after_pair_binding_without_recapture_or_status_initialization': True, 'actual_socket_output_after_pair_and_Hub_release': True, 'original_Source_stats_query_outside_actor_monitor': True, 'strict_repository_proxy_no_SQL': True, 'GameStateStore_nonpersistent_and_no_state_file': True, 'manual_cooldown_tasks_no_threads_and_terminal_cleanup': True, 'candidate_direct_lock_inventory_methods': 57, 'new_global_actor_registry_TLS_queue_domain_or_worker': False, 'byte_identical_frozen_rebuild': True, 'new_SELECTs': 0, 'new_connection_ownership': False, 'full_Source_equipped_weapon_all_material_and_Source_qualified': False, 'bound_actual_server_game_or_Main_shutdown_cases': 0, 'full_producers_Source_backend_Main_memory_game_qualified': False}
v['historical_parent_Source_freshness']=parent_Source_freshness
gap=read(D/'source-weapon-counter-gap-current.json');verify(gap);verify(read(Path(gap['manifest']['path'])));v['counter_gap_reproduction']=gap['manifest']
next_weapon=read(D/'source-weapon-next-reference-current.json');verify(next_weapon);assert next_weapon['runtime']==latest['runtime'];verify(read(Path(next_weapon['manifest']['path'])));v['remaining_weapon_PC_reference_audit']=next_weapon['manifest']
player_gap=read(D/'source-weapon-player-gap-current.json');verify(player_gap);assert player_gap['runtime']==previous['runtime']
historical_gap=copy.deepcopy(read(Path(player_gap['manifest']['path'])));gap_input_aliases=[]
for ref in historical_gap['inputs']:
 if sha(Path(ref['path']))!=ref['sha256']:
  assert Path(ref['path'])==R/'reproduce_source_weapon_player_gap.py'
  frozen=next(x for x in old['editable_inputs']if Path(x['path']).name==Path(ref['path']).name)
  assert frozen['sha256']==ref['sha256'];verify(frozen)
  gap_input_aliases.append(dict(original=copy.deepcopy(ref),frozen=frozen,meaning='Historical mutable replay-script reference is verified through its byte-identical previous checkpoint copy; original receipt is not rewritten or blessed.'))
  ref.update(frozen)
verify(historical_gap);v['historical_weapon_PC_gap_input_aliases']=gap_input_aliases
v['historical_weapon_PC_dynamic_exception_failure']=player_gap['manifest'];v.pop('remaining_weapon_PC_dynamic_exception_failure',None)
wait_gap=read(D/'source-weapon-player-wait-gap-current.json');verify(wait_gap);verify(read(Path(wait_gap['manifest']['path'])));v['historical_weapon_PC_missing_status_wait_failure']=wait_gap['manifest']
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_weapon_player_counter_contract','Source_weapon_durability_oracle','Source_weapon_Earth_expectation_correction','weapon_player_counter_port_census','Source_weapon_counter_magic_contract','Source_weapon_counter_magic_oracle','weapon_counter_magic_port_census','Source_weapon_target_life_contract','weapon_target_life_port_census','Source_target_negative_reserve_contract','target_negative_reserve_port_census','Source_target_negative_life_contract','target_negative_life_port_census','Source_proc_scalar_extras_contract','proc_scalar_extras_port_census','Source_proc68_first_queue_contract','proc68_first_queue_port_census','Source_proc_scalar_skills_contract','Source_proc_scalar_icon_oracle','proc_scalar_skills_port_census','Source_proc_timed_refresh_contract','Source_proc_timed_refresh_oracle','proc_timed_refresh_port_census','Source_target_icon_batch_contract','target_icon_batch_port_census','Source_icons1_output_contract','icons1_output_port_census','Source_icons1_epoch_contract','Source_icons1_epoch_fields','icons1_epoch_port_census','Source_icons1_brave_origin_contract','icons1_brave_origin_port_census','Source_icons1_inputs_contract','icons1_inputs_port_census','Source_icons1_contract','Source_icons1_oracle','icons1_port_census','Source_proc68_icon_contract','Source_proc68_icon_oracle','proc68_icon_port_census','Source_proc68_graphics_contract','Source_proc68_graphics_oracle','proc68_graphics_port_census','Source_context_role_frame_contract','context_role_frame_port_census','Source_batch_role_frame_contract','batch_role_frame_port_census','Source_batch_terminal_contract','batch_terminal_port_census','Source_controlled_role_frame_contract','controlled_role_frame_port_census','Source_controlled_audience_contract','controlled_audience_port_census','Source_poison_audience_contract','poison_audience_port_census','Source_element_audience_contract','element_audience_port_census','Source_element_transport_contract','element_transport_port_census','Source_physical_transport_contract','physical_transport_port_census','Source_physical_audience_contract','physical_audience_port_census','Source_raw_audience_contract','raw_audience_port_census','Source_equipment_final_output_contract','equipment_final_output_port_census','Source_barrier_targets_contract','Source_barrier_target_oracle','Source_barrier_skill_ids','barrier_skill_matrix','barrier_targets_port_census','Source_skill_defaults_contract','Source_skill_config_oracle','skill_defaults_port_census','Source_doll_defense_audience_contract','doll_defense_audience_port_census','Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_material_27_PC_dynamic_counter_exception_wired=True,Source_material_27_controlled_Earth_bind_gate_corrected=True,Source_material_27_after_pair_first_binding_wired=True,Source_material_27_controlled_counter_magic_wired=True,Source_material_27_first_target_context_wired=True,Source_material_173_174_first_peer_receipt_reserve_wired=True,Source_material_173_174_first_context_life_wired=True,Source_eight_custom_PC_scalar_Proc_paths_wired=True,Source_custom68_first_status_queue_wired=True,Source_four_custom_PC_scalar_Proc_paths_wired=True,Source_custom68_timer_refresh_and_actual_ability_guard_wired=True,Source_target_icon_batch_wired=True,Source_icons1_scalar_publication_versions_wired=True,Source_icons1_producer_receiver_output_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'paired-lifecycle':next(group['work']for group in old['remaining_work_groups']if group['id']=='paired-lifecycle')+' Current PC branch additionally passes10 actual target action-lock waits; both original after-pair binding checks borrow the first proof and never recapture missing status. Actual backend/Main/memory and final receiver qualification remain open.', 'materials':next(group['work']for group in old['remaining_work_groups']if group['id']=='materials')+' Superseding current evidence: PC dynamic counter exception and controlled Earth27 gate are corrected; the historical PC counter failure is now fixed.11 PC native casts,123 native frames,10 original pair waits and exact Source durability cover the runtime equipped weapon branch. Corrected controlled checks are514 with60 frames, explicitly superseding the incorrect old Earth expectation. Source-equipped weapon overlay, rollback/final receiver and other materials remain open.'}
for group in v['remaining_work_groups']:
 if group['id']in updates:group['work']=updates[group['id']]
v['remaining']=[x['work']for x in v['remaining_work_groups']];v['historical_remaining_preserved_in']=previous['manifest']
v['inherited_test_coverage_adjustments']=copy.deepcopy(old.get('inherited_test_coverage_adjustments',[]))+[build['Source_weapon_Earth_expectation_correction']];assert len(v['remaining_work_groups'])==12
for key in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[key]is False,key
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),system_goal=v['goal_reset_receipt'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
text=work.read_text('utf-8');text=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{v['runtime']['sha256']}`",text)
text=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",text)
text=re.sub(r'\d+個特定HP消費端直接呼叫點',f"{inv['observations']['current_direct_HP_call_sites']}個特定HP消費端直接呼叫點",text)
text=text.replace('更新：2026-10-25。','更新：2026-10-27。')
artifact_paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']}
text=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:artifact_paths[m[1]],text)
a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
block='## 本次已更新的程式\n\n材料27的PC目標沿原成功魔屏消耗點套用既有Source外控例外。受控NPC依固定Source修正大地屏障例外：PC_PC與PC_NPC都允許壞物術27通過。前一批受控NPC的程式與Earth測試預期有誤；保留歷史證據，另以差分收據明確更正，不能沿用錯誤預期。\n\n原NativeLightAction成對鎖定等待後的兩個Binding.matches呼叫，改借用原武器Context與first proof；status／life／角色過期時，不再進入Binding.capture，不補建狀態或扣除資源。其他技能、WORLD與unmarked後備仍用原matches。\n\n11次完整PC施法使用真RuntimeInventory裝備武器、原耐久亂數與固定Source receiveDamage獨立oracle，檢查耐久上限／已觀測超上限、無武器、魔屏／例外、未命中／Earth、資源／冷卻；123個原生加密記憶體封包，實際socket寫入均在原source／target action locks與Hub鎖釋放後。10次原target action-lock等待確認Main、STOPPED、角色／status／life／缺失壽命均不扣料／MP、不傷舊武器、不送包。\n\n三個原owner、2536方法inverse、4呼叫點、0欄位變更、1無欄位helper；399繼承no-init＋4 explicit owners。385新增＋14100繼承／更正同SHA檢查與同位元重建通過。受控分支更正後為514檢查（原506，額外4個魔屏封包產生8個封包完整性檢查）、8次完整施法／60個封包／12次Source查詢等待。20 SQL reader bytes、57 direct-lock inventory與7個獨立測試SHA強制tie維持。\n\nSource來源武器耐久覆蓋、完整回滾／接受／最終接收端、其餘材料／外控／傷害生產端、正式隔離合成、中文MySQL、後端／Main／memory與實機仍待完成；未部署、未建立新基準或寫入Source／玩家資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-27 material27 PC counter／controlled Earth／first pair：候選`{v['runtime']['sha256']}`；2536原方法inverse、4原呼叫點、385新增＋14100繼承／更正同SHA及同位元重建。11次PC原施法／123記憶體封包／10原pair waits；受控Earth錯誤預期已明確更正，514檢查／8施法／60封包／12查詢等待。Source來源武器覆蓋、回滾／最終接收端及其餘Source／後端／MySQL／實機未完成，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-PC-weapon-counter-controlled-Earth-first-pair-durability',full_goal_achieved=False)
continuation['source_weapon_player_counter_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
