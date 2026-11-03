"""Checkpoint original controlled Snapshot actor/life wiring without promoting qualified baseline."""
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
latest=read(D/'source-companion-first-actor-build-current.json');repro=read(D/'source-companion-first-actor-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_companion_first_actor_contract_scenarios']==204
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_companion_first_actor_contract_scenarios')==3222
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 2699 original methods plus2 complete original action/ingress bodies and1 exact Snapshot constructor','PASS335','PASS204 Source companion first-actor','original-action-calls=40, original-outer-waits=12, original-Ref-waits=4, original-live-calls=16']:assert marker in log,marker
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
for name in ['NativeSourceCompanionFirstActor.java','SourceCompanionFirstActorMethods.java','SourceCompanionFirstActorDelta.java','SourceCompanionFirstActorLoad.java','SourceCompanionFirstActorPlan.java','build_source_companion_first_actor.py','reproduce_source_companion_first_actor.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_companion_first_actor.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-controlled-first-Snapshot-actor-life-lookup-action-Ref-live-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Original synchronized companion Snapshot captures weak actor/profile, existing life token, original immutable stamp/kind/id/npc/pose/command before its field reads. One final transient Object and one exact delegating constructor preserve canonical record identity/schema and old constructors. Original outer BATTLE clock passes the same Snapshot to its later owned-ID lookup and complete original inner action sibling.', 'Different actor identity cannot borrow the earlier Snapshot. Same-object life/profile changes retain original expiry/retirement reachability, but starting a new action requires earlier identity/life/profile/pose/command and original first owner proof. Two complete original action/ingress siblings preserve cleanup and carry the same earlier Snapshot through the original Ref monitor wait. Existing immutable Ref metadata stores its detached weak View; original companion live predicate checks first weak actor/life under its existing pet monitor. Legitimate post-admission movement/command changes remain allowed. No new workers/TLS/registry/queue/SQL or action/RNG/accepted HP policy.', 'Independent inverse of2699 original methods plus2 complete original action/ingress siblings and1 exact Snapshot constructor;335 no-init classes.204 new contracts plus3222 inherited same-SHA checks;40 actual original action bodies,12 actual original outer Snapshot-to-lookup monitor waits,4 original Ref waits and16 original live calls across ARENA/PET/SUMMON/TAMED. Frozen replay byte-identical.20 SQL consumer classes and original full-room doll defense Snapshot byte-retained. Original memory-restore bodies are retained and ignore the new transient View.', 'Actual original outer wait fixtures reject same-ID replacement before replacement retirement, reject life/command rebasing before new action, and complete the original clock body. Original ingress Ref after its actual pet wait cannot retoken the first actor life. A later original live lookup rejects a replacement even if it borrows the same id and token. Legacy null Snapshot metadata and unloaded mode remain explicitly unqualified native fallback; no global or strong actor/owner/Context/Frame/map retention.', 'Full paired locking and earlier waits, owner replacement after original live lookup, NPC targets/bot/guardian/commands/hate/factory, physical/magic/area math/procs and accepted/final HP/status/effect/IO/Main/memory remain open. Positive ingress fixtures deliberately use the original DRAINING gate before Source Context/readers, not actual OPEN Source Context/HP/IO/backend/MySQL/game. Complete materials/controls/formal composition remain open.']
v['program_checks']=dict(JVM_no_initialization_classes=335,original_methods_independently_inverted=2699,complete_original_action_ingress_bodies_independently_inverted=2,exact_original_Snapshot_ctor_delegations=1,byte_identical_frozen_rebuild=True,existing_changed_class_entries=4,new_fieldless_helper_and_weak_View_entries=2,new_final_transient_Snapshot_proof_fields=1,new_Ref_fields=0,existing_immutable_Ref_proof_field_reused=True,pure_companion_first_actor_contract_scenarios=204,inherited_and_revalidated_same_SHA_pure_contract_scenarios=3222,actual_original_action_calls=40,actual_original_outer_Snapshot_to_lookup_monitor_waits=12,actual_original_Ref_monitor_wait_life_rejections=4,actual_original_live_calls=16,controlled_actor_families=['ARENA','PET','SUMMON','TAMED'],capture_under_original_actor_Snapshot_monitor_before_fields=True,same_object_retirement_reachability_after_life_profile_change=True,first_actor_life_not_retokened_after_original_Ref_wait=True,same_id_and_borrowed_token_replacement_rejected_at_original_live_lookup=True,post_admission_motion_command_changes_allowed=True,canonical_record_identity_schema_and_legacy_null_fallback_retained=True,no_strong_actor_owner_Context_Frame_map_or_registry=True,original_memory_restore_bodies_ignore_new_transient_View=True,original_earlier_full_room_defense_Snapshot_byte_retained=True,positive_ingress_stops_at_original_DRAINING_gate_before_Source_Context=True,new_SELECTs=0,new_connection_ownership=False,new_workers_TLS_registries_or_queues=False,original_HP_FIFO_Ctrl_competition_cost_RNG_Main_WORLD_inverse_retained=True,bound_actual_server_game_or_Main_shutdown_cases=0,full_paired_locks_and_owner_replacement_after_live_lookup_qualified=False,actual_OPEN_Source_Context_HP_final_IO_Main_qualified=False,full_other_producer_IO_Main_shutdown_and_Source_qualified=False)
for key in ['Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_controlled_first_Snapshot_actor_life_lookup_action_Ref_live_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-completion-goal-reset-requests/20261026-user-reset-request.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'paired-lifecycle':'Controlled original synchronized Snapshot now carries weak actor/profile/life through original owned-ID lookup/action/Ref and original locked live predicate, paired with earlier owner/audience proof. Same-object retirement remains reachable; new actions reject stale life/pose/command and later lookup rejects same-ID/borrowed-token actor replacement. Complete paired locks/no-parent/tie/earlier waits, owner replacement after live lookup, all accepted/final HP/status/effect/IO/scene/retirement/Main policies, actual shutdown/bounded memory and OPEN/DRAINING/STOPPED. Accepted target HP must stay independent of later actor/caster life. Six historical aliases remain open.', 'npc-factory-ai':'Original controlled Snapshot-to-lookup first weak actor/life/profile/stamp/pose/command now reaches action and Ref/live for ARENA/PET/SUMMON/TAMED, with earlier owner/audience preserved. Complete full paired locking, owner replacement after live lookup, NPC targets/bot/guardian/32-class/De/custom-work/spawn/trigger/hate/targetClear/reacquisition, full factory/reload/rollback/attributes/unknown/manual/master/family/reward/owner-meter. Original restore bodies ignore new transient View. Actual OPEN Source Context/HP/IO/Main and bound full memory remain open.', 'presentation':'Existing marked natural PVE accepted HP/self HPBAR retains first observer/role/life and final receiver proof. Controlled earlier owner Origin and first actor Snapshot now pass through original scope/Ref/live, but actual controlled OPEN Source Context/HP/final receiver remains unqualified. Complete attack/status/digit/effect/old/manual/bot/guardian receivers, spawn/status/login/meter/gfx, damage numbers/kiringku/proc_spr/suppression/resources, Main/memory and final backend/MySQL/game.'}
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
block='## 本次已更新的程式\n\n受控角色的原同步Snapshot已保存首次弱本體／Profile、既有life token、原stamp／kind／id／npc／pose／command，於原欄位讀取前擷取。新增一個final／transient Snapshot欄位與精確ctor delegate，保留canonical record identity／schema及舊ctor後備。原BATTLE clock將同一Snapshot傳入較晚的owned-ID查找與兩個完整原action／ingress sibling；同ID替身不能借用舊Snapshot，同一本體life／Profile變更仍可進入原expiry／retirement，但開始新動作必須符合原首次值。\n\n原Ref等待後必須符合首次actor life，既有immutable Ref proof欄位保存同一弱View；原live predicate在既有pet monitor內核對原本體與生命期。已admit後的正常移動／command變更仍允許；原accepted target HP不新增caster存活依賴。無新worker／TLS／registry／queue／SQL或action／RNG權責；較早全房間防禦快照保留。\n\n2699原方法＋2完整原sibling／1精確Snapshot ctor inverse、335 no-init、204新契約＋3222同SHA繼承通過。40次原action、12次實際原Snapshot→lookup鎖等待、4次原Ref等待、16次原live，涵蓋ARENA／PET／SUMMON／TAMED；原外層等待後換同ID本體不會退休替身，life／command變更不啟動新動作。同位元重建通過，20 SQL consumer保留，原memory restore body忽略新transient View。\n\n完整成對鎖定與較早等待、live lookup後換主人／本體、NPC target／bot／guardian／command／hate／factory、數值／proc／其他輸出、Main／記憶體仍未完成。正向ingress fixture停在原DRAINING gate，未建立Source Context／讀取器，不代表OPEN Source Context／HP／IO或實服驗收。58材料／252外控／正式0.6.71及6歷史current索引仍開放；全部程式完成後才集中後端／中文MySQL／實機驗收。未部署，不啟動實服或連MySQL，不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 controlled first Snapshot actor life：候選`{v['runtime']['sha256']}`，原同步Snapshot弱本體／life→原lookup／action／Ref／locked live，保留同本體expiry／retirement及已admit後正常motion／command。2699原方法＋2完整原sibling／1精確ctor inverse、335 no-init、204新＋3222同SHA繼承、40原action／12原outer等待／4原Ref等待／16原live與同位元重建。完整成對鎖定／OPEN Context／HP／IO／Main／完整Source仍開放；未部署／未連MySQL。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='controlled-first-Snapshot-actor-life-lookup-action-Ref-live-program-only',full_goal_achieved=False)
continuation['source_companion_first_actor_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
