"""Checkpoint Source Monster pre-RNG escape with original Hub/movement/occupancy bodies; full Source remains open."""
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
latest=read(D/'source-npc-escape-build-current.json');repro=read(D/'source-npc-escape-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_NPC_escape_contract_scenarios']==50 and build['actual_detached_original_NPC_escape_calls']==40
assert build['additional_exact_Source_NPC_reverse_comparisons']==648 and build['additional_exact_Source_NPC_checkObject_comparisons']==2048
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_NPC_escape_contract_scenarios')==2418
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 1318 original methods'in log and 'PASS320'in log
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
for name in ['NativeSourceNpcEscape.java','SourceNpcEscapeMethods.java','SourceNpcEscapeDelta.java','SourceNpcEscapeLoad.java','SourceNpcEscapePlan.java','build_source_npc_escape.py','reproduce_source_npc_escape.py','audit_source_completion.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-Monster-pre-RNG-escape-original-Hub-movement-occupancy-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['One original Hub boundary after original cube/sleep/freeze checks and before fixture/AI/RNG routes known Source Monster kind1 sticky escape. It uses current exact Source reverse direction and generic checkObject order, darkness distance1 versus17, Source door phase without retrying after a blocked chosen door, and original schedule for failed geometry/door attempts. Unqualified/nonescaping/other classes retain complete original body; full persistent hate/target-clear/reacquisition is not claimed.', 'Three complete original body siblings retain native movement/geometry/collision/status/position/schedule/receipt/IO policies. Two move siblings change only qualified escaping blind admission; occupancy sibling defers two door results to Source openDoor phase while preserving tower/PC/companion checks. PC footprint discovery and original tick-constructor origin capture stay outside the encounter monitor. Original actor/stamp/life/generation/motion/pose/arena are frozen before footprint discovery and checked again before mutation, with no rebase after the wait. No new fields/constructors/actor registry/TLS/queue/worker/SQL or new lock domain.', '1318 original methods plus3 complete original siblings independently inverse-retained;320 JVM no-init classes.50 new pure contracts plus2418 inherited/revalidated same-SHA checks,648 exact Source reverse and2048 exact generic Source checkObject cases,40 actual detached calls including two actual private Hub hooks and two life replacements during footprint discovery. Original sleep/status/dark-distance/door/all-occupied/legacy/unowned-Hub behavior and unchanged ThreadLocalRandom seed checked. Frozen replay byte-identical;86 Source files,133 prior bounded references plus escape contract,20 SQL consumer classes retained.', 'Complete initial Snapshot-to-actor provenance before first lookup and all source retirement/scene/role/Main/final receiver lifetimes, persistent target/hate clear and reacquisition, other32-class applicability/De/controlled/bot/guardian behavior, custom-work/spawn heading/pack/login/final IO. Full58 materials/252 controls/other producers/formal composition and actual same-SHA backend/read-only Chinese MySQL/game acceptance remain open. This route does not qualify all NPC AI or a bound game tick.']
v['program_checks']={'JVM_no_initialization_classes': 320, 'original_methods_independently_inverted': 1318, 'complete_original_siblings_independently_inverted': 3, 'original_instruction_and_control_flow_inverse_ignores_recomputed_stack_map_frames': True, 'forward_frozen_splice_and_JVM_stack_maps_verified': True, 'byte_identical_frozen_rebuild': True, 'existing_changed_class_entries': 3, 'new_helper_class_entries': 1, 'new_original_fields_or_constructors': 0, 'new_complete_original_body_siblings': 3, 'pure_NPC_escape_contract_scenarios': 50, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 2418, 'exact_Source_reverse_comparisons': 648, 'exact_generic_Source_checkObject_comparisons': 2048, 'actual_detached_escape_calls': 40, 'actual_original_private_Hub_entry_cases': 2, 'actual_footprint_discovery_life_replacement_rejections': 2, 'original_pre_AI_RNG_Hub_hook': 1, 'original_movement_blind_sibling_ports': 2, 'original_door_phase_sibling_ports': 2, 'Source_Monster_kind1_route_only': True, 'escape_ThreadLocalRandom_seed_unchanged': True, 'original_status_geometry_position_schedule_and_tick_constructor_retained': True, 'Source_dark_distance_and_failed_attempt_schedule_verified': True, 'PC_companion_footprint_and_constructor_origin_capture_outside_encounter_monitor': True, 'original_actor_stamp_life_generation_motion_pose_arena_rechecked_before_mutation': True, 'initial_Snapshot_provenance_persistent_hate_target_clear_subclasses_spawn_final_IO_Main_full_AI_qualified': False, 'new_retained_actor_Context_Frame_Member_registry_or_TLS': False, 'new_SELECTs': 0, 'new_workers_TLS_registries': False, 'new_connection_ownership': False, 'new_scheduled_tasks': False, 'other_original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained': True, 'bound_selection_cost_RNG_HP_IO_role_Main_cases': 0}
for key in ['Source_NPC_escape_contract','Source_NPC_escape_oracle','npc_escape_port_census','Source_NPC_motion_contract','Source_NPC_motion_oracle','npc_motion_port_census','Source_NPC_movement_contract','Source_NPC_movement_oracle','npc_movement_port_census','Source_NPC_spell_timing_contract','Source_NPC_spell_timing_oracle','npc_spell_timing_port_census','Source_NPC_base_speed_contract','Source_NPC_base_speed_oracle','npc_base_speed_port_census','Source_NPC_action_interval_contract','Source_NPC_action_interval_oracle','npc_action_interval_port_census','Source_slow_failure_metadata_contract','slow_failure_metadata_port_census','Source_remaining_area_lists_contract','remaining_area_list_port_census','Source_sleep_construction_contract','sleep_construction_port_census','Source_area_caster_membership_contract','area_caster_target_port_census','target_construction_port_census','Source_target_construction_oracle','Source_target_construction_contract','target_mask_port_census','Source_target_mask_oracle','Source_target_mask_contract','doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['npc_escape_'+key]=build[key]
v.update(Source_Monster_pre_AI_RNG_escape_original_Hub_and_sibling_routes_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
v['skills_SQL_id_distinct_from_skill_id_no_extra_identity_gate']=True
v['goal_reset_receipt']=pin(D/'source-completion-goal-reset-current.json');verify(v['goal_reset_receipt'])
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
remaining_updates={'paired-lifecycle': 'Typed precommit pair/scope/light-life guards and new Source Monster escape actor/stamp/life/generation/motion/pose/arena retention across original footprint discovery are program-wired. Complete initial Snapshot provenance before first NPC lookup, all earlier waits/accepted PC-NPC completion/final receiver rules without blanket rejection or caster dependency, manual action-lock holders without parent scope, identity-hash tie paths, role/death/revival/same-id/scene/owner changes, bounded memory and actual Main/DRAINING/STOPPED. Six historical current aliases remain separate evidence work.', 'math-and-status': 'Known Source Monster escape now branches before original AI RNG with exact generic reverse/checkObject order, darkness distance and failed-attempt schedule, while original base/status/movement guards remain. Complete persistent target/hate clear and reacquisition, initial source Snapshot/scene/life provenance, all class/De/controlled/bot/guardian behavior, custom-work/spawn heading/final output, original trigger/cast/RNG semantics and fast move/attack/braveX/elfBrave/INT/leverage/lawful/heal/immunity/status ordering.', 'npc-factory-ai': 'Known Source Monster kind1 pre-RNG escape is wired through original Hub plus complete movement/occupancy siblings, with original footprint/constructor lock order and actor/life/generation/motion/pose/arena rechecks. Complete initial Snapshot provenance before lookup, persistent target/hate clear/reacquisition, all32-class/De/controlled/bot/guardian/tower/door applicability, custom-work/spawn heading/full AI trigger/cast scheduling, first snapshot/reload/rollback/full attributes, unknown/manual/master origins, family/reward and owner-meter. Isolated calls are not bound AI acceptance.', 'presentation': 'New Source escape movement uses original MonsterAiTick constructor and original final publication/receiver path, with constructor origin capture outside encounter monitor. Complete accepted original Snapshot/actor provenance and actual life/scene/role/Main/final IO, source spawn heading/normal physical heading/pack/login/observer motion consumers, brave/status/perception/meter/gfx transport lifetime, HPBAR/digits/kiringku/accepted effects/proc_spr/damage suppression/temp gfx/resource routes. No queued Frame/Context/map ownership; real backend/MySQL/game deferred until all code.'}
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
block='## 本次已更新的程式\n\n本輪將已知Source Monster kind1的sticky脫逃狀態接入原Hub：原cube／sleep／freeze檢查後、fixture／AI／RNG前分流；以目前Source面向計算反向，沿用精確generic checkObject候選順序、DARKNESS距離1／一般17，再做門階段，選中方向遇門不重試別向。失敗的幾何／門嘗試仍走原schedule設定等待時間。非脫逃／未標記／其他class保持原body；沒有宣稱完整hate／targetClear／重新索敵。\n\n增加3個完整原body sibling：兩個move只調整合格逃跑的blind資料，一個occupancy將兩個door結果延到Source openDoor階段，保留towers／PC／companion及原geometry／status／position／schedule／receipt／IO。PC footprint與原tick constructor的origin capture都在encounter鎖外；回鎖提交前比對同actor／stamp／life／generation／motion／pose／arena，等待後不採用替代生命期。無新欄位／constructor／actor registry／TLS／queue／worker／SQL／新鎖域。\n\n1318原方法＋3個完整sibling獨立正規化inverse、JVM stack-map／320 no-init、50新契約＋2418同SHA繼承重驗、648組Source reverse／2048組generic checkObject與40次實際原方法隔離逃跑呼叫通過；含2個真private Hub入口與2個footprint期間生命期替換拒絕。逃跑路徑ThreadLocalRandom seed不變、dark距離／status／門／全佔位／原後備及同位元重建通過。86 Source檔／133既有bounded參考及新契約／20 SQL class保留；不是bound AI／HP／IO／Main／MySQL／game驗收。\n\n尚待補最早Snapshot至首次actor lookup的原provenance、全部scene／role／Main／最後接收生命週期、persistent hate／targetClear／重新索敵、其餘32-class／De／受控／bot／guardian、custom-work／spawn heading／pack／login／final IO與完整AI／施法RNG。全部58材料／252外控／其他生產端／成對鎖定／HPBAR／數字／奇古獸／正式0.6.71隔離合成及6處歷史current索引仍開放。全部程式完成後才集中同SHA後端／中文MySQL真裝備角色唯讀／實機游標驗收；未部署、不啟動服務或連MySQL、不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 Source Monster pre-RNG逃跑路由：候選`{v['runtime']['sha256']}`，原Hub post-status／pre-AI hook＋3完整move／occupancy sibling，保留原geometry／status／IO並分開門階段。footprint與constructor origin capture在encounter鎖外，回鎖比對actor／stamp／life／generation／motion／pose／arena。1318原方法＋3 sibling inverse、320 no-init、50新＋2418同SHA、648 reverse／2048 checkObject／40隔離呼叫、2private Hub與2生命期替換拒絕、no-RNG與同位元重建；86 Source檔／133 bounded參考及新契約／20 SQL classes。最早Snapshot provenance、hate／target-clear／其他class與完整AI／IO／Main及全Source／bound驗收仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-Monster-pre-RNG-escape-original-Hub-movement-occupancy-program-only',full_goal_achieved=False)
continuation['source_npc_escape_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=1318,new_game_runtime_cases=0,new_database_connections=0)))
