"""Advance only equipment accepted-participant program evidence; preserve qualification."""
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
latest=read(D/'source-equipment-proc-life-build-current.json');repro=read(D/'source-equipment-proc-life-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_equipment_proc_life_contract_scenarios']==37 and build['additional_Source_LOS_branch_comparisons']==2080
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_equipment_proc_life_contract_scenarios')==1819
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 23 original methods'in log and 'PASS277'in log
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
for name in ['NativeSourceEquipmentProcLife.java','SourceEquipmentProcLifeMethods.java','SourceEquipmentProcLifeDelta.java','SourceEquipmentProcLifeLoad.java','SourceEquipmentProcLifePlan.java','build_source_equipment_proc_life.py','reproduce_source_equipment_proc_life.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-equipment-proc-accepted-life-ten-phase-ports-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'Equipment-defense player healing and original status68 skills borrow canonical HP-boundary accepted defender/cause participants. Two entry wrappers reserve existing action pairs before original work; three original healing pre-RNG/locked guards, two accepted completion guards, two graphics producer guards and one original target-vital BooleanSupplier carry the old lives. Full original methods independently inverse-retained.',
 'A nested missing peer action lock is try-acquired before entering existing paired scope. Contention rejects before original body and before opening a scope whose close could drain a blocked peer. Both original action locks are required at original healing calculation and guarded status supplier. No new lock or completion lane; original receipts/publishing/cleanup retained.',
 'Immutable proof contains accepted participants, accepted value identity and existing lifetime/Hub only; never a Frame/Context/calculator/registry. Missing/replaced/dead/revived/retired/cross-Hub/closed proof cannot supply new life. Admitted DRAINING survives; STOPPED/Main close rejects. No normal cast range/LOS/reagent/cost is added to proc.',
 '23 original methods inverse-retained;277 JVM no-init classes;37 new detached actual accepted-value/real existing mutex/guarded supplier contracts plus1819 inherited same-SHA pure cases and frozen identical replay.76 frozen Source files,133 bounded references,20 byte-retained SQL classes. Actual bound proc/RNG/HP/role/life/final IO/Main/MySQL/game0.'
]
v['program_checks']=dict(JVM_no_initialization_classes=277,original_methods_independently_inverted=23,byte_identical_frozen_rebuild=True,existing_changed_class_entries=2,new_helper_class_entries=2,new_original_entry_wrappers=2,pure_equipment_proc_life_contract_scenarios=37,inherited_same_SHA_pure_contract_scenarios=1819,original_equipment_proc_phase_ports=10,original_healing_pre_RNG_locked_guards=3,original_accepted_completion_guards=2,original_graphics_producer_guards=2,original_locked_status_supplier=1,nested_contended_pair_pre_scope_rejection_pure_contract=True,inherited_exact_Source_LOS_branch_comparisons=2080,inherited_Source_class_only_comparisons=91476,inherited_exact_Source_NPC_failure_comparisons=4608,inherited_exact_Source_factory_signed_comparisons=896,frozen_Source_NPC_class_declarations=32,frozen_Source_files=76,new_original_fields=0,new_SELECTs=0,new_workers_TLS_registries=False,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True)
for key in ['equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['equipment_proc_life_'+key]=build[key]
v.update(equipment_proc_life_ten_original_phase_ports_program_wired=True,equipment_proc_life_original_accepted_participants_no_refresh_preserved=True,equipment_proc_life_existing_pair_and_locked_supplier_program_wired=True,equipment_proc_life_original_Ctrl_competition_cost_HP_Main_WORLD_inverse_retained=True,equipment_proc_life_all_equipment_stone_weapon_Source_procs_complete=False,equipment_proc_life_actual_bound_cast_cost_RNG_HP_life_role_final_IO_Main_qualified=False,equipment_proc_life_full_Source_lifecycle_qualified=False)
v['remaining']=list(dict.fromkeys(['Equipment proc next: full equipment/stone/weapon effect dispatch, numeric/target/controls and NPC/self/peer origins; final receiver HPBAR/gfx and actual accepted HP/role/life/Main races. Other shared paired-scope contention callers need separate audit; these ten phase ports do not qualify all proc producers or full58 materials/252 controls/NPC-AI/formal composition.',*old['remaining']]))
for key in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[key]is False,key
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),system_goal=previous['system_goal'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))

text=work.read_text('utf-8');text=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{v['runtime']['sha256']}`",text)
text=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",text)
artifact_paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']}
text=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:artifact_paths[m[1]],text)
a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
block='''## 本次已更新的程式

裝備受傷proc的玩家補血與原聖結界技能入口，補接原HP接受邊界的defender／cause角色與生命。兩個入口沿既有成對鎖；三個原補血RNG前／vital鎖內檢查、兩個完成回呼、兩個graphics生產端檢查及一個原status mutation supplier，共10點。原公式／RNG／HP／MP／旗標／receipt／鎖／Ctrl／競技／WORLD完整inverse。

巢狀成對呼叫先tryLock缺少的peer鎖，競爭時在開啟scope之前返回，避免無效果的拒絕請求在scope收尾等待peer drainer。原補血計算與status supplier都要求既有兩個action鎖；沒有新鎖／worker／completion lane。此修正覆蓋本adapter，其餘shared paired-scope caller仍待稽核。

proof只保留原accepted participants與既有lifetime／Hub，不留Frame／Context／calculator／registry。角色替換、死復活、原proof替換、跨Hub、STOPPED／Main關閉拒絕；已接受DRAINING保留。不加入一般施法的射程／LOS／扣料流程。

23原方法inverse、277類no-init、37新accepted-value／實際mutex／supplier純契約＋同SHA1819繼承契約及同位元重建通過。76 Source檔／133 bounded參考、20 SQL class保留。**實際bound proc／RNG／HP／role／life／最終IO／Main／DB／game均0**。

完整裝備／魔法石／武器proc dispatch與數值／目標／外控、NPC／self／peer來源及HPBAR／特效最終接收端仍待補齊；完整58材料／252外控／NPC factory-AI與正式0.6.71隔離合成未完成。程式全部完成後集中驗收，不部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

'''
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 裝備proc原接受生命續作：候選`{v['runtime']['sha256']}`，玩家補血與68原入口／RNG前／vital內／完成／graphics／status supplier共10點借原accepted participants。巢狀缺鎖先tryLock，拒絕前不開scope；不採新角色／生命，DRAINING保留、STOPPED／Main關閉拒絕。23原方法inverse、277 no-init、37新＋1819同SHA繼承純契約及同位元重建通過；76 Source檔／133參考／20 SQL class保留。其他shared scope競爭、完整equipment／stone／weapon proc及全Source／58材料／實際HP／最終IO／Main／DB／game仍開放。未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-equipment-proc-accepted-life-ten-phase-ports-program-only',full_goal_achieved=False)
continuation['source_equipment_proc_life_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=23,new_game_runtime_cases=0,new_database_connections=0)))
