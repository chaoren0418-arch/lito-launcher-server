"""Checkpoint installed Source skill omission/blank defaults with original validation and publication retained."""
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
latest=read(D/'source-skill-defaults-build-current.json');repro=read(D/'source-skill-defaults-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_skill_defaults_contract_scenarios']==321
assert build['test_only_forced_tie_fixture_checks']==7
assert build['test_only_forced_tie_fixture']['sha256']!=latest['runtime']['sha256']
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_skill_defaults_contract_scenarios')==4050
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 26 original methods and1 complete original snapshot sibling','PASS346','PASS321 installed Source skill omission/blank','PASS7 pair admission test-only','omitted=31, blank=62, original-safety=11, frozen-Source-values=3201']:assert marker in log,marker
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
for name in ['NativeSourceSkillDefaults.java','SourceSkillDefaultsMethods.java','SourceSkillDefaultsDelta.java','SourceSkillDefaultsLoad.java','SourceSkillDefaultsPlan.java','build_source_skill_defaults.py','reproduce_source_skill_defaults.py','verify_source_skill_defaults.py','NativeSourceDollDefenseAudience.java','SourceDollDefenseAudienceMethods.java','SourceDollDefenseAudienceDelta.java','SourceDollDefenseAudienceLoad.java','SourceDollDefenseAudiencePlan.java','build_source_doll_defense_audience.py','reproduce_source_doll_defense_audience.py','verify_source_doll_defense_audience.py','NativeSourceDollAudience.java','SourceDollAudienceMethods.java','SourceDollAudienceDelta.java','SourceDollAudienceLoad.java','SourceDollAudiencePlan.java','build_source_doll_audience.py','reproduce_source_doll_audience.py','verify_source_doll_audience.py','NativeSourceDollDetachedOutput.java','SourceDollDetachedOutputMethods.java','SourceDollDetachedOutputDelta.java','SourceDollDetachedOutputLoad.java','SourceDollDetachedOutputPlan.java','build_source_doll_detached_output.py','reproduce_source_doll_detached_output.py','verify_source_doll_detached_output.py','NativeSourceManualScope.java','SourceManualActionScopeMethods.java','SourceManualActionScopeDelta.java','SourceManualActionScopeLoad.java','SourceManualActionScopePlan.java','SourceManualActionLockCensus.java','build_source_manual_action_scope.py','reproduce_source_manual_action_scope.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_manual_action_scope.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-installed-Source-skill-omission-blank-defaults-original-validation-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['31 installed Source ConfigSkill properties now receive the exact omitted/blank defaults from Source._loadFromProperties, on a temporary Properties copy before the complete original snapshot validation sibling. Explicit configured values, caller Properties, file-read gates, original damage rules, type/range/nonfinite/timer validation and startup publication remain retained. Default metadata is merged into an immutable result only after original success; the helper has no static state or publication authority.', 'The exact frozen Source ConfigSkill property methods compile as an independent local oracle, using inert logging only.31 omitted keys,62 empty/whitespace cases and all-omitted/all-blank/valid-overridden profiles compare3201 installed Source values. Existing11 safety cases remain original rejections, including damage-table/nonfinite/type/range/timer cases. Original mana/curse/stun/bind/critical/immune/reflection adapters consume the validated defaults. Three uninstalled keys gain no new consumers; declared-only Nature healing remains0.', 'Independent inverse of26 original methods plus1 complete original snapshot sibling;346 no-init classes.321 new candidate checks plus4050 inherited same-SHA checks, with7 separate test-only forced-tie checks. Frozen replay byte identical;20 SQL consumer classes, original defense full-room Snapshot/Proof/passives/Commit/DefenseVisual/ClientSession and previous helpers byte retained. This changes no HP/skill target/cost/RNG/competition/Ctrl/lifetime policy.', 'These are isolated property/oracle/adapter checks, without real source file startup/reload, Source Context, HP/backend/Main/server/MySQL/game. Explicit malformed-value Source fallback versus existing validation policy, timer zero/clamp/overflow, missing-file fallback, other controls/dynamic aliases/applicability and full Source skill semantics remain open. Concentrated actual acceptance remains deferred. No deployment, formal overwrite, new baseline or Source/player SQL/schema/WAL writes.']
v['program_checks']=dict(JVM_no_initialization_classes=346,original_methods_independently_inverted=26,complete_original_snapshot_factory_siblings=1,original_snapshot_wrapper_ports=1,existing_changed_class_entries=1,new_fieldless_helper_entries=1,new_existing_class_fields=0,pure_skill_defaults_contract_scenarios=321,inherited_and_revalidated_same_SHA_pure_contract_scenarios=4050,test_only_forced_tie_fixture_checks=7,test_only_fixture_is_not_candidate_SHA=True,installed_Source_property_keys=31,omitted_key_profiles=31,blank_and_whitespace_profiles=62,original_safety_rejection_scenarios=11,exact_frozen_Source_value_comparisons=3201,input_Properties_mutated=False,global_control_publication_by_helper=False,three_uninstalled_Source_keys_activated=False,declared_unassigned_Nature_healing_stays_zero=True,original_load_read_damage_type_range_timer_and_consumers_retained=True,default_metadata_added_only_after_original_validation=True,original_defense_Snapshot_Proof_passive_Commit_Visual_ClientSession_byte_retained=True,new_registry_TLS_queue_workers_SQL_HP_RNG_or_lifetime_authority=False,byte_identical_frozen_rebuild=True,new_SELECTs=0,new_connection_ownership=False,bound_actual_server_game_or_Main_shutdown_cases=0,full_Source_config_timer_fallback_startup_HP_Main_game_qualified=False)
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_skill_defaults_contract','Source_skill_config_oracle','skill_defaults_port_census','Source_doll_defense_audience_contract','doll_defense_audience_port_census','Source_doll_audience_contract','doll_audience_port_census','Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_installed_skill_omission_blank_defaults_original_validation_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'external-controls':'31 installed ConfigSkill properties now use exact frozen Source omission/blank defaults before complete original validation.31 omitted and62 blank profiles compare3201 Source values; explicit configured values, caller Properties, original11 safety rejections, publication and installed adapters remain. Three uninstalled properties stay inactive and declared-only Nature remains0. Trace remaining252 property rows/dynamic aliases/class matrices/actual consumers and applicability; explicit malformed Source fallback versus retained validation, timer zero/clamp/overflow and full actual startup/Main/backend/game remain open. Exact literal counts are not missing consumer counts.'}
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
block='## 本次已更新的程式\n\n31個已接線ConfigSkill外控省略／留白時，已依固定Source._loadFromProperties填入精確預設；僅修改暫存Properties副本，再交由完整保留的原snapshot validation factory。原明確設定值、輸入Properties、讀檔門檻、傷害表、型別／範圍／nonfinite／timer檢查與啟動發布保留，成功後才合併default metadata。無新全域欄位、發布權責、SQL／檔案寫入／HP／RNG／生命週期行為。\n\n固定Source屬性方法為獨立oracle：31缺值／62空白設定，加上全省略／全空白／有效覆寫，比對3201個Source值；11原拒絕情境與原mana／curse／stun／bind／critical／immune／reflection adapter通過。3未接線且靜態Source搜尋無直接消費端的舊欄位未啟用，未賦值的Nature healing仍為0。26原方法＋1完整snapshot sibling inverse、346 no-init、321新＋4050同SHA繼承與同位元重建通過；7強制tie測試另列。20 SQL reader與原防禦／Commit／ClientSession位元保留。\n\n這些為隔離屬性／oracle／adapter檢查；未實跑原Source讀檔啟動／重載、HP／Main／MySQL／遊戲。明確錯誤值的Source fallback與既有驗證政策差異、timer零值／clamp／overflow、其餘外控與完整技能語意仍開放。未部署、覆寫正式或寫入資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 Source skill defaults：候選`{v['runtime']['sha256']}`，31已接線技能外控的缺值／留白依固定Source預設，原validation／publication與數值限制保留。26原方法＋1完整snapshot inverse、346 no-init、321新＋4050同SHA繼承、31缺值／62空白／11原拒絕／3201 Source值比較及同位元重建通過。完整Source／啟動／Main／MySQL／實機仍開放；未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='installed-Source-skill-omission-blank-defaults-original-validation-program-only',full_goal_achieved=False)
continuation['source_skill_defaults_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
