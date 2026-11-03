"""Checkpoint Source76 NPC selection/failure/distance/visual metadata; full Source remains open."""
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
latest=read(D/'source-slow-failure-metadata-build-current.json');repro=read(D/'source-slow-failure-metadata-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build=read(Path(latest['manifest']['path']));rep=read(Path(repro['manifest']['path']));inv=read(Path(inventory['manifest']['path']));contract=read(Path(contracts['manifest']['path']))
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert build['pure_slow_failure_metadata_contract_scenarios']==40 and build['additional_Source_LOS_branch_comparisons']==2080
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_slow_failure_metadata_contract_scenarios')==2281
log=Path(build['log']['path']).read_text('utf-8');assert 'inverse: 2471 original methods'in log and 'PASS310'in log
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
for name in ['NativeSourceSlowFailureMetadata.java','SourceSlowFailureMetadataMethods.java','SourceSlowFailureMetadataDelta.java','SourceSlowFailureMetadataLoad.java','SourceSlowFailureMetadataPlan.java','build_source_slow_failure_metadata.py','reproduce_source_slow_failure_metadata.py','audit_source_next_producers.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)

v=copy.deepcopy(old);v.update(status='program-updated-Source76-NPC-selection-failure-distance-visual-frozen-Row-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'Seven original Source76 root data ports reuse its existing pre-cost PC-anchor immutable Row/skill for two selectionErase predicates, two original NPC calculation filters, one accepted visual count and two NPC geometry captures. Exact type1/4 NPC failure gating and original self-primary proof no longer read a later global Row that could clear eligible NPCs. Existing factory-birth/IsErase proof and original NativeSourceNpcTargetFailure.Cursor are retained.',
 'Two complete Quest/Time geometry lambda siblings carry only the original PC collection anchor through synchronous Stream.filter. Original receiver/NPC argument/coordinates/areaRadius and GameHub NPC permission bodies remain independently inverse-retained; only signed-distance metadata gains that anchor. Unmarked/different-caster/WORLD delegates original behavior. No new original fields/constructors, role/life/pose capture, NPC grant, lock/SQL/connection/queue/worker/TLS/actor registry ownership; one field-free helper and two original-owner lambda siblings only.',
 '2471 original normalized instruction/control-flow methods plus two complete lambda sibling bodies independently inverted; forward frozen splice/JVM stack-map verification and310 no-init classes.40 new detached original NPC-cursor/selection/type/self-primary/visual/geometry/private-lambda-metafactory-linkage cases plus2281 inherited/revalidated same-SHA checks and99 original-native signed-distance boundary/overflow/reload comparisons. Frozen replay byte identical;79 Source files/133 existing bounded excerpts plus explicit Source76 contract;20 SQL consumer classes retained. Bound cast/cost/RNG/HP/role/IO/Main/database/game cases0.',
 'This completes these Source76 post-PC-anchor metadata ports, not a whole cast snapshot. Earlier initial primary/PC selection and other range/LOS/numeric/status/proc paths remain separate work. Full isTarget/ghost/GM/invisibility/permission, unknown/manual/controlled NPC provenance, all58 materials/252 controls/AI/producers/presentation/formal and real same-SHA acceptance remain open.'
]
v['program_checks']=dict(JVM_no_initialization_classes=310,original_methods_independently_inverted=2471,complete_original_lambda_siblings_independently_inverted=2,original_instruction_and_control_flow_inverse_ignores_recomputed_stack_map_frames=True,forward_frozen_splice_and_JVM_stack_maps_verified=True,byte_identical_frozen_rebuild=True,existing_changed_class_entries=1,new_helper_class_entries=1,new_original_metadata_fields=0,new_original_constructors=0,new_original_captured_lambda_siblings=2,pure_slow_failure_metadata_contract_scenarios=40,inherited_and_revalidated_same_SHA_pure_contract_scenarios=2281,original_native_signed_distance_boundary_and_reload_comparisons=99,original_Source76_root_metadata_ports=7,sibling_signed_distance_data_ports=2,original_NPC_birth_erase_and_permission_bodies_retained=True,original_NPC_failure_cursor_reused=True,actual_private_lambda_metafactory_linkage_verified=True,original_source_caster_anchor_and_skill_required=True,Source76_post_PC_anchor_selection_filter_visual_NPC_distance_Row_frozen=True,full_cast_primary_PC_geometry_numeric_status_snapshot_qualified=False,new_retained_PC_anchor_Row_Context_Frame_Member_registry_or_TLS=False,new_SELECTs=0,new_workers_TLS_registries=False,new_connection_ownership=False,new_scheduled_tasks=False,other_original_HP_FIFO_Ctrl_competition_cost_RNG_life_Main_WORLD_inverse_retained=True,bound_selection_cost_RNG_HP_IO_role_Main_cases=0)
for key in ['Source_slow_failure_metadata_contract','slow_failure_metadata_port_census','Source_remaining_area_lists_contract','remaining_area_list_port_census','Source_sleep_construction_contract','sleep_construction_port_census','Source_area_caster_membership_contract','area_caster_target_port_census','target_construction_port_census','Source_target_construction_oracle','Source_target_construction_contract','target_mask_port_census','Source_target_mask_oracle','Source_target_mask_contract','doll_slow_wire_port_census','Source_doll_slow_packet_oracle','Source_doll_slow_wire_contract','outer_pair_port_census','light_pair_life_port_census','paired_scope_port_census','equipment_output_port_census','equipment_proc_life_port_census','group_LOS_port_census','Source_proc_dispatch_contract','Source_target_LOS_oracle','target_LOS_port_census','damage_healing_loop_census','Source_area_class_selection_oracle','selected_class_loop_census','Source_object_target_request_oracle','Source_none_target_request_oracle','Source_PC_empty_selection_cost_contract','Source_mass_self_target_oracle','Source_negative_phase_order','Source_factory_type_oracle','Source_factory_class_hierarchy','Source_NPC_target_failure_oracle','method_reference_census']:verify(build[key]);v['slow_metadata_'+key]=build[key]
v.update(Source76_post_PC_anchor_NPC_selection_failure_distance_visual_Row_program_wired=True,Source76_original_NPC_permission_birth_and_failure_cursor_retained=True,Source76_full_cast_Source_snapshot_and_semantics_qualified=False)
v['remaining_work_groups']=copy.deepcopy(old['remaining_work_groups'])
v['remaining_work_groups'][1]['work']='Source76 NPC selectionErase/filter/accepted visual/two NPC distance lambdas now reuse original pre-cost PC-anchor Row; two original NPC permission bodies and exact failure Cursor remain retained, with actual private lambda linkage checked. Prior six-root PC/NPC calculation Row and mutable Nature API remain wired. Complete earlier initial primary/PC collection and other failure/range/LOS/numeric/status/proc metadata paths, Source ghost/GM/invisibility/permission/duplicate construction, normal/proc/login, wind/single/line and all58 materials/all applicable807 rows, including item/bookmark/none/self/normal/Ctrl, clan/party/GM/PK under BATTLE, dead/type/immunity/corpse/status, unknown/manual factories and cost/RNG/rollback/PC empty-list consumption. These metadata contracts do not qualify complete Source or actual casts.'
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

集體緩速術Source76新增7個原root資料接線，讓兩個selectionErase、兩個NPC計算filter、accepted顯示數量及兩個NPC幾何lambda捕捉，沿用既有成本前PC anchor的不可變Row／skill。避免重載後type／area改變，讓原本合格的NPC在自施分支被清空；失敗判定仍使用原出生／IsErase證據與原NativeSourceNpcTargetFailure.Cursor，精確type1／4條件不變。

兩個Quest／Time原幾何lambda建立完整sibling，原receiver／NPC／座標／areaRadius與GameHub競技權限body可逆還原；只有signed-distance資料新增anchor。捕捉的PC集合僅供原同步Stream.filter，不新增queue／worker／TLS。未標記、其他caster或WORLD走原後備；僅1個無狀態helper及2個原owner sibling，不新增原欄位／constructor、角色生命位置capture、NPC權限、鎖、SQL或連線所有權。

2,471原instruction／control-flow方法與2個完整lambda sibling獨立正規化inverse、stack-map正向／JVM核對、310類no-init、40新增detached原NPC cursor／選取／自施／type／顯示／距離／實際private lambdaMetafactory連結檢查、99原生signed-distance邊界／overflow／重載比較、同SHA2,281繼承／重驗及同位元重建通過。79 Source檔／133既有bounded參考與新Source76契約／20 SQL class保留。**真bound施法／成本／RNG／HP／角色／IO／Main／MySQL／game均未驗收**。

這是Source76在PC anchor建立後的資料一致性；更早主目標／PC選取與其他range／LOS／數值／狀態／proc仍待補齊，尚非完整施法快照。完整Source isTarget／ghost／GM／invisibility／權限／重複建構／wind／單體／直線／proc／login、58材料／252外控／其餘生產端／NPC工廠AI／HPBAR／傷害數字／奇古獸／正式0.6.71隔離合成及6處歷史current索引仍開放。程式全部完成後集中同SHA後端、中文MySQL真裝備角色唯讀重載與實機游標驗收；不部署、不連實際MySQL、不寫Source／玩家SQL／schema／WAL。

'''
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-25 Source76 NPC失敗／距離metadata續作：候選`{v['runtime']['sha256']}`，原root7點＋2個完整幾何lambda sibling沿用原PC anchor Row，補選取／NPC filter／accepted顯示與距離，保留NPC出生／IsErase／權限。2,471正規化原方法＋2完整sibling inverse、stack-map／JVM、310 no-init、40新＋2281同SHA繼承／重驗、99原生距離比較、實際private lambda連結及同位元重建；79 Source檔／133既有參考與新契約／20 SQL class。完整前段主目標／PC選取與其他range／LOS／數值狀態／proc、全Source及真bound施法／HP／IO／Main／DB／game仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')

def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index_pin['path']and v.get('sha256')==old_index_pin['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source76-NPC-selection-failure-distance-visual-frozen-Row-program-only',full_goal_achieved=False)
continuation['source_slow_failure_metadata_progress']=fresh
audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
for p,digest in anchors.items():assert sha(p)==digest,p
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],original_methods_inverted=2471,new_game_runtime_cases=0,new_database_connections=0)))
