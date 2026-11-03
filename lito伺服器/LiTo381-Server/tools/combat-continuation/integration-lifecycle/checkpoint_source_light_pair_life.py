"""Advance shared Light pair synchronous task life and receipt evidence; preserve qualification."""
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
latest=read(D/'source-light-pair-life-build-current.json');repro=read(D/'source-light-pair-life-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_light_pair_life_contract_scenarios']==48 and build['additional_Source_LOS_branch_comparisons']==2080
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_light_pair_life_contract_scenarios')==1949
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 47 original methods'in log and 'PASS293'in log
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
for name in ['NativeSourceLightPairLife.java','SourceLightPairLifeMethods.java','SourceLightPairLifeDelta.java','SourceLightPairLifeLoad.java','SourceLightPairLifePlan.java','build_source_light_pair_life.py','reproduce_source_light_pair_life.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-shared-light-pair-first-life-task-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'One original NativeLightAction.commitPair pre-receipt/pre-lock Task data port. Immutable actor/binding/life/dead values borrow canonical accepted participants or original Context discovery; no-Context entry uses the original life-before-binding ingress. Missing earlier BATTLE discovery never falls through to fresh peer proof.',
 'Original Task.run rechecks those exact role/life/Hub values, existing two action locks, Main and STOPPED. Original reservation retains DRAINING admission. No blanket HP-positive, observer, pose or LOS restriction is added: corpse/resurrection/retirement callers retain original policy. The synchronous proof stores no Frame, Context, receipt, mutable member or map.',
 '47 original methods independently inverse-retained;293 JVM no-init;48 new detached proof/actual-entry/receipt-transfer/exception/both-orientation-contention/life-change-during-original-wait contracts plus1949 inherited same-SHA pure cases and byte-identical frozen replay. Eight actual unbound entry scenarios,76 frozen Source files/133 bounded excerpts and existing proc contracts,20 SQL consumer classes retained. Bound cast/cost/RNG/HP/IO/role/Main/database/game0.'
]
v['program_checks']=dict(JVM_no_initialization_classes=293,original_methods_independently_inverted=47,byte_identical_frozen_rebuild=True,existing_changed_class_entries=1,new_helper_class_entries=3,new_original_entry_wrappers=0,pure_light_pair_life_contract_scenarios=48,inherited_same_SHA_pure_contract_scenarios=1949,original_shared_task_data_ports=1,original_shared_direct_caller_sites=5,original_upstream_commitPair_static_edges=68,static_callers_are_locators_only=True,actual_unbound_original_entry_cases=8,actual_unbound_receipt_transfer_completion_cases=1,actual_unbound_life_change_during_pair_wait_cases=2,actual_unbound_rejected_contended_entry_cases=2,canonical_accepted_corpse_not_refreshed_detached_verified=True,synchronous_proof_Frame_Context_receipt_member_map_references=False,new_original_metadata_fields=0,new_SELECTs=0,new_workers_TLS_registries=False,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True,bound_HP_IO_role_Main_cases=0)
for key in ['light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['light_pair_life_'+key]=build[key]
v.update(shared_light_pair_life_pre_receipt_task_values_program_wired=True,shared_light_pair_life_original_Ctrl_competition_cost_HP_Main_WORLD_inverse_retained=True,shared_light_pair_life_all_callers_and_lifecycles_qualified=False)
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
v['remaining_work_groups'][0]['work']='Shared commitPair Task life values are now frozen at its entry, and original unbound receipt transfer/close and two-direction contention/wait cases are verified. Audit earlier outer waits, all custom helpers, lifecycle ownership across mutation and final receiver, death/revival/same-id/scene/owner changes, bounded memory and actual DRAINING/STOPPED/Main. The68 upstream static edges do not qualify complete Source behavior. Six inherited current-alias hash mismatches remain separate evidence work.'
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

for key in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[key]is False,key
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),system_goal=previous['system_goal'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))

text=work.read_text('utf-8');text=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{v['runtime']['sha256']}`",text)
text=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",text)
artifact_paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']}
text=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:artifact_paths[m[1]],text)
a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
block='''## 本次已更新的程式

共用 NativeLightAction.commitPair 增加原 Task 資料接線，在原 receipt 保留與鎖等待前固定雙方角色／生命值。優先借用原 HP 接受邊界的 canonical participant，或先前 Context 的 discovery；沒有 Context 才使用既有 life-before-binding ingress。既有 discovery 缺少角色時不建立新權限；已接受的死亡生命也不被後續復活值替換。

原 Task 執行前再核對原角色／生命／Hub、雙方既有 action lock、Main／STOPPED。DRAINING 是否可保留 receipt 仍由原 reservation 決定。未加入一律要求 HP>0、observer、位置或 LOS 的新規則，保留復活／退役與其他呼叫端原權限。同步新 proof 不保留 Frame／Context／receipt／mutable member／map，無新欄位、鎖域、worker、TLS、queue 或 SQL。

47 原方法 inverse、293 類 no-init、48 新離線檢查與同 SHA 1,949 繼承純契約通過；凍結重建同位元。新增測試實際走過 8 個隔離原入口情境：正常／自施、原 Batch 轉交 peer receipt 並在解鎖後完成、例外清理、雙向拒絕競爭，以及原鎖等待期間任一角色更換生命。receipt 計數均歸零；76 Source 檔／133 bounded 參考與 proc 方法／20 SQL class 保留。**真 bound cast／成本／RNG／HP／角色／正向 IO／Main／MySQL／game 均未驗收**。

68 個上游 commitPair 呼叫點僅定位。此入口檢查不取代更早的外層等待、實際 mutation／接受／最終接收端及其他 helper 的生命證明；完整58材料／252外控／Source數值型別免疫／proc／NPC工廠與AI／HPBAR／傷害數字／奇古獸／正式0.6.71隔離合成仍開放。先前6處歷史current索引雜湊落差另列待核；沒有採用它們建立新正式基準。

程式全部完成後集中同SHA後端、中文MySQL真裝備角色唯讀重載與實機游標驗收。競技／Ctrl與原成本／RNG／回滾、DRAINING／STOPPED／Main限制保留；不部署、不連實際MySQL、不寫Source／玩家SQL／schema／WAL。

'''
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 共用commitPair生命接線：候選`{v['runtime']['sha256']}`，原receipt／鎖等待前固定Task雙方角色生命；借canonical accepted或既有discovery，缺少舊角色不刷新，保留corpse／復活／退役原權限。47 inverse、293 no-init、48新＋1949同SHA繼承純契約與同位元重建；8個隔離原入口案確認receipt轉交／清理／雙向競爭／鎖等待換生命。68上游edge只定位，真bound HP／角色／IO／Main／DB／game0，完整Source與6處歷史索引仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-shared-light-pair-first-life-task-program-only',full_goal_achieved=False)
continuation['source_light_pair_life_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=47,new_game_runtime_cases=0,new_database_connections=0)))
