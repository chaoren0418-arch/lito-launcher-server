"""Checkpoint exact Source construction precedence on existing calculation cursors; no full Source qualification."""
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
latest=read(D/'source-target-construction-build-current.json');repro=read(D/'source-target-construction-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_target_construction_contract_scenarios']==22 and build['additional_Source_LOS_branch_comparisons']==2080
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_target_construction_contract_scenarios')==2151
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 19 original methods'in log and 'PASS298'in log
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
for name in ['NativeSourceTargetConstruction.java','SourceTargetConstructionMethods.java','SourceTargetConstructionDelta.java','SourceTargetConstructionLoad.java','SourceTargetConstructionPlan.java','SourceTargetMaskPlan.java','build_source_target_construction.py','reproduce_source_target_construction.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-target-construction-precedence-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'Four original immutable-Row/cursor data ports preserve exact makeTargetList construction precedence on existing probability and damage/healing calculation lists: TARGET_TO_ME self-only precedes class predicates except attack-bit and IMMUNE68; explicit nonattack area caster precedes isTarget self/class vetoes with exact TYPE_ATTACK64 and HEAL_ALL49 distinctions.',
 'Existing final Integer metadata carries normalized five mask bits plus two construction flags frozen from the same immutable Row. No new fields, constructors, wrappers, actor/Row/Context registry, queue, worker, TLS, SQL or connection ownership. Original selected list/order/duplicates and absent-caster behavior remain unchanged. This does not construct missing candidates or qualify complete makeTargetList, login/proc paths or full isTarget.',
 '19 original methods independently inverse-retained;298 no-init;22 new detached actual-cursor contracts plus2151 inherited/revalidated same-SHA checks and16128 exact frozen Source construction branch comparisons. Two earlier target-mask fixtures isolated from the ME shortcut. Frozen replay byte identical;79 Source files/133 existing bounded excerpts plus explicit construction contract;20 SQL consumer classes retained. Original admission/cost/RNG/HP/relationships/Ctrl/life/Main/WORLD retained; bound runtime cases0.'
]
v['program_checks']=dict(JVM_no_initialization_classes=298,original_methods_independently_inverted=19,byte_identical_frozen_rebuild=True,existing_changed_class_entries=4,new_helper_class_entries=1,new_original_metadata_fields=0,new_original_cursor_constructor_siblings=0,original_constructor_ABI_and_generic_metadata_retained=True,original_constructor_has_no_invented_mask=True,pure_target_construction_contract_scenarios=22,inherited_and_revalidated_same_SHA_pure_contract_scenarios=2151,additional_exact_Source_target_construction_comparisons=16128,corrected_prior_mask_only_fixture_isolation_cases=2,original_immutable_Row_and_predicate_ports=4,original_selected_candidates_only=True,full_target_list_membership_and_proc_login_paths_qualified=False,original_PC_empty_selected_list_consumption_retained=True,new_Row_Context_Frame_player_registry_references=False,new_SELECTs=0,new_workers_TLS_registries=False,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True,bound_selection_cost_RNG_HP_IO_role_Main_cases=0)
for key in ['target_construction_port_census','Source_target_construction_oracle','Source_target_construction_contract','target_mask_port_census','Source_target_mask_oracle','Source_target_mask_contract','doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['target_construction_'+key]=build[key]
v.update(target_construction_precedence_on_original_calculation_cursor_program_wired=True,target_construction_original_cost_RNG_Ctrl_competition_life_Main_WORLD_inverse_retained=True,target_construction_full_makeTargetList_isTarget_and_relation_privilege_mapping_qualified=False)
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
v['remaining_work_groups'][1]['work']='Original probability and damage/healing cursors now preserve Source TARGET_TO_ME and explicit nonattack area-caster construction precedence using frozen Row scalar metadata, together with PC/NPC/owned-Pet class-mask projection. Complete actual target-list membership including absent-caster construction, login/proc primary bypasses and all58 material skills/all applicable807 historical rows: request/item/bookmark/none/self and normal/Ctrl routes, clan/party/GM/PK mapping under BATTLE competition, primary/secondary/line/area/LOS, ghost/zone/hidden/dead/type/immunity/corpse/status, single roots and unknown/manual factory cases, original cost/RNG/rollback and PC empty-list normal consumption. Detached projections do not qualify full makeTargetList or isTarget.'
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
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),system_goal=previous['system_goal'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))

text=work.read_text('utf-8');text=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{v['runtime']['sha256']}`",text)
text=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",text)
artifact_paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']}
text=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:artifact_paths[m[1]],text)
a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
block='''## 本次已更新的程式

原機率與傷害／補血計算cursor新增4個資料接線，依同一份不可變Row固定Source目標清單建構旗標。TARGET_TO_ME在非attack-bit且非IMMUNE68時先保留已選取的施法者、排除其他候選；非攻擊範圍技的明確caster加入規則先於isTarget類別與self排除，保留精確TYPE_ATTACK64與HEAL_ALL49差異。

沿用既有Integer欄位，不新增欄位／constructor／wrapper／Row或角色保留。原陣列／順序／重複／缺少caster時不新增目標，以及成本／RNG／HP／競技／Ctrl／生命／Main／WORLD保持原方法逆向還原一致。此處只修正既有計算清單的篩選優先序，完整目標建構、login／proc primary捷徑、血盟／隊伍／GM／ghost／zone／免疫等仍待補齊。

19原方法inverse、298類no-init、22新增detached實際cursor檢查、16,128精確Source建構分支比較及同SHA2,151繼承／重驗檢查通過；修正2個舊mask測試隔離條件，凍結重建同位元。79 Source檔／133既有bounded參考及新增建構契約／20 SQL class保留。**真bound目標建構／成本／RNG／HP／角色／IO／Main／MySQL／game均未驗收**。

完整58材料／252外控／其餘Source數值狀態／proc／已接受完成continuation／NPC工廠與AI／HPBAR／傷害數字／奇古獸／正式0.6.71隔離合成與6處歷史current索引仍開放。程式全部完成後集中同SHA後端、中文MySQL真裝備角色唯讀重載與實機游標驗收；不部署、不連實際MySQL、不寫Source／玩家SQL／schema／WAL。

'''
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 Source目標建構優先序續作：候選`{v['runtime']['sha256']}`，原Row／cursor共4點保留TARGET_TO_ME與非攻擊area明確caster規則，再進入既有class-mask判定；沿用scalar metadata，不新增目標、欄位、角色或生命擷取。19 inverse、298 no-init、22新＋2151同SHA繼承／重驗與16128精確Source分支、同位元重建。79 Source檔／133既有參考與新增建構契約／20 SQL class保留。完整makeTargetList／isTarget／全Source與真bound目標／HP／IO／Main／DB／game仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-target-construction-precedence-program-only',full_goal_achieved=False)
continuation['source_target_construction_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=19,new_game_runtime_cases=0,new_database_connections=0)))
