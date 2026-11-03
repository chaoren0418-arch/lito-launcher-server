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
latest=read(D/'source-npc-view-action-build-current.json');repro=read(D/'source-npc-view-action-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_NPC_view_action_contract_scenarios']==205
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_NPC_view_action_contract_scenarios')==2516
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 1322 original methods plus7 complete original synchronized siblings','PASS323','detached calls=97, rejected=84, monitor-waits=7, catalog-kind-decisions=6']:assert marker in log,marker
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
for name in ['NativeSourceNpcViewAction.java','SourceNpcViewActionMethods.java','SourceNpcViewActionDelta.java','SourceNpcViewActionLoad.java','SourceNpcViewActionPlan.java','build_source_npc_view_action.py','reproduce_source_npc_view_action.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_npc_view_action.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-original-NPC-first-view-through-synchronized-AI-and-move-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Seven original Hub operands carry the same original Snapshot into five AI entry siblings and two move siblings, retaining the original synchronized access and every original body/delegate. A narrow first-view guard at entry checks original weak actor/owner identity, original life/motion/stamp/pose/NPC/force and owner generation before the first original instruction/RNG. The existing encounter monitor remains held through the original delegated core and all scheduling; no additional lock domain, fields, registry/TLS, refresh, worker or SQL.', 'Independent inverse of1322 original methods plus7 complete original synchronized siblings and7 original observed Snapshot operands;323 no-init classes.205 new detached contracts plus2516 inherited same-SHA checks;97 detached calls,84 rejects,7 actual original-monitor waits with life changes,6 original catalog magic/physical/summon decisions including magic costs, schedules, summon use counts and actual child creation. Frozen replay byte-identical;20 SQL consumers byte-retained.', 'This stage protects normal/physical/magic/summon AI decisions/scheduling and pursuit movement at these original PVE entries. Final tick-origin construction occurs outside the encounter monitor and still needs original admitted authority through final attacker/receiver/IO. PC target original discovery, controlled/bot/guardian/other subclasses, persistent hate/target clear/reacquisition/spawn/presentation/Main/source retirement and full58 materials/252 controls/formal0.6.71 remain open. Legacy null first views remain unqualified fallback. Actual backend/MySQL/game acceptance is deferred until all code.']
v['program_checks']=dict(JVM_no_initialization_classes=323,original_methods_independently_inverted=1322,complete_original_synchronized_siblings_independently_inverted=7,original_Hub_observed_snapshot_operand_ports=7,byte_identical_frozen_rebuild=True,existing_changed_class_entries=3,new_fieldless_helper_class_entries=1,new_fields=0,pure_NPC_view_action_contract_scenarios=205,inherited_and_revalidated_same_SHA_pure_contract_scenarios=2516,actual_detached_calls=97,actual_stale_foreign_weak_life_pose_generation_rejections=84,actual_original_monitor_wait_life_rejections=7,actual_original_catalog_magic_physical_summon_decisions=6,actual_original_summon_child_creation=True,guard_before_original_RNG_and_mutation=True,original_encounter_monitor_held_through_original_core_and_schedule=True,original_snapshot_authority_not_refreshed_after_wait=True,legacy_null_view_fallback_explicitly_unqualified=True,new_strong_actor_owner_Context_Frame_registry_or_TLS=False,new_SELECTs=0,new_connection_ownership=False,new_workers_or_scheduled_tasks=False,other_original_HP_FIFO_Ctrl_competition_cost_RNG_Main_WORLD_inverse_retained=True,bound_selection_cost_RNG_HP_IO_role_Main_cases=0,final_attacker_PC_target_receiver_and_full_Main_lifecycle_qualified=False)
for key in ['Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_NPC_original_first_view_AI_and_move_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
updates={'paired-lifecycle':'Original first NPC view now reaches five synchronized AI and two synchronized movement entries before RNG and all original delegated core/schedules. Seven actual monitor waits reject life changes. Carry original admitted authority through final tick-origin/attacker/receiver/IO and all PC target discovery; finish controlled/bot/guardian/class families, manual/no-parent locks/ties/earlier waits, role/death/revival/same-id/scene/owner/retirement, bounded memory and Main OPEN/DRAINING/STOPPED. Legacy null views and six historical aliases remain unqualified/open.', 'npc-factory-ai':'First-view authority now protects original normal/physical/magic/summon decisions/schedules and pursuit movement under their existing encounter monitor, with original catalog costs/summon counters/actual children tested. Complete final-output/origin lifetime, persistent hate/targetClear/reacquisition,32-class/De/controlled/bot/guardian/custom-work/spawn/full trigger semantics, initial factory/reload/rollback/full attributes/unknown/manual/master/family/reward/owner-meter. No bound AI/scene/Main acceptance yet.', 'presentation':'Original NPC view reaches synchronized AI decisions/schedules and movement, but tick constructors still capture origin after releasing the encounter monitor. Wire original admitted authority through attacker/final receiver/scene/role/Main/IO without a fresh post-wait identity. Complete spawn heading/status/pack/login/observer motion/brave/meter/gfx and HPBAR/digits/kiringku/proc_spr/suppression/temp resources. Full Source and real backend/MySQL/game remain open.'}
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
block='## 本次已更新的程式\n\n原NPC快照已傳至5個AI與2個追擊移動入口，於原synchronized方法第一次原指令／RNG之前驗證弱actor／owner身分、原life／motion／stamp／pose／NPC／force與encounter generation。原monitor持續涵蓋原delegate core及排程，未新增鎖域／欄位／角色快取／TLS／worker／SQL，拒絕不補採新token。原方法與未標記舊ctor後備保留，後者明確未驗收。\n\n1322原方法＋7完整同步sibling及7原Snapshot operand獨立inverse、323 no-init、205新契約＋2516同SHA繼承重驗、97隔離呼叫／84拒絕／7次原monitor實際等待期間life變更、6次原catalog魔法／物理技能／召喚決策（含MP成本、排程、召喚次數及實際子角色建立）與同位元重建通過，20 SQL consumer class保留。這些不是實服／Main／MySQL／游標驗收。\n\n下一段須將原admitted權限穿過encounter鎖外的tick origin constructor、attacker／final receiver／IO；全部PC target初始發現、其他受控／bot／guardian／subclass、hate／targetClear／重新索敵／spawn／presentation／Main／retirement與完整58材料／252外控／正式0.6.71隔離合成仍待完成。6處歷史current索引不改寫為通過。全部程式完成後才集中同SHA後端、中文MySQL真裝備角色唯讀與實機游標驗收；未部署、不啟動實服或連MySQL，不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 NPC first-view AI／move：候選`{v['runtime']['sha256']}`，原快照傳至5 AI／2 move完整同步入口，在原RNG前驗證且原monitor涵蓋core／排程。1322原方法＋7 sibling inverse、323 no-init、205新＋2516繼承同SHA、97隔離呼叫／84拒絕／7次原鎖等待失效／6種族別決策案例與同位元重建。最終origin／IO、PC target／其他AI與完整Source／Main仍開放。未部署／未連MySQL。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='original-NPC-first-view-synchronized-AI-and-move-program-only',full_goal_achieved=False)
continuation['source_npc_view_action_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
