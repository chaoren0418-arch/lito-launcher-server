"""Checkpoint Source atomic NPC motion state at original phases and transient memory; full Source remains open."""
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
latest=read(D/'source-npc-motion-build-current.json');repro=read(D/'source-npc-motion-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_NPC_motion_contract_scenarios']==36 and build['actual_detached_original_NPC_motion_action_calls']==24
assert build['additional_exact_Source_NPC_heading_comparisons']==32768 and build['additional_exact_Source_NPC_sticky_escape_comparisons']==1568
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_NPC_motion_contract_scenarios')==2382
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 487 original methods'in log and 'PASS319'in log
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
for name in ['NativeSourceNpcMotion.java','SourceNpcMotionMethods.java','SourceNpcMotionDelta.java','SourceNpcMotionLoad.java','SourceNpcMotionPlan.java','build_source_npc_motion.py','reproduce_source_npc_motion.py','audit_source_completion.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-NPC-atomic-heading-sticky-state-original-phase-and-memory-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Each natural Quest/Time actor owns one volatile/transient Object holding an immutable scalar heading/escaped record. Source float0.59 targetDirection is computed at original successful coordinate writes and two physical scheduling sites per encounter. Original validated target-distance phase records the exact sticky ISASCAPE condition attack0 AND move>0. No original attack/RNG/geometry/status/cost/HP/life/Main decision bodies are replaced; reverse escape is not yet consumed by AI.', 'Seven existing carry/proof/restore data ports extend the original in-memory-only Snapshot carrier with immutable original factory Stamp plus scalar Motion. Zero canonical Snapshot component/constructor/equality/hash/serialization changes; companion proof branch is not unwrapped or promoted. Both original natural restore paths keep primary motion, including release-tamed; legacy/missing/foreign/invalid motion does not load a new template or adopt replacement state. No actor/Context/Frame/row/snapshot retained by the memory payload or a new global registry/TLS/queue/worker/lock/SQL.', '487 original normalized methods independently inverse-retained across five original owners;319 JVM no-init classes.36 new pure contracts plus2382 inherited/revalidated same-SHA checks.32768 exact frozen Source directions including rounding/coincident/int-overflow and1568 sticky-state cases;24 actual detached original action calls preserve accepted/rejected phases and schedules. Actual original snapshots/restores/legacy/wrong-NPC/invalid-motion/release-tamed/unchanged record schema checked. Frozen rebuild byte-identical;86 Source files,133 prior bounded references plus motion contract,20 SQL consumers retained.', 'Remaining Source escape needs actual pre-RNG AI routing/reverse movement/checkObject/openDoor/escape distance/target clear/hate, original spawn heading and final pack/status/login/perception/IO consumption. New actor state is a prerequisite, not complete escape or presentation. Full custom-work/factory/AI/cast/RNG/controlled/bot/guardian/cross-role lifecycle,58 materials/252 controls/formal composition and concentrated actual backend/read-only Chinese MySQL/game acceptance remain open.']
v['program_checks']={'JVM_no_initialization_classes': 319, 'original_methods_independently_inverted': 487, 'original_instruction_and_control_flow_inverse_ignores_recomputed_stack_map_frames': True, 'forward_frozen_splice_and_JVM_stack_maps_verified': True, 'byte_identical_frozen_rebuild': True, 'existing_changed_class_entries': 5, 'new_helper_and_immutable_record_class_entries': 3, 'new_original_atomic_transient_actor_fields': 2, 'new_original_constructors': 0, 'new_canonical_Snapshot_components_or_constructors': 0, 'pure_NPC_motion_contract_scenarios': 36, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 2382, 'exact_frozen_Source_targetDirection_comparisons': 32768, 'exact_frozen_Source_sticky_escape_comparisons': 1568, 'actual_detached_original_motion_action_calls': 24, 'original_accepted_move_state_ports': 2, 'original_validated_target_phase_state_ports': 2, 'original_physical_facing_ports': 4, 'original_transient_memory_data_ports': 7, 'original_MP_nextAction_snapshot_and_decision_bodies_retained': True, 'both_natural_restore_and_release_tamed_keep_primary_motion': True, 'companion_proof_branch_not_unwrapped_or_promoted': True, 'original_record_equality_hash_and_serialization_schema_retained': True, 'legacy_foreign_invalid_motion_no_template_or_state_adoption': True, 'Source_reverse_escape_spawn_heading_final_IO_and_full_AI_qualified': False, 'new_retained_actor_Context_Frame_Member_registry_or_TLS': False, 'new_SELECTs': 0, 'new_workers_TLS_registries': False, 'new_connection_ownership': False, 'new_scheduled_tasks': False, 'other_original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained': True, 'bound_selection_cost_RNG_HP_IO_role_Main_cases': 0}
for key in ['Source_NPC_motion_contract','Source_NPC_motion_oracle','npc_motion_port_census','Source_NPC_movement_contract','Source_NPC_movement_oracle','npc_movement_port_census','Source_NPC_spell_timing_contract','Source_NPC_spell_timing_oracle','npc_spell_timing_port_census','Source_NPC_base_speed_contract','Source_NPC_base_speed_oracle','npc_base_speed_port_census','Source_NPC_action_interval_contract','Source_NPC_action_interval_oracle','npc_action_interval_port_census','Source_slow_failure_metadata_contract','slow_failure_metadata_port_census','Source_remaining_area_lists_contract','remaining_area_list_port_census','Source_sleep_construction_contract','sleep_construction_port_census','Source_area_caster_membership_contract','area_caster_target_port_census','target_construction_port_census','Source_target_construction_oracle','Source_target_construction_contract','target_mask_port_census','Source_target_mask_oracle','Source_target_mask_contract','doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['npc_motion_'+key]=build[key]
v.update(Source_NPC_atomic_heading_sticky_state_original_phases_and_memory_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
v['skills_SQL_id_distinct_from_skill_id_no_extra_identity_gate']=True
v['goal_reset_receipt']=pin(D/'source-completion-goal-reset-current.json');verify(v['goal_reset_receipt'])
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
remaining_updates={'math-and-status': 'Natural NPC atomic heading/escaped state now follows original accepted movement/physical and validated target phases; Source float0.59 direction and sticky attack0/move>0 condition are wired into state, with old timing/movement guards retained. Complete actual reverse-escape AI and original spawn heading/final IO consumption, custom-work stats, pre-RNG trigger/cast timing, fast move/attack/braveX/elfBrave, INT/leverage/lawful/heal/immunity/status ordering and manual/controlled/bot/guardian routes.', 'npc-factory-ai': 'Natural actors now retain immutable atomic heading/escaped state through existing transient Snapshot carry/proof/restore, including release-tamed original-primary behavior; old/foreign/invalid data cannot adopt a new template or motion, and companion proof branch is unchanged. Next wire actual pre-RNG sticky escape routing, reverseDirection/checkObject/openDoor/distance/target-clear/hate, original spawn heading and actual presentation; new state is not complete escape. Complete custom-work/full trigger/cast scheduling, first snapshots/reload/rollback/full attributes, unknown/manual/master/controlled/bot/guardian/tower/door origins, family/reward and owner-meter.', 'presentation': 'Natural NPC stored heading now follows exact Source direction and is carried by immutable transient snapshots; original publication/pack/login/observer/final IO consumers still need wiring to accepted motion state without a fresh actor lookup. Complete Source spawn heading, brave/status/perception/meter/gfx transport lifetime, HPBAR/digits/kiringku/accepted effects/proc_spr/damage suppression/temp gfx/resource routes and accepted/final cross-role proofs. No queued Frame/Context/map ownership; real backend/MySQL/game deferred until all code.'}
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
block='## 本次已更新的程式\n\n本輪補自然Quest／Time NPC面向與黏著脫逃狀態的原階段接線及記憶體保存。每個actor只增1個volatile／transient Object，指向immutable heading／escaped兩個scalar。原成功座標寫入、每種encounter兩個物理排程點更新Source float0.59 targetDirection；原驗證後target距離階段記錄attack0且move>0的sticky ISASCAPE，不改原攻擊／RNG／geometry／status／cost／HP／life／Main決策body。\n\n既有Snapshot暫存metadata的7個carry／proof／restore資料點，保存原不可變factory Stamp與scalar Motion；不增canonical component／constructor，不改equals／hash／標準序列化schema。companion proof分支保持原判斷，不接受自然NPC motion carrier。兩種自然還原與release-tamed保留主NPC原狀態；legacy／foreign／invalid資料不載入新template或套用替代狀態。payload不持有actor／Context／Frame／可變row／snapshot，也不新增global registry／TLS／queue／worker／鎖／SQL。\n\n487原方法獨立正規化inverse、JVM stack-map／319 no-init、36新契約＋2382同SHA繼承重驗、32768組凍結Source方向與1568組sticky狀態比較、24次原方法隔離動作呼叫及同位元重建通過；並檢查原snapshot／兩種restore／release-tamed／wrong-NPC／invalid-motion／legacy與record schema。86 Source檔／133既有bounded參考及新契約／20 SQL class保留；不是bound AI／HP／IO／Main／MySQL／game驗收。\n\n新狀態尚未接入實際逃跑決策與最終顯示。仍須補pre-RNG AI脫逃路由、reverseDirection／checkObject／openDoor／距離／targetClear／hate、原spawn heading及pack／status／login／observer／final IO消費；不得從最後送出封包或重新查actor推回接受時狀態。完整custom-work／工廠AI／施法RNG／受控bot／guardian／跨角色生命週期、全部58材料／252外控／其他生產端／成對鎖定／HPBAR／數字／奇古獸／正式0.6.71隔離合成及6處歷史current索引仍開放。全部程式完成後才集中同SHA後端／中文MySQL真裝備角色唯讀／實機游標驗收；未部署、不啟動服務或連MySQL、不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 Source NPC原階段motion／memory續作：候選`{v['runtime']['sha256']}`，2個actor-owned atomic transient motion slots，原移動／物理／target階段與7個既有memory資料點攜帶heading／sticky escaped；不改原決策／RNG／life／Main與Snapshot canonical schema，不promote companion。487原方法inverse、319 no-init、36新＋2382同SHA重驗、32768方向／1568sticky／24原動作、restore／release-tamed／legacy／invalid與同位元重建；86 Source檔／133 bounded參考及新契約／20 SQL classes。實際逃跑路由／spawn heading／最終IO、全Source與bound驗收仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-NPC-atomic-heading-sticky-state-original-phase-and-memory-program-only',full_goal_achieved=False)
continuation['source_npc_motion_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=487,new_game_runtime_cases=0,new_database_connections=0)))
