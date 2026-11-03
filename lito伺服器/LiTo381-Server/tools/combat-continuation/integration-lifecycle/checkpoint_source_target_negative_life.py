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
latest=read(D/'source-target-negative-life-build-current.json');repro=read(D/'source-target-negative-life-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_target_negative_life_contract_scenarios']==800
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
inherited_counts={k:v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_target_negative_life_contract_scenarios'}
assert inherited_counts==parent_counts and sum(inherited_counts.values())==11780
assert build['inherited_same_SHA_scenarios']==11780
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['PASS independent target-negative life inverse: 24 original methods,3 original method ports,1 bounded Context field','PASS394 inherited runtime no-init coverage plus4 explicit target-negative life owners','PASS800 target-negative material first-life actual-original-monitor','original-waits=90, native-validation-boundaries=12, original-casts=16','PASS7 pair admission test-only']:assert marker in log,marker
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
for name in ['NativeSourceTargetNegativeLife.java', 'SourceTargetNegativeLifeMethods.java', 'SourceTargetNegativeLifeDelta.java', 'SourceTargetNegativeLifeLoad.java', 'SourceTargetNegativeLifePlan.java', 'build_source_target_negative_life.py', 'reproduce_source_target_negative_life.py', 'verify_source_target_negative_life.py', 'checkpoint_source_target_negative_life.py']:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-material-173-174-first-context-life-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Material173/174 captures its existing OPEN native lifetime and original discovered caster, peer/master, actor, timed containers and life/generation into one bounded Context proof before original pair selection/receipt work. Source target=none request is projected through the existing Source adapter before selection; pointed/Ctrl packet identity is retained. Original costs, RNG, immunity, competition, native status/HP and receipt bodies remain.', 'The actual controlled-negative continuation borrows that proof before original Hub/actor/timed/admission monitor waits, then uses the first timed container and lifetime instead of a late creating lifetime lookup. Original Context.valid now rejects stale first metadata before its original scene/target validation. Original four monitor spans/handlers and native body remain; original cast normal/exception cleanup clears the bounded proof. WORLD preserves original lookups and control flow.', '24 original methods independently invert across two original owners;3 original method ports,1 transient volatile Context field,2 new runtime class entries.394 inherited no-init plus4 explicit owners;800 new plus11780 inherited same-SHA checks, frozen byte-identical replay,20 original SQL reader entries and57 direct lock methods.90 actual original continuation waits and12 native validation boundaries cover both skills across Hub/actor/timed waits;16 actual original casts cover self/cross PC/controlled target, target-none projection and original normal/exception receipt cleanup. Two detached natural NPC variants check first status/epoch/generation metadata.', 'Full spell validation, material consumption, probability/status/output behavior, native peer receipt reservation and other creating lifetime lookups remain subject to the full lifecycle audit. A separate original receipt reserve still obtains its lifetime via NativePlayerLifetime.of before registration; this stage retains that body and does not claim this race qualified. Full Source/default SkillUse, all materials/controls, backend/Main/memory, Chinese MySQL and cursor acceptance remain open. No deployment, formal overwrite, baseline or Source/player SQL/schema/WAL writes.']
v['program_checks']={'JVM_inherited_no_initialization_checks': 394, 'additional_target_negative_life_owners_explicitly_verified': 4, 'original_methods_independently_inverted': 24, 'original_changed_owners': 2, 'original_method_ports': 3, 'new_original_fields': 1, 'new_field_is_bounded_transient_volatile_Context_proof': True, 'new_runtime_class_entries': 2, 'pure_target_negative_life_contract_scenarios': 800, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 11780, 'test_only_forced_tie_fixture_checks': 7, 'test_only_fixture_is_not_candidate_SHA': True, 'actual_original_continuation_monitor_waits': 90, 'actual_original_native_validation_boundaries': 12, 'actual_original_casts': 16, 'original_cast_exception_and_normal_receipt_cleanup': True, 'missing_waited_status_or_lifetime_never_initialized': True, 'Source_target_none_before_pair_selection': True, 'pointed_Ctrl_packet_identity_retained': True, 'candidate_direct_lock_inventory_methods': 57, 'new_global_actor_registry_TLS_queue_domain_or_worker': False, 'byte_identical_frozen_rebuild': True, 'new_SELECTs': 0, 'new_connection_ownership': False, 'full_material_cost_RNG_status_IO_and_receipt_reserve_qualified': False, 'bound_actual_server_game_or_Main_shutdown_cases': 0, 'full_producers_Source_backend_Main_memory_game_qualified': False}
v['historical_parent_Source_freshness']=parent_Source_freshness
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_target_negative_life_contract','target_negative_life_port_census','Source_proc_scalar_extras_contract','proc_scalar_extras_port_census','Source_proc68_first_queue_contract','proc68_first_queue_port_census','Source_proc_scalar_skills_contract','Source_proc_scalar_icon_oracle','proc_scalar_skills_port_census','Source_proc_timed_refresh_contract','Source_proc_timed_refresh_oracle','proc_timed_refresh_port_census','Source_target_icon_batch_contract','target_icon_batch_port_census','Source_icons1_output_contract','icons1_output_port_census','Source_icons1_epoch_contract','Source_icons1_epoch_fields','icons1_epoch_port_census','Source_icons1_brave_origin_contract','icons1_brave_origin_port_census','Source_icons1_inputs_contract','icons1_inputs_port_census','Source_icons1_contract','Source_icons1_oracle','icons1_port_census','Source_proc68_icon_contract','Source_proc68_icon_oracle','proc68_icon_port_census','Source_proc68_graphics_contract','Source_proc68_graphics_oracle','proc68_graphics_port_census','Source_context_role_frame_contract','context_role_frame_port_census','Source_batch_role_frame_contract','batch_role_frame_port_census','Source_batch_terminal_contract','batch_terminal_port_census','Source_controlled_role_frame_contract','controlled_role_frame_port_census','Source_controlled_audience_contract','controlled_audience_port_census','Source_poison_audience_contract','poison_audience_port_census','Source_element_audience_contract','element_audience_port_census','Source_element_transport_contract','element_transport_port_census','Source_physical_transport_contract','physical_transport_port_census','Source_physical_audience_contract','physical_audience_port_census','Source_raw_audience_contract','raw_audience_port_census','Source_equipment_final_output_contract','equipment_final_output_port_census','Source_barrier_targets_contract','Source_barrier_target_oracle','Source_barrier_skill_ids','barrier_skill_matrix','barrier_targets_port_census','Source_skill_defaults_contract','Source_skill_config_oracle','skill_defaults_port_census','Source_doll_defense_audience_contract','doll_defense_audience_port_census','Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_material_173_174_first_context_life_wired=True,Source_eight_custom_PC_scalar_Proc_paths_wired=True,Source_custom68_first_status_queue_wired=True,Source_four_custom_PC_scalar_Proc_paths_wired=True,Source_custom68_timer_refresh_and_actual_ability_guard_wired=True,Source_target_icon_batch_wired=True,Source_icons1_scalar_publication_versions_wired=True,Source_icons1_producer_receiver_output_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'paired-lifecycle':next(group['work']for group in old['remaining_work_groups']if group['id']=='paired-lifecycle')+' Material173/174 now keeps first Context lifetime/status and Source pre-pair target-none projection through90 actual Hub/actor/timed waits and16 original cast normal/exception paths. Original receipt reserve still uses a creating lifetime lookup; reservation waits and complete material cost/status/output qualification remain open.'}
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
block='## 本次已更新的程式\n\n材料173／174在原Context探索後、原成對選擇前，保留既有OPEN lifetime、來源／目標或主人、actor及第一次timed status／role／life／generation；Source target=none沿用既有投影器先轉成原施法者，其他pointed／Ctrl封包保持原物件。\n\n受控NPC原續行在Hub／actor／timed／admission等待後，使用第一次status與lifetime，拒絕缺失或替換資料，不走較晚建立式lifetime查找。原Context.valid、四個monitor spans、原成本／機率／狀態處理與receipt body均保留；原施法正常／例外收尾釋放Context proof。WORLD仍走原路徑。\n\n24原方法inverse、3個原方法接線、1個transient volatile Context欄位、2個新增class。800新增＋11780同SHA繼承檢查與同位元重建、394繼承no-init＋4 explicit owner通過。90次實際原續行等待、12原生驗證邊界、16原始施法入口，涵蓋self／cross PC／controlled、target-none、正常／例外收尾。20 SQL reader bytes、57 direct lock inventory及7個獨立測試SHA強制tie維持。\n\n完整成本／RNG／狀態／IO、原peer receipt reservation的建立式lifetime查找與其餘生命周期仍待逐路徑補齊；未以本批monitor／入口測試宣告完整技能完成。其餘Source、材料／外控、中文MySQL、同SHA後端／Main／memory與實機仍待完成；未部署或寫入資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-27 material173/174 first Context life：候選`{v['runtime']['sha256']}`，24原方法inverse、3原方法接線、800新增＋11780繼承、90實際續行waits／16原cast及同位元重建通過。原成本／RNG／狀態／IO、peer reserve與完整Source生命週期仍未完成，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-material-173-174-first-context-life',full_goal_achieved=False)
continuation['source_target_negative_life_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
