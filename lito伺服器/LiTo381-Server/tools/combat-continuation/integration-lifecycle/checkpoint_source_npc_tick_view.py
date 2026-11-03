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
latest=read(D/'source-npc-tick-view-build-current.json');repro=read(D/'source-npc-tick-view-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_NPC_tick_view_contract_scenarios']==188
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_NPC_tick_view_contract_scenarios')==2721
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 1103 original methods plus2 complete original tick constructor bodies and1 exact Ref delegation','PASS324','constructors=72, rejected=58, original-Hub-roots=4, encounter-waits=2']:assert marker in log,marker
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
for name in ['NativeSourceNpcTickView.java','SourceNpcTickViewMethods.java','SourceNpcTickViewDelta.java','SourceNpcTickViewLoad.java','SourceNpcTickViewPlan.java','build_source_npc_tick_view.py','reproduce_source_npc_tick_view.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_npc_tick_view.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-original-NPC-first-view-through-tick-Ref-origin-and-live-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Four original Hub/escape output constructor operands pass the same original Snapshot. Two complete original tick constructor extensions store its weak view in one final/transient noncanonical field before original target-origin and attacker capture. The original encounter lookup consumes original weak actor/owner/life/stamp/id/NPC/force/generation under existing Hub and encounter monitors; legitimate post-action pose/motion changes are allowed. One exact prior9-argument Ref constructor delegation adds a final/transient weak-view field. The original natural Ref live-same result validates that same original identity under its existing monitor. No new actor/owner/Context/Frame roots, registry/TLS/worker/SQL or lock domain.', 'Independent inverse of1103 original methods plus2 complete original tick constructor bodies and1 exact Ref delegation;324 no-init classes.188 new contracts plus2721 inherited same-SHA checks;72 direct actual constructors,58 pre-capture/post-capture stale/foreign/weak/role/life rejections,4 actual original Hub normal/escape output calls and2 actual encounter-monitor waits. Same-id replacement including copied old life token, owner/generation/stamp/weak changes, accepted movement/heading, target role/life, record identity/schema and old constructor fallbacks checked. Frozen replay byte-identical;20 SQL consumers byte-retained.', 'Original NPC first-view authority now survives these normal/escape output ticks, attacker origin capture, immutable Ref and existing live validation. Complete initial PC target discovery before target-origin capture, full accepted-versus-final receiver HP/IO/role/retirement/Main policies, controlled/bot/guardian/other classes/manual/legacy origins, hate/targetClear/reacquisition/spawn/presentation, full58 materials/252 controls/formal0.6.71 and actual deferred backend/MySQL/game acceptance. Old null-view constructor paths remain unqualified fallback, not complete lifecycle coverage.']
v['program_checks']=dict(JVM_no_initialization_classes=324,original_methods_independently_inverted=1103,complete_original_tick_constructor_bodies_independently_inverted=2,exact_prior_Ref_constructor_delegations=1,original_Hub_escape_observed_snapshot_output_ports=4,byte_identical_frozen_rebuild=True,existing_changed_class_entries=5,new_fieldless_helper_class_entries=1,new_final_transient_weak_view_fields=2,new_canonical_record_components=0,pure_NPC_tick_view_contract_scenarios=188,inherited_and_revalidated_same_SHA_pure_contract_scenarios=2721,actual_direct_original_tick_constructors=72,actual_pre_or_post_capture_stale_identity_role_life_rejections=58,actual_original_Hub_normal_and_escape_output_calls=4,actual_original_encounter_lookup_wait_life_rejections=2,original_same_view_retained_in_tick_and_Ref=True,original_encounter_and_live_lookup_monitors_and_cleanup_retained=True,post_action_motion_and_pose_changes_allowed=True,no_post_wait_authority_refresh=True,old_canonical_constructor_schema_and_fallback_unqualified_retained=True,new_strong_actor_owner_Context_Frame_registry_or_TLS=False,new_SELECTs=0,new_connection_ownership=False,new_workers_or_scheduled_tasks=False,other_original_HP_FIFO_Ctrl_competition_cost_RNG_Main_WORLD_inverse_retained=True,bound_selection_cost_RNG_HP_IO_role_Main_cases=0,full_accepted_receiver_HP_IO_Main_and_original_PC_discovery_qualified=False)
for key in ['Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_NPC_original_first_view_tick_Ref_origin_and_live_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
updates={'paired-lifecycle':'Original first NPC view now survives normal/escape tick constructors, attacker origin capture, immutable Ref and existing live checks under original monitors. Complete original PC target discovery before the target-origin capture, phase-specific accepted-versus-final receiver HP/IO/role/retirement/Main policies, controlled/bot/guardian/class families/manual legacy origins, manual/no-parent locks/ties/earlier waits, bounded memory and OPEN/DRAINING/STOPPED. Do not add blanket source-alive vetoes to already target-owned acceptance. Old null views and six historical aliases remain open.', 'npc-factory-ai':'First-view authority now protects AI/move entry and original normal/escape tick/Ref/origin/live handoffs, preserving legitimate post-action pose/motion and original costs/summons. Complete initial PC target discovery, all controlled/bot/guardian/32-class/De/custom-work/spawn/trigger/hate/targetClear/reacquisition, initial factory/reload/rollback/full attributes/unknown/manual/master/family/reward/owner-meter and full receiver/Main. No bound AI/scene/Main acceptance yet.', 'presentation':'Original weak NPC view now reaches normal/escape tick publication, attacker origin, immutable Ref and original live validation without retokening a replacement actor after encounter unlock. Finish original PC target discovery, accepted/final receiver/role/scene/retirement/Main/IO policies, spawn heading/status/pack/login/observer/brave/meter/gfx, HPBAR/digits/kiringku/proc_spr/suppression/temp resources and other AI families. No bound full Source/IO acceptance; actual backend/MySQL/game remains deferred.'}
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
block='## 本次已更新的程式\n\n同一原NPC快照已從4個原Hub／escape輸出constructor傳入2個完整原tick constructor延伸，在原target-origin與attacker lookup前存入final／transient弱view。原encounter lookup及自然NPC Ref live-same結果，在既有Hub／encounter monitor核對原weak actor／owner、life／stamp／id／NPC／force／generation；不採用替換角色的新token。Ref透過原9參數constructor精確委派新增弱view，原record identity／canonical schema與舊ctor未改。動作成功後正常改方向／位置仍允許；不新增角色強參照／registry／TLS／worker／SQL或鎖域。\n\n1103原方法＋2完整原tick ctor＋1精確Ref delegate獨立inverse、324 no-init、188新契約＋2721同SHA繼承重驗、72直接原constructor、58前後生命期／同ID／複製舊token／foreign／weak／role拒絕、4次原Hub正常／逃跑輸出與2次原encounter等待換生命期，以及同位元重建通過。20 SQL consumer class保留；不是實服／Main／MySQL／游標或完整最終IO驗收。\n\n下一段是target-origin capture之前的原PC target初始發現與生命期，並補accepted／final receiver HP／IO／role／retirement／Main完整政策，保留原target-owned接受語意。其他受控／bot／guardian／subclass／manual／legacy、hate／targetClear／重新索敵／spawn／presentation、58材料／252外控／正式0.6.71隔離合成仍待完成。舊null-view只是未標記後備；6處歷史current索引仍未解。全部程式完成後才集中同SHA後端、中文MySQL真裝備角色唯讀與實機游標驗收；未部署、不啟動實服或連MySQL，不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 NPC first-view tick／Ref：候選`{v['runtime']['sha256']}`，4原輸出operand→2完整原tick ctor→原attacker encounter capture／immutable Ref／live檢查；既有monitor／cleanup／WORLD與target-owned接受保留，成功動作的pose／motion變更仍允許。1103原方法＋2 ctor／1精確delegate inverse、324 no-init、188新＋2721同SHA繼承、72直接ctor／58拒絕／4原Hub root／2原encounter等待與同位元重建。PC初始發現／accepted-final HP-IO-Main及其他完整Source仍開放；未部署／未連MySQL。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='original-NPC-first-view-tick-Ref-origin-and-live-program-only',full_goal_achieved=False)
continuation['source_npc_tick_view_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
