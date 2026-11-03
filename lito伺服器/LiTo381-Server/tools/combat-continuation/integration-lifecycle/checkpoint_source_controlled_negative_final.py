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
assert previous['runtime']['sha256']=='c4d8bdaf436050e7ea9e64bf1702256e7b529a67892b0c03b10baa32c695a1b6'
names=['source-controlled-negative-final-build','source-controlled-negative-final-common-reproduction','source-completion-inventory','source-next-producer-contracts','source-invocation-ABI-census','source-weapon-next-reference','source-weapon-overlay-checks']
receipts={n:read(D/(n+'-current.json'))for n in names}
for ref in receipts.values():verify(ref)
latest=receipts[names[0]];runtime=latest['runtime'];assert runtime['sha256']=='e7701e624f503b7078714efc346c5481e6778acd6dd1ce5ce9e2399fa773bc4a'
assert receipts[names[1]]['runtime']['sha256']==runtime['sha256']and receipts[names[1]]['status']=='passed-frozen-byte-identical-common-build'
for name in names[2:]:assert receipts[name]['runtime']==runtime,name
data={n:read(ref['manifest']['path'])for n,ref in receipts.items()}
for m in data.values():verify(m)
build=data[names[0]];rep=data[names[1]];inv=data[names[2]];abi=data[names[4]];overlay=data[names[6]]
for b in [build,rep]:
 assert b['base']==previous['runtime']and b['pure_mass_teleport_exit_contract_scenarios']==193 and b['inherited_same_SHA_scenarios']==15910
 assert len(b['inherited_same_SHA_plan_receipts'])==124 and sum(r['checks']for r in b['inherited_same_SHA_plan_receipts'])==15910
 assert b['controlled_negative_final_assertions']==695 and b['controlled_negative_final_monitor_waits']==78 and b['controlled_NPC_pair_assertions']==456 and b['controlled_NPC_pair_waits']==60 and b['PC_pair_assertions']==312 and b['PC_pair_waits']==42
 assert b['fresh_plan_closure_has_parent_classpath']is False and b['test_only_forced_tie_fixture_checks']==7 and b['test_only_forced_tie_fixture']['sha256']!=runtime['sha256']
assert abi['mismatched_call_sites']==0 and inv['goal']['status']=='active'and overlay['status'].startswith('passed')
text=Path(build['log']['path']).read_text('utf-8')
for marker in ['51 complete original methods','PASS193 mass-teleport exit original-lock','NPC FINAL LIFECYCLE checks=695','PAIR LIFECYCLE checks=456','PAIR LIFECYCLE checks=312','plus7 controlled-negative owners','PASS7 pair admission test-only']:assert marker in text,marker
with zipfile.ZipFile(old['readonly_SQL_consumers_byte_retained']['base']['path'])as source,zipfile.ZipFile(runtime['path'])as jar,zipfile.ZipFile(previous['runtime']['path'])as parent:
 for n in old['readonly_SQL_consumers_byte_retained']['entries']:assert source.read(n)==jar.read(n)
 changed={n for n in parent.namelist()if parent.read(n)!=jar.read(n)};assert changed=={'local/lito381/server/'+n+'.class'for n in ['NativeAreaMaterialAction$Context','NativeControlledNegativeMaterial','NativeSourceTargetNegativeLife','NativeSourceWeaponFinalFrame']}
 assert len(set(jar.namelist())-set(parent.namelist()))==3
assert subprocess.run(['powershell','-NoProfile','-Command',"@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"],capture_output=True,text=True,check=True).stdout.strip()=='0'
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';proof=read(proofpath);audit=read(auditpath)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
anchors=[pin(R/'current.json'),*old['protected'],*old['additional_protected']];verify(anchors)
run=D/'source-completion-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');snapshot=run/'editable-input-snapshot';snapshot.mkdir(parents=True)
current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';work=S/'docs/maintenance/source-completion-work-current.zh-TW.md';detail=S/'docs/maintenance/source-controlled-negative-final.zh-TW.md'
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md'),(work,'previous-work.md')]:shutil.copyfile(p,run/name)
old_index=pin(index);frozen_index=pin(run/'previous-program-progress.json');editable={Path(x['path']).name:x for x in old['editable_inputs']}
new_names=['NativeSourceControlledNegativeFinal.java','SourceControlledNegativeFinalMethods.java','SourceControlledNegativeFinalDelta.java','SourceControlledNegativeFinalLoad.java','SourceControlledNegativeFinalPlan.java','probe_source_controlled_negative_final.py','verify_source_controlled_negative_final.py','build_source_controlled_negative_final.py','checkpoint_source_controlled_negative_final.py','verify_source_controlled_negative_final_progress.py','SourceMaterialControlledNpcIngressPlan.java','probe_source_material_controlled_NPC_ingress.py','SourceMaterialElementalIngressPlan.java','probe_source_material_elemental_ingress.py','SourceMaterialNpcLifecyclePlan.java','probe_source_material_NPC_lifecycle.py','SourceMaterialNpcFinalPlan.java','probe_source_material_NPC_final.py','SourceMaterialControlLifecyclePlan.java','SourceMaterialControlIngressPlan.java','SourceSelfFinalFramePlan.java','run_source_weapon_overlay_checks.py']
for name in new_names:
 p=R/name;assert p.exists(),name
 if name not in editable or editable[name]['sha256']!=sha(p):shutil.copyfile(p,snapshot/name);editable[name]=pin(snapshot/name)
v=copy.deepcopy(old);v.update(status='program-updated-controlled-NPC-negative-final-first-role-lifecycle-full-Source-open',runtime=runtime,build=latest['manifest'],reproduction=receipts[names[1]]['manifest'],inventory=receipts[names[2]]['manifest'],next_producer_contracts=receipts[names[3]]['manifest'],internal_invocation_ABI_census=receipts[names[4]]['manifest'],remaining_weapon_PC_reference_audit=receipts[names[5]]['manifest'],source_weapon_overlay_and_rollback_checks=receipts[names[6]]['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=list(editable.values()),all_owned_JVMs_terminal=True)
v['earlier_replay_wait_observation']=dict(failed_manifest=pin(D/'source-controlled-negative-final-builds/20261027T050512403877Z/manifest.json'),diagnostic_log=pin(D/'source-controlled-negative-final-builds/20261027T050512403877Z/diagnostic.log'),diagnostic_source=pin(D/'source-controlled-negative-final-builds/20261027T050512403877Z/diagnostic-inputs/SourceElementAudiencePlan.java'),observed='Earlier inherited73 replay did not observe BLOCKED within3 seconds. Exact same runtime separately passed5 full73 diagnostic runs. Cause was not established; revised fixture requires the actual selected receiver monitor identity and owner, waits up to10 seconds and reports state/stack/failure. Assertion semantics were strengthened, not bypassed. Original failed run retained.')
v['implemented_scope']=[
 'Controlled NPC negative material173/174 now carries first source/controller/NPC/receiver identity and status to the original receiver monitor before cipher. Real decoded Ctrl final-wait reproduction on the parent exposed stale target effects; the same native factory and producer now drop stale output.',
 'Capture follows the original successful NPC first-proof call before the pair wait. Complete original NPC producer executes once with unchanged Source cost/RNG/immunity/competition. First receiver map is cleared before deferred sending, Context slot is retired, and explicit immutable DTO retains no producing Context/Frame/receipt/Member/Scope/Task/collection. No new global root, registry, TLS, queue or worker.',
 '51 complete original methods independently inverted across4 original owners,7 explicit call ports,1 transient Context slot,3 new runtime classes. Original PC/Motion complete sender bytes retained. Main/STOPPED, role/status/life/attachment/actor/application/lifetime and receiver changes reject stale effects; DRAINING and already committed actor movement remain allowed. Caster committed costs keep original separate ownImpact policy.',
 'Fresh self-contained tools/plan compiler closure and frozen byte-identical rebuild pass on this SHA.124 inherited ordinary plans15910 checks plus mass69 exit193; supplemental NPC final695 assertions/78 waits, NPC owner pair456/60 and PC pair312/42.464 decoded PC material and464 elemental NPC outcomes plus232 configured entry calls have no unexpected exceptions, but per-skill rejections/layouts are not full effect qualification. Forced-tie and rollback fault tests remain separate test SHAs.',
 'The rejected skill29/152/153 fallback-entry experiment is frozen and withdrawn: real entry sites were already connected. Source target-class/cost/effect semantics still need per-skill review. All12 work groups, full Source, other producers, formal composition, backend, Chinese MySQL and actual cursor remain open. No deployment/baseline/Source/player SQL/schema/WAL writes.'
]
v['program_checks'].update(controlled_negative_final_assertions=695,controlled_negative_final_monitor_waits=78,controlled_NPC_pair_assertions=456,controlled_NPC_pair_waits=60,PC_pair_assertions=312,PC_pair_waits=42,controlled_negative_original_methods_independently_inverted=51,controlled_negative_changed_owners=4,controlled_negative_explicit_call_ports=7,controlled_negative_transient_producing_fields=1,controlled_negative_new_runtime_classes=3,controlled_negative_first_viewer_scope_released=True,fresh_self_contained_plan_closure=True,ordinary_same_SHA_checks=16103,inherited_and_revalidated_same_SHA_pure_contract_scenarios=15910,inherited_same_SHA_plan_runs=124,byte_identical_frozen_rebuild=True)
v.update(controlled_negative_final_program_contract=build['controlled_negative_final_port_census'],controlled_negative_final_outcomes=build['controlled_negative_final_artifacts'],controlled_negative_staged_verification=build['staged_program_verification'],rejected_negative_entry_experiment=pin(D/'source-battle-negative-entry-rejection-current.json'),parent_direct_lock_census=build['parent_direct_lock_census'],candidate_direct_lock_census=build['candidate_direct_lock_census'],test_only_forced_tie_fixture=build['test_only_forced_tie_fixture'],configured_material_native_outcomes=build['material_configured_ingress_outcomes'],material69_original_exception_exit=build['material_native69_exception_exit'])
for group in v['remaining_work_groups']:
 if group['id']=='materials':group['work']='Continue58 material rows/all applicable Source models: per-skill class/target/normal/Ctrl/NPC/primary/secondary/range/LOS/immunity, original costs/probability/rollback and full effects. Controlled NPC173/174 stale final output is fixed;464 elemental outcomes include class/layout skips and rejections, not full effects.29/152/153 entries already exist; review actual Source class and phase before changing behavior.'
 if group['id']=='paired-lifecycle':group['work']='Continue original first acceptance/ordered pair/commit/final receiver across remaining NPC/PC/area producers. Controlled NPC27/173/174 has60 real owner pair waits and78 final waits;173/174 uses immutable first proofs and clears producing viewer scope before output. Other skills/factories/area/invite/full Main/memory/backend/game remain open.'
v['remaining']=[g['work']for g in v['remaining_work_groups']];v['historical_remaining_preserved_in']=previous['manifest'];assert len(v['remaining_work_groups'])==12
for k in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[k]is False,k
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),system_goal=v['goal_reset_receipt'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
w=work.read_text('utf-8');w=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{runtime['sha256']}`",w)
w=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",w)
paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']};w=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:paths[m[1]],w)
a=w.index('## 本次已更新的程式');b=w.index('## 尚未完成與完成門檻',a)
block='## 本次已更新的程式\n\n受控NPC的173／174狀態技能已補首次來源／主人／actor／接收者證據，沿既有完整送包副本到原cipher監視器。修正實際解碼Ctrl提交後等待送包時，Main／生命替換仍漏出舊特效的缺口；DRAINING與提交後移動保留。首次接收者暫存表在送包等待前清空，佇列僅帶不可變值；沒有新增全域根／工作執行緒或重建生命。\n\n同SHA獨立工具／plan閉包與固定輸入重建通過：124繼承plan共15910檢查＋69退出193；另列NPC最終695斷言／78等待、NPC成對456／60、PC成對312／42。51完整原方法inverse、4既有owner、7接點、1暫存Context欄位、3新runtime class。原PC／Motion sender位元不變。464次元素NPC與464次PC解碼入口仍含型別／layout不適用及正常拒絕，不能宣稱完整技能效果。\n\n29／152／153入口原已接線，錯誤判斷的fallback實驗已封存撤回；接續核對Source目標類別、成本及效果。全部12工作群仍開放；不部署、不寫Source／玩家SQL／schema／WAL。詳見[source-controlled-negative-final.zh-TW.md](source-controlled-negative-final.zh-TW.md)。\n\n'
work.write_text(w[:a]+block+w[b:],'utf-8')
detail.write_text(f'# 受控NPC狀態技能的最終送包生命週期\n\n候選 `{runtime["sha256"]}`。本次只更新隔離候選。\n\n'+block.split('## 本次已更新的程式\n\n',1)[1].replace('詳見[source-controlled-negative-final.zh-TW.md](source-controlled-negative-final.zh-TW.md)。','')+f'共同進度：[manifest]({(run/"manifest.json").as_posix()})。\n','utf-8')
note=f'\n\n2026-10-27 controlled NPC final：候選`{runtime["sha256"]}`；173／174舊特效最終cipher缺口修正，695斷言／78最終等待、456／60 NPC成對；16103繼承普通檢查與固定輸入重建通過。全部Source／技能／後端／MySQL／實機仍開放，未部署。詳見[source-controlled-negative-final.zh-TW.md](source-controlled-negative-final.zh-TW.md)。\n'
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(obj):
 if isinstance(obj,dict):
  if obj.get('path')==old_index['path']and obj.get('sha256')==old_index['sha256']:obj.update(frozen_index)
  else:
   for x in obj.values():freeze_previous(x)
 elif isinstance(obj,list):
  for x in obj:freeze_previous(x)
continuation=read(C);freeze_previous(continuation);fresh=dict(index=pin(index),runtime=runtime,native_runtime_qualified=False,complete=False,program_wired='controlled-NPC-negative-first-proof-final-cipher',full_goal_achieved=False)
continuation['source_controlled_negative_final_progress']=fresh;proof['source_program_update_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['docs']=[pin(p)for p in [current,doc,work,detail]]
write(C,continuation);write(proofpath,proof);write(auditpath,audit)
pending=D/'source-controlled-negative-final-pending-integration-current.json';shutil.copyfile(pending,run/'previous-pending-integration.json');write(pending,dict(status='integrated-common-program-other-material-Source-semantics-open',runtime=runtime,progress=pin(run/'manifest.json'),full_goal_achieved=False))
cache.clear();verify(anchors)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchors,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),ordinary_inherited_checks=16103,new_final_assertions=695,new_game_runtime_cases=0,new_SQL_connections=0),ensure_ascii=False))
