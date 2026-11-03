"""Checkpoint detached original doll output Tasks and the complete original synchronized final frame."""
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
latest=read(D/'source-doll-detached-output-build-current.json');repro=read(D/'source-doll-detached-output-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_doll_detached_output_contract_scenarios']==135
assert build['test_only_forced_tie_fixture_checks']==7
assert build['test_only_forced_tie_fixture']['sha256']!=latest['runtime']['sha256']
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_doll_detached_output_contract_scenarios')==3752
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 2489 original methods,1 factory and1 complete original synchronized final frame','PASS341','PASS135 detached doll output original-producer','PASS7 pair admission test-only','memory-IO=3, original-route-parity=3, receiver-waits=3, original-producer=1']:assert marker in log,marker
with zipfile.ZipFile(build['base']['path'])as parent,zipfile.ZipFile(latest['runtime']['path'])as candidate:
 for entry in ['local/lito381/server/NativeSourceDollDefense.class','local/lito381/server/NativeSourceDollDefense$Snapshot.class']:
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
for name in ['NativeSourceDollDetachedOutput.java','SourceDollDetachedOutputMethods.java','SourceDollDetachedOutputDelta.java','SourceDollDetachedOutputLoad.java','SourceDollDetachedOutputPlan.java','build_source_doll_detached_output.py','reproduce_source_doll_detached_output.py','verify_source_doll_detached_output.py','NativeSourceManualScope.java','SourceManualActionScopeMethods.java','SourceManualActionScopeDelta.java','SourceManualActionScopeLoad.java','SourceManualActionScopePlan.java','SourceManualActionLockCensus.java','build_source_manual_action_scope.py','reproduce_source_manual_action_scope.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_manual_action_scope.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-detached-original-doll-output-task-final-receiver-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['The original doll output invokedynamic factory is replaced by one identical-descriptor helper. Original producer checks, output proof, synchronous Snapshot and deferScoped/Batch stay intact. Queued Task captures only Output and the original failure-stat caster; it stores no Context, View, Frame or audience map and does not attach or mutate the producer Context on execution.', 'One complete original synchronized ClientSession final-frame sibling accepts the detached proof at the exact original doll guard. All other guard order, cipher/socket/log/inventory body, original role/summon/target/timer epochs, requested Source29 duration and phase-specific proc_spr/caster rules remain. Actual opcodes55/32/41 bypass the original self clock, static-motion and cancellation selectors; actual encrypted memory IO matches the old Context-owned sender for all three.', 'Independent inverse of2489 original methods and1 complete original synchronized final body;341 no-init classes.135 new candidate checks plus3752 inherited same-SHA checks, with7 separate test-only forced-tie checks. Three original receiver-monitor waits reject intervening role replacement; one actual original kind2 producer queues and executes the detached Task through the existing Runnable Batch wrapper. Frozen replay byte identical;20 SQL consumer classes and original Pending summon proof byte retained.', 'This is detached memory IO and model proof evidence, without a real Source Context, HP backend, actual server/Main shutdown, database or game session. Initial audience/observer and receiver-life discovery, other Context-carried producers, complete Source semantics and concentrated actual acceptance remain open. No deployment, formal overwrite, new baseline or Source/player SQL/schema/WAL writes.']
v['program_checks']=dict(JVM_no_initialization_classes=341,original_methods_independently_inverted=2489,complete_original_synchronized_final_frame_siblings=1,original_deferred_factory_ports=1,existing_changed_class_entries=2,new_fieldless_helper_entries=1,new_existing_class_fields=0,pure_doll_detached_output_contract_scenarios=135,inherited_and_revalidated_same_SHA_pure_contract_scenarios=3752,test_only_forced_tie_fixture_checks=7,test_only_fixture_is_not_candidate_SHA=True,original_encrypted_memory_IO_frames=3,original_route_cipher_parity_frames=3,actual_original_receiver_monitor_waits=3,actual_original_doll_producer_Batch_runs=1,queued_Context_View_Frame_or_audience_map=False,producer_Context_installed_or_mutated_by_new_task=False,original_Pending_summon_proof_byte_retained=True,original_other_final_guards_cipher_socket_log_inventory_body_inverse_retained=True,original_Source29_requested_duration_timer_epoch_and_phase_rules_retained=True,new_registry_TLS_queue_workers_or_SQL=False,byte_identical_frozen_rebuild=True,new_SELECTs=0,new_connection_ownership=False,bound_actual_server_game_or_Main_shutdown_cases=0,full_Source_HP_backend_role_Main_IO_qualified=False)
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_doll_detached_output_contract','doll_detached_output_port_census','Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_doll_detached_original_task_and_final_receiver_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-goal-reset-request-20261026.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'presentation':'Doll output Task now carries only original Output and failure-stat caster through existing Batch; complete original synchronized final frame checks explicit proof at the original doll guard. Three encrypted memory routes match the original sender, and three receiver-lock waits reject role replacement. Existing Pending summon proof remains byte identical. Complete initial audience/observer/receiver-life discovery, other transported Context producers, all HPBAR/digits/kiringku/proc_spr/suppression/RO login/resource paths and full backend/Main/actual game acceptance remain open.'}
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
block='## 本次已更新的程式\n\n娃娃特效／動作／速度輸出的既有Task已改為只捕捉單次Output與錯誤統計角色，不再捕捉產生者Context或View／Frame／觀眾map；保留原send前置條件、Snapshot證明及deferScoped／Batch。執行時不重掛或修改產生者Context。新增完整原synchronized final-frame sibling，在原娃娃guard位置傳入明確Output；其他guard順序、cipher／socket／log／inventory body、召喚／目標／計時器epoch與Source29原請求duration保留。\n\n2489原方法＋1完整final body獨立inverse、341 no-init、135新＋3752同SHA繼承與同位元重建通過。3原始加密記憶體IO與原路徑逐位元一致，3接收鎖等待會拒絕角色置換；1原kind2 producer經原Runnable Batch wrapper執行。7強制tie測試另列，非候選SHA。既有Pending召喚證明與20 SQL reader class位元保留。\n\n最初觀眾／observer／receiver life、其他Context傳遞路徑、完整技能／Source數值及Main／後端／中文MySQL／實機仍開放；未啟動實際服務，未部署或寫入資料。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 detached doll output：候選`{v['runtime']['sha256']}`，原Task不再持有Context；完整原同步final body在原娃娃guard接收明確Output。2489原方法inverse、341 no-init、135新＋3752同SHA繼承、3加密IO／原路徑比對／接收鎖等待、1原producer與同位元重建通過。Pending召喚證明保留。完整Source／Main／MySQL／實機仍開放；未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='detached-original-doll-output-task-and-complete-final-receiver-program-only',full_goal_achieved=False)
continuation['source_doll_detached_output_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
