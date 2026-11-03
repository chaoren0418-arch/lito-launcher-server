"""Checkpoint original NPC first-snapshot authority through initial escape lookup; full Source remains open."""
from pathlib import Path
from datetime import datetime, timezone
import json, hashlib, shutil, zipfile, re, subprocess, copy, csv

R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2]
read=lambda p:json.loads(p.read_text('utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:assert sha(Path(v['path']))==v['sha256'],v['path']
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)

index=D/'source-program-update-progress-current.json';proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';C=R.parent/'continuation-current.json'
previous=read(index);verify(previous);old=read(Path(previous['manifest']['path']));verify(old)
proof=read(proofpath);audit=read(auditpath)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
latest=read(D/'source-npc-first-view-build-current.json');repro=read(D/'source-npc-first-view-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_NPC_first_view_contract_scenarios']==48 and build['actual_detached_original_NPC_first_view_handoffs']==24
assert build['actual_stale_first_view_rejections']==22
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_NPC_first_view_contract_scenarios')==2468
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 1543 original methods'in log and 'PASS322'in log
assert subprocess.run(['powershell','-NoProfile','-Command',"@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"],capture_output=True,text=True,check=True).stdout.strip()=='0'
anchors={p:sha(p)for p in [R/'current.json',D/'source-kiringku-progress-current.json',D/'competition-only-product-scope-current.json',D/'source-equipment-mysql-reload-readonly-origin-current.json',D/'source-raw-producer-build-current.json',D/'source-arena-poison-build-current.json']}
with zipfile.ZipFile(old['readonly_SQL_consumers_byte_retained']['base']['path'])as sql,zipfile.ZipFile(latest['runtime']['path'])as jar:
 entries=old['readonly_SQL_consumers_byte_retained']['entries'];assert len(entries)==20
 for name in entries:assert sql.read(name)==jar.read(name),name

run=D/'source-completion-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'editable-input-snapshot';inputs.mkdir(parents=True)
current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';work=S/'docs/maintenance/source-completion-work-current.zh-TW.md'
old_index_pin=pin(index)
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md'),(work,'previous-work.md')]:shutil.copyfile(p,run/name)
frozen_index=pin(run/'previous-program-progress.json')
for ref in old['editable_inputs']:
 name=Path(ref['path']).name;shutil.copyfile(R/name,inputs/name)
for name in ['NativeSourceNpcFirstView.java','SourceNpcFirstViewMethods.java','SourceNpcFirstViewDelta.java','SourceNpcFirstViewLoad.java','SourceNpcFirstViewPlan.java','build_source_npc_first_view.py','reproduce_source_npc_first_view.py','audit_source_completion.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-NPC-first-snapshot-weak-authority-initial-escape-lookup-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Two natural actor snapshot entries capture first weak actor identity, original life/motion/static stamp and pose before original field reads. Original synchronized list emissions attach weak encounter identity/generation without refreshing the captured actor facts. Two final/transient Snapshot fields and two exact delegating constructors preserve canonical components/equality/hash/standard serialization and the prior factory-memory carrier; no strong actor/owner/Context/Frame/global registry/TLS/queue/worker/SQL ownership.', 'Two original PVE loop call operands carry their same original Snapshot through complete original Hub append and escape-dispatch siblings. The first original escape actor lookup under the existing encounter monitor compares that exact view/weak actor/life/stamp/motion/pose/NPC/force/owner generation before mutation/RNG, using the original handled-return monitor cleanup on rejection. Memory restore ignores the old first view and creates a fresh live authority; old constructors/missing original lifecycle state remain explicit unqualified fallback. Other AI and PC target/final IO paths are not qualified by this handoff.', '1543 original methods plus2 complete original siblings and2 exact delegating Snapshot constructors independently inverse-retained;322 JVM no-init classes.48 new pure contracts plus2468 inherited/revalidated same-SHA checks,24 actual detached handoffs with22 stale/life/same-id/pose/motion/stamp/generation/weak/foreign/unowned/malformed rejections before schedule/output/RNG. Actual snapshot emission, both original memory restores, old ctor identity/schema, weak references and missing-lifecycle fallback checked. Frozen replay byte-identical;86 Source files/133 prior bounded references plus explicit first-view contract;20 SQL consumer classes retained.', 'Guard consumption currently covers the Source Monster escape PVE handoff. Extend original first-view authority through normal/magic/summon/controlled/bot/guardian AI and accepted/final packet paths, all PC target discovery and scene/source-retirement/Main lifetimes. Complete persistent hate/target-clear/reacquisition/subclass/custom-work/spawn/presentation, full58 materials/252 controls/other producers/formal and actual same-SHA backend/read-only Chinese MySQL/game acceptance. Old canonical snapshots are unqualified fallback, not completed lifecycle cases.']
v['program_checks']={'JVM_no_initialization_classes': 322, 'original_methods_independently_inverted': 1543, 'complete_original_siblings_independently_inverted': 2, 'exact_delegating_snapshot_constructors': 2, 'original_instruction_and_control_flow_inverse_ignores_recomputed_stack_map_frames': True, 'forward_frozen_splice_and_JVM_stack_maps_verified': True, 'byte_identical_frozen_rebuild': True, 'existing_changed_class_entries': 8, 'new_helper_and_immutable_weak_view_class_entries': 2, 'new_original_final_transient_snapshot_fields': 2, 'new_canonical_Snapshot_components': 0, 'pure_NPC_first_view_contract_scenarios': 48, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 2468, 'actual_detached_initial_snapshot_handoffs': 24, 'actual_stale_foreign_weak_life_pose_generation_rejections': 22, 'original_snapshot_entry_captures_before_field_reads': 2, 'original_PVE_loop_snapshot_transport_ports': 2, 'original_locked_list_owner_generation_wired': True, 'original_first_escape_lookup_consumes_original_view': True, 'original_monitor_cleanup_and_no_RNG_on_rejection': True, 'memory_restore_creates_fresh_view_never_rebases_old_authority': True, 'old_ctor_schema_identity_and_missing_lifecycle_fallback_retained': True, 'first_view_guard_scope': 'Source Monster escape PVE handoff only', 'other_AI_PC_target_final_IO_and_full_Main_lifecycle_qualified': False, 'new_strong_actor_owner_Context_Frame_registry_or_TLS': False, 'new_SELECTs': 0, 'new_workers_TLS_registries': False, 'new_connection_ownership': False, 'new_scheduled_tasks': False, 'other_original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained': True, 'bound_selection_cost_RNG_HP_IO_role_Main_cases': 0}
for key in ['Source_NPC_first_view_contract','npc_first_view_port_census','Source_NPC_escape_contract','Source_NPC_escape_oracle','npc_escape_port_census','Source_NPC_motion_contract','Source_NPC_motion_oracle','npc_motion_port_census','Source_NPC_movement_contract','Source_NPC_movement_oracle','npc_movement_port_census','Source_NPC_spell_timing_contract','Source_NPC_spell_timing_oracle','npc_spell_timing_port_census','Source_NPC_base_speed_contract','Source_NPC_base_speed_oracle','npc_base_speed_port_census','Source_NPC_action_interval_contract','Source_NPC_action_interval_oracle','npc_action_interval_port_census','Source_slow_failure_metadata_contract','slow_failure_metadata_port_census','Source_remaining_area_lists_contract','remaining_area_list_port_census','Source_sleep_construction_contract','sleep_construction_port_census','Source_area_caster_membership_contract','area_caster_target_port_census','target_construction_port_census','Source_target_construction_oracle','Source_target_construction_contract','target_mask_port_census','Source_target_mask_oracle','Source_target_mask_contract','doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['npc_first_view_'+key]=build[key]
v.update(Source_NPC_original_first_snapshot_authority_initial_escape_lookup_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
v['skills_SQL_id_distinct_from_skill_id_no_extra_identity_gate']=True
v['goal_reset_receipt']=pin(D/'source-completion-goal-reset-current.json');verify(v['goal_reset_receipt'])
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
remaining_updates={'paired-lifecycle': 'Source Monster escape now carries first weak snapshot actor/life/stamp/motion/pose/owner generation through initial lookup, in addition to existing within-footprint rechecks and typed pair/scope/light-life ports. Extend original snapshot authority through remaining normal/magic/summon/controlled/bot/guardian and accepted/final output, all PC target discovery and earlier waits without rebase/blanket rejection/caster dependence. Complete manual lock holders/no-parent scopes, identity-hash tie paths, role/death/revival/same-id/scene/owner/retirement, bounded memory and actual Main/DRAINING/STOPPED. Legacy null views are unqualified fallback; six historical aliases remain separate.', 'npc-factory-ai': 'Source Monster escape initial PVE snapshot handoff now uses weak first actor/life/motion/proof/pose and locked owner generation; old memory restore creates fresh authority. Normal/magic/summon and other controlled/bot/guardian/32-class consumers still need original authority through mutation/output. Complete persistent target/hate clear/reacquisition, subclass/De/custom-work/spawn/full trigger/cast scheduling, first snapshot/reload/rollback/full attributes, unknown/manual/master origins, family/reward and owner-meter. No bound AI/scene/Main acceptance yet.', 'presentation': 'First weak NPC view is transported through Source Monster escape handoff only. Extend it to normal/physical/magic/summon/controlled/bot/guardian publications and all accepted/final life/scene/role/Main/IO; old Snapshot ctor fallbacks are not qualified output authority. Complete source spawn heading/normal heading/pack/login/observer motion consumers, brave/status/perception/meter/gfx, HPBAR/digits/kiringku/proc_spr/suppression/temp gfx/resource routes. No queued Frame/Context/map ownership; real backend/MySQL/game deferred until all code.'}
for group in v['remaining_work_groups']:
 if group['id']in remaining_updates:group['work']=remaining_updates[group['id']]
v['remaining']=[x['work']for x in v['remaining_work_groups']]
v['historical_remaining_preserved_in']=previous['manifest']

def static_calls(manifest):
 ref=next(x for x in manifest['artifacts']if Path(x['path']).name=='call-sites.tsv');verify(ref)
 with Path(ref['path']).open(encoding='utf-8',newline='')as f:return list(csv.DictReader(f,delimiter='\t'))
rows=static_calls(inv);prior_inv=read(Path(old['inventory']['path']));verify(prior_inv)
select=lambda data:[r for r in data if r['callee_owner']=='local/lito381/server/NativePairedCastSafety'and r['callee']=='run']
callers=select(rows);signature=lambda data:[{k:v for k,v in row.items()if k!='call_ordinal'}for row in data];assert len(callers)==5 and signature(callers)==signature(select(static_calls(prior_inv)))
upstream=[r for r in rows if r['callee_owner']=='local/lito381/server/NativeLightAction'and r['callee']=='commitPair'];assert len(upstream)==68
write(run/'shared-pair-caller-locators.json',dict(runtime=v['runtime'],static_only=True,original_direct_caller_signatures_retained=True,original_shared_commitPair_call_ordinal_shifted_by_task_port=True,original_direct_callers=callers,upstream_commitPair_edges=upstream,full_lifecycle_qualified=False,bound_cases=0))
v['shared_pair_caller_locators']=pin(run/'shared-pair-caller-locators.json')
typed=[r for r in rows if r['callee_owner']=='local/lito381/server/NativeSourceOuterPair'and r['callee']=='run'];assert len(typed)==2 and {(r['owner'],r['method'])for r in typed}=={('local/lito381/server/NativeSourceDollAction','enter'),('local/lito381/server/NativeSourceKiringkuAdmission','owned')}
old_outer=[r for r in static_calls(prior_inv)if r['callee_owner']=='local/lito381/server/NativeSourcePlayerActionLock'and r['callee']=='runPairOrdered'];assert len(old_outer)==36
write(run/'typed-outer-pair-callers.json',dict(runtime=v['runtime'],typed_program_ports=typed,original_outer_callers=old_outer,static_only=True,accepted_completion_paths_complete=False,positive_bound_doll_Aim_cast_cases=0))
v['typed_outer_pair_callers']=pin(run/'typed-outer-pair-callers.json')


for key in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[key]is False,key
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),system_goal=v['goal_reset_receipt'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))

text=work.read_text('utf-8');text=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{v['runtime']['sha256']}`",text)
text=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",text)
artifact_paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']}
text=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:artifact_paths[m[1]],text)
a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
block='## 本次已更新的程式\n\n本輪補原NPC快照至首次逃跑actor lookup的生命週期。兩種actor在原snapshot欄位讀取前，捕捉弱actor身分、原life／motion／不可變stamp與pose；原synchronized snapshots列表發出前，再附弱encounter與generation，不重讀或採用新的actor狀態。兩個final／transient Snapshot欄位及精確委派constructor，不改canonical component／equals／hash／標準序列化或原memory carrier。\n\n兩個原PVE迴圈呼叫點傳同一原Snapshot，經完整原Hub append／escape sibling，在既有encounter monitor內首次lookup後核對原弱身分／life／stamp／motion／pose／NPC／force／owner generation。拒絕沿用原handled-return monitor cleanup，不移動／不排程／不輸出／不抽RNG。memory restore不採用舊first-view，而建立新即時權限；舊ctor與缺原生命期的手建物件保持明確未標記後備，不建立新token。\n\n1543原方法＋2完整sibling＋2精確Snapshot constructor獨立正規化inverse、JVM stack-map／322 no-init、48新契約＋2468同SHA繼承重驗、24次原body隔離handoff含22個stale／life／同ID替換／pose／motion／stamp／generation／weak／foreign／unowned／malformed拒絕，以及snapshot發出、兩種memory restore／舊ctor identity schema與同位元重建通過。86 Source檔／133既有bounded參考及新契約／20 SQL class保留；不是bound AI／HP／IO／Main／MySQL／game驗收。\n\n目前guard消費範圍是Source Monster逃跑PVE handoff。正常／magic／summon／受控／bot／guardian AI、accepted／final packet、全部PC target discovery與scene／retirement／Main生命週期仍須延伸原權限；legacy null view只是未標記後備，不是驗收完成。完整hate／targetClear／重新索敵／subclass／custom-work／spawn／presentation、全部58材料／252外控／其他生產端／成對鎖定／HPBAR／數字／奇古獸／正式0.6.71隔離合成及6處歷史current索引仍開放。全部程式完成後才集中同SHA後端／中文MySQL真裝備角色唯讀／實機游標驗收；未部署、不啟動服務或連MySQL、不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 NPC original first-snapshot handoff：候選`{v['runtime']['sha256']}`，snapshot entry弱actor／life／motion／stamp／pose＋原locked list owner generation，2個原PVE snapshot call operands經2完整body sibling於首次escape lookup驗證；原monitor cleanup／RNG／IO保留，memory restore建立fresh view。1543原方法＋2 sibling／2精確ctor inverse、322 no-init、48新＋2468同SHA、24handoff／22拒絕與同位元重建；86 Source檔／133 bounded參考及新契約／20 SQL classes。其他AI／PC target／final IO／Main與全Source及bound驗收仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-NPC-first-snapshot-weak-authority-initial-escape-lookup-program-only',full_goal_achieved=False)
continuation['source_npc_first_view_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=1543,new_game_runtime_cases=0,new_database_connections=0)))
