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
assert previous['runtime']['sha256']=='e7701e624f503b7078714efc346c5481e6778acd6dd1ce5ce9e2399fa773bc4a'
names=['source-negative-pair-first-build','source-negative-pair-first-common-reproduction','source-completion-inventory','source-next-producer-contracts','source-invocation-ABI-census','source-weapon-next-reference','source-weapon-overlay-checks']
receipts={n:read(D/(n+'-current.json'))for n in names}
for ref in receipts.values():verify(ref)
latest=receipts[names[0]];runtime=latest['runtime'];assert runtime['sha256']=='bf4aa2a61d488bd68c2cdaa0d9a7df3221f2e7848443f225c584ea137ddcbaf2'
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
 assert b['fresh_plan_closure_has_parent_classpath']is False and b['test_only_forced_tie_fixture_checks']==7 and b['test_only_forced_tie_fixture']['sha256']!=runtime['sha256']
assert abi['mismatched_call_sites']==0 and inv['goal']['status']=='active'and overlay['status'].startswith('passed')
text=Path(build['log']['path']).read_text('utf-8')
for marker in ['69 complete original methods','PASS193 mass-teleport exit original-lock','NPC FINAL LIFECYCLE checks=695','PAIR LIFECYCLE checks=456','PAIR LIFECYCLE checks=312','plus4 negative first-intent owners','PASS7 pair admission test-only']:assert marker in text,marker
with zipfile.ZipFile(old['readonly_SQL_consumers_byte_retained']['base']['path'])as source,zipfile.ZipFile(runtime['path'])as jar,zipfile.ZipFile(previous['runtime']['path'])as parent:
 for n in old['readonly_SQL_consumers_byte_retained']['entries']:assert source.read(n)==jar.read(n)
 changed={n for n in parent.namelist()if parent.read(n)!=jar.read(n)};assert changed=={'local/lito381/server/'+n+'.class'for n in ['NativeAreaMaterialAction','NativeSourceNegativeTargets$Aim']}
 assert len(set(jar.namelist())-set(parent.namelist()))==2
assert subprocess.run(['powershell','-NoProfile','-Command',"@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"],capture_output=True,text=True,check=True).stdout.strip()=='0'
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';proof=read(proofpath);audit=read(auditpath)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
anchors=[pin(R/'current.json'),*old['protected'],*old['additional_protected']];verify(anchors)
run=D/'source-completion-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');snapshot=run/'editable-input-snapshot';snapshot.mkdir(parents=True)
current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';work=S/'docs/maintenance/source-completion-work-current.zh-TW.md';detail=S/'docs/maintenance/source-negative-pair-first.zh-TW.md'
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md'),(work,'previous-work.md')]:shutil.copyfile(p,run/name)
old_index=pin(index);frozen_index=pin(run/'previous-program-progress.json');editable={Path(x['path']).name:x for x in old['editable_inputs']}
new_names=['NativeSourceNegativePairFirst.java','SourceNegativePairFirstMethods.java','SourceNegativePairFirstDelta.java','SourceNegativePairFirstLoad.java','SourceMaterialNegativeNpcLifecyclePlan.java','probe_source_material_negative_NPC_lifecycle.py','probe_source_negative_pair_first.py','verify_source_negative_pair_first.py','build_source_negative_pair_first.py','checkpoint_source_negative_pair_first.py','verify_source_negative_pair_first_progress.py','SourceMaterialElementalIngressPlan.java','run_source_weapon_overlay_checks.py']
for name in new_names:
 p=R/name;assert p.exists(),name
 if name not in editable or editable[name]['sha256']!=sha(p):shutil.copyfile(p,snapshot/name);editable[name]=pin(snapshot/name)
v=copy.deepcopy(old);v.update(status='program-updated-negative29-152-153-first-roster-pair-lifecycle-full-Source-open',runtime=runtime,build=latest['manifest'],reproduction=receipts[names[1]]['manifest'],inventory=receipts[names[2]]['manifest'],next_producer_contracts=receipts[names[3]]['manifest'],internal_invocation_ABI_census=receipts[names[4]]['manifest'],remaining_weapon_PC_reference_audit=receipts[names[5]]['manifest'],source_weapon_overlay_and_rollback_checks=receipts[names[6]]['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=list(editable.values()),all_owned_JVMs_terminal=True)
v['negative_pair_first_original_gap']=pin(D/'source-material-negative-NPC-lifecycle/20261027T052213195508Z/manifest.json')
v['implemented_scope']=[
 'Negative material29/152/153 controlled NPC paths now preserve first source/owner/NPC status identities and original Hub/lifetime/application across the original roster action-lock retry. Parent actual decoded input exposed12 errors: replacement owner/actor status still charged, missing source status threw Binding.capture NPE and missing Hub lifetime threw before shared pair preparation.',
 'Capture borrows existing immutable first source and NPC builder values before the first peer wait, then immediately restores the original shared intent slot. Bounded First lives only in the existing Aim; complete original close clears it. Existing self-pair entry remains self-pair. Readonly checks wrap only the original retry binding predicate, precommit scene predicate and Aim.valid. No missing life initialization, extra cost/RNG/effect producer, lock ordering, worker, registry or global root.',
 '69 complete original methods independently inverted across2 original owners;5 explicit ports,1 transient Aim field,2 new runtime classes. All remaining archive entries and20 readonly SQL readers retain original bytes. Normal/DRAINING/owner-only movement preserve costs; source/NPC movement and stale source/owner/NPC status/life/application/Main reject before charge. Existing Source summon-class failure consumes no probability RNG and leaves Counter/Erase/Sleep/move unchanged.',
 '150 actual normal/Ctrl NPC-owner waits with1878 assertions pass, including first-proof release and unchanged shared self-pair intent slot.124 inherited ordinary plans15910 plus mass69 exit193 rerun on the same SHA; inherited controlled final695/78, NPC pair456/60 and PC pair312/42 also rerun with the corrected unique real45306 fixture. Forced-tie/rollback fault tests use separate SHAs; normal/Ctrl464 and elemental464 outcome tables are scoped ingress observations, not full effects.',
 'All12 work groups remain open, including remaining materials, complete area/final receiver/value-only proof, Source controls/math/status/AI/producers, formal composition, backend, Chinese MySQL and actual cursor. No deployment, new baseline or Source/player SQL/schema/WAL writes.'
]
v['program_checks'].update(negative_pair_first_assertions=1878,negative_pair_first_waits=150,negative_pair_first_normal_waits=75,negative_pair_first_Ctrl_waits=75,negative_pair_original_methods_independently_inverted=69,negative_pair_changed_owners=2,negative_pair_explicit_call_ports=5,negative_pair_transient_Aim_fields=1,negative_pair_new_runtime_classes=2,negative_pair_original_self_pair_slot_unchanged=True,negative_pair_first_proof_released=True,negative_pair_Source_class_failure_no_RNG_or_status_mutation=True,fresh_self_contained_plan_closure=True,ordinary_same_SHA_checks=16103,inherited_and_revalidated_same_SHA_pure_contract_scenarios=15910,inherited_same_SHA_plan_runs=124,byte_identical_frozen_rebuild=True)
v.update(negative_pair_first_program_contract=build['negative_pair_first_port_census'],negative_pair_first_outcomes=build['negative_pair_first_artifacts'],negative_pair_first_staged_verification=build['staged_program_verification'],parent_direct_lock_census=build['parent_direct_lock_census'],candidate_direct_lock_census=build['candidate_direct_lock_census'],test_only_forced_tie_fixture=build['test_only_forced_tie_fixture'],configured_material_native_outcomes=build['material_configured_ingress_outcomes'],material69_original_exception_exit=build['material_native69_exception_exit'])
v['controlled_negative_final_outcomes']=dict(build['negative_pair_first_artifacts'])
for group in v['remaining_work_groups']:
 if group['id']=='materials':group['work']='Continue all58 material rows/applicable Source models and complete status/RNG/cost/rollback effects.29/152/153 original entry and Source summon-class failure were preserved; first-lock lifecycle is fixed and150 normal/Ctrl waits pass. Per-skill PC/NPC/primary/secondary/area/LOS/final output, other materials and full controls remain open.'
 if group['id']=='paired-lifecycle':group['work']='Continue first acceptance/ordered pairs/commit/final receiver and value-only memory proof across all remaining producers.29/152/153 now freeze original source/owner/NPC statuses before roster-lock retry, preserve existing self-pair path and clear bounded First at finish;150 waits1878 assertions. Inherited27/173/174 NPC/PC checks rerun; complete final receiver/area/nested roles/Main/backend/game remains open.'
v['remaining']=[g['work']for g in v['remaining_work_groups']];v['historical_remaining_preserved_in']=previous['manifest'];assert len(v['remaining_work_groups'])==12
for k in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[k]is False,k
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),system_goal=v['goal_reset_receipt'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
w=work.read_text('utf-8');w=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{runtime['sha256']}`",w)
w=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",w)
paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']};w=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:paths[m[1]],w)
a=w.index('## 本次已更新的程式');b=w.index('## 尚未完成與完成門檻',a)
block='## 本次已更新的程式\n\n29／152／153受控NPC施法已補首次來源、主人與NPC的status／life、Hub lifetime與application證據，覆蓋原roster鎖重試、precommit scene與Aim.valid。修正status替換仍扣款及來源status／Hub lifetime消失拋例外；不重建生命，不改原鎖重試／解鎖／producer／成本／免疫／RNG。共用self-pair意圖欄位在擷取後立即還原，新增First僅留在既有Aim並於原finish清空；無新增global root／TLS／queue／worker。\n\n150次一般／Ctrl原主人鎖等待、1878斷言通過：normal／DRAINING／僅主人移動保留原成本；退役、status／life替換、來源／NPC移動、Main／STOPPED拒絕。Source召喚物必定失敗不抽機率、不消除既有Counter／Erase／Sleep或變動移速。69完整原方法inverse、2既有owner、5接點、1暫存Aim欄位、2新runtime class；20唯讀SQL reader位元保留。\n\n獨立工具／plan閉包與固定輸入重建、124繼承plan15910檢查＋69退出193通過；既有NPC最終695／78、NPC成對456／60、PC成對312／42均在同SHA重跑。唯一真實45306場景及464入口統計仍逐項限縮，完整Source／各技能最終接收者與值資料生命週期／其餘12工作群未完成。不部署、不寫Source／玩家SQL／schema／WAL。詳見[source-negative-pair-first.zh-TW.md](source-negative-pair-first.zh-TW.md)。\n\n'
work.write_text(w[:a]+block+w[b:],'utf-8')
detail.write_text(f'# 受控NPC負面材料技能的首次成對生命週期\n\n候選 `{runtime["sha256"]}`。本次只更新隔離候選。\n\n'+block.split('## 本次已更新的程式\n\n',1)[1].replace('詳見[source-negative-pair-first.zh-TW.md](source-negative-pair-first.zh-TW.md)。','')+f'共同進度：[manifest]({(run/"manifest.json").as_posix()})。\n','utf-8')
note=f'\n\n2026-10-27 negative29/152/153 first：候選`{runtime["sha256"]}`；首次status／life／lifetime跨原roster鎖重試修正，150次一般／Ctrl等待、1878斷言；原成本／Source類別失敗／不抽RNG與既有self-pair保留；16103普通檢查與固定重建通過。其餘Source／最終接收者／後端／MySQL／實機仍開放，未部署。詳見[source-negative-pair-first.zh-TW.md](source-negative-pair-first.zh-TW.md)。\n'
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(obj):
 if isinstance(obj,dict):
  if obj.get('path')==old_index['path']and obj.get('sha256')==old_index['sha256']:obj.update(frozen_index)
  else:
   for x in obj.values():freeze_previous(x)
 elif isinstance(obj,list):
  for x in obj:freeze_previous(x)
continuation=read(C);freeze_previous(continuation);fresh=dict(index=pin(index),runtime=runtime,native_runtime_qualified=False,complete=False,program_wired='negative29-152-153-first-roster-pair-intent',full_goal_achieved=False)
continuation['source_negative_pair_first_progress']=fresh;proof['source_program_update_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['docs']=[pin(p)for p in [current,doc,work,detail]]
write(C,continuation);write(proofpath,proof);write(auditpath,audit)
pending=D/'source-negative-pair-first-pending-integration-current.json';shutil.copyfile(pending,run/'previous-pending-integration.json');write(pending,dict(status='integrated-common-program-other-material-Source-semantics-open',runtime=runtime,progress=pin(run/'manifest.json'),full_goal_achieved=False))
cache.clear();verify(anchors)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchors,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),ordinary_inherited_checks=16103,new_negative_first_assertions=1878,new_game_runtime_cases=0,new_SQL_connections=0),ensure_ascii=False))
