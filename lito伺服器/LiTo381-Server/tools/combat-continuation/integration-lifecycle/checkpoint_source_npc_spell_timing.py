"""Checkpoint immutable Source NPC spell bases/routes and six original schedules; full Source remains open."""
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
latest=read(D/'source-npc-spell-timing-build-current.json');repro=read(D/'source-npc-spell-timing-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']or(previous['runtime']['sha256']=='af20195916500212aa4a0077ad021dd9d3040a3de87f5f52dac2d67c8ddf0ed4'and read(Path(old['build']['path']))['base']==build['base'])
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_NPC_spell_timing_contract_scenarios']==24 and build['actual_detached_original_NPC_Source_spell_base_schedules']==1440
assert build['additional_exact_Source_NPC_spell_base_choice_comparisons']==9000
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_NPC_spell_timing_contract_scenarios')==2347
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 187 original methods'in log and 'PASS315'in log
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
for name in ['NativeSourceNpcSpellTiming.java','SourceNpcSpellTimingMethods.java','SourceNpcSpellTimingDelta.java','SourceNpcSpellTimingLoad.java','SourceNpcSpellTimingPlan.java','SourceNpcFactoryTypePlan.java','build_source_npc_spell_timing.py','reproduce_source_npc_spell_timing.py','audit_source_completion.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-NPC-immutable-spell-bases-mobskill-routes-six-original-schedules-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Source normal magic chooses attack magic base only for exact target attack and skill!=18; mobskill AreaWidth>0 OR AreaHeight>0 takes support magic base before that choice, as does summon. Source MobSkillTable uses actNo as array index. One existing original birth port captures two template delays and immutable four-int-per-route records from the exact original NPC/mobskill static snapshot. No mutable NPC/mobskill/template/snapshot or actor/Context/Frame retained.', 'Six original natural Quest/Time scheduleMagic/scheduleSummon/scheduleGround/scheduleVisual base data ports carry their same original definition into exact Source route/type/NPC/skill checked selection. Skills target is read at the original post-cast delay stage, not frozen into NPC birth. Missing or mismatched actor/NPC/skill/type/route and unloaded skills retain complete prior fallback. Original MP debit/readiness/reuse/summon counts/nextAction and status multiplier/life/Main bodies retained; original proof transfers unchanged through both encounter snapshots/memory restore.', '187 original normalized methods independently inverted plus one exact delegating immutable-proof constructor;315 JVM no-init classes;24 new pure contracts including independent skills.id versus skill_id regression plus2347 inherited/revalidated same-SHA checks.9000 exact frozen Source magic/area choice/default comparisons and1440 actual detached original Quest/Time spell schedules preserve MP/reuse/counts and exact Source choice/status order. Immutable birth mutation, wrong identities/types/missing data, independent AreaHeight and original memory restore verified. Frozen replay byte identical;83 Source files,133 existing bounded references plus explicit spell timing contract,20 SQL classes retained.', 'Full Source AI remains open: sticky ISASCAPE on atkspeed0 AND passispeed>0 and original reverse movement cannot be replaced by blanket zero-attack rejection; custom-work/stat mutation, original trigger/RNG/cast timing, unsupported/manual/controlled/bot/guardian origins and all other status/perception/hate/factory/HPBAR/digits/kiringku/cross-role accepted and final receiver lifetimes still need completion. Full58 materials/252 controls/formal composition and actual same-SHA backend/read-only Chinese MySQL/game acceptance remain open.']
v['program_checks']={'JVM_no_initialization_classes': 315, 'original_methods_independently_inverted': 187, 'new_exact_delegating_immutable_proof_constructors': 1, 'original_instruction_and_control_flow_inverse_ignores_recomputed_stack_map_frames': True, 'forward_frozen_splice_and_JVM_stack_maps_verified': True, 'byte_identical_frozen_rebuild': True, 'existing_changed_class_entries': 4, 'new_helper_and_immutable_record_class_entries': 3, 'new_immutable_original_metadata_fields': 1, 'new_original_constructors': 1, 'pure_NPC_spell_timing_contract_scenarios': 24, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 2347, 'exact_frozen_Source_spell_choice_comparisons': 9000, 'actual_detached_original_Source_spell_schedules': 1440, 'original_spell_schedule_points': 6, 'original_MP_reuse_summon_count_and_nextAction_ownership_retained': True, 'both_original_memory_restores_keep_exact_immutable_spell_proof': True, 'old_ctors_and_mismatched_missing_data_keep_complete_prior_fallback': True, 'Source_mobskill_area_precedes_direction_and_skill18_exception': True, 'Source_skills_target_read_at_original_post_cast_delay_stage': True, 'upstream_escape_custom_work_original_cast_AI_or_final_IO_qualified': False, 'new_retained_actor_Context_Frame_Member_registry_or_TLS': False, 'new_SELECTs': 0, 'new_workers_TLS_registries': False, 'new_connection_ownership': False, 'new_scheduled_tasks': False, 'other_original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained': True, 'bound_selection_cost_RNG_HP_IO_role_Main_cases': 0}
for key in ['Source_NPC_spell_timing_contract','Source_NPC_spell_timing_oracle','npc_spell_timing_port_census','Source_NPC_base_speed_contract','Source_NPC_base_speed_oracle','npc_base_speed_port_census','Source_NPC_action_interval_contract','Source_NPC_action_interval_oracle','npc_action_interval_port_census','Source_slow_failure_metadata_contract','slow_failure_metadata_port_census','Source_remaining_area_lists_contract','remaining_area_list_port_census','Source_sleep_construction_contract','sleep_construction_port_census','Source_area_caster_membership_contract','area_caster_target_port_census','target_construction_port_census','Source_target_construction_oracle','Source_target_construction_contract','target_mask_port_census','Source_target_mask_oracle','Source_target_mask_contract','doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['npc_spell_'+key]=build[key]
v.update(Source_NPC_immutable_spell_bases_known_mobskill_routes_six_schedules_program_wired=True,Source_NPC_spell_target_read_at_original_post_cast_stage=True,full_Source_NPC_AI_and_presentation_qualified=False)
v['skills_SQL_id_distinct_from_skill_id_no_extra_identity_gate']=True
v['superseded_preliminary_spell_timing_progress']=previous['manifest']
v['goal_reset_receipt']=pin(D/'source-completion-goal-reset-current.json');verify(v['goal_reset_receipt'])
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
remaining_updates={'math-and-status': 'Immutable natural Quest/Time move/attack bases and six spell-base ports now follow known original mobskill area/type and post-cast skill-target selection, with Source slow/haste/brave/wind/default/clamp preserved. Complete sticky Source ISASCAPE/reverse movement, custom-work speed/stat mutations, exact original trigger/RNG/cast phase, fast move/attack/braveX/elfBrave, unsupported/manual/controlled/bot/guardian routes and other INT/leverage/lawful/heal/immunity/status ordering. Scoped delay tests do not qualify complete Source skills.', 'npc-factory-ai': 'Original birth/memory now retain Source passispeed/atkspeed/brave plus two magic bases and immutable mobskill actNo/type/skill/area facts; all eight normal/spell timing points wired for known routes. Complete sticky Source ISASCAPE only when atkspeed0 AND passispeed>0, original reverse movement/escape distance/target clearing, custom-work mutation, full original AI trigger/RNG/cast scheduling, first snapshot/reload/rollback/full attributes, missing/unknown/manual/master/controlled/bot/guardian/tower/door origins, hate/family/reward and owner-meter. Isolated schedule proofs remain scoped, not full AI.', 'presentation': 'Natural NPC interval source mapping is wired for known original routes; Source brave is separate from haste with no invented commented S_NPCPack brave bit. Complete actual brave/status/perception/meter/gfx IO and transport lifetime, HPBAR/digits/kiringku/accepted effects/observers/proc_spr/damage suppression/temp gfx/login sync/resource routes and accepted/final cross-role proofs. No queued Frame/Context/map ownership; real backend/MySQL/game deferred until all code.'}
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
block='## 本次已更新的程式\n\n本輪接上自然Quest／Time NPC六個原魔法／召喚／地面／視覺排程的Source延遲選擇。原出生點從同一NPC／mobskill靜態snapshot複製兩個魔法基底與各actNo的type／skill／AreaWidth／AreaHeight四個int；只存immutable Map與record，不保留可變template／snapshot／actor／Context／Frame。\n\nSource先判斷mobskill的width或height大於零走範圍延遲；一般魔法僅target精確attack且skill不等於18才取攻擊魔法延遲，其他及召喚取輔助魔法延遲。skills target仍在原施法後排程階段讀取，不混入NPC出生快照。skills.id是獨立欄位，不要求等於skill_id；僅檢查原mobskill的NPC／skill與type。只接原delay資料，保留原MP／ready／reuse／召喚次數／nextAction與slow／haste／brave／wind／960預設／最小1；缺失或不相符的來源／NPC／技能／路由／type及未載入skills走原後備。\n\n187原方法獨立正規化inverse與1個精確委派constructor、JVM stack-map／315 no-init、24新契約（含skills.id與skill_id不同的修正回歸）＋2347同SHA繼承重驗、9000組凍結Source選擇比較、1440組實際原方法隔離排程與同位元重建通過。原出生mutation與兩種encounter記憶體還原保留相同不可變proof；83 Source檔／133既有bounded參考及新增契約／20 SQL class保留。這不是bound AI／HP／IO／Main／MySQL／game驗收。\n\n尚待補Source sticky ISASCAPE與反向移動／距離／targetClear，不能把atkspeed0一律當成禁止攻擊；custom-work變更、原trigger／RNG／施法時序、未知／手建／受控／bot／guardian工廠與完整AI／hate／perception／最後接收生命週期仍開放。全部58材料／252外控／其他生產端／成對鎖定／HPBAR／數字／奇古獸／正式0.6.71隔離合成及6處歷史current索引仍未完成。全部程式完成後才集中同SHA後端／中文MySQL真裝備角色唯讀／實機游標驗收；未部署、不啟動服務或連MySQL、不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 Source NPC魔法基底／mobskill路由續作：候選`{v['runtime']['sha256']}`，原出生不可變兩魔法基底／actNo路由，6個原排程點採精確area／target attack／18例外／summon選擇，保留MP／reuse／count／nextAction／原生命週期與記憶體proof。已移除初稿對skills.id等於skill_id的錯誤額外門檻，保留原mobskill身分檢查。187原方法inverse＋1constructor、315 no-init、24新＋2347同SHA重驗、9000 Source選擇／1440原方法排程與同位元重建；83 Source檔／133 bounded參考及新契約／20 SQL classes。Source脫逃／custom-work／原trigger施法／完整AI顯示與全Source及bound驗收仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-NPC-immutable-spell-bases-mobskill-routes-six-original-schedules-program-only',full_goal_achieved=False)
continuation['source_npc_spell_timing_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=187,new_game_runtime_cases=0,new_database_connections=0)))
