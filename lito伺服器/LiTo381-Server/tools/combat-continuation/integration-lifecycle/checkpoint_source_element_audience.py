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
latest=read(D/'source-element-audience-build-current.json');repro=read(D/'source-element-audience-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_element_audience_contract_scenarios']==147
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
inherited_counts={k:v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_element_audience_contract_scenarios'}
assert inherited_counts==parent_counts and sum(inherited_counts.values())==6176
assert build['inherited_same_SHA_scenarios']==6176
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 118 original methods,2 first rooms,2 first recipients,4 immutable list copies,2 receiver prefixes','PASS359 candidate no-init classes with first cube/poison HP audiences','PASS147 element first-audience original-ctor/final-cipher','PASS7 pair admission test-only','original-ctors=8, memory-frames=8, final-receiver-waits=20, first-discovery-waits=4']:assert marker in log,marker
with zipfile.ZipFile(build['base']['path'])as parent,zipfile.ZipFile(latest['runtime']['path'])as candidate:
 for entry in ['local/lito381/server/'+name+'.class'for name in ['NativeSourcePhysicalNpc$Commit','NativeSourcePhysicalNpc$Output','NativeSourcePhysicalAudience','NativeSourcePhysicalAudience$Audience','NativeSourcePhysicalAudience$Receiver','NativeAreaMaterialAction','NativeSourceRawDamage','NativeSourceRawDamage$Output','NativeSourceRawAudience','NativeSourceRawAudience$Proof','NativeSourceFirstPlayers','NativeSourceFirstPlayers$Snapshot','NativeSourceDollAudience','ClientSession','NativeSourceEquipmentOutput','NativeSourceEquipmentOutput$Viewer','NativeSourceEquipmentFinalOutput','NativeSourceEquipmentFinalOutput$First','NativeSourceDollDefense$Snapshot','NativeSourceDollDefense$Proof','NativeSourceDollPassive','NativeSourceDollPassive$Choice','NativePlayerCommit','NativePlayerCommit$DefenseVisual','NativeSourceDollAudience','NativeSourceDollDetachedOutput','NativeSourceEquipmentProcLife','NativeSourceEquipmentOutput$Detached','NativeSourceEquipmentOutput$Scope','NativeSourceEquipmentOutput$Link','NativeSourceEquipmentOutput$Output','NativeProcHealingPlayer','NativeProcHealingPlayer$Target','NativeLightAction']]:
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
for name in ['NativeSourceElementAudience.java','SourceElementAudienceMethods.java','SourceElementAudienceDelta.java','SourceElementAudienceLoad.java','SourceElementAudiencePlan.java','build_source_element_audience.py','reproduce_source_element_audience.py','verify_source_element_audience.py','checkpoint_source_element_transport.py','NativeSourceElementTransport.java','SourceElementTransportMethods.java','SourceElementTransportDelta.java','SourceElementTransportLoad.java','SourceElementTransportPlan.java','build_source_element_transport.py','reproduce_source_element_transport.py','verify_source_element_transport.py','checkpoint_source_physical_transport.py','SourcePhysicalTransportMethods.java','SourcePhysicalTransportDelta.java','SourcePhysicalTransportLoad.java','SourcePhysicalTransportPlan.java','build_source_physical_transport.py','reproduce_source_physical_transport.py','verify_source_physical_transport.py','NativeSourcePhysicalAudience.java','SourcePhysicalAudienceMethods.java','SourcePhysicalAudienceDelta.java','SourcePhysicalAudienceLoad.java','SourcePhysicalAudiencePlan.java','build_source_physical_audience.py','reproduce_source_physical_audience.py','verify_source_physical_audience.py','NativeSourceRawAudience.java','SourceRawAudienceMethods.java','SourceRawAudienceDelta.java','SourceRawAudienceLoad.java','SourceRawAudiencePlan.java','build_source_raw_audience.py','reproduce_source_raw_audience.py','verify_source_raw_audience.py','NativeSourceEquipmentFinalOutput.java','SourceEquipmentFinalOutputMethods.java','SourceEquipmentFinalOutputDelta.java','SourceEquipmentFinalOutputLoad.java','SourceEquipmentFinalOutputPlan.java','build_source_equipment_final_output.py','reproduce_source_equipment_final_output.py','verify_source_equipment_final_output.py','NativeSourceBarrierTargets.java','SourceBarrierTargetsMethods.java','SourceBarrierTargetsDelta.java','SourceBarrierTargetsLoad.java','SourceBarrierTargetsPlan.java','build_source_barrier_targets.py','reproduce_source_barrier_targets.py','verify_source_barrier_targets.py','NativeSourceSkillDefaults.java','SourceSkillDefaultsMethods.java','SourceSkillDefaultsDelta.java','SourceSkillDefaultsLoad.java','SourceSkillDefaultsPlan.java','build_source_skill_defaults.py','reproduce_source_skill_defaults.py','verify_source_skill_defaults.py','NativeSourceDollDefenseAudience.java','SourceDollDefenseAudienceMethods.java','SourceDollDefenseAudienceDelta.java','SourceDollDefenseAudienceLoad.java','SourceDollDefenseAudiencePlan.java','build_source_doll_defense_audience.py','reproduce_source_doll_defense_audience.py','verify_source_doll_defense_audience.py','NativeSourceDollAudience.java','SourceDollAudienceMethods.java','SourceDollAudienceDelta.java','SourceDollAudienceLoad.java','SourceDollAudiencePlan.java','build_source_doll_audience.py','reproduce_source_doll_audience.py','verify_source_doll_audience.py','NativeSourceDollDetachedOutput.java','SourceDollDetachedOutputMethods.java','SourceDollDetachedOutputDelta.java','SourceDollDetachedOutputLoad.java','SourceDollDetachedOutputPlan.java','build_source_doll_detached_output.py','reproduce_source_doll_detached_output.py','verify_source_doll_detached_output.py','NativeSourceManualScope.java','SourceManualActionScopeMethods.java','SourceManualActionScopeDelta.java','SourceManualActionScopeLoad.java','SourceManualActionScopePlan.java','SourceManualActionLockCensus.java','build_source_manual_action_scope.py','reproduce_source_manual_action_scope.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_manual_action_scope.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-cube-poison-HP-original-first-audience-role-life-observer-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Two original cube/poison HP Commit constructors use exact room/recipient/list-copy ports. Immutable first audiences retain existing list content/order and original schema; recipients reuse prior receiver metadata. Original Hub/status lock domains capture first mode/life before binding. Only living receivers may initialize their existing canonical token at first discovery; nullable observers never acquire an actor token.', 'Two original shared receiver prefixes compare weak original status identity and first binding/life/mode before the complete original guard. Original HP/cost/RNG/admission/FIFO/accepted seal/reaction/HPBAR/packet/Task/Batch/monitor/cipher/cleanup methods remain inverse-identical. No new original owner fields/methods or TLS/registry/queue/workers. Original Commit lists already retain receiver PCs; no zero-retention or full memory claim.', '118 original method inverses,2 first rooms,2 first recipients,4 list copies,2 receiver prefixes and359 no-init.147 new plus6176 inherited same-SHA checks;7 forced-tie checks belong to separate test-only jar.8 original Commit constructors,8 encrypted frames,20 real final receiver-monitor waits and4 original-constructor first-discovery waits across both natural kinds and both HP producers. Current late binding after actual scene ABA cannot override first identity. Frozen replay byte-identical and20 SQL reader classes unchanged.', 'Poison status first-viewer role/life/observer remains open, as do other controlled/bot/NPC paths, full submit/HP/whole accepted continuation, normal role/memory/Main/Source/backend/MySQL/game acceptance and other material/proc/control semantics. No deployment, baseline, formal overwrite or Source/player SQL/schema/WAL writes.']
v['program_checks']={'JVM_no_initialization_classes': 359, 'original_methods_independently_inverted': 118, 'original_first_room_ports': 2, 'original_first_recipient_ports': 2, 'original_immutable_list_copy_ports': 4, 'original_shared_receiver_prefixes': 2, 'existing_changed_class_entries': 4, 'new_runtime_classes': 3, 'new_original_owner_fields_methods': 0, 'pure_element_audience_contract_scenarios': 147, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 6176, 'test_only_forced_tie_fixture_checks': 7, 'test_only_fixture_is_not_candidate_SHA': True, 'natural_NPC_kinds': 2, 'HP_producers': 2, 'actual_original_Commit_ctors': 8, 'original_encrypted_memory_frames': 8, 'actual_original_final_receiver_monitor_waits': 20, 'actual_original_ctor_first_discovery_waits': 4, 'living_canonical_life_initializes_at_first_discovery_only': True, 'nullable_observer_gains_no_actor_life': True, 'first_weak_status_identity_checked': True, 'later_original_binding_cannot_override_first_role': True, 'original_Hub_status_lock_domains_only': True, 'original_owner_schema_HP_cost_RNG_queue_publication_final_body_retained': True, 'poison_status_first_viewer_life_qualified': False, 'new_registry_TLS_queue_workers_SQL_HP_RNG_or_lifetime_authority': False, 'byte_identical_frozen_rebuild': True, 'new_SELECTs': 0, 'new_connection_ownership': False, 'bound_actual_server_game_or_Main_shutdown_cases': 0, 'full_producers_Source_backend_Main_memory_game_qualified': False}
v['historical_parent_Source_freshness']=parent_Source_freshness
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_element_audience_contract','element_audience_port_census','Source_element_transport_contract','element_transport_port_census','Source_physical_transport_contract','physical_transport_port_census','Source_physical_audience_contract','physical_audience_port_census','Source_raw_audience_contract','raw_audience_port_census','Source_equipment_final_output_contract','equipment_final_output_port_census','Source_barrier_targets_contract','Source_barrier_target_oracle','Source_barrier_skill_ids','barrier_skill_matrix','barrier_targets_port_census','Source_skill_defaults_contract','Source_skill_config_oracle','skill_defaults_port_census','Source_doll_defense_audience_contract','doll_defense_audience_port_census','Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_cube_poison_HP_first_audience_role_life_observer_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'hp-producers': 'Cube/poison HP first audience now freezes weak status identity, binding/life/mode at original first room/recipient discovery under existing Hub/status lock domains. Living canonical token initializes only at first capture; nullable observer gains no token. Original late binding loops and receiver bodies retained;8 original constructors/8 memory frames/20 final waits/4 discovery waits pass. Poison status first viewer still open, along with other NPCs, full submit/HP/whole accepted continuation/Source/Main/memory/backend/game and other material/proc/control semantics.'}
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
block='## 本次已更新的程式\n\n立方體／毒傷HP的兩個原Commit constructor，在原roomMembers／battleRecipients首次探索時固定binding／life／observer及weak狀態容器身分。recipient沿用已存在的first member；活人只在首次探索初始化既有canonical token，nullable觀眾不建立actor life。原名單內容／順序／不可變性與owner schema保留，未加owner field／method／TLS／registry／queue／worker。\n\n兩個shared receiver只加first-proof prefix，再執行完整原guard。118原方法inverse、2first room／2first recipient／4list-copy／2receiver prefix、359 no-init、147新＋6176同SHA繼承與同位元重建通過。兩種NPC及兩個HP生產端共8原constructor、8加密memory frame、20最終接收鎖等待、4首次探索等待；後續原binding在實際scene ABA後雖仍current，也不能覆蓋first身分。另7強制tie非候選SHA，20SQL reader保留。\n\n毒色status首次觀眾生命仍待補齊；其他NPC／完整生產端／Source／Main／memory／後端／MySQL／實機與其他技能／外控仍未完成。未部署或寫入資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 cube/poison HP first audience：候選`{v['runtime']['sha256']}`，原首次名單固定weak status／binding／life／observer，兩個shared receiver核對first proof；原schema與HP／cost／RNG／queue／final body保留。118原方法inverse、359 no-init、147新＋6176同SHA繼承、8constructor／8加密IO／20final等待／4discovery等待與同位元重建通過。status first viewer與完整Source／Main／memory／MySQL／實機仍開放；未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-cube-poison-HP-first-audience-role-life-observer-program-only',full_goal_achieved=False)
continuation['source_element_audience_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
