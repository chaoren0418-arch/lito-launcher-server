"""Checkpoint original Sleep self/ME construction with borrowed first-life data; full Source remains open."""
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
latest=read(D/'source-sleep-construction-build-current.json');repro=read(D/'source-sleep-construction-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_sleep_construction_contract_scenarios']==37 and build['additional_Source_LOS_branch_comparisons']==2080
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_sleep_construction_contract_scenarios')==2212
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 2481 original methods'in log and 'PASS305'in log
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
for name in ['NativeSourceSleepConstruction.java','SourceSleepConstructionMethods.java','SourceSleepConstructionDelta.java','SourceSleepConstructionLoad.java','SourceSleepConstructionPlan.java','build_source_sleep_construction.py','reproduce_source_sleep_construction.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source-Sleep-self-ME-mixed-construction-frozen-row-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'Original nativeAreaBattleSleepBody freezes one existing immutable Row/skill and exact sealed first-discovery source Member into an invocation local. Frozen request/two phantom maps/shape/two LOS, ME-only enumeration bypass and mixed target construction preserve Source probability self-primary and eligible caster construction. Source ME uses only original caster identity and skips original secondary NPC-stat enumeration before normal costs/RNG. Original nonself primary moves first, then required caster, preserving secondary relative order and original duplicate wrappers.',
 'Two additional identity.player-proven PC competition ports permit only original caster self under the same Row/member/action-lock proof. Owner-Pet/companionOwner and other-player competition paths remain original. A primitive-only original ClientSession bridge constructs its existing private PatienceTarget/BattleSleepTarget records with original ID/pose/PC, no fresh role/life/pose or NPC authority. Root contains11 data/branch ports; two calculation factories have four Row/availability owner ports. No new original fields, one new record-factory method, four new helper/carrier classes.',
 'Local final Frozen/list/iterator carriers retain only immutable Row/skill, original source Member and original selected wrappers, never Context/Frame or a global actor registry. Original source mutex is required for construction; no new lock, SQL/connection, queue, worker or TLS ownership. Mixed iterator carries same skill/Row through original probability/damage-healing cursors including empty global reload; all unmarked/different-skill/WORLD paths delegate original adapters.',
 '2481 original instruction/control-flow bodies independently normalized and inverse-retained; rewritten stack maps forward-verified and305 classes JVM no-init.37 new detached original-record/source-life/self-primary/ME/mixed-cursor contracts plus2212 inherited/revalidated same-SHA checks and4032 exact frozen Source self/construction comparisons. Frozen replay byte identical;79 Source files/133 existing bounded excerpts plus explicit Sleep contract;20 SQL consumer classes retained. Bound cast/HP/role/IO/Main/database/game cases0.',
 'Original invisibility and other-player/native-status gates remain retained, pending complete Source ghost/GM/invisibility/status mapping. This is Sleep construction program wiring, not full isTarget/Source duplicate construction/proc/login/numerical semantics or all material roots. Healing/nature/other area/line/single roots and complete external controls/NPC-AI/accepted output/formal composition remain open.'
]
v['program_checks']=dict(JVM_no_initialization_classes=305,original_methods_independently_inverted=2481,original_instruction_and_control_flow_inverse_ignores_recomputed_stack_map_frames=True,forward_frozen_splice_and_JVM_stack_maps_verified=True,byte_identical_frozen_rebuild=True,existing_changed_class_entries=3,new_helper_class_entries=4,new_bounded_invocation_carrier_classes=3,new_original_metadata_fields=0,new_original_record_factory_methods=1,original_constructor_ABI_and_generic_metadata_retained=True,pure_sleep_construction_contract_scenarios=37,inherited_and_revalidated_same_SHA_pure_contract_scenarios=2212,additional_exact_Source_Sleep_self_construction_comparisons=4032,original_Sleep_root_ports=11,original_calculation_Row_availability_ports=4,original_identity_player_PC_self_competition_ports=2,original_companionOwner_and_other_player_competition_retained=True,original_invisibility_gates_retained=True,sealed_first_discovery_role_life_pose_and_original_source_mutex_required=True,ME_skips_original_secondary_NPC_stat_enumeration=True,source_Row_and_skill_frozen_across_request_construction_and_calculation=True,full_isTarget_and_duplicate_proc_login_modes_qualified=False,original_PC_empty_selected_list_consumption_retained=True,new_Context_Frame_registry_or_TLS_references=False,new_SELECTs=0,new_workers_TLS_registries=False,new_connection_ownership=False,new_scheduled_tasks=False,other_original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True,bound_selection_cost_RNG_HP_IO_role_Main_cases=0)
for key in ['Source_sleep_construction_contract','sleep_construction_port_census','Source_area_caster_membership_contract','area_caster_target_port_census','target_construction_port_census','Source_target_construction_oracle','Source_target_construction_contract','target_mask_port_census','Source_target_mask_oracle','Source_target_mask_contract','doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['sleep_'+key]=build[key]
v.update(Sleep_original_self_ME_mixed_construction_program_wired=True,Sleep_original_identity_player_self_ports_and_frozen_row_wired=True,Sleep_full_isTarget_status_duplicates_all_modes_qualified=False)
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
v['remaining_work_groups'][1]['work']='Sleep now has frozen original-member/Row request, both phantom maps, self shape, ME enumeration bypass, ordered mixed caster list, two typed identity.player self-competition and same-Row original calculation ports. Original invisibility/other-player gates remain retained. Complete Source ghost/GM/invisibility/status/permission and duplicate construction, full initial/nonself/ME applicability and normal/proc/login modes, healing/nature/other area/line/single roots and all58 materials/all applicable807 historical rows: request/item/bookmark/none/self/normal/Ctrl, clan/party/GM/PK under BATTLE, primary/secondary/LOS, dead/type/immunity/corpse/status, unknown/manual factories, original cost/RNG/rollback/PC empty-list consumption. Detached Sleep and prior Silence/MassSlow construction evidence does not qualify full Source or actual casts.'
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

睡眠原root固定同一份不可變Row／skill與首次發現的原施法者Member，接上封包自施投影、兩次phantom map、原主目標shape、兩次LOS、ME-only次要目標列舉跳躍及混合目標清單。Source機率技能自施可建立原私有PatienceTarget／BattleSleepTarget；ME-only略過原NPC屬性列舉，仍進入原成本與RNG。明確主目標先於caster，保留其餘目標相對順序與原重複wrapper。

成本提交與效果前另有2個競技資料接線，僅接受原identity.player確定的施法者自己；companionOwner／受控NPC擁有者及其他玩家仍走原競技規則。新增1個原ClientSession factory method只接收凍結ID／位置scalar與PC本身，建立既有私有record；不新抓角色、生命或NPC權限。原root共11點，兩個原計算factory共4點；未新增原欄位。

4個新helper／carrier類只保留不可變Row／skill、原source Member與已選取wrapper，不保留Context／Frame或全域角色registry；不新增鎖、SQL、連線、queue、worker或TLS所有權。原鎖與首次角色／生命／位置證據不足時不建立自施目標。同skill／Row傳至原計算cursor，含全域資料空表重載；不同skill／未標記／WORLD保留原後備。

2,481原instruction／control-flow方法經獨立正規化inverse，重算stack-map由正向凍結splice與JVM verifier核對；305類no-init、37新增detached原私有record／生命／自施／ME／mixed cursor檢查、4,032精確Source自施與建構分支比較、同SHA2,212繼承／重驗及同位元重建通過。79 Source檔／133既有bounded參考與新Sleep契約／20 SQL class保留。**真bound施法／成本／RNG／狀態／HP／角色／IO／Main／MySQL／game均未驗收**。

原invisible及其他玩家守衛維持；完整Source ghost／GM／invisibility／status／權限對應、重複建構／proc／login、補血／自然祝福／其餘範圍／直線／單體root仍待補齊。完整58材料／252外控／proc／已接受完成continuation／NPC工廠與AI／HPBAR／傷害數字／奇古獸／正式0.6.71隔離合成與6處歷史current索引仍開放。程式全部完成後集中同SHA後端、中文MySQL真裝備角色唯讀重載與實機游標驗收；不部署、不連實際MySQL、不寫Source／玩家SQL／schema／WAL。

'''
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 Source Sleep建構續作：候選`{v['runtime']['sha256']}`，原root11點、兩個原factory4點與1個scalar record factory，固定首次source Member／Row／skill，補self-primary／ME-only跳過NPC列舉／mixed主目標與caster順序；兩個identity.player self競技點保留其他玩家與companionOwner規則。2,481正規化instruction／control-flow inverse、stack-map正向與JVM核對、305 no-init、37新＋2212同SHA繼承／重驗、4032精確Source分支與同位元重建；79 Source檔／133既有參考及新契約／20 SQL class保留。完整Source狀態權限／其餘root與真bound施法／HP／IO／Main／DB／game仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-Sleep-self-ME-mixed-construction-frozen-row-program-only',full_goal_achieved=False)
continuation['source_sleep_construction_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=2481,new_game_runtime_cases=0,new_database_connections=0)))
