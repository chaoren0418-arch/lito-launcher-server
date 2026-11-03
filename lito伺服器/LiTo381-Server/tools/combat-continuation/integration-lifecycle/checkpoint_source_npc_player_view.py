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
latest=read(D/'source-npc-player-view-build-current.json');repro=read(D/'source-npc-player-view-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_NPC_player_view_contract_scenarios']==96
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_NPC_player_view_contract_scenarios')==2909
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 1368 original methods plus12 complete original selection/append/synchronized-AI/escape/tick bodies','PASS326','original-roots=12, AI-move-waits=7, escape-footprint-rejections=2, constructors=14']:assert marker in log,marker
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
for name in ['NativeSourceNpcPlayerView.java','SourceNpcPlayerViewMethods.java','SourceNpcPlayerViewDelta.java','SourceNpcPlayerViewLoad.java','SourceNpcPlayerViewPlan.java','build_source_npc_player_view.py','reproduce_source_npc_player_view.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_npc_player_view.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-original-NPC-first-PC-discovery-through-selection-AI-waits-and-tick-origin-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['The two original PVE target selection-to-append handoffs now transport an earlier player Origin. Twelve complete original selection/append/synchronized-AI/escape/tick constructor siblings retain original bodies. Under the original Hub monitor, room-member/candidate life precedes binding at first discovery; original eligibility/force/distance/unsigned tie criteria are retained, duplicate observations do not refresh authority, and the immutable output audience stays original room members plus primary, excluding nonselected roster-only candidates.', 'The exact earlier Origin and primary Member reach the original tick target-origin field without recapture. Seven original synchronized AI/move entries recheck the first player proof after their existing monitor wait before RNG/costs; both original escape monitor phases validate across footprint discovery. No new persistent fields, lock domain, actor registry/TLS/worker/SQL. Unloaded/null legacy routes remain explicitly unqualified fallback.', 'Independent inverse of1368 original methods plus12 complete original bodies;326 no-init classes.96 new contracts plus2909 inherited same-SHA checks;12 actual original monsterAiTicks roots with same/role/life targets across normal/escape,14 original selection comparisons,7 actual AI/move monitor waits with player-life changes,2 target-life changes during escape footprint discovery and14 actual tick constructors preserving original Member across role/life/scene/source/account/disconnect. Frozen replay byte-identical;20 SQL consumers byte-retained.', 'Complete initial target authority and phase-specific accepted-versus-final receiver HP/IO/role/source-retirement/Main policies for other controlled/bot/guardian/classes and manual/legacy callers. Preserve target-owned accepted HP rather than introducing a blanket current attacker/caster veto. Persistent hate/targetClear/reacquisition/spawn/presentation, full58 materials/252 controls/procs/formal0.6.71 and actual deferred backend/MySQL/game acceptance remain open.']
v['program_checks']=dict(JVM_no_initialization_classes=326,original_methods_independently_inverted=1368,complete_original_selection_append_AI_escape_tick_bodies_independently_inverted=12,original_PVE_selection_to_append_handoffs=2,byte_identical_frozen_rebuild=True,existing_changed_class_entries=5,new_fieldless_helper_and_invocation_local_builder_class_entries=2,new_persistent_fields=0,pure_NPC_player_view_contract_scenarios=96,inherited_and_revalidated_same_SHA_pure_contract_scenarios=2909,actual_original_monsterAiTicks_roots=12,actual_original_selection_comparisons=14,actual_original_AI_move_monitor_wait_player_life_rejections=7,actual_escape_footprint_player_life_rejections=2,actual_direct_original_tick_constructors=14,earliest_player_life_precedes_binding_and_original_selection_checks=True,original_eligibility_force_distance_unsigned_tie_inverse_retained=True,exact_earlier_Origin_and_primary_Member_reused_in_tick=True,output_audience_not_widened_by_nonselected_roster_only_candidates=True,no_authority_refresh_after_wait=True,old_unloaded_null_and_PC_legacy_routes_unqualified_retained=True,new_registry_or_TLS=False,new_SELECTs=0,new_connection_ownership=False,new_workers_or_scheduled_tasks=False,original_locks_cleanup_HP_FIFO_Ctrl_competition_cost_RNG_Main_WORLD_inverse_retained=True,bound_selection_cost_RNG_HP_IO_role_Main_cases=0,complete_accepted_receiver_HP_IO_Main_and_other_AI_qualified=False)
for key in ['Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_NPC_original_first_PC_discovery_selection_AI_wait_and_tick_origin_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
updates={'paired-lifecycle':'Original PVE PC first discovery now retains life-before-binding through unchanged target selection, seven existing AI/move monitor waits, two escape phases and original tick Origin without recapture. Complete phase-specific initial/accepted/final receiver HP/IO/role/scene/source-retirement/Main policies and full paired locking for other controlled/bot/guardian/classes/manual callers, all no-parent/tie/earlier-wait cases, bounded memory and OPEN/DRAINING/STOPPED. Keep target-owned accepted HP independent from blanket current-attacker/caster checks. Legacy fallbacks and six historical aliases remain open.', 'npc-factory-ai':'Original natural NPC first-view and initial PVE PC target proofs now cover unchanged selection, AI/move scheduling waits, escape footprint rechecks and tick/Ref/origin/live transport. Complete controlled/bot/guardian/32-class/De/custom-work/spawn/trigger/hate/targetClear/reacquisition, initial factory/reload/rollback/full attributes/unknown/manual/master/family/reward/owner-meter and full accepted/final receiver/Main. No bound complete AI/scene/Main acceptance yet.', 'presentation':'Original first NPC and first PVE PC target identity/life now reach these natural AI/move/escape/tick/Ref/origin paths; immutable audience stays original room members plus primary. Finish accepted-versus-final HP/IO/receiver role/scene/retirement/Main and other AI families, source spawn heading/status/pack/login/observer/brave/meter/gfx, HPBAR/digits/kiringku/proc_spr/suppression/temp resources. Existing legacy/unknown origins and actual backend/MySQL/game remain unqualified/deferred.'}
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
block='## 本次已更新的程式\n\n兩個原PVE索敵→append呼叫已攜帶首次玩家Origin，透過12個完整原selection／append／synchronized AI／escape／tick constructor sibling接線。在原Hub monitor內，房間成員與候選第一次發現即先life後binding，不重採重複候選；原資格／force／距離／unsigned tie規則保留。不可變接收名單維持原房間成員加primary，不加入非選中的roster-only候選。\n\n七個原AI／move同步入口，在既有等待後、原RNG／成本前驗證首次PC proof；escape在前後兩個原monitor跨footprint discovery驗證。原tick target-origin欄位直接保留同一早期Origin與Member，不在constructor採新角色／token。無新持久欄位／鎖域／registry／TLS／worker／SQL；unloaded／null舊路徑仍是明確未標記後備。\n\n1368原方法＋12完整原body inverse、326 no-init、96新契約＋2909同SHA繼承、12次原monsterAiTicks正常／逃跑same／role／life呼叫、14次原索敵比較、7次原AI／move等待換PC生命期、2次escape footprint期間PC生命期變更、14次原tick ctor早期Member保留及同位元重建通過。20 SQL consumer class保留；不是完整accepted／final HP／IO／Main／實服／MySQL／游標驗收。\n\n仍須補其他受控／bot／guardian／subclass／manual／legacy的初始權限、完整accepted／final receiver HP／IO／role／retirement／Main與成對鎖定，保留target-owned接受語意；hate／targetClear／重新索敵／spawn／presentation、58材料／252外控／proc／正式0.6.71隔離合成及6處歷史current索引仍開放。全部程式完成後才集中同SHA後端、中文MySQL真裝備角色唯讀與實機游標驗收；未部署、不啟動實服或連MySQL，不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 NPC first-PC discovery：候選`{v['runtime']['sha256']}`，2原PVE選取→append保留首次life-before-binding，12完整原selection／AI／escape／tick body接線；7原AI／move等待與escape兩次monitor驗證，原tick重用同一Origin／Member，接收名單不擴大。1368原方法＋12 sibling inverse、326 no-init、96新＋2909同SHA繼承、12原root／14索敵比較／7等待／2 footprint失效／14 ctor與同位元重建。完整accepted-final HP-IO-Main、其他AI與Source仍開放；未部署／未連MySQL。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='original-NPC-first-PC-discovery-selection-AI-waits-tick-origin-program-only',full_goal_achieved=False)
continuation['source_npc_player_view_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
