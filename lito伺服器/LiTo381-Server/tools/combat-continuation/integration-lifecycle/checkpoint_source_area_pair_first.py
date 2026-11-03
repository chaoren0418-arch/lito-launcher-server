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
assert previous['runtime']['sha256']=='50635f7edca8ec6ce77787cc32371c7590b497e48ea202f8a679caf3f517b729'
names=['source-area-pair-first-build','source-area-pair-first-common-reproduction','source-completion-inventory','source-next-producer-contracts','source-invocation-ABI-census','source-weapon-next-reference','source-weapon-overlay-checks']
receipts={n:read(D/(n+'-current.json'))for n in names}
for ref in receipts.values():verify(ref)
latest=receipts[names[0]];runtime=latest['runtime'];assert runtime['sha256']=='3ccbb6f5957436cc0ba9bd67d373f9dfcbedad16553009842eaf5db6d0e88e9a'
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
 assert b['area_primary_assertions']==1402 and b['area_primary_waits']==100 and b['area_secondary_assertions']==224 and b['area_secondary_waits']==14 and b['area_stale_secondary_only_caster_seed_advances']==15 and b['area_unchanged_caster_and_secondary_seed_advances']==30
 assert b['fresh_plan_closure_has_parent_classpath']is False and b['test_only_forced_tie_fixture_checks']==7 and b['test_only_forced_tie_fixture']['sha256']!=runtime['sha256']
assert abi['mismatched_call_sites']==0 and inv['goal']['status']=='active'and overlay['status'].startswith('passed')
text=Path(build['log']['path']).read_text('utf-8')
for marker in ['2573 complete original methods','PASS193 mass-teleport exit original-lock','NPC FINAL LIFECYCLE checks=695','PAIR LIFECYCLE checks=456','PAIR LIFECYCLE checks=312','plus9 area primary/secondary owners','PASS7 pair admission test-only']:assert marker in text,marker
with zipfile.ZipFile(old['readonly_SQL_consumers_byte_retained']['base']['path'])as source,zipfile.ZipFile(runtime['path'])as jar,zipfile.ZipFile(previous['runtime']['path'])as parent:
 for n in old['readonly_SQL_consumers_byte_retained']['entries']:assert source.read(n)==jar.read(n)
 changed={n for n in parent.namelist()if parent.read(n)!=jar.read(n)};assert changed=={'local/lito381/server/'+n+'.class'for n in ['NativeAreaMaterialAction$Context','NativeAreaMaterialAction','NativeSourceMassSlowTargets$Aim','NativeSourceWindTargets$Aim','NativeSourceAreaPlayerProof','ClientSession']}
 assert len(set(jar.namelist())-set(parent.namelist()))==3
assert subprocess.run(['powershell','-NoProfile','-Command',"@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"],capture_output=True,text=True,check=True).stdout.strip()=='0'
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';proof=read(proofpath);audit=read(auditpath)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
anchors=[pin(R/'current.json'),*old['protected'],*old['additional_protected']];verify(anchors)
run=D/'source-completion-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');snapshot=run/'editable-input-snapshot';snapshot.mkdir(parents=True)
current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';work=S/'docs/maintenance/source-completion-work-current.zh-TW.md';detail=S/'docs/maintenance/source-area-pair-first.zh-TW.md'
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md'),(work,'previous-work.md')]:shutil.copyfile(p,run/name)
old_index=pin(index);frozen_index=pin(run/'previous-program-progress.json');editable={Path(x['path']).name:x for x in old['editable_inputs']}
new_names=['NativeSourceAreaPairFirst.java','SourceAreaPairFirstMethods.java','SourceAreaPairFirstDelta.java','SourceAreaPairFirstLoad.java','SourceAreaPairFirstPlan.java','SourceMaterialAreaNpcLifecyclePlan.java','SourceMaterialAreaSecondaryLifecyclePlan.java','probe_source_material_area_NPC_lifecycle.py','probe_source_material_area_secondary_lifecycle.py','probe_source_area_pair_first.py','verify_source_area_pair_first.py','build_source_area_pair_first.py','checkpoint_source_area_pair_first.py','verify_source_area_pair_first_progress.py','run_source_weapon_overlay_checks.py']
for name in new_names:
 p=R/name;assert p.exists(),name
 if name not in editable or editable[name]['sha256']!=sha(p):shutil.copyfile(p,snapshot/name);editable[name]=pin(snapshot/name)
v=copy.deepcopy(old);v.update(status='program-updated-area76-167-first-primary-and76-secondary-before-RNG-full-Source-open',runtime=runtime,build=latest['manifest'],reproduction=receipts[names[1]]['manifest'],inventory=receipts[names[2]]['manifest'],next_producer_contracts=receipts[names[3]]['manifest'],internal_invocation_ABI_census=receipts[names[4]]['manifest'],remaining_weapon_PC_reference_audit=receipts[names[5]]['manifest'],source_weapon_overlay_and_rollback_checks=receipts[names[6]]['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=list(editable.values()),all_owned_JVMs_terminal=True)
v['area_first_original_primary_gap']=pin(D/'source-material-area-NPC-lifecycle/20261027T062506241372Z/manifest.json')
v['area_first_original_secondary_gap']=pin(D/'source-material-area-secondary-lifecycle/20261027T063522270800Z/manifest.json')
v['implemented_scope']=[
 'Controlled76/167 first primary source/controller/NPC statuses, life, Hub lifetime and applications are frozen before original roster lock waits. Parent actual input reproduced24 failures in100 normal/Ctrl cases: replacement statuses/application still charged, missing source status/Hub lifetime threw. Readonly first checks now reject without binding recapture, owner rotation or life repair; normal/DRAINING/owner-only movement retain original costs.',
 'Source76 first PC status map also prevents same-owner/life replacement secondary containers from being adopted. Existing original frozen target-class iterator remains first; a bounded PC-only cursor then validates original role/life/status before original wake/probability/dice/percentile. Only retired secondary is excluded; original caster membership, geometry/class lists, duplicates/order and once-only caster costs remain.14 actual secondary waits retain original14MP/2material and required caster work while leaving retired secondary sleep unchanged.',
 'Actual Source fixture consumes15 RNG seed advances per eligible PC for probability dice plus percentile. Normal/DRAINING caster+secondary consumes30; retired secondary consumes15 for the original required caster. Test replays seed advances on the same caller and restores its seed; no runtime RNG implementation or probability formula changed. Source167 controlled SUMMON class failure consumes no probability draw, and primary NPC Counter/Erase/Sleep/move remains unchanged.',
 '2573 complete original methods independently inverted across6 original owners,10 bounded ports,1 transient Context field,3 new runtime classes. Original NPC/PC list construction/cost/probability/locks/handlers and complete final senders retained;20 readonly SQL readers unchanged. No new global root, registry, TLS, queue or worker.100 primary waits1402 assertions and14 secondary waits224 assertions pass; bounded First/status map clears at original finish.',
 'Fresh self-contained tools/plan closure and frozen byte-identical rebuild rerun124 ordinary plans15910 plus mass69 exit193 on this SHA. Previous negative first150, negative final240+60, controlled final78, NPC pair60, PC pair42 and cleanup17 assertions also rerun;464 PC/464 unique NPC ingress outcomes remain scoped. All12 groups stay open, including PC primaries/natural or controlled secondary NPCs/area final independent accepted outputs/full Source/formal/backend/MySQL/cursor. No deployment/new baseline/Source/player SQL/schema/WAL writes.'
]
v['program_checks'].update(area_primary_waits=100,area_primary_assertions=1402,area_secondary_waits=14,area_secondary_assertions=224,area_original_methods_independently_inverted=2573,area_changed_owners=6,area_explicit_call_ports=10,area_transient_Context_fields=1,area_new_runtime_classes=3,area_first_primary_and_secondary_map_released=True,area_stale_secondary_excluded_before_probability_dice_and_percentile=True,area_unchanged_caster_and_secondary_seed_advances=30,area_retired_secondary_caster_only_seed_advances=15,area_caster_cost_preserved_with_retired_secondary=True,area_final_receiver_complete=False,all_secondary_NPC_paths_qualified=False,fresh_self_contained_plan_closure=True,ordinary_same_SHA_checks=16103,inherited_and_revalidated_same_SHA_pure_contract_scenarios=15910,inherited_same_SHA_plan_runs=124,byte_identical_frozen_rebuild=True)
v.update(area_pair_first_program_contract=build['area_pair_first_port_census'],area_pair_first_outcomes=build['area_pair_first_artifacts'],area_pair_first_staged_verification=build['staged_program_verification'],parent_direct_lock_census=build['parent_direct_lock_census'],candidate_direct_lock_census=build['candidate_direct_lock_census'],test_only_forced_tie_fixture=build['test_only_forced_tie_fixture'],configured_material_native_outcomes=build['material_configured_ingress_outcomes'],material69_original_exception_exit=build['material_native69_exception_exit'])
v['controlled_negative_final_outcomes']=dict(build['area_pair_first_artifacts']);v['negative_pair_first_outcomes']=dict(build['area_pair_first_artifacts']);v['negative_final_frame_outcomes']=dict(build['area_pair_first_artifacts'])
for group in v['remaining_work_groups']:
 if group['id']=='materials':group['work']='Continue all58 material rows/applicable Source models and full effects/cost/RNG/rollback/target/area/LOS.76/167 controlled primaries now retain exact first statuses across roster locks;76 stale PC secondary skipped before original wake/probability/RNG while required caster and14MP/2material remain.100 primary and14 secondary waits pass. PC primaries, other/natural secondary NPCs, area accepted/final output and remaining materials/controls remain open.'
 if group['id']=='paired-lifecycle':group['work']='Continue remaining first-discovery/PC/NPC/area independent accepted/final receiver and memory paths.76/167 first controlled primary and76 secondary PC role/status/life now fixed at prewait/pre-RNG boundaries;100+14 waits pass and original finish releases bounded map. Existing29/152/153 final proof rerun. Area final receiver still uses original separate flow and remains unqualified; actual Main/backend/memory/game remain open.'
v['remaining']=[g['work']for g in v['remaining_work_groups']];v['historical_remaining_preserved_in']=previous['manifest'];assert len(v['remaining_work_groups'])==12
for k in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[k]is False,k
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),system_goal=v['goal_reset_receipt'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
w=work.read_text('utf-8');w=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{runtime['sha256']}`",w)
w=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",w)
paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']};w=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:paths[m[1]],w)
a=w.index('## 本次已更新的程式');b=w.index('## 尚未完成與完成門檻',a)
block='## 本次已更新的程式\n\n76／167受控NPC主目標已在原roster鎖等待前保留來源／主人／NPC的精確status、life、Hub lifetime與application，修正100次一般／Ctrl矩陣重現的24項舊缺陷；Main／STOPPED／替換或遺失資料不扣款、不重建生命，normal／DRAINING／僅主人移動保留。\n\n76另保留首次PC status表，在既有凍結型別iterator之後、原wake／機率骰／百分比之前排除失效次目標，沒有移除整批施法或Source原本加入的施法者。14次次目標等待確認舊status不被修改，14MP／2材料成本保留。此Source測試資料每名有效PC有15次RNG seed推進；正常施法者＋次目標為30，次目標失效為15，保留施法者原計算。未修改生產RNG／機率公式；167召喚物類別必定失敗不抽機率，NPC既有Counter／Erase／Sleep／移速保留。\n\n100次主目標等待1402斷言＋14次次目標224斷言、2573完整原方法inverse、6既有owner、10接點、1暫存Context欄位、3新class及20唯讀SQL reader位元保留通過。原幾何／名單建構／型別／順序與重複／成本／原鎖及handler保留，新增首次表隨原finish釋放；無新global root／TLS／queue／worker。獨立工具／plan閉包及固定輸入同位元重建、16103普通繼承檢查與既有負面技能／NPC／PC矩陣均在同SHA重跑。\n\nPC主目標、自然／其他次目標NPC、範圍技能獨立接受／最終接收者、完整Source與其餘12工作群仍未完成；不部署、不寫Source／玩家SQL／schema／WAL。詳見[source-area-pair-first.zh-TW.md](source-area-pair-first.zh-TW.md)。\n\n'
work.write_text(w[:a]+block+w[b:],'utf-8')
detail.write_text(f'# 76／167首次主目標與76失效次目標排除\n\n候選 `{runtime["sha256"]}`，只更新隔離程式。\n\n'+block.split('## 本次已更新的程式\n\n',1)[1].replace('詳見[source-area-pair-first.zh-TW.md](source-area-pair-first.zh-TW.md)。','')+f'共同進度：[manifest]({(run/"manifest.json").as_posix()})。\n','utf-8')
note=f'\n\n2026-10-27 area first：候選`{runtime["sha256"]}`；76／167受控主目標100次等待1402斷言、76次目標14次224斷言；失效次目標在原機率骰之前排除，保留Source施法者30→15 RNG seed推進與原成本。16103普通繼承檢查及固定重建通過；範圍最終接收者／完整Source／後端／MySQL／實機仍開放，未部署。詳見[source-area-pair-first.zh-TW.md](source-area-pair-first.zh-TW.md)。\n'
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(obj):
 if isinstance(obj,dict):
  if obj.get('path')==old_index['path']and obj.get('sha256')==old_index['sha256']:obj.update(frozen_index)
  else:
   for x in obj.values():freeze_previous(x)
 elif isinstance(obj,list):
  for x in obj:freeze_previous(x)
continuation=read(C);freeze_previous(continuation);fresh=dict(index=pin(index),runtime=runtime,native_runtime_qualified=False,complete=False,program_wired='area76-167-first-primary-and76-secondary-pre-RNG',full_goal_achieved=False)
continuation['source_area_pair_first_progress']=fresh;proof['source_program_update_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['docs']=[pin(p)for p in [current,doc,work,detail]]
write(C,continuation);write(proofpath,proof);write(auditpath,audit)
pending=D/'source-area-pair-first-pending-integration-current.json';shutil.copyfile(pending,run/'previous-pending-integration.json');write(pending,dict(status='integrated-common-program-other-material-Source-semantics-open',runtime=runtime,progress=pin(run/'manifest.json'),full_goal_achieved=False))
cache.clear();verify(anchors)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchors,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),ordinary_inherited_checks=16103,new_area_primary_assertions=1402,new_area_secondary_assertions=224,new_game_runtime_cases=0,new_SQL_connections=0),ensure_ascii=False))
