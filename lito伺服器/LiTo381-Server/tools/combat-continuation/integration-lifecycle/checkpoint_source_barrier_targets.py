"""Checkpoint exact Source PC absolute-barrier target return while preserving original self/construction/caller phases."""
from pathlib import Path
from datetime import datetime,timezone
import copy,csv,hashlib,json,re,shutil,subprocess,zipfile
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];C=R.parent/'continuation-current.json'
read=lambda p:json.loads(p.read_text('utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
pin=lambda p:dict(path=str(p.resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
cache={}
def verify(v):
 if isinstance(v,dict):
  if 'path'in v and 'sha256'in v:
   p=Path(v['path']);key=str(p);actual=cache.get(key)
   if actual is None:actual=sha(p);cache[key]=actual
   assert actual==v['sha256'],key
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
index=D/'source-program-update-progress-current.json';previous=read(index);verify(previous);old=read(Path(previous['manifest']['path']));verify(old)
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';proof=read(proofpath);audit=read(auditpath)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
latest=read(D/'source-barrier-targets-build-current.json');repro=read(D/'source-barrier-targets-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_barrier_targets_contract_scenarios']==1032
assert build['test_only_forced_tie_fixture_checks']==7
assert build['test_only_forced_tie_fixture']['sha256']!=latest['runtime']['sha256']
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_barrier_targets_contract_scenarios')==4371
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 4 original methods and1 exact active-barrier return','PASS347','PASS1032 Source barrier target original-predicate','PASS7 pair admission test-only','scalar-Source-comparisons=811, protected-PC-comparisons=14598, original-cursors=5, Source-allowed-skills=22']:assert marker in log,marker
with zipfile.ZipFile(build['base']['path'])as parent,zipfile.ZipFile(latest['runtime']['path'])as candidate:
 for entry in ['local/lito381/server/'+name+'.class'for name in ['NativeSourceDollDefense$Snapshot','NativeSourceDollDefense$Proof','NativeSourceDollPassive','NativeSourceDollPassive$Choice','NativePlayerCommit','NativePlayerCommit$DefenseVisual','ClientSession','NativeSourceDollAudience','NativeSourceDollDetachedOutput']]:
  assert parent.read(entry)==candidate.read(entry),entry
assert inv['goal']['status']=='active' and read(D/'source-completion-goal-current.json')['status']=='active'
assert subprocess.run(['powershell','-NoProfile','-Command',"@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"],capture_output=True,text=True,check=True).stdout.strip()=='0'
anchors=[R/'current.json',D/'source-kiringku-progress-current.json',D/'competition-only-product-scope-current.json',D/'source-equipment-mysql-reload-readonly-origin-current.json',D/'source-raw-producer-build-current.json',D/'source-arena-poison-build-current.json',S.parent.parent/'lito登入器/LiTo381-Launcher/app/server/LiTo381-SharedResources-candidate.jar']
anchor_refs=[pin(p)for p in anchors]
with zipfile.ZipFile(old['readonly_SQL_consumers_byte_retained']['base']['path'])as sql,zipfile.ZipFile(latest['runtime']['path'])as jar:
 entries=old['readonly_SQL_consumers_byte_retained']['entries'];assert len(entries)==20
 for name in entries:assert sql.read(name)==jar.read(name),name
run=D/'source-completion-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');inputs=run/'editable-input-snapshot';inputs.mkdir(parents=True)
current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';work=S/'docs/maintenance/source-completion-work-current.zh-TW.md';old_index=pin(index)
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md'),(work,'previous-work.md')]:shutil.copyfile(p,run/name)
frozen_index=pin(run/'previous-program-progress.json')
for ref in old['editable_inputs']:
 name=Path(ref['path']).name;shutil.copyfile(R/name,inputs/name)
for name in ['NativeSourceBarrierTargets.java','SourceBarrierTargetsMethods.java','SourceBarrierTargetsDelta.java','SourceBarrierTargetsLoad.java','SourceBarrierTargetsPlan.java','build_source_barrier_targets.py','reproduce_source_barrier_targets.py','verify_source_barrier_targets.py','NativeSourceSkillDefaults.java','SourceSkillDefaultsMethods.java','SourceSkillDefaultsDelta.java','SourceSkillDefaultsLoad.java','SourceSkillDefaultsPlan.java','build_source_skill_defaults.py','reproduce_source_skill_defaults.py','verify_source_skill_defaults.py','NativeSourceDollDefenseAudience.java','SourceDollDefenseAudienceMethods.java','SourceDollDefenseAudienceDelta.java','SourceDollDefenseAudienceLoad.java','SourceDollDefenseAudiencePlan.java','build_source_doll_defense_audience.py','reproduce_source_doll_defense_audience.py','verify_source_doll_defense_audience.py','NativeSourceDollAudience.java','SourceDollAudienceMethods.java','SourceDollAudienceDelta.java','SourceDollAudienceLoad.java','SourceDollAudiencePlan.java','build_source_doll_audience.py','reproduce_source_doll_audience.py','verify_source_doll_audience.py','NativeSourceDollDetachedOutput.java','SourceDollDetachedOutputMethods.java','SourceDollDetachedOutputDelta.java','SourceDollDetachedOutputLoad.java','SourceDollDetachedOutputPlan.java','build_source_doll_detached_output.py','reproduce_source_doll_detached_output.py','verify_source_doll_detached_output.py','NativeSourceManualScope.java','SourceManualActionScopeMethods.java','SourceManualActionScopeDelta.java','SourceManualActionScopeLoad.java','SourceManualActionScopePlan.java','SourceManualActionLockCensus.java','build_source_manual_action_scope.py','reproduce_source_manual_action_scope.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_manual_action_scope.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-Source-exact-PC-barrier-target-return-original-self-construction-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['The original shared PC target predicate unconditionally allowed every selected protected PC at its active absolute-barrier short return. That one constant now calls the exact Source22-skill whitelist. Original Source self/TYPE_ATTACK/HEAL_ALL/PC-clan-party return order and makeTargetList ME/area-caster construction bypasses remain unchanged. Original timer read and monitor, class mask and all other predicates/callers stay retained; the change grants no targets beyond the previous unconditional branch.', 'The exact frozen Source self/barrier/late-class statements compile as an independent local oracle. The whitelist uses actual frozen constants, including CURSE_BLIND20 and EARTH_SKIN151.807 audited static IDs plus four unknown boundary IDs produce811 scalar and14598 protected-PC type/mask comparisons. Five original shared area/damage-healing/private-Sleep cursor paths exercise the gate, original duplicate/order/wrapper identity and immutable Row reload behavior. WORLD/unmarked and expired-barrier fallbacks remain original.', 'Independent inverse of4 original methods and1 exact active-barrier return;347 no-init classes.1032 new candidate checks plus4371 inherited same-SHA checks,7 separate test-only forced-tie checks. Frozen replay byte identical;20 SQL consumer classes and original full-room defense/Proof/passive/Commit/Visual/ClientSession/skill-default helpers byte retained. No new fields, methods on original classes, actor/context retention, locks, registry, queue, TLS, worker or HP/RNG/competition/Ctrl/lifetime authority.', 'These are detached original predicate/cursor/metadata fixtures, without real Source Context, complete isTarget/makeTargetList/primary-secondary/Proc fallback/cost/RNG/HP/backend/Main/server/MySQL/game. Earlier ghost/GM/scene/permission, other skill/proc/numeric/control/producer paths and concentrated actual acceptance remain open. No deployment, formal overwrite, new baseline or Source/player SQL/schema/WAL writes.']
v['program_checks']=dict(JVM_no_initialization_classes=347,original_methods_independently_inverted=4,exact_original_active_barrier_returns=1,existing_changed_class_entries=1,new_fieldless_helper_entries=1,new_existing_class_fields_or_methods=0,pure_barrier_targets_contract_scenarios=1032,inherited_and_revalidated_same_SHA_pure_contract_scenarios=4371,test_only_forced_tie_fixture_checks=7,test_only_fixture_is_not_candidate_SHA=True,audited_static_skill_IDs=807,scalar_frozen_Source_comparisons=811,protected_PC_type_mask_comparisons=14598,actual_original_shared_and_private_wrapper_cursors=5,exact_Source_allowed_skill_constants=22,original_self_ME_area_caster_and_exact_type_shortcut_order_retained=True,original_Row_reload_duplicate_order_and_wrapper_identity_retained=True,unconditional_active_barrier_branch_no_longer_grants_unlisted_skills=True,World_unmarked_and_expired_barrier_original_paths_retained=True,all_other_original_caller_cost_RNG_HP_locks_life_competition_Ctrl_Main_code_byte_retained=True,new_registry_TLS_queue_workers_SQL_HP_RNG_or_lifetime_authority=False,byte_identical_frozen_rebuild=True,new_SELECTs=0,new_connection_ownership=False,bound_actual_server_game_or_Main_shutdown_cases=0,full_isTarget_Proc_primary_fallback_makeTargetList_cost_HP_Main_game_qualified=False)
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_barrier_targets_contract','Source_barrier_target_oracle','Source_barrier_skill_ids','barrier_skill_matrix','barrier_targets_port_census','Source_skill_defaults_contract','Source_skill_config_oracle','skill_defaults_port_census','Source_doll_defense_audience_contract','doll_defense_audience_port_census','Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_exact_PC_barrier_return_original_self_and_construction_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'materials':'Shared protected-PC target predicate now implements exact Source22-skill absolute-barrier return after original self/construction gates.807 audited IDs plus unknown boundaries,811 scalar/14598 protected-PC comparisons and5 actual shared/wrapped cursors pass without altering original Root/cost/RNG/life/competition/Ctrl code. Complete earlier primary/secondary selection, ghost/GM/scene/permission, nonattack Proc primary fallback, all58 materials/all applicable807 rows, normal/Ctrl/item/bookmark/none/self/class/immune/corpse/status, failure messages and actual cost/rollback/backend/Main/game remain open.'}
for group in v['remaining_work_groups']:
 if group['id']in updates:group['work']=updates[group['id']]
v['remaining']=[x['work']for x in v['remaining_work_groups']];v['historical_remaining_preserved_in']=previous['manifest'];assert len(v['remaining_work_groups'])==12
for key in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[key]is False,key
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),system_goal=v['goal_reset_receipt'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
text=work.read_text('utf-8');text=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{v['runtime']['sha256']}`",text)
text=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",text)
artifact_paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']}
text=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:artifact_paths[m[1]],text)
a=text.index('## 本次已更新的程式');b=text.index('## 尚未完成與完成門檻',a)
block='## 本次已更新的程式\n\n共用PC目標predicate的絕對屏障分支已由原無條件放行，改為固定Source的22項允許清單；僅替換該原short return的常數，保留原timer讀取／monitor、TYPE_ATTACK／HEAL_ALL／PC-clan-party自施先後，以及makeTargetList的ME／非攻擊area-caster捷徑。未新增原類別欄位／方法或改動其他Root／cost／RNG／HP／生命／競技／Ctrl／Main程式；未擴張原分支可選目標。\n\n固定Source self／barrier／late-mask語句為獨立oracle，採實際常數CURSE_BLIND20／EARTH_SKIN151等。807盤點ID＋4未知邊界：811 scalar／14598受保護PC型別遮罩比對、5原shared area／damage-healing／private Sleep cursor及原Row reload／順序／重複／wrapper identity通過。4原方法／1精確return inverse、347 no-init、1032新＋4371同SHA繼承與同位元重建通過；7強制tie測試另列。20 SQL reader、原防禦／Commit／ClientSession／技能外控helper位元保留。\n\n尚未驗證完整isTarget／makeTargetList／Proc主目標fallback／cost／RNG／HP／Main／Source Context或實機。更早的ghost／GM／scene／permission、其餘材料／外控／傷害來源與集中驗收仍開放；未部署或寫入資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 Source barrier target：候選`{v['runtime']['sha256']}`，原PC絕對屏障short return改為Source22項清單，原自施／ME／area-caster捷徑保留。4原方法／1return inverse、347 no-init、1032新＋4371同SHA繼承、811 scalar／14598型別遮罩／5原cursor及同位元重建通過。完整施法／Proc／cost／HP／Main／MySQL／實機仍開放；未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='Source-exact-PC-barrier-target-return-original-construction-self-program-only',full_goal_achieved=False)
continuation['source_barrier_targets_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
