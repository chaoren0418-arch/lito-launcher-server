"""Checkpoint scopes for exact original manual critical spans without changing original gameplay bodies."""
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
latest=read(D/'source-manual-action-scope-build-current.json');repro=read(D/'source-manual-action-scope-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_manual_action_scope_contract_scenarios']==57
assert build['test_only_forced_tie_fixture_checks']==7
assert build['test_only_forced_tie_fixture']['sha256']!=latest['runtime']['sha256']
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_manual_action_scope_contract_scenarios')==3695
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 2637 original methods,18 exact original lock spans and43 original exits','PASS340','PASS57 manual action-scope original-state','PASS7 pair admission test-only','original-state-calls=9, original-scalar-calls=7, actual-pair-contentions=2, manual-three-actor-completions=1']:assert marker in log,marker
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
for name in ['NativeSourceManualScope.java','SourceManualActionScopeMethods.java','SourceManualActionScopeDelta.java','SourceManualActionScopeLoad.java','SourceManualActionScopePlan.java','SourceManualActionLockCensus.java','build_source_manual_action_scope.py','reproduce_source_manual_action_scope.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_manual_action_scope.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-exact-original-manual-player-action-critical-spans-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Eleven original manual state/respawn critical sections and seven role/teleport-token scalar sections borrow one existing scope Ticket in an invocation-local variable. The Ticket starts before the original action-lock acquisition and closes at each exact original unlock. All43 normal/early/catch exit data ports retain original monitors/handlers and bodies; no whole-cast or post-respawn-output scope is added.', 'A fieldless helper closes entry-failure scopes and keeps the original body or exit failure primary. WORLD/unloaded uses null Tickets and the exact old lock path. No original fields/methods or new TLS/registry/queue/workers/receipt authority, and no new cost/RNG/HP/Main/competition/Ctrl policy. Nested scopes use the original parent root and depth. Manual third actors are therefore visible to existing pair guards only while their original lock span is active.', 'Independent inverse of2637 original methods,18 exact spans and43 original exits;340 no-init classes.57 candidate contracts plus3695 inherited same-SHA checks. Nine selected original state roots,7 scalar roots,2 real pair contention scenarios during the complete original storeReady method and1 original three-actor receipt completion at manual unlock. Original entry failure/body exception/nested depth/respawn early return and root clearing pass. Frozen replay byte-identical;20 SQL consumer classes and earlier full-room defense retained. Separate7 inherited test-only forced-tie checks are not candidate SHA.', 'Read-only lock-domain inventory records59 parent and55 candidate methods. It includes vital/item/NPC locks and cannot be interpreted as missing-path counts. Actual scalar tests cover original null-status role assignment/advance and teleport-token bodies; they do not qualify a complete normal selected-role transition, retirement or restoration. Fake object/model fixtures construct no real Source Context, execute no real HP/IO/server/MySQL/game.', 'Complete other direct/native lock origins and accepted whole-continuation phases, earlier role/life/primary-secondary/normal/Ctrl/area proofs, materials/procs/math/252 controls/NPC-AI/final output/Main/memory/formal composition and concentrated actual acceptance remain open. Already accepted target HP stays independent of later actor/caster life. No deployment or Source/player SQL/schema/WAL writes.']
v['program_checks']=dict(JVM_no_initialization_classes=340,original_methods_independently_inverted=2637,exact_original_manual_lock_spans=18,original_normal_early_and_catch_exit_ports=43,original_state_respawn_spans=11,original_role_token_scalar_spans=7,existing_changed_class_entries=5,new_fieldless_helper_entries=1,new_fields_methods_or_queues_TLS_registries_workers=0,pure_manual_action_scope_contract_scenarios=57,inherited_and_revalidated_same_SHA_pure_contract_scenarios=3695,test_only_forced_tie_fixture_checks=7,test_only_fixture_is_not_candidate_SHA=True,selected_original_state_roots=9,original_scalar_roots=7,actual_manual_third_pair_contentions=2,original_three_actor_receipt_completions_at_manual_unlock=1,scope_ticket_only_in_original_invocation_local=True,scope_ends_at_original_unlock_before_remaining_cast_or_respawn_output=True,original_acquisition_body_exception_and_nested_parent_depth_retained=True,original_closed_root_members_and_pending_receipts_clear=True,WORLD_unloaded_null_ticket_uses_original_lock_path=True,parent_direct_lock_inventory_methods=59,candidate_direct_lock_inventory_methods=55,inventory_counts_are_not_missing_path_counts=True,scalar_role_fixtures_cover_null_status_branch_only=True,fixture_creates_no_real_Source_Context_HP_IO_Main=True,no_new_HP_admission_caster_liveness_policy=True,original_earlier_full_room_defense_Snapshot_byte_retained=True,byte_identical_frozen_rebuild=True,new_SELECTs=0,new_connection_ownership=False,bound_actual_server_game_or_Main_shutdown_cases=0,full_normal_role_transition_other_accepted_phases_Main_IO_Source_qualified=False)
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_manual_action_scope_contract','manual_action_scope_port_census','parent_direct_lock_census','candidate_direct_lock_census','Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_exact_original_manual_player_action_spans_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-completion-goal-reset-requests/20261026-user-reset-request.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'paired-lifecycle':'Eighteen fixed manual player action-lock sections now borrow existing invocation-local scope Tickets exactly until their43 original unlocks. Original storeReady manual third actor becomes visible to pair guards, and transferred receipt waits for its release. Entry/body exceptions, nested depth, respawn early return and null-status role/token scalar bodies retain original behavior. No whole-cast scope or new registry. Complete other direct/native lock origins, full normal role transition/retirement/restore and accepted whole-continuation phases, earlier role/life/primary-secondary/normal/Ctrl/area proofs, actual Main/IO/bounded memory. Inventory59/55 includes vital/item/NPC domains, not missing counts. Prior owner/audience/controlled Snapshot/Ref and target-owned accepted HP retained; six historical aliases open.'}
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
block='## 本次已更新的程式\n\n11個原狀態／復活臨界區與7個角色／傳送token scalar臨界區已借用既有scope Ticket，僅存在原呼叫區域變數。Ticket在原action lock取得前建立，在43個原normal／early／catch解鎖點結束；原monitor／handler／body保留，不延長整段施法dispatch或復活後封包輸出的scope。取得鎖失敗會關閉ticket，原body／exit例外仍為primary；WORLD／未載入使用null Ticket與原鎖行為。無既有欄位／方法增加，無新TLS／registry／queue／worker／receipt或成本／RNG／HP／Main權責。\n\n2637原方法、18精確span／43原exit獨立inverse、340 no-init、57候選契約＋3695同SHA繼承與同位元重建通過。9個選定原state root／7個scalar root、2次完整原storeReady內手動第三鎖配對競爭、1次原三角色receipt在原手動解鎖後完成通過；entry失敗／body例外／nested depth／respawn早退與原root清空亦通過。20 SQL consumer及較早全房間防禦保留；另7項test-only強制tie fixture不計入候選SHA。\n\n唯讀鎖域盤點為parent59／candidate55個方法，包含vital／item／NPC內部鎖，不能當作缺漏數字。角色scalar fixture只驗原null-status分支及token方法，沒有完成正常選角切換／退休／還原或Source Context／HP／IO驗收。其他direct／native lock來源、accepted完整continuation、較早生命期／Ctrl／範圍主次目標、材料／數值／proc／252外控／NPC-AI／最終輸出／Main與記憶體仍開放；6歷史current索引未解。全部程式完成後才集中後端／中文MySQL／實機驗收；未部署，不啟動實服或連MySQL，不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 exact manual critical spans：候選`{v['runtime']['sha256']}`，11 state／respawn＋7 role／token span沿用原Ticket至43原解鎖點；無整段cast scope或新registry。2637原方法inverse、340 no-init、57新＋3695同SHA繼承、9選定state／7scalar／2手動第三配對競爭／1原三角色receipt及同位元重建通過。正常角色切換／其他accepted phase／完整Source／Main仍開放；未部署／未連MySQL。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='exact-original-manual-player-action-critical-spans-program-only',full_goal_achieved=False)
continuation['source_manual_action_scope_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
