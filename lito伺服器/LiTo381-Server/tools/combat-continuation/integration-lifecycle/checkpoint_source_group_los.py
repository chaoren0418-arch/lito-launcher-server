"""Advance only original group-healing predicate program evidence; preserve qualified pointers."""
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
latest=read(D/'source-group-los-build-current.json');repro=read(D/'source-group-los-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_group_LOS_contract_scenarios']==27 and build['additional_Source_LOS_branch_comparisons']==2080
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_group_LOS_contract_scenarios')==1792
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 2468 original methods'in log and 'PASS275'in log
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
for name in ['SourceGroupLosMethods.java','SourceGroupLosDelta.java','SourceGroupLosLoad.java','SourceGroupLosPlan.java','build_source_group_los.py','reproduce_source_group_los.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-group-LOS-two-original-predicates-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'Two original group-healing stream predicate factories capture the original caster and GroupHealingSpell. Two bounded siblings preserve original PC/Snapshot coordinate reads, SAM types, clan/selection/order and WORLD geometry. Original lambda methods remain unchanged, and every original method including the root is independently inverse-retained.',
 'Existing field-free Source LOS policy receives the same original model skill key on these two remaining synchronous geometry paths. No new target resolution, fresh source member or Context capture. Closed/missing/foreign/unmarked/WORLD contexts retain original behavior; no new workers, registries, locks, HP or SQL paths.',
 'Frozen exact Source proc dispatch distinguishes direct doll L1Magic from stone TYPE_PROCS and equipment original-skill TYPE_GMBUFF setProcCast entries. The nonattack proc primary fallback remains separate. No blanket normal-cast LOS was added to raw dolls, and no proc production runtime is changed or newly qualified by this evidence.',
 '2468 original methods independently inverse-retained;275 JVM no-init classes;27 new actual-LambdaMetafactory/geometry/model/closed/WORLD detached contracts plus1792 inherited same-SHA pure cases. 2080 inherited exact Source LOS branch comparisons,91476 class-only,4608 failure and896 type comparisons retained.76 frozen Source files,133 bounded excerpts,20 byte-retained SQL classes and frozen identical replay. Actual bound cast/cost/RNG/HP/role/life/IO/Main/database/game0.'
]
v['program_checks']=dict(JVM_no_initialization_classes=275,original_methods_independently_inverted=2468,byte_identical_frozen_rebuild=True,existing_changed_class_entries=1,new_helper_class_entries=0,new_bounded_original_predicate_methods=2,pure_group_LOS_contract_scenarios=27,inherited_same_SHA_pure_contract_scenarios=1792,inherited_exact_Source_LOS_branch_comparisons=2080,original_synchronous_predicate_factories=2,original_geometry_sibling_ports=2,inherited_Source_class_only_comparisons=91476,inherited_exact_Source_NPC_failure_comparisons=4608,inherited_exact_Source_factory_signed_comparisons=896,frozen_Source_NPC_class_declarations=32,frozen_Source_files=76,new_original_fields=0,new_SELECTs=0,new_workers_TLS_registries=False,new_connection_ownership=False,new_scheduled_tasks=False,original_HP_FIFO_audience_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True)
for key in ['group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['group_LOS_'+key]=build[key]
v.update(group_LOS_two_original_synchronous_factories_program_wired=True,group_LOS_original_model_source_member_and_SAM_target_preserved=True,group_LOS_exact_Source_proc_dispatch_frozen_not_runtime_qualified=True,group_LOS_original_Ctrl_competition_cost_HP_life_Main_WORLD_inverse_retained=True,group_LOS_all_Source_LOS_and_proc_roots_complete=False,group_LOS_actual_bound_cast_cost_RNG_HP_life_role_IO_Main_qualified=False,group_LOS_full_Source_lifecycle_qualified=False)
v['remaining']=list(dict.fromkeys(['Group LOS next: stone/equipment proc upstream skill selection, acceptance/lifecycle and exact Source nonattack primary fallback; other untyped/line/area/NPC LOS consumers and full single-target invalid-class PC-empty consumption still open. Two synchronous group predicates do not qualify complete isTarget/self/zone/ghost/hidden/corpse/target bits/PK/status/math or bound role/life/Main behavior.',*old['remaining']]))
for key in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[key]is False,key
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),system_goal=previous['system_goal'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))

text=work.read_text('utf-8');text=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{v['runtime']['sha256']}`",text)
text=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",text)
artifact_paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']}
text=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:artifact_paths[m[1]],text)
a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
block='''## 本次已更新的程式

群體治癒原玩家與隨從兩個stream predicate factory，補攜原caster及GroupHealingSpell，使用兩個有界原lambda sibling。保留原SAM目標、PC／Snapshot位置讀取、篩選／血盟／排序、WORLD幾何與所有原方法。既有Source LOS policy借同一原skill key，不新抓Context／目標／生命；closed／foreign／unmarked／缺失scope沿原行為。

固定Source的proc分流另凍結三檔原方法：娃娃SKILL直接L1Magic，魔法石TYPE_PROCS與部分裝備TYPE_GMBUFF才setProcCast；非攻擊proc主目標還有獨立fallback。因此本輪不向娃娃加上一般施法LOS，也未宣稱proc生產端已接齊。

2468原方法inverse、275類no-init、27新actual-LambdaMetafactory契約＋同SHA1792繼承純契約及同位元重建通過。2080 LOS／91476 class-only／4608失敗／896型別比較繼承保留；76 Source檔、133 bounded參考及20 SQL class保留。**實際bound cast／cost／RNG／HP／role／life／IO／Main／DB／game均0**。

魔法石／裝備proc的完整上游與生命週期、其他單體／直線／範圍／NPC、完整isTarget及PC空目標原成本仍待補齊。58材料／252外控／NPC factory-AI、正式0.6.71隔離合成及集中實機驗收均未完成。不部署、不連MySQL、不寫Source／玩家SQL／schema／WAL。

'''
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 群補原predicate技能資料續作：候選`{v['runtime']['sha256']}`，兩個原stream factory補原caster／model，兩個有界原lambda sibling，保留PC／Snapshot原位置及WORLD幾何。2468原方法inverse、275 no-init、27新＋1792同SHA繼承純契約、同位元重建通過；76 Source檔／133參考／20 SQL class保留。三檔固定Source proc分流另存，不將娃娃直接L1Magic誤套正常施法LOS；魔法石／裝備proc與其餘完整Source／58材料／生命／Main／DB／game仍開放。先更新程式再集中驗收，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-group-LOS-two-original-predicates-program-only',full_goal_achieved=False)
continuation['source_group_LOS_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=2468,new_game_runtime_cases=0,new_database_connections=0)))
