"""Checkpoint original parent-third pair entries and original release/receipt/root lifetime evidence."""
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
latest=read(D/'source-pair-third-build-current.json');repro=read(D/'source-pair-third-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_pair_third_contract_scenarios']==203
assert build['test_only_forced_tie_fixture_checks']==7
assert build['test_only_forced_tie_fixture']['sha256']!=latest['runtime']['sha256']
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_pair_third_contract_scenarios')==3492
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 19 original methods including3 complete original entry bodies','PASS337','PASS203 third-actor pair actual-parent','PASS7 pair admission test-only','rejections=15, original-waits=9, positive-original-calls=15, original-three-actor-completions=3, preheld-release-policies=3']:assert marker in log,marker
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
for name in ['NativeSourcePairThird.java','SourcePairThirdMethods.java','SourcePairThirdDelta.java','SourcePairThirdLoad.java','SourcePairThirdPlan.java','build_source_pair_third.py','reproduce_source_pair_third.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_pair_third.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-parent-third-actor-three-original-pair-entries-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['An exact read-only bridge reads the existing action-scope ThreadLocal. One fieldless synchronous helper scans actually held third actor locks under the original root fence. Three complete original entry bodies are preserved behind exact thunks: conditional shared pairs, typed outer producer pairs and explicit new-action admission. Original null/WORLD/unloaded fallbacks precede new Frame/root reads; current accepted Frames on shared/outer routes retain original synchronous waits.', 'Only missing pair locks are tried nonblocking before any original peer registration. Partial acquisition failure rolls back only new locks, without changing original root membership/order/releaseOrder, including an already registered first peer. Existing caller lock counts are preserved. After entering original bodies, release notification follows each original shared versus outer/admission policy, including when the caller already holds another pair side. No new registry/TLS/worker/queue/receipt/HP/cost/RNG/Main policy.', 'Independent inverse of19 original methods including3 complete original entry bodies, plus3 exact route thunks and1 read-only bridge;337 no-init classes.203 candidate contracts plus3492 inherited same-SHA checks;15 prompt contention rejections,9 original fallback waits,15 positive original bodies,3 actual original three-actor receipt completions and3 caller-preheld release-policy checks. Original parent finish clears members/order/releaseOrder and pending receipts. Prior7 test-only forced-tie checks remain separate from candidate SHA. Frozen replay byte-identical;20 SQL consumer classes and earlier full-room defense retained.', 'Actual original root and mutex fixtures cover third held versus merely registered, first/second busy, both pair directions, partial rollback with earlier membership, self pair, exact exceptions, inherited parent locks, transferred completion after all three locks release, accepted Frame/WORLD/unloaded fallbacks and original root clearing. No actual Source Context/cast/cost/RNG/HP/IO/Main/backend/MySQL/game qualification is inferred.', 'Unregistered manually held third actor locks, other direct/accepted whole-continuation pair phases, earlier role/life/primary-secondary/normal/Ctrl/area proofs, complete materials/procs/math/controls/NPC-AI/final outputs/formal composition and concentrated actual Main/memory/backend/MySQL/game acceptance remain open. Already accepted target HP stays independent of later actor/caster liveness. No deployment or Source/player SQL/schema/WAL writes.']
v['program_checks']=dict(JVM_no_initialization_classes=337,original_methods_independently_inverted=19,complete_original_pair_entry_bodies_retained=3,exact_original_entry_route_thunks=3,read_only_original_ThreadLocal_bridges=1,new_fieldless_helper_entries=1,new_fields_or_queues_TLS_registries_workers=0,existing_changed_class_entries=4,pure_pair_third_contract_scenarios=203,inherited_and_revalidated_same_SHA_pure_contract_scenarios=3492,test_only_forced_tie_fixture_checks=7,test_only_fixture_is_not_candidate_SHA=True,actual_prompt_third_parent_contention_rejections=15,actual_original_fallback_waits=9,positive_original_pair_calls=15,actual_original_three_actor_receipt_completions=3,caller_preheld_original_release_policies=3,partial_rejection_preserves_prior_members_order_releaseOrder=True,read_only_third_scan_under_existing_fence=True,only_actually_held_third_mutex_triggers_nonblocking_acquisition=True,original_shared_versus_outer_release_policy_retained=True,current_accepted_shared_outer_Frame_keeps_original_wait=True,WORLD_unloaded_null_before_new_Frame_root_reads=True,no_new_HP_admission_caster_liveness_policy=True,original_closed_root_members_order_releaseOrder_clear=True,original_receipt_completes_after_all_three_unlock=True,fixture_creates_no_Source_Context_or_actual_HP_IO_Main=True,original_earlier_full_room_defense_Snapshot_byte_retained=True,byte_identical_frozen_rebuild=True,new_SELECTs=0,new_connection_ownership=False,bound_actual_server_game_or_Main_shutdown_cases=0,unregistered_manual_locks_other_accepted_pair_phases_and_full_Source_qualified=False)
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_pair_third_contract','pair_third_port_census','Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_parent_third_actor_three_original_pair_entries_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-completion-goal-reset-requests/20261026-user-reset-request.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'paired-lifecycle':'Original parent-root third held actors now trigger nonblocking missing-pair acquisition at three prechecked entries; mere membership without a held third keeps old wait. Partial rejection preserves earlier members/order/releaseOrder. Complete original entry bodies, per-route preheld release policy, transferred three-actor receipts and root clearing retain original behavior. Shared/outer current accepted Frames keep original waits. Complete unregistered manually held third locks, other direct and accepted whole-continuation phases, earlier role/life/primary-secondary/normal/Ctrl/area proofs, actual Main/IO/bounded memory. Prior first owner/audience/controlled Snapshot/Ref and target-owned accepted HP retained; six historical aliases open.'}
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
block='## 本次已更新的程式\n\n父scope實際持有第三角色、而下一組pair缺鎖的情況，已接上原共用conditional pair／typed outer pair／明確new-action admission三個入口。原scope ThreadLocal新增精確唯讀bridge，無欄位或新鎖登記；helper沿用原root fence辨識實際由目前執行緒持有的第三角色，僅對缺少的兩側tryLock。僅在名單但沒有持鎖時保留原等待，原null／WORLD／未載入後備在新增Frame／root讀取前返回原方法。\n\n三個完整原entry body由精確thunk呼叫；第二側競爭失敗只釋放新取得的第一側，連既已登記的peer也不增加members／order／releaseOrder。進入原body後，各路徑保留原shared與outer／admission釋放通知政策；呼叫者已持有另一側時的hold counts亦保留。shared／outer遇current accepted Frame維持原同步等待，不新增accepted一律丟棄／延後規則。無新worker／TLS／registry／queue／receipt／SQL或HP／成本／RNG權責。\n\n19原方法（含3完整原entry body）inverse＋3精確thunk／1唯讀bridge、337 no-init、203候選契約＋3492同SHA繼承及同位元重建通過。15次立即拒絕、9次原後備等待、15次正向原body、3次原三角色receipt完成與3次caller預持鎖通知政策檢查通過；原parent root關閉後清空members／order／releaseOrder，原receipt在三角色解鎖後完成。另7項test-only強制tie fixture仍與候選SHA分開；20 SQL consumer保留。\n\n範圍僅上述三個prechecked入口與既有parent scope。未登記的手動第三鎖、其他direct／accepted phase完整continuation、較早角色生命期／Ctrl／範圍主次目標、完整材料／數值／proc／外控／NPC-AI／最終輸出／Main與記憶體仍開放；fixture沒有建立Source Context或執行實際施法／HP／IO。6歷史current索引未解。全部程式完成後才集中後端／中文MySQL／實機驗收；未部署、不啟動實服或連MySQL，不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 parent third-actor pair：候選`{v['runtime']['sha256']}`，原parent fence／實際第三持鎖→三個原entry body，partial rollback不改先前scope名單／順序；accepted共用／outer仍走原等待。19原方法含3完整entry inverse＋3thunk／1唯讀bridge、337 no-init、203新＋3492同SHA繼承、15拒絕／9原等待／15原body／3三角色receipt／3預持鎖policy與同位元重建。其他pair phase／未登記手動鎖／完整Source／Main仍開放；未部署／未連MySQL。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='parent-third-actor-three-original-pair-entries-program-only',full_goal_achieved=False)
continuation['source_pair_third_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
