"""Checkpoint controlled first-owner clock wiring; keep original earlier defense and qualified baseline."""
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
latest=read(D/'source-companion-clock-build-current.json');repro=read(D/'source-companion-clock-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_companion_clock_contract_scenarios']==121
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_companion_clock_contract_scenarios')==3101
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 3551 original methods plus4 complete original clock/action/ingress/scope bodies and1 exact constructor','PASS333','PASS121 Source companion first-clock','stale-clock-calls=24, original-ingresses=8, original-Ref-waits=4, original-action-calls=8']:assert marker in log,marker
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
for name in ['NativeSourceCompanionClock.java','SourceCompanionClockMethods.java','SourceCompanionClockDelta.java','SourceCompanionClockLoad.java','SourceCompanionClockPlan.java','build_source_companion_clock.py','reproduce_source_companion_clock.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_companion_clock.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-controlled-first-owner-clock-Ref-scope-original-defense-retained-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Original synchronized BATTLE Hub companion-clock discovery captures first owner and room audience with original role/life/observer proofs. One final transient Object on the existing two-component CompanionTick record carries the same Origin without changing canonical constructors/equality/schema. Four complete original clock/action/ingress/scope siblings and one exact delegating constructor preserve original bodies and cleanup.', 'Owner and original owned-companion identity are checked before new clock work and again after the original first pet Snapshot wait. The original companion Ref is stamped with the earlier owner member, and the original scope reuses the exact early Origin. First selected PC proofs reject stale role/life before original movement/strike/RNG. Non-PC, legacy unmarked and WORLD paths retain original fallback; no new worker/TLS/registry/queue/SQL or action/RNG logic.', 'Independent inverse of3551 original methods plus4 complete original siblings and1 exact constructor;333 no-init classes.121 new contracts plus3101 inherited same-SHA checks;24 actual stale outer clock calls,8 original ingresses,4 actual original companion Ref monitor waits and8 original inner action calls across ARENA/PET/SUMMON/TAMED. Frozen replay byte-identical. Original full-room doll defense snapshot and20 SQL consumer classes byte-retained.', 'Review correction: an isolated experimental later defense substitution was removed before this candidate. The earlier doll defense Snapshot already holds all room members. This candidate retains it and does not resample from a later target Context. The superseded experimental candidate is not promoted or checkpointed.', 'Original controlled actor Snapshot-to-lookup birth/life provenance remains open, as do full paired locks, controlled NPC targets, bot/guardian/command/hate/factory paths and actual OPEN Source Context/HP/final IO/Main/memory qualification. Detached positive ingress fixtures intentionally stop at the original DRAINING gate before Source Context/readers. They do not qualify actual HP/IO/backend/MySQL/game; full materials/controls/formal composition remain open.']
v['program_checks']=dict(JVM_no_initialization_classes=333,original_methods_independently_inverted=3551,complete_original_clock_action_ingress_scope_bodies_independently_inverted=4,exact_original_clock_ctor_delegations=1,byte_identical_frozen_rebuild=True,existing_changed_class_entries=5,new_fieldless_helper_entries=1,new_final_transient_clock_proof_fields=1,pure_companion_clock_contract_scenarios=121,inherited_and_revalidated_same_SHA_pure_contract_scenarios=3101,actual_original_stale_clock_calls=24,actual_original_ingresses=8,actual_original_Ref_monitor_wait_life_rejections=4,actual_original_inner_action_calls=8,controlled_actor_families=['ARENA','PET','SUMMON','TAMED'],first_owner_and_audience_role_life_observer_reused=True,original_Ref_master_life_not_retokened_after_wait=True,first_selected_PC_checked_before_movement_strike_RNG=True,same_id_owned_companion_replacement_rejected=True,original_earlier_full_room_defense_Snapshot_byte_retained=True,experimental_later_defense_resampling_removed=True,legacy_record_identity_and_WORLD_fallback_retained=True,positive_ingress_stops_at_original_DRAINING_gate_before_Source_Context=True,new_SELECTs=0,new_connection_ownership=False,new_workers_TLS_registries_or_queues=False,original_HP_FIFO_Ctrl_competition_cost_RNG_Main_WORLD_inverse_retained=True,bound_actual_server_game_or_Main_shutdown_cases=0,controlled_actor_first_Snapshot_to_lookup_life_provenance_qualified=False,actual_OPEN_Source_Context_HP_final_IO_Main_qualified=False,full_other_producer_IO_Main_shutdown_and_Source_qualified=False)
for key in ['Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_controlled_first_owner_clock_Ref_scope_selected_player_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-completion-goal-reset-requests/20261026-user-reset-request.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'paired-lifecycle':'Controlled BATTLE clock now retains first owner/audience through original Ref wait and scope, checking original owned actor and first selected PC before new action/RNG. Earlier full-room defense map is retained. Complete original controlled actor Snapshot-to-lookup birth/life, paired locks/no-parent/tie/earlier waits, all accepted/final HP/status/effect/IO/scene/retirement/Main policies, actual shutdown/bounded memory and OPEN/DRAINING/STOPPED. Accepted target HP must stay independent of current NPC/caster life. Six historical aliases remain open.', 'npc-factory-ai':'First owner/audience clock proof now reaches original controlled action/Ref/scope and first selected PC for ARENA/PET/SUMMON/TAMED. Complete original controlled actor Snapshot-to-lookup proof, NPC targets, bot/guardian/32-class/De/custom-work/spawn/trigger/hate/targetClear/reacquisition, original factory/reload/rollback/full attributes/unknown/manual/master/family/reward/owner-meter and other receiver/Main policies. Positive fixture ingress uses original DRAINING gate before Source Context/readers; actual OPEN Source Context/HP/IO remains open.', 'presentation':'Existing marked natural PVE accepted HP/self HPBAR retains original first observer/role/life audience and final receiver proof. Controlled early Origin now carries first observer members to existing scope, but actual controlled OPEN Source Context/HP/final receiver is unqualified. Original earlier defense full-room map is retained. Complete old/manual/bot/guardian/attack/status/digit/effect receivers, spawn/status/login/meter/gfx, damage numbers/kiringku/proc_spr/suppression/resources, Main/memory and final backend/MySQL/game.'}
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
block='## 本次已更新的程式\n\n受控角色的首次主人／接收者生命期已接上原BATTLE companion clock。原Hub同步發現主人時保存room audience的role／life／observer；既有兩欄CompanionTick新增final／transient proof欄位，原record identity與schema保留。四個完整原clock／action／ingress／scope sibling與一個精確ctor delegate，將同一Origin帶過原主人／pet等待；新動作前及原pet Snapshot等待後核對主人與owned pet，原Ref沿用較早主人life，首次選中PC在移動／strike／RNG前核對原生命期。WORLD／legacy後備及原成本／HP／expiry／retirement／清理保留；無新worker／TLS／registry／queue／SQL。\n\n3551原方法＋4完整原sibling／1精確ctor inverse、333 no-init、121新契約＋3101同SHA繼承通過；24次stale clock、8次原ingress、4次原Ref monitor等待與8次原inner action，涵蓋ARENA／PET／SUMMON／TAMED。同位元重建通過；20 SQL consumer及原NativeSourceDollDefense／Snapshot class保留。\n\n稽核修正：較早的防禦Snapshot本來已保存全房間玩家，隔離實驗中的較晚target Context防禦替換已撤回，本候選不重新採樣。較早實驗JAR未提升成進度或正式版本。\n\n原受控actor Snapshot→lookup首次出生／生命期仍待接線，完整成對鎖定、NPC target／bot／guardian／command／hate／factory、其他輸出及Main／記憶體亦未完成。正向ingress fixture刻意停在原DRAINING gate，未建立Source Context／讀取器，不代表OPEN Source Context／HP／IO或實服驗收。58材料／252外控／正式0.6.71仍須補齊，6歷史current索引未解；全部程式完成後再集中後端／中文MySQL／實機驗收。未部署，不啟動實服或連MySQL，不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 controlled first-owner clock：候選`{v['runtime']['sha256']}`，原Hub首次主人／audience→CompanionTick→原action／Ref等待／scope→首次selected PC，保留較早全房間防禦快照。3551原方法＋4完整原sibling／1精確ctor inverse、333 no-init、121新＋3101同SHA繼承、24 stale clock／8 ingress／4原Ref等待／8 inner action與同位元重建。受控actor Snapshot→lookup生命期／完整成對鎖定／OPEN Context／HP／IO／Main／完整Source仍開放；未部署／未連MySQL。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='controlled-first-owner-clock-Ref-scope-original-defense-retained-program-only',full_goal_achieved=False)
continuation['source_companion_clock_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
