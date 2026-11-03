"""Advance two typed pre-commit outer producer ports; preserve accepted-completion policies."""
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
latest=read(D/'source-outer-pair-build-current.json');repro=read(D/'source-outer-pair-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_outer_pair_contract_scenarios']==55 and build['additional_Source_LOS_branch_comparisons']==2080
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_outer_pair_contract_scenarios')==1997
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 33 original methods'in log and 'PASS294'in log
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
for name in ['NativeSourceOuterPair.java','SourceOuterPairMethods.java','SourceOuterPairDelta.java','SourceOuterPairLoad.java','SourceOuterPairPlan.java','build_source_outer_pair.py','reproduce_source_outer_pair.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-two-precommit-outer-pair-contention-first-life-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'Two typed original pre-commit invocation ports only: NativeSourceDollAction.enter and NativeSourceKiringkuAdmission.owned. Their original OPEN/cost/RNG/HP/proc guards and null-Aim fallback remain inverse-retained. No global NativeSourcePlayerActionLock mutation or blanket policy for accepted PC/NPC callbacks.',
 'Nested missing mutex is tried before discovery/scope registration. Successful calls freeze immutable original first/accepted actor values before their original wait and recheck before their original Runnable. Exact acquired mutex is released; the original parent scope owns completion drain. Never begin a peer drain while the caller still owns either action mutex. No new fields, queue, worker, TLS, SQL or connection ownership.',
 '33 original methods inverse-retained;294 no-init;55 new detached adapter/parent-receipt/contention/life-wait/original-DRAINING checks plus1997 inherited same-SHA pure cases and frozen byte-identical replay. Four actual unbound typed Aim negative entry cases;positive bound doll/Aim casts0.76 frozen Source files/133 bounded excerpts and proc contracts;20 SQL consumer classes retained. Bound cast/cost/RNG/HP/IO/role/Main/database/game0.',
 'A green broad common-wrapper prototype was rejected for promotion after semantic audit:36 original callers include already accepted completions, which require their original continuation/output policies rather than blanket contention rejection or two-actor liveness. Its immutable candidate/evidence is retained separately and is not the authoritative runtime.'
]
v['program_checks']=dict(JVM_no_initialization_classes=294,original_methods_independently_inverted=33,byte_identical_frozen_rebuild=True,existing_changed_class_entries=2,new_helper_class_entries=1,new_original_entry_wrappers=0,pure_outer_pair_contract_scenarios=55,inherited_same_SHA_pure_contract_scenarios=1997,typed_precommit_data_ports=2,original_source_pair_api_byte_identical=True,accepted_completion_classes_outside_selected_ports_byte_identical=True,actual_unbound_typed_negative_Aim_entry_cases=4,actual_unbound_original_null_Aim_fallback_cases=1,actual_unbound_original_Aim_DRAINING_veto_cases=1,pure_original_parent_scope_receipt_completion_case=1,positive_bound_doll_Aim_cast_cases=0,new_original_metadata_fields=0,new_SELECTs=0,new_workers_TLS_registries=False,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True,bound_HP_IO_role_Main_cases=0)
for key in ['outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['outer_pair_'+key]=build[key]
v.update(two_precommit_outer_pair_contention_first_life_program_wired=True,two_precommit_outer_pair_original_Ctrl_competition_cost_HP_Main_WORLD_inverse_retained=True,all_outer_producer_and_accepted_completion_paths_complete=False)
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
v['remaining_work_groups'][0]['work']='Two typed pre-commit outer pairs now reject nested contention before scope registration and retain original life across their own wait. Complete phase-specific accepted PC/NPC continuation/commit/final-output policies without blanket rejection or caster dependency; audit manual action-lock holders without parent scopes, identity-hash tie monitor paths, all earlier waits, role/death/revival/same-id/scene/owner changes, bounded memory and actual Main/DRAINING/STOPPED. Six inherited current-alias mismatches remain separate evidence work.'
v['remaining']=[x['work']for x in v['remaining_work_groups']]
v['historical_remaining_preserved_in']=previous['manifest']
prototype=read(D/'source-outer-pair-builds/20261025T094605006785Z/manifest.json');verify(prototype);assert prototype['runtime']!=v['runtime']
write(run/'rejected-common-wrapper-prototype.json',dict(status='not-promoted-after-phase-semantic-audit',runtime=prototype['runtime'],build=pin(D/'source-outer-pair-builds/20261025T094605006785Z/manifest.json'),reason='The36 original outer callers include already accepted PC/NPC completion work. Green unbound tests do not prove that blanket nested rejection or generic caster-life gating preserves required accepted effects.',selected_runtime=v['runtime'],formal_deployment_changed=False,qualified_for_release=False))
v['unpromoted_common_wrapper_prototype']=pin(run/'rejected-common-wrapper-prototype.json')
with zipfile.ZipFile(build['base']['path'])as parent_jar,zipfile.ZipFile(v['runtime']['path'])as selected_jar:
 retained=['NativeSourcePlayerActionLock','NativeSourceDollAccepted','NativeSourceNpcMagicEffects','NativeSourcePhysicalNpc','NativeSourcePoisonNpc','NativeAcceptedAreaFreezeMutation','NativeAcceptedFreezeMutation','NativeAcceptedShockMutation']
 for name in retained:assert parent_jar.read('local/lito381/server/'+name+'.class')==selected_jar.read('local/lito381/server/'+name+'.class'),name
v['global_pair_and_selected_accepted_class_bytes_retained']=retained

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

娃娃物理攻擊入口 NativeSourceDollAction.enter 與非null奇古獸 Aim 的 owned，各替換一個原成對鎖呼叫。巢狀呼叫已持一方鎖時，先 tryLock 另一把原鎖；競爭失敗在 discovery／scope 建立前返回。成功先固定原 first／accepted 角色生命，再沿用原 ordered pair／scope，執行前核對原生命／角色／鎖。釋放實際取得的同一把鎖；原 parent scope 管理完成回呼，仍持任何一方鎖時不啟動 peer drain。

原33方法、OPEN／Ctrl／競技／成本／RNG／HP／proc／Main／WORLD及null Aim後備完整inverse。全域成對鎖API與已接受完成回呼class均保持原位元。沒有新增欄位／queue／worker／TLS／SQL或連線所有權；新同步proof沿用既有不可變值，不增加Frame／Context／receipt保留。

294類no-init、55新離線adapter／parent receipt／雙向競爭／等待換生命／原DRAINING檢查，與同SHA1,997繼承純契約、同位元重建通過。包含4個實際隔離Aim負向入口，以及原null Aim後備與原OPEN判定在DRAINING的拒絕；**正向bound娃娃／Aim施放、成本／RNG／HP／角色／IO／Main／MySQL／game均未驗收**。76 Source檔／133 bounded參考與proc方法／20 SQL class保留。

原36個outer caller混合新提交與已接受完成工作。曾通過純測試的全域wrapper原型已因階段語意不足排除，另保留immutable紀錄，沒有升為候選。已接受PC／NPC完成回呼須依原continuation與輸出政策處理，不能一律在鎖忙時丟棄，也不能新增不適用的caster生命依賴。

接續補accepted continuation／最終接收端、沒有parent scope的手動鎖持有者、identity-hash tie monitor與更早等待；完整58材料／252外控／Source數值型別免疫／proc／NPC工廠與AI／HPBAR／傷害數字／奇古獸／正式0.6.71隔離合成及6處歷史current索引仍開放。程式全部完成後集中同SHA後端、中文MySQL真裝備角色唯讀重載與實機游標驗收；不部署、不連實際MySQL、不寫Source／玩家SQL／schema／WAL。

'''
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 兩處pre-commit outer pair：候選`{v['runtime']['sha256']}`，娃娃enter／nonnull奇古獸Aim owned各一個原呼叫接線；鎖競爭先於discovery／scope拒絕，成功保留first-life與原ordered scope／OPEN／成本／RNG／HP／Ctrl／WORLD，parent scope管理完成排空。33 inverse、294 no-init、55新＋1997同SHA繼承純契約與同位元重建；4個隔離Aim負向入口，正向bound施放／HP／IO／Main／DB／game0。全域pair與已接受回呼class byte-retained；廣泛wrapper原型因未證明accepted語意而未升候選。其餘accepted continuation／手動鎖／tie monitor／完整Source仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-two-precommit-outer-pair-contention-first-life-program-only',full_goal_achieved=False)
continuation['source_outer_pair_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=33,new_game_runtime_cases=0,new_database_connections=0)))
