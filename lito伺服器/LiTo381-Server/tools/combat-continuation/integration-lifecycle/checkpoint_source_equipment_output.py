"""Advance only equipment healing accepted-output evidence; preserve qualification."""
from pathlib import Path
from datetime import datetime, timezone
import json, hashlib, shutil, zipfile, re, subprocess, copy

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
latest=read(D/'source-equipment-output-build-current.json');repro=read(D/'source-equipment-output-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_equipment_output_contract_scenarios']==62 and build['additional_Source_LOS_branch_comparisons']==2080
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_equipment_output_contract_scenarios')==1856
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 2487 original methods'in log and 'PASS288'in log
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
for name in ['NativeSourceEquipmentOutput.java','SourceEquipmentOutputMethods.java','SourceEquipmentOutputDelta.java','SourceEquipmentOutputLoad.java','SourceEquipmentOutputPlan.java','build_source_equipment_output.py','reproduce_source_equipment_output.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-equipment-healing-accepted-output-twelve-phase-ports-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'Twelve original equipment healing snapshot constructor/post-original-HP seal/completion/Hub-discovery/packet-capture/final receiver data ports. Two object metadata slots on original Target and Frame preserve original private visibility/record schemas, formulas, HP/RNG/cost/competition/material/lock/WORLD bodies by independent inverse. No NPC/nonhealing68/stone/weapon effect completion is inferred.',
 'Canonical accepted participants seed original private healing Target; the first original HP commit seals its exact HP/MAX. Queued HP output drops unrelated caster reference and remains independent of later caster retirement. Graphics retains original caster proof, actor proc_spr and actor-centered X10. Snapshot/result metadata is never re-created from a later role or life.',
 'Original roomMembers lists/order/duplicates remain unchanged. First receiver role/life is frozen under original Hub discovery ownership; explicit observers remain role-only audience without gaining target eligibility or a new actor token. A Frame-local CAS chain resolves scopes by primitive thread id; out-of-order callbacks cannot overwrite each other. Scope close revokes authority before removal and clears the map on success/exception. Queued values retain no Frame, Context, audience map, snapshot or calculator.',
 'Original receiver monitor checks exact packet/value/id, target and receiver role/life, HP/MAX for meter, hpbar/proc_spr, DRAINING/STOPPED and Main. Existing completionFrame(null) clears producer Frame; a temporary transport-only Context cannot authorize or replace detached output metadata. No new worker, TLS, HP queue, SQL or connection ownership.',
 '2487 original methods inverse-retained;288 JVM no-init classes;62 new detached original-snapshot/seal/observer/packet/actual-negative-final-frame contracts plus1856 inherited same-SHA pure cases and frozen identical replay.76 frozen Source files,133 existing bounded excerpts plus exact proc contracts,20 SQL classes byte-retained. Actual bound healing/positive IO/role/Main/MySQL/game0.'
]
v['program_checks']=dict(JVM_no_initialization_classes=288,original_methods_independently_inverted=2487,byte_identical_frozen_rebuild=True,existing_changed_class_entries=4,new_helper_class_entries=6,new_original_completion_wrappers=1,pure_equipment_output_contract_scenarios=62,inherited_same_SHA_pure_contract_scenarios=1856,original_equipment_output_phase_ports=12,shared_Frame_CAS_scope_thread_id_isolation_pure_contract=True,scope_close_revokes_before_pop_program_wired=True,original_private_snapshot_seed_and_seal_ports=2,original_Hub_audience_ports=2,original_packet_capture_ports=1,original_receiver_monitor_ports=1,actual_unbound_negative_final_frame_cases=1,positive_bound_IO_cases=0,queued_Frame_Context_audience_map_calculator_references=False,queued_HP_unrelated_caster_reference=False,accepted_meter_caster_retirement_independence_program_wired=True,inherited_exact_Source_LOS_branch_comparisons=2080,inherited_Source_class_only_comparisons=91476,inherited_exact_Source_NPC_failure_comparisons=4608,inherited_exact_Source_factory_signed_comparisons=896,frozen_Source_NPC_class_declarations=32,frozen_Source_files=76,new_original_metadata_fields=2,new_SELECTs=0,new_workers_TLS_registries=False,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True)
for key in ['equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['equipment_output_'+key]=build[key]
v.update(equipment_output_twelve_original_phase_ports_program_wired=True,equipment_output_original_accepted_target_no_refresh_and_private_snapshot_preserved=True,equipment_output_detached_caster_independent_HP_final_receiver_program_wired=True,equipment_output_original_actor_graphics_control_and_observer_audience_program_wired=True,equipment_output_original_Ctrl_competition_cost_HP_Main_WORLD_inverse_retained=True,equipment_output_all_equipment_stone_weapon_Source_procs_complete=False,equipment_output_actual_bound_healing_HP_positive_IO_role_Main_qualified=False,equipment_output_full_Source_lifecycle_qualified=False)
v['remaining']=list(dict.fromkeys(['Equipment output next: positive bound healing/HPBAR/gfx final IO, role/death/revival/Main races and transport carrier lifetime. Nonhealing68/equipment/stone/weapon/NPC variants, full Source math/target/control and all58 material/252 control/NPC-AI/formal composition remain open. Twelve selected output ports and one actual unbound negative final-frame check do not qualify bound output.',*old['remaining']]))
for key in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[key]is False,key
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),system_goal=previous['system_goal'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))

text=work.read_text('utf-8');text=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{v['runtime']['sha256']}`",text)
text=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",text)
artifact_paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']}
text=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:artifact_paths[m[1]],text)
a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
block='''## 本次已更新的程式

裝備proc玩家補血的原private Target建立／原HP提交後封存／完成回呼／Hub觀眾發現／送包捕捉／最終接收端，共12個資料接線。Target／Frame各一個Object metadata欄位；原private可見性與record schema不變，原HP／公式／RNG／成本／Ctrl／競技／鎖／WORLD完整inverse。

已接受的HP／HPBAR借原目標生命與封存HP／MAX，延後封包移除無關施法者參考，後續施法者退役不改寫目標證明。特效仍檢查原施法者、Source proc_spr與原actor中心X10。觀眾在原Hub發現時固定第一份角色／生命；observer只保留接收權，不變成可受補目標，也不建立actor token。

Frame內CAS暫時鏈依primitive thread id辨識scope，交錯退出不互相覆蓋；先關閉授權，再移除並在正常／例外清除觀眾表；排隊值不留Frame／Context／觀眾map／calculator／snapshot。最終原receiver monitor再驗原packet／id／HP／MAX、觀眾角色生命、hpbar／proc_spr、STOPPED／Main，已接受DRAINING保留。沿用completionFrame(null)與暫時transport Context；新的transport metadata不能授權或刷新原輸出證明。

2487原方法inverse、288類no-init、62新detached snapshot／seal／observer／packet／實際負向final-frame契約＋同SHA1856繼承純契約及同位元重建通過。76 Source檔／133既有bounded參考與額外proc原方法、20 SQL class保留。**真bound healing／正向IO／角色／Main／DB／game均0**。

本輪只涵蓋玩家補血輸出；68非補血與其餘equipment／stone／weapon／NPC variants、完整58材料／252外控／Source數值目標／NPC-AI及正式0.6.71隔離合成仍待補齊。正向送包與角色／Main競態集中驗收延後；不部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

'''
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 裝備補血原接受輸出續作：候選`{v['runtime']['sha256']}`，原snapshot／HP後seal／完成／Hub觀眾／capture／final receiver共12點。HP封包只保留原目標，後續caster退役獨立；gfx保留原actor控制；observer保留原角色接收權。排隊值無Frame／Context／觀眾表／calculator；最後原receiver monitor驗packet／id／HP／MAX及生命／Main／STOPPED，DRAINING保留。2487 inverse、288 no-init、62新＋1856同SHA繼承純契約與同位元重建通過；76 Source檔／133既有參考／20 SQL class。僅一個實際unbound負向frame案，正向bound IO／角色／Main與全Source／58材料／其餘proc仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-equipment-healing-accepted-output-twelve-phase-ports-program-only',full_goal_achieved=False)
continuation['source_equipment_output_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=2487,new_game_runtime_cases=0,new_database_connections=0)))
