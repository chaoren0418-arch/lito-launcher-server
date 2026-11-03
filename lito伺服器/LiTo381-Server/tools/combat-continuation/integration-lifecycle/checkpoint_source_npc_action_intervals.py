"""Checkpoint immutable Source NPC brave and eight original AI schedules; full Source remains open."""
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
latest=read(D/'source-npc-action-intervals-build-current.json');repro=read(D/'source-npc-action-intervals-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_NPC_action_intervals_contract_scenarios']==13 and build['additional_exact_Source_NPC_action_interval_comparisons']==1200
assert build['actual_detached_original_NPC_spell_schedule_comparisons']==96 and build['actual_detached_original_NPC_normal_schedule_comparisons']==96
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_NPC_action_intervals_contract_scenarios')==2321
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 185 original methods'in log and 'PASS311'in log
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
for name in ['NativeSourceNpcActionIntervals.java','SourceNpcActionIntervalsMethods.java','SourceNpcActionIntervalsDelta.java','SourceNpcActionIntervalsLoad.java','SourceNpcActionIntervalsPlan.java','SourceNpcFactoryTypePlan.java','build_source_npc_action_intervals.py','reproduce_source_npc_action_intervals.py','audit_source_completion.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-NPC-immutable-brave-eight-AI-intervals-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Original Source NPC construction starts at moveSpeed0 and applies template brave separately from haste. One existing loaded-template row birth port freezes nullable final Boolean on the original scalar Stamp, with one new eight-argument delegating constructor. Old constructors remain unqualified and retain complete original native interval policy; original record components/equality/hash and actual memory restore proof remain retained.', 'Eight original natural Quest/Time schedules: two normal movement/attack data ports and six magic/summon/ground/visual base/brave ports. Qualified NPC status multiplier subset follows exact Source default960, slow/haste then brave then attack/spell wind, final minimum1. Original MP debit, readiness/reuse, summon counts, nextAction writes, locks/generation/life/Main and WORLD bodies independently inverse-retained. Brave is not haste; no invented S_NPCPack brave bit or final IO qualification.', '185 original normalized methods independently inverted plus exact new scalar constructor;311 no-init classes;13 new detached contracts plus2321 inherited/revalidated same-SHA checks;1200 exact frozen Source calcSleepTime/ActionIntervalCalculator comparisons and192 actual detached original schedules (96 spell,96 normal including legacy fallback). Original birth mutation and memory restore checked. Frozen replay byte identical;80 Source files,133 existing bounded excerpts plus new calculator contract,20 SQL classes retained.', 'Only existing natural NPC status multipliers are completed here. Full passispeed/atkspeed and sprite/template base selection, fast move/attack/braveX/elf-brave, other custom-work/status, source brave perception/final receiver, controlled/bot/guardian/factory/full AI/hate remain open. No bound cast/AI/cost/RNG/HP/role/IO/Main/MySQL/game qualification; all58 materials/252 controls/full Source/formal composition still open.']
v['program_checks']={'JVM_no_initialization_classes': 311, 'original_methods_independently_inverted': 185, 'new_exact_delegating_scalar_constructors': 1, 'original_instruction_and_control_flow_inverse_ignores_recomputed_stack_map_frames': True, 'forward_frozen_splice_and_JVM_stack_maps_verified': True, 'byte_identical_frozen_rebuild': True, 'existing_changed_class_entries': 4, 'new_helper_class_entries': 1, 'new_immutable_original_metadata_fields': 1, 'new_original_constructors': 1, 'pure_NPC_action_intervals_contract_scenarios': 13, 'inherited_and_revalidated_same_SHA_pure_contract_scenarios': 2321, 'exact_frozen_Source_NPC_interval_comparisons': 1200, 'actual_detached_original_spell_schedules': 96, 'actual_detached_original_normal_schedules': 96, 'original_AI_schedule_points': 8, 'original_MP_reuse_summon_count_and_nextAction_ownership_retained': True, 'original_memory_restore_keeps_exact_brave_proof': True, 'old_ctors_unqualified_original_native_fallback': True, 'Source_ctor_moveSpeed_zero_brave_separate_from_haste': True, 'all_Source_NPC_speed_AI_or_presentation_qualified': False, 'new_retained_actor_Context_Frame_Member_registry_or_TLS': False, 'new_SELECTs': 0, 'new_workers_TLS_registries': False, 'new_connection_ownership': False, 'new_scheduled_tasks': False, 'other_original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained': True, 'bound_selection_cost_RNG_HP_IO_role_Main_cases': 0}
for key in ['Source_NPC_action_interval_contract','Source_NPC_action_interval_oracle','npc_action_interval_port_census','Source_slow_failure_metadata_contract','slow_failure_metadata_port_census','Source_remaining_area_lists_contract','remaining_area_list_port_census','Source_sleep_construction_contract','sleep_construction_port_census','Source_area_caster_membership_contract','area_caster_target_port_census','target_construction_port_census','Source_target_construction_oracle','Source_target_construction_contract','target_mask_port_census','Source_target_mask_oracle','Source_target_mask_contract','doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['npc_interval_'+key]=build[key]
v.update(Source_NPC_immutable_brave_eight_AI_intervals_program_wired=True,Source_NPC_brave_not_haste=True,full_Source_NPC_AI_and_presentation_qualified=False)
v['goal_reset_receipt']=pin(D/'source-completion-goal-reset-current.json');verify(v['goal_reset_receipt'])
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
for group in v['remaining_work_groups']:
 if group['id']in {'math-and-status': 'Source29 private/public duration payload and timer scalars retained. Natural Quest/Time birth brave metadata plus eight existing AI status multiplier schedules are wired; ctor moveSpeed0 and brave separate from haste. Complete passispeed/atkspeed/sprite base mapping, fastMovable/fastAttackable/braveX/elfBrave and other status/custom-work, controlled/bot variants, effective actor INT/leverage/lawful/healing/immunity/guardian/talent and mutation/consume ordering. These interval checks do not qualify full Source skills or actual actors.', 'npc-factory-ai': 'Immutable Source template brave now travels through original birth and memory restore into eight natural Quest/Time schedule points; old unqualified constructors retain original native fallback. Complete factory missing/unknown policy, first static snapshot/reload/rollback/full attribute/custom-work, actual passispeed/atkspeed base mapping, companion/bot/guardian/tower/door/master/manual origins, AI/hate/family/reward and controlled owner-meter.32 Source classes and isolated schedules remain scoped evidence, not complete AI.', 'presentation': 'Source29 requested private/public speed duration and packet/epoch/target/receiver proof retained. Source NPC brave status is separate from haste; do not invent the commented S_NPCPack brave bit. Complete actual brave/status/perception/meter/gfx IO and transport lifetime, HPBAR/digits/kiringku/accepted effects/observers/proc_spr/damage suppression/temp gfx/login sync/resource routes. No queued Frame/Context/map ownership; real backend/MySQL/game deferred until all code.'}:group['work']={'math-and-status': 'Source29 private/public duration payload and timer scalars retained. Natural Quest/Time birth brave metadata plus eight existing AI status multiplier schedules are wired; ctor moveSpeed0 and brave separate from haste. Complete passispeed/atkspeed/sprite base mapping, fastMovable/fastAttackable/braveX/elfBrave and other status/custom-work, controlled/bot variants, effective actor INT/leverage/lawful/healing/immunity/guardian/talent and mutation/consume ordering. These interval checks do not qualify full Source skills or actual actors.', 'npc-factory-ai': 'Immutable Source template brave now travels through original birth and memory restore into eight natural Quest/Time schedule points; old unqualified constructors retain original native fallback. Complete factory missing/unknown policy, first static snapshot/reload/rollback/full attribute/custom-work, actual passispeed/atkspeed base mapping, companion/bot/guardian/tower/door/master/manual origins, AI/hate/family/reward and controlled owner-meter.32 Source classes and isolated schedules remain scoped evidence, not complete AI.', 'presentation': 'Source29 requested private/public speed duration and packet/epoch/target/receiver proof retained. Source NPC brave status is separate from haste; do not invent the commented S_NPCPack brave bit. Complete actual brave/status/perception/meter/gfx IO and transport lifetime, HPBAR/digits/kiringku/accepted effects/observers/proc_spr/damage suppression/temp gfx/login sync/resource routes. No queued Frame/Context/map ownership; real backend/MySQL/game deferred until all code.'}[group['id']]
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
block='## 本次已更新的程式\n\n依使用者重設範圍完成最新缺口盤點：8組程式缺口、正式0.6.71隔離合成，以及程式全部完成後的3組同SHA後端／中文MySQL唯讀／實機游標驗收。系統目標工具仍回報舊目標啟用並拒絕重建；本地另存重設範圍與拒絕證據，不把舊目標標完成。\n\n本輪補NPC出生勇敢狀態及8個自然Quest／Time原AI排程點。Source建構moveSpeed0，template brave與haste分開；原Stamp新增1個nullable final Boolean與1個委派constructor，舊constructor維持未標記原生後備。原出生／記憶體還原保留同一不可變proof，不持有可變template／actor／Context。\n\n兩個移動／攻擊與六個魔法／召喚／地面／視覺原排程，沿用原MP、ready／reuse、召喚次數與nextAction寫入；僅已標記自然NPC的既有狀態倍率採Source順序：slow／haste、brave、攻擊或施法wind，預設960與最小1。未將brave視為haste，也未發明Source S_NPCPack已註解的brave bit。\n\n185原方法獨立正規化inverse與1個精確委派constructor、JVM stack-map／311 no-init、13新契約與同SHA2321繼承重驗、1200組凍結Source公式比較、192組實際原方法隔離排程（96施法、96移動／攻擊含舊後備）、出生mutation／記憶體還原與同位元重建通過。80 Source檔／133既有bounded參考及新增公式契約／20 SQL class保留。這些不是bound AI／施法／HP／IO／Main／MySQL／game驗收。\n\n仍須補passispeed／atkspeed與sprite／template基底選擇、fast move／attack／braveX／elf-brave、其他status／custom-work、受控／bot／guardian及完整AI／hate、brave感知封包與最終接收生命週期，另有58材料／252外控／傷害生產端／成對鎖定／HPBAR／數字／奇古獸／正式隔離合成及6處歷史current索引。全部程式完成後再集中驗收；不部署、不連實際MySQL、不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 Source NPC勇敢／AI排程續作：候選`{v['runtime']['sha256']}`。出生不可變brave與八個原排程點接線，保留MP／reuse／count／nextAction；brave與haste分開。185原方法inverse＋1委派constructor、311 no-init、13新＋2321同SHA重驗、1200 Source公式／192原方法隔離排程與同位元重建；80 Source檔／133 bounded參考及新增契約／20 SQL classes。完整base／status／工廠AI／顯示及全Source與真bound驗收仍開放。使用者重設範圍已存本地，系統工具因舊目標仍啟用拒絕重建，未虛報完成；未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-NPC-immutable-brave-eight-original-AI-intervals-program-only',full_goal_achieved=False)
continuation['source_npc_action_intervals_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=185,new_game_runtime_cases=0,new_database_connections=0)))
