"""Advance Source target mask frozen Row metadata on original calculation cursors; preserve admission and relationship authority."""
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
latest=read(D/'source-target-mask-build-current.json');repro=read(D/'source-target-mask-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_target_mask_contract_scenarios']==29 and build['additional_Source_LOS_branch_comparisons']==2080
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_target_mask_contract_scenarios')==2122
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 17 original methods'in log and 'PASS297'in log
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
for name in ['NativeSourceTargetMask.java','SourceTargetMaskMethods.java','SourceTargetMaskDelta.java','SourceTargetMaskLoad.java','SourceTargetMaskPlan.java','SourceDamageHealingSelectionPlan.java','build_source_target_mask.py','reproduce_source_target_mask.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-target-mask-frozen-row-calculation-projection-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'Four original immutable-Row/cursor predicate data ports on probability and damage/healing calculation lists. Source targetTo PC/NPC/owned-Pet projection, exact TYPE_ATTACK self exclusion, HEAL_ALL49 self exclusion and PC/clan/party self shortcut follow original class clauses, after original admission/cost and before original per-target RNG.',
 'Two final Integer cursor fields and constructor siblings freeze mask with the same original immutable Row type/none. Original constructors remain unmarked with exact original behavior; sibling generic metadata preserves the correct six-argument ABI. Reload cannot replace a mask while old type/none remains. Original selected arrays/wrapper objects/order/duplicates/application/cost/RNG/HP/role/life/Main/WORLD bodies are inverse-retained.',
 'Original relationship/competition predicates remain authoritative; no new clan/party/GM grant, Source membership query, discovery or actor capture. Earlier absolute-barrier/hidden-detection returns and unknown factory proof retain original guard/fallback. Stateless helper and scalar boxes add no Frame/Context/Row/player registry, queue, worker, SQL or connection ownership.',
 '17 original methods inverse-retained;297 no-init;29 new detached actual-cursor/self/owned-Pet/primary/reload/constructor-metadata contracts with30720 exact frozen Source projected branch comparisons plus2122 inherited/revalidated same-SHA checks, including two additional per-field schema assertions. One earlier class-only attack-self expectation corrected against Source.79 Source files/133 existing bounded excerpts plus explicit projection contract;20 SQL consumer classes retained. Bound selection/cost/RNG/HP/IO/role/Main/database/game0.'
]
v['program_checks']=dict(JVM_no_initialization_classes=297,original_methods_independently_inverted=17,byte_identical_frozen_rebuild=True,existing_changed_class_entries=4,new_helper_class_entries=1,new_original_metadata_fields=2,new_original_cursor_constructor_siblings=2,original_constructor_ABI_and_generic_metadata_retained=True,original_constructor_has_no_invented_mask=True,pure_target_mask_contract_scenarios=29,inherited_and_revalidated_same_SHA_pure_contract_scenarios=2122,additional_inherited_per_field_schema_checks=2,additional_exact_Source_target_mask_projection_comparisons=30720,corrected_prior_class_only_attack_self_expectations=1,original_immutable_Row_and_predicate_ports=4,original_PC_empty_selected_list_consumption_retained=True,new_Row_Context_Frame_player_registry_references=False,new_SELECTs=0,new_workers_TLS_registries=False,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True,bound_selection_cost_RNG_HP_IO_role_Main_cases=0)
for key in ['target_mask_port_census','Source_target_mask_oracle','Source_target_mask_contract','doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['target_mask_'+key]=build[key]
v.update(target_mask_frozen_original_Row_and_calculation_cursor_program_wired=True,target_mask_original_cost_RNG_Ctrl_competition_life_Main_WORLD_inverse_retained=True,target_mask_full_isTarget_and_relation_privilege_mapping_qualified=False)
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
v['remaining_work_groups'][1]['work']='Source PC/NPC/owned-Pet class-mask projection and exact self/HEAL_ALL rules are now wired on original probability and damage/healing calculation iterators with frozen Row metadata. Complete all58 material skills/all applicable807 historical rows: all request/item/bookmark/none/self and normal/Ctrl routes, exact clan/party/GM/PK mapping under BATTLE competition, primary/secondary/line/area/LOS, ghost/zone/hidden/dead/type/immunity/corpse/status, single roots and unknown/manual factory cases, original cost/RNG/rollback and PC empty-list normal consumption. Projected comparisons do not qualify full isTarget.'
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

原機率與傷害／補血計算iterator新增4個資料接線：原Row的targetTo與原type／none一起固定到cursor，依Source投影PC／NPC／自有Pet／Summon類別，並保留self捷徑、精確TYPE_ATTACK=64排除自己與HEAL_ALL49排除自己。仍在原成本／MP扣除後、原每目標RNG前過濾；PC最後目標清單空白仍維持原正常消耗。

兩個cursor各新增一個final Integer與一個ctor sibling；原constructor保留null metadata及完整舊行為，新的generic參數簽章符合6參數ABI。後續靜態資料重載不會混用新mask與舊type／none；原選取陣列／wrapper／順序／重複／HP套用與原生命守衛不變。血盟／隊伍／GM／競技關係仍由原已選取候選的守衛決定，沒有新增權限或查詢；更早AB／hidden detection與未知工廠proof保留原守衛／後備。

17原方法inverse、297類no-init、29新detached實際cursor／self／own-Pet／primary／reload／constructor metadata檢查、30,720精確Source分類分支比較通過；同SHA2,122繼承／重驗檢查包含2個新增逐欄位schema斷言，凍結重建同位元。依Source修正1個舊class-only測試預期：none TYPE_ATTACK不再把caster列入計算。79 Source檔／133既有bounded參考與新增projection契約／20 SQL class保留。

新metadata只含兩個不可變scalar box；沒有新增Row／Frame／Context／player registry保留、queue、worker、TLS、SQL或連線所有權。**真bound選取／成本／RNG／HP／角色／正向IO／Main／MySQL／game均未驗收**。此為class-mask投影，完整isTarget／關係權限／ghost／zone／hidden／dead／免疫與single roots仍待完成。

完整58材料／252外控／其餘Source數值狀態／proc／已接受完成continuation／NPC工廠與AI／HPBAR／傷害數字／奇古獸／正式0.6.71隔離合成與6處歷史current索引仍開放。程式全部完成後集中同SHA後端、中文MySQL真裝備角色唯讀重載與實機游標驗收；不部署、不連實際MySQL、不寫Source／玩家SQL／schema／WAL。

'''
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 Source target_to cursor續作：候選`{v['runtime']['sha256']}`，機率與傷害／補血原Row與predicate共4點，兩個final Integer／ctor sibling固定targetTo與原type／none，重載不混用；原ctor與generic ABI保留。PC／NPC／own-Pet／self／HEAL_ALL投影在原成本後、RNG前，保留既有關係／競技／Ctrl／生命／WORLD。17 inverse、297 no-init、29新＋2122同SHA繼承／重驗檢查與30720精確Source投影、同位元重建；修正1個舊class-only攻擊self預期。79 Source檔／133既有參考與新增projection契約／20 SQL class。完整isTarget與全Source、真bound選取／HP／IO／Main／DB／game仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-target-mask-frozen-row-calculation-projection-program-only',full_goal_achieved=False)
continuation['source_target_mask_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=17,new_game_runtime_cases=0,new_database_connections=0)))
