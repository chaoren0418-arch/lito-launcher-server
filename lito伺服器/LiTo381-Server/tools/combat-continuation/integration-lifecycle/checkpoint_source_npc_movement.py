"""Checkpoint Source positive movement speed at two original NPC movement entries; full Source remains open."""
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
latest=read(D/'source-npc-movement-build-current.json');repro=read(D/'source-npc-movement-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_NPC_movement_contract_scenarios']==11 and build['actual_detached_original_NPC_movement_calls']==50
assert build['additional_exact_Source_NPC_movement_comparisons']==900
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_NPC_movement_contract_scenarios')==2371
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 316 original methods'in log and 'PASS316'in log
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
for name in ['NativeSourceNpcMovement.java','SourceNpcMovementMethods.java','SourceNpcMovementDelta.java','SourceNpcMovementLoad.java','SourceNpcMovementPlan.java','build_source_npc_movement.py','reproduce_source_npc_movement.py','audit_source_completion.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-NPC-positive-speed-original-move-admission-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Source pursuit/follow/random movement is gated by instance getPassispeed()>0, separately from calcSleepTime zero/default960 arithmetic. Two original natural Quest/Time moveForAi ready-result data ports narrow movement only for the existing immutable known birth movement interval. Original false ready returns without new discovery; unsupported/old constructor metadata retains complete original movement fallback. Attack speed is not used as a blanket veto.', 'No new original fields, constructors, captures, lifecycle snapshots, locks, queues, workers or SQL. Original geometry/collision/blind/sleep/paralysis/stun/Bone/Grab/freeze/earth-bind/position/MP/nextAction/snapshot and all attack/RNG/life/Main bodies inverse-retained. Helper is field-free and reads only the same original resolved actor and original ready result.', '316 original methods across both original outer encounters independently inverse-retained;316 JVM no-init classes.11 new contracts plus2371 inherited/revalidated same-SHA checks.900 exact frozen Source instance getter/positive-move comparisons,50 actual detached original calls:28 Source speed/attack combinations with accepted position/schedule/MP/snapshot checks,4 complete legacy paths,18 original ready/status rejections. Frozen replay byte identical;86 Source files,133 prior bounded references plus explicit movement contract,20 SQL consumer classes retained.', 'Full Source escape remains open: onTarget starts sticky ISASCAPE only when atkspeed==0 and passispeed>0. Source current heading, reverse direction/checkObject/openDoor/escape distance/target clearing and snapshot/memory must be wired together; current native NPC Snapshot has no heading component, so mirrored target coordinates are not an equivalent implementation. Full custom-work/factory/AI/hate/cast/RNG/controlled/bot/guardian/presentation/cross-role life,58 materials/252 controls/formal and actual concentrated acceptance remain open.']
v['program_checks']={'JVM_no_initialization_classes': 316, 'original_methods_independently_inverted': 316, 'original_instruction_and_control_flow_inverse_ignores_recomputed_stack_map_frames': True, 'forward_frozen_splice_and_JVM_stack_maps_verified': True, 'byte_identical_frozen_rebuild': True, 'existing_changed_class_entries': 2, 'new_helper_class_entries': 1, 'new_immutable_original_metadata_fields': 0, 'new_original_constructors': 0, 'pure_NPC_movement_contract_scenarios': 11, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 2371, 'exact_frozen_Source_movement_comparisons': 900, 'actual_detached_original_move_calls': 50, 'actual_known_speed_attack_combination_moves': 28, 'actual_legacy_fallback_moves': 4, 'actual_original_ready_status_rejections': 18, 'original_move_ready_data_ports': 2, 'original_position_MP_nextAction_snapshot_retained': True, 'no_attack_speed_zero_blanket_veto': True, 'old_ctors_unknown_metadata_and_original_false_ready_retained': True, 'Source_escape_current_heading_memory_AI_final_IO_qualified': False, 'new_retained_actor_Context_Frame_Member_registry_or_TLS': False, 'new_SELECTs': 0, 'new_workers_TLS_registries': False, 'new_connection_ownership': False, 'new_scheduled_tasks': False, 'other_original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained': True, 'bound_selection_cost_RNG_HP_IO_role_Main_cases': 0}
for key in ['Source_NPC_movement_contract','Source_NPC_movement_oracle','npc_movement_port_census','Source_NPC_spell_timing_contract','Source_NPC_spell_timing_oracle','npc_spell_timing_port_census','Source_NPC_base_speed_contract','Source_NPC_base_speed_oracle','npc_base_speed_port_census','Source_NPC_action_interval_contract','Source_NPC_action_interval_oracle','npc_action_interval_port_census','Source_slow_failure_metadata_contract','slow_failure_metadata_port_census','Source_remaining_area_lists_contract','remaining_area_list_port_census','Source_sleep_construction_contract','sleep_construction_port_census','Source_area_caster_membership_contract','area_caster_target_port_census','target_construction_port_census','Source_target_construction_oracle','Source_target_construction_contract','target_mask_port_census','Source_target_mask_oracle','Source_target_mask_contract','doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['npc_move_'+key]=build[key]
v.update(Source_NPC_two_original_move_entries_positive_known_birth_speed_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
v['skills_SQL_id_distinct_from_skill_id_no_extra_identity_gate']=True
v['goal_reset_receipt']=pin(D/'source-completion-goal-reset-current.json');verify(v['goal_reset_receipt'])
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
remaining_updates={'math-and-status': 'Known natural NPC positive movement speed now gates both original moveForAi ready operands before geometry/collision/position/scheduling; delay default960 does not grant zero-speed movement. Prior move/attack/magic bases and exact status multipliers retained. Complete sticky ISASCAPE/current heading/reverse movement/memory, custom-work stats, exact trigger/RNG/cast timing, fast move/attack/braveX/elfBrave, other INT/leverage/lawful/heal/immunity/status ordering and manual/controlled/bot/guardian routes.', 'npc-factory-ai': 'Original birth/memory retain Source move/attack/brave/two spell bases/immutable mobskill routes, eight timing points and two positive-speed movement admission ports. Complete sticky ISASCAPE only when attack0 AND move>0 plus current heading/reverse direction/checkObject/openDoor/escape distance/target clearing and memory; native NPC Snapshot currently has no heading component. Complete custom-work/full trigger/RNG/cast scheduling, snapshot/reload/rollback/full attributes, missing/unknown/manual/master/controlled/bot/guardian/tower/door origins, hate/family/reward and owner-meter. Isolated movement/schedule proofs are not full AI.'}
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
block='## 本次已更新的程式\n\n本輪接上自然Quest／Time兩個原moveForAi的Source移動速度准入。Source追擊／跟隨／隨機移動以getPassispeed()>0為條件，與calcSleepTime將非正基底換成960毫秒是兩件事。僅在原ready結果後，用同一已解析actor的既有不可變出生速度縮限；不把attack0當成一律禁止攻擊。舊constructor／未知來源保持原後備，原ready為false不做新discovery。\n\n只增加2個ready資料點與1個無狀態helper，不增原欄位／constructor／角色生命位置capture／鎖／queue／worker／TLS／SQL。原地圖／碰撞／blind／睡眠／麻痺／stun／Bone／Grab／freeze／earth-bind、位置／MP／nextAction／snapshot及所有攻擊／RNG／生命週期／Main body可逆還原。\n\n316原方法獨立正規化inverse、JVM stack-map／316 no-init、11新契約＋2371同SHA繼承重驗、900組Source getter／正速度比較、50次實際原方法隔離呼叫（28速度／attack組合、4舊後備、18原ready／狀態拒絕）及同位元重建通過。正速度含attack0仍可移動；非正速度拒絕時不改位置／排程／MP，成功snapshot保留原出生proof。86 Source檔／133既有bounded參考及新契約／20 SQL class保留；不是bound AI／HP／IO／Main／MySQL／game驗收。\n\n完整脫逃仍待補：Source在attack0且move>0才啟用sticky ISASCAPE，還需要當前heading、reverseDirection／checkObject／openDoor／距離／targetClear與snapshot／memory一起接線。現有NPC Snapshot沒有heading，不能以鏡射追擊座標取代。完整custom-work／工廠AI／hate／施法RNG／受控bot／guardian／最後接收生命週期、全部58材料／252外控／其他生產端／成對鎖定／HPBAR／數字／奇古獸／正式0.6.71隔離合成及6處歷史current索引仍開放。全部程式完成後才集中同SHA後端／中文MySQL真裝備角色唯讀／實機游標驗收；未部署、不啟動服務或連MySQL、不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 Source NPC正速度移動准入續作：候選`{v['runtime']['sha256']}`，2個原moveForAi ready資料點按既有出生passispeed>0縮限；原geometry／collision／status／position／MP／nextAction／snapshot／attack／RNG／life／Main保留。316原方法inverse、316 no-init、11新＋2371同SHA重驗、900 Source比較／50原方法隔離呼叫及同位元重建；86 Source檔／133 bounded參考及新契約／20 SQL classes。完整ISASCAPE／heading／reverse／memory、AI與全Source及bound驗收仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-NPC-positive-speed-two-original-move-entries-program-only',full_goal_achieved=False)
continuation['source_npc_movement_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=316,new_game_runtime_cases=0,new_database_connections=0)))
