"""Advance Source doll29 requested packet duration and exact final-byte proof; preserve scalar and lifecycle policies."""
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
latest=read(D/'source-doll-slow-wire-build-current.json');repro=read(D/'source-doll-slow-wire-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_doll_slow_wire_contract_scenarios']==28 and build['additional_Source_LOS_branch_comparisons']==2080
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_doll_slow_wire_contract_scenarios')==2092
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 11 original methods'in log and 'PASS296'in log
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
for name in ['NativeSourceDollSlowWire.java','SourceDollSlowWireMethods.java','SourceDollSlowWireDelta.java','SourceDollSlowWireLoad.java','SourceDollSlowWirePlan.java','SourceDollSlowPlan.java','SourceDollOutputPlan.java','build_source_doll_slow_wire.py','reproduce_source_doll_slow_wire.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-doll29-requested-broadcast-private-wire-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'Source UnifiedProcExecutor.handleSlowSkill constructs SkillHaste with the original requested buff duration for both private PC and broadcast. One original NativeSourceDollSlow packet data port now sends those low16 bits to all viewers, retaining the native opcode envelope and exact frozen Source constructor/writeD/writeC/writeH payload behavior.',
 'One original final-frame wrapper checks exact frozen opcode/id/mode/duration bytes plus the original receiver/status/epoch/role/life/Main/STOPPED/DRAINING policies. Private bytes are not rewritten to a later countdown; a permanent server timer caused by Source int-millisecond overflow may still carry the original requested writeH value. All other final-frame branches retain the original method. No new actor lookup, field, queue, worker, TLS, SQL or connection ownership.',
 '11 original methods inverse-retained;296 JVM no-init;28 new detached PC/natural/controlled packet/final-hook contracts with192 exact frozen Source payload comparisons, plus2092 inherited/revalidated same-SHA pure cases including25 original slow and15 output scalar checks; frozen byte-identical rebuild.79 Source files,133 existing bounded excerpts and added exact packet contract;20 SQL consumer classes byte-retained. Bound slow/cast/cost/RNG/HP/IO/role/Main/database/game0.',
 'The existing runtime already handles Source29 zero/long/int-overflow timers; older helper Java files were not used to infer runtime behavior. Natural NPC innate move-speed/AI/factory semantics remain separate: Source template brave speed is not an inferred haste flag.'
]
v['program_checks']=dict(JVM_no_initialization_classes=296,original_methods_independently_inverted=11,byte_identical_frozen_rebuild=True,existing_changed_class_entries=1,new_helper_class_entries=1,new_original_entry_wrappers=1,pure_doll_slow_wire_contract_scenarios=28,inherited_and_revalidated_same_SHA_pure_contract_scenarios=2092,revalidated_original_slow_scalar_contract_scenarios=25,revalidated_original_output_scalar_contract_scenarios=15,additional_exact_Source_payload_comparisons=192,original_packet_and_frame_ports=2,requested_private_and_broadcast_writeH_duration_program_wired=True,exact_frozen_speed_bytes_required_at_final_hook=True,final_hook_retimes_original_speed_bytes=False,underlying_timer_status_epoch_and_actor_mutation_bodies_inverse_retained=True,frozen_Source_files=79,new_original_metadata_fields=0,new_SELECTs=0,new_workers_TLS_registries=False,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True,bound_HP_IO_role_Main_cases=0)
for key in ['doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['doll_slow_wire_'+key]=build[key]
v.update(doll29_requested_broadcast_and_private_wire_program_wired=True,doll29_final_frozen_packet_and_original_epoch_lifetime_program_wired=True,doll29_full_bound_status_IO_and_Source_semantics_qualified=False)
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
v['remaining_work_groups'][4]['work']='Source29 zero/long/int-millisecond timer scalars are revalidated and its requested private/public writeH payload is wired. Complete natural NPC innate move-speed/AI metadata and other effective actor stats/INT/leverage/lawful/healing, NPC immunity/IMMUNE_TO_HARM/SKNPC/PLNPC/guardian/talent and all status mutation/consume ordering; packet comparisons do not qualify full skill semantics.'
v['remaining_work_groups'][6]['work']='Doll29 requested private/public duration and exact final packet/epoch/target/receiver proof are wired. Complete actual status/meter/gfx IO and transport-carrier lifetime, all HPBAR/digits/kiringku/accepted effects, observers and actor proc_spr, damage suppression/PC exclusion/temp gfx/RO login sync/multiple resource routes, and no queued Frame/Context/map ownership. Real backend/MySQL/game acceptance remains deferred until all code.'
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

娃娃技能29緩速的原packet資料點，改讓所有廣播與目標本人都帶Source原buffDuration的writeH低16位值。Source handleSlowSkill／S_SkillHaste建構子與writeD／writeC／writeH已凍結為可執行payload oracle；native opcode envelope保留。

原最終frame新增精確比對捕捉封包的opcode／id／mode／duration，沿用原receiver、target生命／角色、status epoch、範圍、DRAINING／STOPPED／Main守衛；不再把私人封包改寫成晚一刻的countdown。Source int秒數乘1000溢位可產生永久server timer，同時保留原請求秒數的writeH值；不將兩者混為同一數值。

原11方法inverse、296類no-init、28新detached PC／自然NPC／受控NPC／最終hook檢查與192精確Source payload比較通過；同SHA2,092繼承／重新驗證純契約包含原25個slow與15個output scalar案，凍結重建同位元。79 Source檔、133既有bounded參考與新增packet契約、20 SQL class保留。**真bound緩速／施放／成本／RNG／HP／角色／正向IO／Main／MySQL／game均未驗收**。

本輪核對現行JAR後確認duration0與過大秒數已有既有adapter；較舊輔助Java不是現行runtime。底層timer／status／epoch／HP／RNG／成本／Ctrl／競技／WORLD保持原方法；沒有新欄位、queue、worker、TLS、SQL或連線所有權。自然NPC天生move-speed／AI仍另待補齊，Source template brave speed不當成haste旗標。

完整58材料／252外控／其餘Source技能數值型別免疫／proc／已接受完成continuation／NPC工廠與AI／HPBAR／傷害數字／奇古獸／正式0.6.71隔離合成與6處歷史current索引仍開放。程式全部完成後集中同SHA後端、中文MySQL真裝備角色唯讀重載與實機游標驗收；不部署、不連實際MySQL、不寫Source／玩家SQL／schema／WAL。

'''
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 娃娃29 Source wire續作：候選`{v['runtime']['sha256']}`，原緩速packet改所有receiver均保留原buffDuration writeH；最終hook核對捕捉完整bytes與原角色／生命／status epoch／Main，不重寫私人countdown。11 inverse、296 no-init、28新＋2092同SHA繼承／重驗純契約與192精確Source payload比較、同位元重建；79 Source檔／133既有bounded參考與新增packet契約／20 SQL class。底層0／long／int-overflow timer既有行為保留；自然NPC innate speed／AI、全Source與真bound IO／Main／DB／game仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-doll29-requested-broadcast-private-wire-program-only',full_goal_achieved=False)
continuation['source_doll_slow_wire_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=11,new_game_runtime_cases=0,new_database_connections=0)))
