"""Preserve original first-view AI/move program evidence without promoting the qualified baseline."""
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
latest=read(D/'source-npc-hp-output-build-current.json');repro=read(D/'source-npc-hp-output-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_NPC_HP_output_contract_scenarios']==96
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_NPC_HP_output_contract_scenarios')==3005
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 2575 original methods plus1 complete original synchronized final frame,1 exact Member ctor and1 existing Batch bridge','PASS331','original-memory-IO-frames=6, original-route-parity-frames=6, final-receiver-waits=2']:assert marker in log,marker
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
for name in ['NativeSourceNpcHpOutput.java','SourceNpcHpOutputMethods.java','SourceNpcHpOutputDelta.java','SourceNpcHpOutputLoad.java','SourceNpcHpOutputPlan.java','build_source_npc_hp_output.py','reproduce_source_npc_hp_output.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_npc_hp_output.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-target-owned-accepted-NPC-HP-HPBAR-final-receiver-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Original first NPC player ingress stores nullable immutable observer metadata before its original life/binding capture, using one exact original explicit-life/pose Member constructor delegate. Original NPC damage binds detached target/audience values to one existing Plan slot. After the original death seal and accepted-participant capture, immutable target/death/HP/maximum values seal under the existing vital lock. No producing Frame/Context/Scope/NPC Ref/map is retained by these values.', 'Original HPBAR broadcast order and duplicates are retained while receivers intersect the earlier audience. HP opcodes13/104 use the existing optional captureSelf Batch output list or immediate output; one complete original synchronized final-frame sibling checks the detached accepted target/receiver/life/observer/bytes again before the original cipher/socket/log/inventory body. Source hpbar suppresses bars rather than self HP. Already accepted target output does not depend on current NPC/caster liveness and may flush in DRAINING until STOPPED/closed. No new worker/queue/TLS/registry/SQL or HP/cost/RNG logic.', 'Independent inverse of2575 original methods plus1 complete original synchronized final frame,1 exact Member ctor and1 existing Batch bridge;331 no-init classes.96 new contracts plus3005 inherited same-SHA checks. Six guarded original encrypted in-memory frames match six original self/static-frame-route ciphertexts; two actual original receiver-monitor waits reject role changes before cipher/socket. Actual acceptedState/death seal, unsealed marked input rejection, legacy unmarked fallback, original recipient intersection, observer, packet cloning, hpbar and DRAINING/STOPPED/closed checks pass. Frozen replay byte-identical;20 SQL consumer classes byte-retained.', 'Scope covers marked natural PVE origins with first observer metadata and HP/self-HPBAR opcodes13/104. Complete old/manual/controlled/bot/guardian/unknown authority, other attack/status/digit/effect/final receiver paths, all paired locking/math/procs/materials/252 controls/formal0.6.71 and actual Main shutdown/memory/backend/MySQL/game. Detached encrypted memory-socket tests do not qualify actual game/backend/Main shutdown or complete Source.']
v['program_checks']=dict(JVM_no_initialization_classes=331,original_methods_independently_inverted=2575,complete_original_synchronized_final_frame_bodies_independently_inverted=1,exact_original_explicit_life_pose_Member_ctor_delegations=1,existing_optional_captureSelf_Batch_output_bridges=1,original_discovery_bind_accepted_capture_and_audience_ports=5,byte_identical_frozen_rebuild=True,existing_changed_class_entries=7,new_fieldless_helper_and_detached_record_entries=5,new_original_Plan_visible_detached_proof_slot=1,new_original_Member_final_transient_nullable_observer_slot=1,pure_NPC_HP_output_contract_scenarios=96,inherited_and_revalidated_same_SHA_pure_contract_scenarios=3005,actual_original_encrypted_guarded_memory_IO_frames=6,actual_original_route_ciphertext_parity_frames=6,actual_original_final_receiver_monitor_wait_role_rejections=2,actual_original_acceptedState_and_death_participant_hook=True,unsealed_marked_input_and_legacy_unmarked_fallback_distinguished=True,final_rejection_before_cipher_and_socket=True,accepted_target_only_authority_without_current_NPC_caster_liveness=True,already_accepted_DRAINING_allowed_STOPPED_and_closed_rejected=True,original_audience_order_duplicates_intersect_first_receivers=True,Source_hpbar_controls_bars_and_not_self_HP=True,queued_values_retain_no_Frame_Context_Scope_Ref_or_map=True,new_SELECTs=0,new_connection_ownership=False,new_workers_TLS_registries_or_queues=False,original_HP_FIFO_Ctrl_competition_cost_RNG_Main_WORLD_inverse_retained=True,bound_actual_server_game_or_Main_shutdown_cases=0,full_other_producer_IO_Main_shutdown_and_Source_qualified=False)
for key in ['Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_NPC_target_owned_accepted_HP_HPBar_final_receiver_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
updates={'paired-lifecycle':'Marked natural PVE first player discovery now reaches target-only accepted NPC HP output: original death/participant boundary seals values under vital lock and the original synchronized receiver frame checks original role/life/observer/packet before cipher/socket. Existing Batch only; no pending Frame/Context/Scope/Ref/map. Complete old/manual/controlled/bot/guardian/class authority, all other accepted/final HP/status/effect/IO/scene/retirement/Main policies, full paired no-parent/tie/earlier-wait locking, actual shutdown/bounded memory and OPEN/DRAINING/STOPPED. Keep accepted target HP independent of current NPC/caster liveness. Six historical aliases remain open.', 'npc-factory-ai':'Original NPC/player first proofs cover natural PVE selection, AI/move waits, escape/tick/Ref/origin/live and marked accepted HP/self HPBAR final receiver. Complete controlled/bot/guardian/32-class/De/custom-work/spawn/trigger/hate/targetClear/reacquisition, original factory/reload/rollback/full attributes/unknown/manual/master/family/reward/owner-meter and all other receiver/Main policies. No bound complete AI/scene/Main shutdown acceptance yet.', 'presentation':'Marked natural PVE accepted HP/self HPBAR now uses first observer/role/life audience, post-HP target/death seal and final original receiver monitor, retaining Source hpbar control and ciphertext parity with original route. Only opcodes13/104 and tagged first-discovery origins are wired. Complete old/manual/controlled/bot/guardian/other attack/status/digit/effect receivers, source spawn heading/status/pack/login/observer/brave/meter/gfx, damage numbers/kiringku/proc_spr/suppression/resources, full Main shutdown/memory and actual backend/MySQL/game.'}
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
block='## 本次已更新的程式\n\n標記自然PVE來源的HP／HPBAR已補接受後的最終接收端。首次玩家ingress在原life／binding之前保存nullable final／transient observer標記；原NPC damage在既有Plan保存分離的target／audience值，原death seal與accepted participant後於vital lock內封存接受後的目標生命期與HP／maximum，不保留Frame／Context／Scope／NPC Ref／map。\n\n原HPBAR名單維持順序／重複但限於首次接收者。opcode13／104沿用既有可選captureSelf Batch輸出或即時送出，完整原synchronized final-frame sibling在加密／socket前再核對原目標與接收者role／life／observer／bytes，原cipher／socket／log／inventory body保留。Source hpbar只關閉bar、不關閉自身HP；已接受輸出不新增NPC／caster存活條件，DRAINING可完成，STOPPED／closed拒絕。無新worker／queue／TLS／registry／SQL或HP／成本／RNG變更。\n\n2575原方法＋1完整原final frame／1精確Member ctor／1既有Batch bridge inverse、331 no-init、96新契約＋3005同SHA繼承；6個新路徑的原加密記憶體IO frame與6個原self／static-frame路徑密文一致，2次原接收端monitor等待換角在cipher前拒絕。實際acceptedState／death封存、未封存input拒絕、legacy後備、觀察者／封包複製／hpbar／DRAINING／STOPPED／closed與同位元重建通過；20 SQL consumer class保留。\n\n範圍僅first-observer標記自然PVE與opcode13／104。舊manual／受控／bot／guardian／unknown權限、其他attack／status／digit／effect／receiver、完整成對鎖定／數值／proc／58材料／252外控／正式0.6.71及實際Main shutdown／記憶體／後端仍待補齊；6歷史current索引未解。記憶體Socket不是實服／實機／MySQL驗收，全部程式完成後才集中驗證；未部署、不啟動實服或連MySQL，不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 NPC accepted HP／HPBAR final receiver：候選`{v['runtime']['sha256']}`，首次observer／life／binding→既有Plan→原vital-lock接受後target／death seal→原同步final frame，兩HP packet kinds保留原cipher／socket／log及既有Batch；無current NPC／caster存活新依賴。2575原方法＋1完整final frame／精確ctor／Batch bridge inverse、331 no-init、96新＋3005同SHA繼承、6 guarded＋6原路徑密文比對／2原接收鎖等待及同位元重建。其他producer／IO／Main shutdown／完整Source仍開放；未部署／未連MySQL。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='target-owned-accepted-NPC-HP-HPBAR-final-receiver-program-only',full_goal_achieved=False)
continuation['source_npc_hp_output_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
