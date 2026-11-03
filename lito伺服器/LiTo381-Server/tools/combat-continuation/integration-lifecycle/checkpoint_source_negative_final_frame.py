"""Advance the isolated candidate only after exact-byte common build, replay and audits."""
from pathlib import Path
from datetime import datetime,timezone
import copy,csv,hashlib,json,re,shutil,subprocess,zipfile
R=Path(__file__).resolve().parent;D=R/'build';S=R.parents[2];C=R.parent/'continuation-current.json'
read=lambda p:json.loads(Path(p).read_text('utf-8'))
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
pin=lambda p:dict(path=str(Path(p).resolve()),sha256=sha(p))
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n','utf-8')
cache={}
def verify(v):
 if isinstance(v,dict):
  if set(v)=={'original','frozen','meaning'}and v['meaning'].startswith('Historical mutable replay-script reference'):
   a,b=v['original'],v['frozen'];assert Path(a['path'])==R/'reproduce_source_weapon_player_gap.py'and a['sha256']==b['sha256']and sha(a['path'])!=a['sha256'];assert Path(b['path']).parent.name=='editable-input-snapshot';verify(b);return
  if 'path'in v and 'sha256'in v:
   p=v['path'];actual=cache.get(p)
   if actual is None:actual=sha(p);cache[p]=actual
   assert actual==v['sha256'],p
  for x in v.values():verify(x)
 elif isinstance(v,list):
  for x in v:verify(x)
index=D/'source-program-update-progress-current.json';previous=read(index);verify(previous);old=read(previous['manifest']['path']);verify(old)
assert previous['runtime']['sha256']=='bf4aa2a61d488bd68c2cdaa0d9a7df3221f2e7848443f225c584ea137ddcbaf2'
names=['source-negative-final-frame-build','source-negative-final-frame-common-reproduction','source-completion-inventory','source-next-producer-contracts','source-invocation-ABI-census','source-weapon-next-reference','source-weapon-overlay-checks']
receipts={n:read(D/(n+'-current.json'))for n in names}
for ref in receipts.values():verify(ref)
latest=receipts[names[0]];runtime=latest['runtime'];assert runtime['sha256']=='50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729'
assert receipts[names[1]]['runtime']['sha256']==runtime['sha256']and receipts[names[1]]['status']=='passed-frozen-byte-identical-common-build'
for name in names[2:]:assert receipts[name]['runtime']==runtime,name
data={n:read(ref['manifest']['path'])for n,ref in receipts.items()}
for m in data.values():verify(m)
build=data[names[0]];rep=data[names[1]];inv=data[names[2]];abi=data[names[4]];overlay=data[names[6]]
for b in [build,rep]:
 assert b['base']==previous['runtime']and b['pure_mass_teleport_exit_contract_scenarios']==193 and b['inherited_same_SHA_scenarios']==15910
 assert len(b['inherited_same_SHA_plan_receipts'])==124 and sum(r['checks']for r in b['inherited_same_SHA_plan_receipts'])==15910
 assert b['controlled_negative_final_assertions']==695 and b['controlled_negative_final_monitor_waits']==78 and b['controlled_NPC_pair_assertions']==456 and b['controlled_NPC_pair_waits']==60 and b['PC_pair_assertions']==312 and b['PC_pair_waits']==42
 assert b['negative_pair_first_assertions']==1878 and b['negative_pair_first_waits']==150
 assert b['negative_final_viewer_assertions']==2280 and b['negative_final_viewer_waits']==240 and b['negative_final_self_assertions']==480 and b['negative_final_self_waits']==60 and b['negative_final_cleanup_assertions']==17
 assert b['fresh_plan_closure_has_parent_classpath']is False and b['test_only_forced_tie_fixture_checks']==7 and b['test_only_forced_tie_fixture']['sha256']!=runtime['sha256']
assert abi['mismatched_call_sites']==0 and inv['goal']['status']=='active'and overlay['status'].startswith('passed')
text=Path(build['log']['path']).read_text('utf-8')
for marker in ['34 complete original methods','PASS193 mass-teleport exit original-lock','NPC FINAL LIFECYCLE checks=695','PAIR LIFECYCLE checks=456','PAIR LIFECYCLE checks=312','plus6 negative final owners','PASS7 pair admission test-only']:assert marker in text,marker
with zipfile.ZipFile(old['readonly_SQL_consumers_byte_retained']['base']['path'])as source,zipfile.ZipFile(runtime['path'])as jar,zipfile.ZipFile(previous['runtime']['path'])as parent:
 for n in old['readonly_SQL_consumers_byte_retained']['entries']:assert source.read(n)==jar.read(n)
 changed={n for n in parent.namelist()if parent.read(n)!=jar.read(n)};assert changed=={'local/lito381/server/'+n+'.class'for n in ['NativeSourceNegativeTargets$Aim','NativeSourceNegativeTargets','NativeSourceWeaponFinalFrame']}
 assert len(set(jar.namelist())-set(parent.namelist()))==3
assert subprocess.run(['powershell','-NoProfile','-Command',"@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"],capture_output=True,text=True,check=True).stdout.strip()=='0'
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';proof=read(proofpath);audit=read(auditpath)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
anchors=[pin(R/'current.json'),*old['protected'],*old['additional_protected']];verify(anchors)
run=D/'source-completion-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');snapshot=run/'editable-input-snapshot';snapshot.mkdir(parents=True)
current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';work=S/'docs/maintenance/source-completion-work-current.zh-TW.md';detail=S/'docs/maintenance/source-negative-final-frame.zh-TW.md'
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md'),(work,'previous-work.md')]:shutil.copyfile(p,run/name)
gapdoc=S/'docs/maintenance/source-negative-final-gap.zh-TW.md';shutil.copyfile(gapdoc,run/'previous-final-gap.md')
old_index=pin(index);frozen_index=pin(run/'previous-program-progress.json');editable={Path(x['path']).name:x for x in old['editable_inputs']}
new_names=['NativeSourceNegativeFinalFrame.java','SourceNegativeFinalFrameMethods.java','SourceNegativeFinalFrameDelta.java','SourceNegativeFinalFrameLoad.java','SourceNegativeFinalFramePlan.java','SourceNegativeSelfFinalPlan.java','SourceNegativeFinalCleanupPlan.java','probe_source_negative_final_frame.py','probe_source_negative_final_cleanup.py','probe_source_negative_self_final.py','verify_source_negative_final_frame.py','build_source_negative_final_frame.py','checkpoint_source_negative_final_frame.py','verify_source_negative_final_frame_progress.py','SourceMaterialNegativeNpcFinalPlan.java','probe_source_material_negative_NPC_final.py','record_source_negative_final_gap.py','SourceNegativeSelfGateDiagnostic.java','probe_source_negative_self_gate.py','run_source_weapon_overlay_checks.py']
for name in new_names:
 p=R/name;assert p.exists(),name
 if name not in editable or editable[name]['sha256']!=sha(p):shutil.copyfile(p,snapshot/name);editable[name]=pin(snapshot/name)
v=copy.deepcopy(old);v.update(status='program-updated-negative29-152-153-immutable-final-frame-and-early-cleanup-full-Source-open',runtime=runtime,build=latest['manifest'],reproduction=receipts[names[1]]['manifest'],inventory=receipts[names[2]]['manifest'],next_producer_contracts=receipts[names[3]]['manifest'],internal_invocation_ABI_census=receipts[names[4]]['manifest'],remaining_weapon_PC_reference_audit=receipts[names[5]]['manifest'],source_weapon_overlay_and_rollback_checks=receipts[names[6]]['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=list(editable.values()),all_owned_JVMs_terminal=True)
v['negative_final_original_viewer_gap']=pin(D/'source-material-negative-NPC-final/20261027T054048017161Z/manifest.json')
v['negative_final_original_self_gap']=pin(D/'source-negative-self-final/20261027T055742634209Z/manifest.json')
v['implemented_scope']=[
 'Controlled29/152/153 outputs now carry immutable first source/owner/NPC/receiver values through existing complete PC/motion sender siblings to the original cipher monitor. Exact source/receiver status containers, observer state, Hub lifetime and server/application identities are rechecked without action/Hub/status locks or life creation. Old29/152 viewer gaps and153 own-output gaps were actually reproduced on the parent.',
 'Original complete producer lambda is preserved in an accessible same-descriptor sibling. Only scoped BATTLE controlled29/152/153 replaces the producer finish callback; normal PC/WORLD fallbacks keep original registration/producer behavior. Producer finally closes original Aim and all companion receipts before deferred IO. First/viewer maps clear; queued records retain no Aim/Context/Frame/receipt/Member/Scope/Task/collection. Original primary exceptions survive with cleanup failures suppressed; remaining real receipts still release after an individual failure.',
 'Original accepted self MP/HP/direction outputs survive later target retirement and movement while the first source/receiver/global proof remains valid. Non-own presentation keeps original source/NPC position and placement guards, while owner-only movement remains allowed. Costs/RNG/immunity/competition/Ctrl and complete original encoders remain; DRAINING is retained and Main/STOPPED rejects before cipher.',
 '34 complete original methods independently inverted across3 original owners,7 call ports plus1 complete producer handle,1 original complete sibling,1 transient Aim field,3 new runtime classes; complete PC/motion senders and20 readonly SQL reader bytes retained. Zero new global roots/queue/TLS/worker.300 final waits:240 normal/Ctrl viewer/observer waits2280 assertions plus60 actual153 post-cost self waits480 assertions.3 memory-only cleanup fault cases17 assertions restore all original pending/receipt counters and preserve primary exceptions.',
 'Fresh self-contained tools/plan compiler closure and frozen byte-identical common rebuild rerun124 ordinary plans15910 plus mass69 exit193 on this SHA.150 negative first waits1878, inherited controlled final695/78, NPC pair456/60 and PC pair312/42 also rerun.464 PC and464 unique real45306 NPC outcomes remain scoped, not full effects. Forced-tie and rollback fault fixtures have separate SHAs. All12 groups/full Source/formal/backend/MySQL/cursor stay open; no deployment/baseline/Source/player SQL/schema/WAL writes.'
]
v['program_checks'].update(negative_final_viewer_assertions=2280,negative_final_viewer_waits=240,negative_final_self_assertions=480,negative_final_self_waits=60,negative_final_cleanup_assertions=17,negative_final_cleanup_fault_cases=3,negative_final_original_methods_independently_inverted=34,negative_final_changed_owners=3,negative_final_explicit_call_ports=7,negative_final_complete_producer_handles=1,negative_final_complete_producer_siblings=1,negative_final_transient_Aim_fields=1,negative_final_new_runtime_classes=3,negative_final_original_PC_motion_senders_byte_retained=True,negative_final_producing_Aim_First_map_and_all_receipts_released_before_IO=True,negative_final_queued_values_no_Aim_Context_Frame_receipt_Member_Scope_Task_collection=True,negative_final_source_owned_accepted_MP_survives_target_retirement=True,negative_final_actual153_post_cost_self_monitor_gate=True,fresh_self_contained_plan_closure=True,ordinary_same_SHA_checks=16103,inherited_and_revalidated_same_SHA_pure_contract_scenarios=15910,inherited_same_SHA_plan_runs=124,byte_identical_frozen_rebuild=True)
v.update(negative_final_frame_program_contract=build['negative_final_frame_port_census'],negative_final_frame_outcomes=build['negative_final_frame_artifacts'],negative_final_frame_staged_verification=build['staged_program_verification'],negative_final_same_SHA_expanded_probe=build['same_SHA_expanded_final_probe'],parent_direct_lock_census=build['parent_direct_lock_census'],candidate_direct_lock_census=build['candidate_direct_lock_census'],test_only_forced_tie_fixture=build['test_only_forced_tie_fixture'],configured_material_native_outcomes=build['material_configured_ingress_outcomes'],material69_original_exception_exit=build['material_native69_exception_exit'])
v['controlled_negative_final_outcomes']=dict(build['negative_final_frame_artifacts']);v['negative_pair_first_outcomes']=dict(build['negative_final_frame_artifacts'])
for group in v['remaining_work_groups']:
 if group['id']=='materials':group['work']='Continue all58 rows/applicable Source models, full statuses/RNG/cost/rollback/normal/Ctrl/friendly/PC/NPC/area/LOS semantics.29/152/153 controlled NPC first intent and final sender are wired:150 pair waits and300 final waits with complete original producer/encoding retained. Other target kinds/PC and area/remaining materials/controls stay open; SUMMON class-failure evidence does not qualify all effects.'
 if group['id']=='paired-lifecycle':group['work']='Continue all remaining producer/receiver/nested/area/first-discovery and memory paths.29/152 viewer/observer and153 own sender now use immutable explicit first values, original complete cipher sibling and early Aim/receipt cleanup;240+60 final waits pass normal/Ctrl, DRAINING and original movement/own-MP policies. Other skills, complete backend/Main/memory/game remain open.'
v['remaining']=[g['work']for g in v['remaining_work_groups']];v['historical_remaining_preserved_in']=previous['manifest'];assert len(v['remaining_work_groups'])==12
for k in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[k]is False,k
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),system_goal=v['goal_reset_receipt'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
w=work.read_text('utf-8');w=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{runtime['sha256']}`",w)
w=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",w)
paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']};w=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:paths[m[1]],w)
a=w.index('## 本次已更新的程式');b=w.index('## 尚未完成與完成門檻',a)
block='## 本次已更新的程式\n\n29／152／153受控NPC已將首次來源／主人／NPC／接收者不可變值接入既有完整PC／Motion sender副本，在原cipher監視器重驗status容器、observer、Hub lifetime及application。修正舊29／152觀察者封包與153自接收的退役漏洞；已提交自身MP／HP／方向更新不被後續目標退役誤丟，非自身呈現仍保留來源／NPC位置與placement檢查。normal／DRAINING／僅主人移動保留，Main／STOPPED及替換身分拒絕；不改Source成本、免疫、RNG或競技／Ctrl。\n\n完整原producer lambda副本保留所有呼叫／分支／handler；scoped BATTLE不再將Aim強參照放入finish callback，原finally關閉Aim、清空首次接收者表並釋放全部主人receipt後才等待IO。queued record不含Aim／Context／Frame／receipt／Member／Scope／Task／collection；無新global root／TLS／queue／worker。34完整原方法inverse、3既有owner、7接點＋1producer handle、1完整副本、1暫存Aim欄位、3新runtime class；原完整PC／Motion sender及20唯讀SQL reader位元不變。\n\n300次最終等待通過：29／152一般／Ctrl、玩家／觀察者240次／2280斷言，153原扣款後自接收一般／Ctrl60次／480斷言；3組清理故障17斷言保留原primary、suppressed及全部pending／receipt釋放。獨立工具／plan閉包與固定輸入同位元重建、16103普通繼承檢查、150次首次等待1878斷言及原NPC／PC矩陣均在同SHA重跑。\n\n全部58材料完整效果、PC／自然NPC／其他範圍與Source路徑、正式隔離合成、後端、中文MySQL與實機仍未完成；保留12工作群。不部署、不寫Source／玩家SQL／schema／WAL。詳見[source-negative-final-frame.zh-TW.md](source-negative-final-frame.zh-TW.md)。\n\n'
work.write_text(w[:a]+block+w[b:],'utf-8')
detail.write_text(f'# 受控NPC負面材料技能的不可變最終送包\n\n候選 `{runtime["sha256"]}`，只更新隔離程式。\n\n'+block.split('## 本次已更新的程式\n\n',1)[1].replace('詳見[source-negative-final-frame.zh-TW.md](source-negative-final-frame.zh-TW.md)。','')+f'共同進度：[manifest]({(run/"manifest.json").as_posix()})。\n','utf-8')
note=f'\n\n2026-10-27 negative final：候選`{runtime["sha256"]}`；29／152玩家／觀察者240次最終等待2280斷言＋153原扣款後自接收60次480斷言，3清理故障17斷言；不可變首次值接原cipher，Aim／receipt在IO前釋放。16103普通繼承檢查與固定重建通過。其餘Source／後端／MySQL／實機仍開放，未部署。詳見[source-negative-final-frame.zh-TW.md](source-negative-final-frame.zh-TW.md)。\n'
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
gapdoc.write_text(gapdoc.read_text('utf-8')+f'\n本頁所列受控NPC缺口已在隔離候選`{runtime["sha256"]}`修正並接入共同程式：一般／Ctrl、玩家／觀察者及153原扣款後自接收共300最終等待通過，Aim／receipt在IO前釋放。先前資料保留為重現證據；完整Source與其他工作群仍未完成。詳見[source-negative-final-frame.zh-TW.md](source-negative-final-frame.zh-TW.md)。\n','utf-8')
def freeze_previous(obj):
 if isinstance(obj,dict):
  if obj.get('path')==old_index['path']and obj.get('sha256')==old_index['sha256']:obj.update(frozen_index)
  else:
   for x in obj.values():freeze_previous(x)
 elif isinstance(obj,list):
  for x in obj:freeze_previous(x)
continuation=read(C);freeze_previous(continuation);fresh=dict(index=pin(index),runtime=runtime,native_runtime_qualified=False,complete=False,program_wired='negative29-152-153-immutable-final-cipher-and-early-producer-release',full_goal_achieved=False)
continuation['source_negative_final_frame_progress']=fresh;proof['source_program_update_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['docs']=[pin(p)for p in [current,doc,work,detail,gapdoc]]
write(C,continuation);write(proofpath,proof);write(auditpath,audit)
pending=D/'source-negative-final-frame-pending-integration-current.json';shutil.copyfile(pending,run/'previous-pending-integration.json');write(pending,dict(status='integrated-common-program-other-material-Source-semantics-open',runtime=runtime,progress=pin(run/'manifest.json'),full_goal_achieved=False))
gap_index=D/'source-negative-final-gap-current.json';old_gap=read(gap_index);shutil.copyfile(gap_index,run/'previous-final-gap-index.json');write(gap_index,dict(status='fixed-scoped-controlled-negative-final-gaps-common-program-other-Source-open',runtime=runtime,manifest=pin(run/'manifest.json'),previous_gap=old_gap['manifest'],actual_final_waits=300,full_goal_achieved=False,qualified_for_release=False))
cache.clear();verify(anchors)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchors,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),ordinary_inherited_checks=16103,new_final_assertions=2760,cleanup_assertions=17,new_game_runtime_cases=0,new_SQL_connections=0),ensure_ascii=False))
