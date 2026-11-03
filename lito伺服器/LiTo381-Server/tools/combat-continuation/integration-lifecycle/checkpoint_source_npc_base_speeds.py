"""Checkpoint immutable Source NPC move/attack bases and two original schedules; full Source remains open."""
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
latest=read(D/'source-npc-base-speeds-build-current.json');repro=read(D/'source-npc-base-speeds-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_NPC_base_speeds_contract_scenarios']==13 and build['actual_detached_original_NPC_Source_base_schedule_comparisons']==960
assert build['original_partial_and_unqualified_base_fallback_comparisons']==32
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_NPC_base_speeds_contract_scenarios')==2334
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 186 original methods'in log and 'PASS312'in log
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
for name in ['NativeSourceNpcBaseSpeeds.java','SourceNpcBaseSpeedsMethods.java','SourceNpcBaseSpeedsDelta.java','SourceNpcBaseSpeedsLoad.java','SourceNpcBaseSpeedsPlan.java','SourceNpcFactoryTypePlan.java','build_source_npc_base_speeds.py','reproduce_source_npc_base_speeds.py','audit_source_completion.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-NPC-immutable-move-attack-bases-two-original-schedules-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Source NPC applyNpcTemplate copies passispeed and atkspeed to instance movement/attack intervals; normal movement/physical attack use those getters in calcSleepTime rather than animation duration. One existing row birth port freezes two nullable final Integer scalars using one new constructor delegating original brave/type/flags constructor. Original record components/equality/hash retained and previous constructors keep complete prior fallback.', 'Two existing natural Quest/Time normal schedule data ports now consume the immutable Source template movement/attack bases plus prior exact Source slow/haste/brave/wind/default/clamp arithmetic. No animation lookup is required for qualified template timing. Original MP/cost/reuse/counts/nextAction/generation/locks/life/Main and all other archive entries retained; original proof travels unchanged through existing snapshots and memory restore.', '186 original normalized methods independently inverted plus one exact scalar delegating constructor,312 JVM no-init classes,13 new pure contracts plus2334 inherited/revalidated same-SHA checks.960 actual detached original schedules compare against exact frozen Source template application/accessors plus prior exact Source calculator,32 complete partial/unqualified fallback comparisons; old birth remains fixed after template mutation and both encounter families preserve original memory proof. Frozen replay byte identical;81 Source files,133 existing bounded excerpts plus explicit base-speed contract,20 SQL consumer classes retained.', 'Upstream zero-speed movement/attack permissions, custom-work mutation, magic/summon/area/physical-mobskill base selection and metadata timing remain open, as do other statuses, controlled/bot/guardian/full factory/AI/hate/perception and HPBAR/digits/kiringku/final receiver lifetime. No full AI or bound HP/IO/Main/MySQL/game qualification; all58 materials/252 controls/full Source/formal composition and concentrated actual acceptance remain open.']
v['program_checks']={'JVM_no_initialization_classes': 312, 'original_methods_independently_inverted': 186, 'new_exact_delegating_scalar_constructors': 1, 'original_instruction_and_control_flow_inverse_ignores_recomputed_stack_map_frames': True, 'forward_frozen_splice_and_JVM_stack_maps_verified': True, 'byte_identical_frozen_rebuild': True, 'existing_changed_class_entries': 4, 'new_helper_class_entries': 1, 'new_immutable_original_metadata_fields': 2, 'new_original_constructors': 1, 'pure_NPC_base_speeds_contract_scenarios': 13, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 2334, 'actual_detached_original_Source_base_schedules': 960, 'original_partial_and_unsupported_fallback_comparisons': 32, 'original_normal_AI_schedule_points': 2, 'original_MP_reuse_summon_count_and_nextAction_ownership_retained': True, 'both_original_memory_restores_keep_exact_birth_proof': True, 'old_ctors_keep_complete_prior_fallback': True, 'Source_move_passispeed_attack_atkspeed_instead_of_animation': True, 'upstream_zero_speed_eligibility_magic_base_custom_work_full_AI_and_presentation_qualified': False, 'new_retained_actor_Context_Frame_Member_registry_or_TLS': False, 'new_SELECTs': 0, 'new_workers_TLS_registries': False, 'new_connection_ownership': False, 'new_scheduled_tasks': False, 'other_original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained': True, 'bound_selection_cost_RNG_HP_IO_role_Main_cases': 0}
for key in ['Source_NPC_base_speed_contract','Source_NPC_base_speed_oracle','npc_base_speed_port_census','Source_NPC_action_interval_contract','Source_NPC_action_interval_oracle','npc_action_interval_port_census','Source_slow_failure_metadata_contract','slow_failure_metadata_port_census','Source_remaining_area_lists_contract','remaining_area_list_port_census','Source_sleep_construction_contract','sleep_construction_port_census','Source_area_caster_membership_contract','area_caster_target_port_census','target_construction_port_census','Source_target_construction_oracle','Source_target_construction_contract','target_mask_port_census','Source_target_mask_oracle','Source_target_mask_contract','doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['npc_base_'+key]=build[key]
v.update(Source_NPC_immutable_move_attack_base_program_wired=True,Source_normal_NPC_template_timing_does_not_require_animation=True,full_Source_NPC_AI_and_presentation_qualified=False)
v['goal_reset_receipt']=pin(D/'source-completion-goal-reset-current.json');verify(v['goal_reset_receipt'])
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
remaining_updates={'math-and-status': 'Natural Quest/Time immutable passispeed/atkspeed now supply original normal schedule bases instead of animation; prior brave birth plus eight status-multiplier schedules and Source29 timer/payload retained. Complete upstream zero-speed permission, custom-work speed mutation, magic/summon/area/physical-mobskill base selection with original metadata timing, fast move/attack/braveX/elfBrave and other actor stats/status/INT/leverage/lawful/heal/immunity/guardian/talent mutation ordering. Isolated intervals do not qualify full Source skills.', 'npc-factory-ai': 'Original birth freezes exact Source passispeed/atkspeed/brave and both Quest/Time original memory restorations keep the same proof; two normal schedule bases and eight prior status multiplier points wired. Complete movement/attack zero-speed admission before mutation, custom-work updates, magic/area/summon/mobskill base source, first snapshot/reload/rollback/full attributes, missing/unknown/master/manual origins, companion/bot/guardian/tower/door, AI/hate/family/reward and owner-meter.32 Source classes and detached schedules remain scoped evidence only.', 'presentation': 'NPC normal timing uses frozen Source template base, independently of animation duration; source brave remains separate from haste with no invented commented S_NPCPack brave bit. Complete actual brave/status/perception/meter/gfx IO and transport lifetime, HPBAR/digits/kiringku/accepted effects/observers/proc_spr/damage suppression/temp gfx/login sync/resource routes. No queued Frame/Context/map ownership; real backend/MySQL/game deferred until all code.'}
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
block='## 本次已更新的程式\n\n本輪補齊自然Quest／Time NPC移動／物理攻擊的Source基底來源。Source在applyNpcTemplate將passispeed／atkspeed寫入instance；移動與物理攻擊再以原getter進calcSleepTime，並非動畫duration。原出生Stamp新增2個nullable final Integer與1個委派既有brave／type／flag的constructor，舊constructor明確保留原後備，原record身分不變。\n\n兩個原normal schedule資料點改讀不可變的出生基底，再用前輪精確Source slow／haste／brave／wind／960預設／最小1計算；不查後來的可變template，也不要求動畫資源存在才能計算Source時間。MP／reuse／召喚計數／nextAction及生命週期仍由原body持有。出生後template改值只影響新出生，兩種encounter原snapshot／memory restore保留相同proof。\n\n186原方法獨立正規化inverse與1個精確委派constructor、JVM stack-map／312 no-init、13新契約＋2334同SHA繼承重驗、960組實際原方法隔離排程與32組完整舊後備比較，以及同位元重建通過。81 Source檔／133既有bounded參考及新增基底契約／20 SQL class保留；不是bound AI／HP／IO／Main／MySQL／game驗收。\n\n仍須補上游速度為零的移動／攻擊准入、custom-work速度變更、魔法／召喚／範圍／物理mobskill基底選擇與metadata時點、其他status、受控／bot／guardian與完整AI／hate／perception／最終顯示生命週期；另有全部58材料／252外控／其他生產端／成對鎖定／HPBAR／數字／奇古獸／正式0.6.71隔離合成及6處歷史current索引。全部程式完成後再集中同SHA後端／中文MySQL真裝備角色唯讀／實機游標驗收。不部署、不啟動服務或連MySQL、不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 Source NPC移動／攻擊基底續作：候選`{v['runtime']['sha256']}`，原出生凍結passispeed／atkspeed，2個原normal排程採Source模板基底／既有倍率，保留MP／nextAction／生命週期與原記憶體proof。186原方法inverse＋1委派constructor、312 no-init、13新＋2334同SHA重驗、960原方法排程／32舊後備比較與同位元重建；81 Source檔／133 bounded參考及新契約／20 SQL classes。上游zero-speed准入／custom-work／magic基底／完整AI顯示及全Source與bound驗收仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-NPC-immutable-move-attack-bases-two-original-schedules-program-only',full_goal_achieved=False)
continuation['source_npc_base_speeds_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=186,new_game_runtime_cases=0,new_database_connections=0)))
