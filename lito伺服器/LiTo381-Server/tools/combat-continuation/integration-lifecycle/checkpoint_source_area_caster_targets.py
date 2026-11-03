"""Checkpoint original caster membership and same-Row PC calculation data; full Source remains open."""
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
latest=read(D/'source-area-caster-targets-build-current.json');repro=read(D/'source-area-caster-targets-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_area_caster_membership_contract_scenarios']==39 and build['additional_Source_LOS_branch_comparisons']==2080
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_area_caster_membership_contract_scenarios')==2173
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 2481 original methods'in log and 'PASS301'in log
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
for name in ['NativeSourceAreaCasterTargets.java','SourceAreaCasterTargetsMethods.java','SourceAreaCasterTargetsDelta.java','SourceAreaCasterTargetsLoad.java','SourceAreaCasterTargetsPlan.java','build_source_area_caster_targets.py','reproduce_source_area_caster_targets.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-area-caster-membership-pre-cost-same-Row-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'Two original typed PC list ports in nativeMaterialAreaSilenceBody/nativeAreaBattleSlowBody complete conditional missing caster membership before original MP/cost and per-target RNG. Only exact original caster with sealed first-discovery binding/life/pose, live role and existing original source action mutex can be added. Source TARGET_TO_ME/nonattack area rules and none/explicit original PC-primary ordering apply; existing selected caster/secondary order and duplicates are retained.',
 'Read-only invocation list/iterator carriers retain the same immutable Row and skill through two original cursor Row accessors and two original empty-global-metadata guards. The one typed-PC MassSlow intermediate iterator preserves existing self/owned-primary policy using that Row, without reloading metadata or applying an NPC filter to a PC list. Unmarked/different-skill/unscoped/WORLD retain original behavior. No new original fields/constructors/aliases, discovery, identity/life/pose adoption, queues, workers, TLS, SQL, connection or lock ownership. The helper has no global state; bounded carriers own only the original list/caster/Row/scalars.',
 '2481 original methods independently inverse-retained;301 no-init;39 new detached membership/first-life/row-reload/empty-metadata/skill-identity/typed-slow contracts plus2173 inherited/revalidated same-SHA checks and16128 exact frozen Source construction comparisons on actual membership adapter. Frozen replay byte identical;79 Source files/133 existing bounded excerpts plus explicit membership contract;20 SQL consumer classes retained. Original cost/RNG/HP/relationship/Ctrl/life/Main/WORLD body retained; bound runtime cases0.',
 'This is conditional original-caster membership on two PC lists, not full makeTargetList or all material roots. Sleep/healing/line/single roots, initial primary/ME-only validation, Source duplicate construction and primary/caster order across mixed NPC/player lists, login/proc bypasses, full relation/status/isTarget and complete Source/numerical/AI/output/formal composition remain open. Skill167 has Source area0 and its native area method name grants no area semantics.'
]
v['program_checks']=dict(JVM_no_initialization_classes=301,original_methods_independently_inverted=2481,byte_identical_frozen_rebuild=True,existing_changed_class_entries=3,new_helper_class_entries=3,new_bounded_list_iterator_carrier_classes=2,new_original_metadata_fields=0,new_original_cursor_constructor_siblings=0,original_constructor_ABI_and_generic_metadata_retained=True,pure_area_caster_membership_contract_scenarios=39,inherited_and_revalidated_same_SHA_pure_contract_scenarios=2173,additional_exact_Source_caster_membership_adapter_comparisons=16128,pre_cost_typed_PC_membership_ports=2,typed_PC_slow_iterator_port=1,original_cursor_Row_accessor_ports=2,original_cursor_empty_metadata_guard_ports=2,sealed_first_discovery_role_life_pose_and_original_source_mutex_required=True,source_Row_and_skill_frozen_across_PC_selection_and_calculation=True,full_target_list_membership_and_proc_login_paths_qualified=False,original_PC_empty_selected_list_consumption_retained=True,new_Context_Frame_registry_or_TLS_references=False,new_SELECTs=0,new_workers_TLS_registries=False,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True,bound_selection_cost_RNG_HP_IO_role_Main_cases=0)
for key in ['Source_area_caster_membership_contract','area_caster_target_port_census','target_construction_port_census','Source_target_construction_oracle','Source_target_construction_contract','target_mask_port_census','Source_target_mask_oracle','Source_target_mask_contract','doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['area_caster_'+key]=build[key]
v.update(area_caster_conditional_membership_before_cost_program_wired=True,area_caster_PC_selection_and_calculation_Row_skill_frozen=True,area_caster_full_makeTargetList_isTarget_and_all_roots_qualified=False)
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
v['remaining_work_groups'][1]['work']='Conditional missing caster membership is now wired on original Silence/MassSlow PC lists before original cost with sealed first-discovery/source mutex and same immutable Row/skill through original PC calculation paths, including empty global metadata reload. Complete Sleep/healing/other area/line/single roots, initial primary and full ME-only construction, exact duplicate construction and mixed NPC/player primary-caster order, login/proc bypasses and all58 materials/all applicable807 historical rows: request/item/bookmark/none/self and normal/Ctrl, clan/party/GM/PK under BATTLE, primary/secondary/LOS, ghost/zone/hidden/dead/type/immunity/corpse/status, unknown/manual factories, original cost/RNG/rollback and PC empty-list normal consumption. These program checks do not qualify full makeTargetList/isTarget or actual casts.'
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

沉默之殼與集體緩速術原PC選取清單新增2個資料接線：Source自施捷徑／非攻擊範圍規則需要caster時，在原MP／材料／每目標RNG前補入原施法者。只使用已封存的首次發現角色／生命／位置與既有施法者動作鎖，不新抓角色或生命；已有caster保留原順序與重複。明確PC主目標在caster之前，none則忽略敵對封包主目標。

兩個局部唯讀list／iterator carrier保留同一份不可變Row與skill，透過2個原cursor Row accessor、2個空metadata守衛及1個PC集體緩速中間iterator接線，避免重載混用；全域技能表變空時已建立清單仍用原Row。不同技能／未標記／WORLD保留原後備。未新增原欄位、constructor、別名、queue、worker、TLS、SQL、連線或鎖所有權；carrier只保留既有list／caster／Row與scalar。

2,481原方法inverse、301類no-init、39新增detached原生命／角色／位置／主目標順序／同Row重載／空表／技能身分／中間iterator檢查、16,128精確Source分支對實際membership adapter比較及同SHA2,173繼承／重驗檢查通過；凍結重建同位元。79 Source檔／133既有bounded參考及新增membership契約／20 SQL class保留。**真bound施法／成本／RNG／HP／角色／IO／Main／MySQL／game均未驗收**。

完整makeTargetList／isTarget、睡眠／補血／其他範圍／直線／單體root、最初主目標與完整ME-only建構、Source重複與跨NPC／PC主次順序、login／proc及完整Source數值狀態仍待補齊。風之枷鎖Source167的area為0，未依原生方法名稱擴成範圍技。

完整58材料／252外控／proc／已接受完成continuation／NPC工廠與AI／HPBAR／傷害數字／奇古獸／正式0.6.71隔離合成與6處歷史current索引仍開放。程式全部完成後集中同SHA後端、中文MySQL真裝備角色唯讀重載與實機游標驗收；不部署、不連實際MySQL、不寫Source／玩家SQL／schema／WAL。

'''
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 Source範圍caster清單續作：候選`{v['runtime']['sha256']}`，Silence／MassSlow原PC清單在成本前依Source條件補原caster，保留首次角色／生命／位置及原動作鎖；兩個局部唯讀carrier固定Row／skill，含空表重載與中間iterator，明確PC主目標先於caster。2,481 inverse、301 no-init、39新＋2173同SHA繼承／重驗、16128精確Source分支比較與同位元重建；79 Source檔／133既有參考及新契約／20 SQL class保留。完整目標建構／其餘root／全Source與真bound目標／HP／IO／Main／DB／game仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-area-caster-membership-pre-cost-same-Row-program-only',full_goal_achieved=False)
continuation['source_area_caster_membership_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=2481,new_game_runtime_cases=0,new_database_connections=0)))
