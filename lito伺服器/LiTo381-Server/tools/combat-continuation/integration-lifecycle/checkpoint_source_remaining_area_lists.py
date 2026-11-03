"""Checkpoint remaining PC collections and same-Row non-PC calculation data; full Source remains open."""
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
latest=read(D/'source-remaining-area-lists-build-current.json');repro=read(D/'source-remaining-area-lists-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_remaining_area_lists_contract_scenarios']==32 and build['additional_Source_LOS_branch_comparisons']==2080
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_remaining_area_lists_contract_scenarios')==2249
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 2482 original methods'in log and 'PASS309'in log
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
for name in ['NativeSourceRemainingAreaLists.java','SourceRemainingAreaListsMethods.java','SourceRemainingAreaListsDelta.java','SourceRemainingAreaListsLoad.java','SourceRemainingAreaListsPlan.java','build_source_remaining_area_lists.py','reproduce_source_remaining_area_lists.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-remaining-area-PC-NPC-calculation-Row-and-mutable-Nature-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'Four original pre-cost PC collection data ports on GroupHealing, Nature, targetedArea and centeredArea reuse original caster construction/first-discovery role-life-pose/source-mutex proof. Targeted area port precedes the entire bot/primary/empty disjunction, so a selected primary cannot bypass frozen metadata. Nature preserves its concrete mutable LinkedHashSet, order/dedup/direct mutation and iterator remove API.',
 'Fourteen original non-PC calculation iterator ports across these four plus original Silence/MassSlow borrow the same immutable Row/skill from the PC collection anchor, even for an empty-PC NPC-only cast. Generic iterator carriers retain only original iterator/Row/skill, never the PC anchor or Context/Frame/Member. Four original metadata accessor owner ports preserve all prior ordinary-PC/mixed-Sleep/unmarked/different-skill/WORLD adapter chains, including empty global reload.',
 'New PC collection boundaries dominate original MP debit and original MP debit dominates all fourteen non-PC calculation ports, including exception edges. Original target arrays/wrapper identities and role permission/life/cost/RNG/HP/output/Main bodies are inverse-retained after removing only these data ports. No new original fields/constructors/methods, role/life/pose capture, other-player grant, lock, SQL/connection, queue, worker, TLS or actor registry ownership. Four new helper/local-carrier classes only.',
 '2482 original normalized instruction/control-flow methods inverse-retained; forward frozen splice/JVM rewritten stack-map verification and309 no-init classes.32 new detached mutable set/API/role-life/actual PC-NPC cursor/mask differential/empty global metadata/NPC-only anchor cases plus2249 inherited/revalidated same-SHA checks. Frozen replay byte identical;79 Source files/133 existing bounded excerpts plus explicit remaining-area contract;20 SQL consumer classes retained. Bound healing/area/cost/RNG/HP/role/IO/Main/database/game cases0.',
 'This is original calculation-mask Row coherence across six roots, not a whole cast snapshot or complete Source skill implementation. Earlier target/failure/geometry and later numerical/status/proc consumers can have separate metadata paths. Full isTarget/ghost/GM/invisibility/permission/Source duplicate construction, wind/single/line/proc/login and all material/control/AI/output/formal and real acceptance remain open.'
]
v['program_checks']=dict(JVM_no_initialization_classes=309,original_methods_independently_inverted=2482,original_instruction_and_control_flow_inverse_ignores_recomputed_stack_map_frames=True,forward_frozen_splice_and_JVM_stack_maps_verified=True,byte_identical_frozen_rebuild=True,existing_changed_class_entries=3,new_helper_class_entries=4,new_bounded_collection_iterator_carrier_classes=3,new_original_metadata_fields=0,new_original_methods_or_constructors=0,original_constructor_ABI_and_generic_metadata_retained=True,pure_remaining_area_lists_contract_scenarios=32,inherited_and_revalidated_same_SHA_pure_contract_scenarios=2249,original_pre_cost_PC_collection_ports=4,original_non_PC_calculation_Row_ports=14,original_same_Row_calculation_roots=6,original_calculation_Row_availability_owner_ports=4,original_mutable_Nature_LinkedHashSet_order_dedup_direct_and_iterator_remove_preserved=True,PC_ports_dominate_original_MP_debit_including_exception_edges=True,original_MP_debit_dominates_non_PC_calculation_ports_including_exception_edges=True,empty_PC_NPC_only_anchor_supported=True,sealed_first_discovery_role_life_pose_and_original_source_mutex_required=True,calculation_Row_and_skill_frozen_across_original_PC_and_non_PC_lists=True,full_cast_selection_failure_geometry_numeric_status_metadata_snapshot_qualified=False,original_PC_empty_selected_list_consumption_policy_retained=True,new_Context_Frame_Member_anchor_registry_or_TLS_references=False,new_SELECTs=0,new_workers_TLS_registries=False,new_connection_ownership=False,new_scheduled_tasks=False,other_original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True,bound_selection_cost_RNG_HP_IO_role_Main_cases=0)
for key in ['Source_remaining_area_lists_contract','remaining_area_list_port_census','Source_sleep_construction_contract','sleep_construction_port_census','Source_area_caster_membership_contract','area_caster_target_port_census','target_construction_port_census','Source_target_construction_oracle','Source_target_construction_contract','target_mask_port_census','Source_target_mask_oracle','Source_target_mask_contract','doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['remaining_area_'+key]=build[key]
v.update(remaining_area_original_PC_collections_and_non_PC_calculation_Row_program_wired=True,remaining_area_mutable_Nature_set_API_and_empty_PC_anchor_preserved=True,remaining_area_full_cast_Source_metadata_and_all_skill_semantics_qualified=False)
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
v['remaining_work_groups'][1]['work']='Group/Nature/targeted/centered-area PC collections now freeze same Row/skill before costs, preserving mutable Nature set API;14 original non-PC calculation ports across these plus Silence/MassSlow use the same PC-anchor Row, including empty-PC NPC-only casts. Prior Sleep self/ME/mixed construction is retained. Complete earlier target/failure/geometry and later numeric/status/proc metadata paths, full Source ghost/GM/invisibility/status/permission/duplicate construction, initial/nonself/ME and normal/proc/login applicability, wind/other single/line roots and all58 materials/all applicable807 historical rows: item/bookmark/none/self/normal/Ctrl, clan/party/GM/PK under BATTLE, dead/type/immunity/corpse/status, unknown/manual factories and original cost/RNG/rollback/PC empty-list consumption. Calculation Row coherence is not full Source or actual cast acceptance.'
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

群體治癒、自然祝福、指定範圍及施法者中心範圍的原PC清單新增4個成本前資料接線，沿用既有首次角色／生命／位置及施法者動作鎖證據。指定範圍接在整段bot／主目標／空清單分支之前；自然祝福維持原可變LinkedHashSet型別、順序、去重、add／remove及iterator remove行為。

另有14個原非PC計算iterator資料接線，涵蓋上述4條與沉默之殼／集體緩速術，共6條root，使PC與非PC計算分類沿用同一份不可變Row／skill。原PC清單空白的NPC-only施法也能借用metadata；非PC carrier只保留原iterator／Row／skill，不保留PC anchor、Context／Frame／Member。4個原metadata accessor接線維持先前普通PC／睡眠mixed／未標記／不同skill／WORLD後備，含空表重載。

原PC接線支配MP扣除，MP扣除支配14個非PC計算接線，均包含exception edges。未新增原欄位／constructor／method、角色／生命／位置capture、其他角色權限、鎖、SQL／連線、queue、worker、TLS或全域actor registry所有權；僅4個helper／局部carrier類。

2,482原instruction／control-flow方法正規化inverse、重算stack-map正向與JVM核對、309類no-init、32新增detached可變set／生命／實際PC與NPC cursor／mask差分／空表／NPC-only anchor檢查、同SHA2,249繼承／重驗及同位元重建通過。79 Source檔／133既有bounded參考與新契約／20 SQL class保留。**真bound治癒／範圍／成本／RNG／HP／角色／IO／Main／MySQL／game均未驗收**。

這是6條root的計算分類Row一致性；前段目標／失敗／幾何與後段數值／狀態／proc仍可能有獨立metadata路徑，尚非完整施法快照。完整Source isTarget／ghost／GM／invisibility／權限／重複建構／wind／單體／直線／proc／login與58材料／252外控／生產端／NPC工廠AI／HPBAR／傷害數字／奇古獸／正式0.6.71隔離合成及6處歷史current索引仍開放。程式全部完成後集中同SHA後端、中文MySQL真裝備角色唯讀重載與實機游標驗收；不部署、不連實際MySQL、不寫Source／玩家SQL／schema／WAL。

'''
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 其餘範圍PC／NPC計算Row續作：候選`{v['runtime']['sha256']}`，Group／Nature／targeted／centered原PC清單4點，加14個非PC iterator跨6條root共用原Row／skill；自然祝福保留可變LinkedHashSet及iterator remove，空PC的NPC-only anchor有效。2,482正規化inverse、stack-map正向／JVM、309 no-init、32新＋2249同SHA繼承／重驗及同位元重建；79 Source檔／133既有參考與新契約／20 SQL class保留。完整前段選取／失敗／幾何與後段數值狀態／proc、全Source及真bound施法／HP／IO／Main／DB／game仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-remaining-area-PC-NPC-calculation-Row-program-only',full_goal_achieved=False)
continuation['source_remaining_area_lists_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=2482,new_game_runtime_cases=0,new_database_connections=0)))
