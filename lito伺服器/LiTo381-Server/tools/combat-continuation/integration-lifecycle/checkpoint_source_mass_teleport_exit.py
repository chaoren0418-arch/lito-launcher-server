"""Advance only the isolated common program index after the exact exit build and audits finish."""
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
names=['source-mass-teleport-exit-build','source-mass-teleport-exit-reproduction','source-completion-inventory','source-next-producer-contracts','source-invocation-ABI-census','source-weapon-next-reference','source-weapon-overlay-checks']
receipts={n:read(D/(n+'-current.json'))for n in names}
for ref in receipts.values():verify(ref)
latest=receipts[names[0]];runtime=latest['runtime'];assert runtime['sha256']=='c4d8bdaf436050e7ea9e64bf1702256e7b529a67892b0c03b10baa32c695a1b6'
# The replay has another path with the same independently rebuilt content.
assert receipts[names[1]]['runtime']['sha256']==runtime['sha256']and receipts[names[1]]['status']=='passed-frozen-byte-identical-common-build'
for name in names[2:]:assert receipts[name]['runtime']==runtime,name
data={n:read(ref['manifest']['path'])for n,ref in receipts.items()}
for m in data.values():verify(m)
build=data[names[0]];rep=data[names[1]];inv=data[names[2]];abi=data[names[4]];overlay=data[names[6]]
assert build['base']==previous['runtime']and build['pure_mass_teleport_exit_contract_scenarios']==rep['pure_mass_teleport_exit_contract_scenarios']==193
assert build['inherited_same_SHA_scenarios']==rep['inherited_same_SHA_scenarios']==15910
assert len(build['inherited_same_SHA_plan_receipts'])==124 and sum(r['checks']for r in build['inherited_same_SHA_plan_receipts'])==15910
assert build['test_only_forced_tie_fixture_checks']==7 and build['test_only_forced_tie_fixture']['sha256']!=runtime['sha256']
assert abi['mismatched_call_sites']==0 and inv['goal']['status']=='active'
assert overlay['status'].startswith('passed'),overlay['status']
text=Path(build['log']['path']).read_text('utf-8')
for marker in ['PASS independent mass-teleport exit inverse: 116 complete original methods','PASS193 mass-teleport exit original-lock','source-waits=13, final-monitor-waits=13, nested=5','PASS404 inherited runtime no-init coverage plus13 explicit self ingress/final owners','plus4 explicit mass-teleport exit owners','PASS7 pair admission test-only']:assert marker in text,marker
with Path(build['material_configured_ingress_outcomes']['path']).open(encoding='utf-8')as f:rows=list(csv.DictReader(f,delimiter='\t'))
assert len(rows)==232 and all(not r['error_type']and not r['cleanup_error']for r in rows)
closed=[r for r in rows if r['mode']in ('Main','STOPPED')];assert len(closed)==116 and all(r['status']=='rejected-or-no-output'and r['remaining_mp']=='2000'and r['encrypted_memory_bytes']=='0'for r in closed)
for r in rows:
 if r['skill_id']=='69'and r['mode']in ('peer','self'):assert r['remaining_mp']=='1946'and r['remaining_material']=='2'and r['encrypted_memory_bytes']=='138'
with zipfile.ZipFile(old['readonly_SQL_consumers_byte_retained']['base']['path'])as source,zipfile.ZipFile(runtime['path'])as jar,zipfile.ZipFile(previous['runtime']['path'])as parent:
 for n in old['readonly_SQL_consumers_byte_retained']['entries']:assert source.read(n)==jar.read(n)
 changed={n for n in parent.namelist()if parent.read(n)!=jar.read(n)};assert changed=={'local/lito381/server/NativeAreaMaterialAction.class','local/lito381/server/NativeLightAction.class'}
assert subprocess.run(['powershell','-NoProfile','-Command',"@(Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object {$_.CommandLine -like '*integration-lifecycle*'}).Count"],capture_output=True,text=True,check=True).stdout.strip()=='0'
proofpath=D/'Source-lifecycle-progress-verified-current.json';auditpath=D/'source-summon-lifetime-audit-current.json';proof=read(proofpath);audit=read(auditpath)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
anchors=[pin(p)for p in [R/'current.json',D/'source-kiringku-progress-current.json'if(D/'source-kiringku-progress-current.json').exists()else D/'source-kiringku-program-current.json']if p.exists()]
anchors.extend(old['protected']);anchors.extend(old['additional_protected']);verify(anchors)
run=D/'source-completion-progress'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');snapshot=run/'editable-input-snapshot';snapshot.mkdir(parents=True)
current=S/'docs/maintenance/combat-continuation-current.zh-TW.md';doc=S/'docs/maintenance/combat-integration-lifecycle.zh-TW.md';work=S/'docs/maintenance/source-completion-work-current.zh-TW.md';detail=S/'docs/maintenance/source-mass-teleport-exit.zh-TW.md'
for p,name in [(index,'previous-program-progress.json'),(proofpath,'previous-proof.json'),(auditpath,'previous-audit.json'),(C,'previous-continuation.json'),(current,'previous-current.md'),(doc,'previous-lifecycle.md'),(work,'previous-work.md'),(detail,'previous-mass-exit.md')]:shutil.copyfile(p,run/name)
old_index=pin(index);frozen_index=pin(run/'previous-program-progress.json')
editable={Path(x['path']).name:x for x in old['editable_inputs']}
for name in ['NativeSourceMassTeleportExit.java','SourceMassTeleportExitMethods.java','SourceMassTeleportExitDelta.java','SourceMassTeleportExitLoad.java','SourceMassTeleportExitPlan.java','SourceMaterialConfiguredIngressPlan.java','probe_source_material_configured_ingress.py','probe_source_mass_teleport_exit.py','build_source_mass_teleport_exit.py','verify_source_mass_teleport_exit.py','checkpoint_source_mass_teleport_exit.py','verify_source_mass_teleport_exit_progress.py','run_source_weapon_overlay_checks.py']:
 shutil.copyfile(R/name,snapshot/name);editable[name]=pin(snapshot/name)
v=copy.deepcopy(old);v.update(status='program-updated-material69-exit-first-role-original-lock-final-cipher-remaining-Source-open',runtime=runtime,build=latest['manifest'],reproduction=receipts[names[1]]['manifest'],inventory=receipts[names[2]]['manifest'],next_producer_contracts=receipts[names[3]]['manifest'],internal_invocation_ABI_census=receipts[names[4]]['manifest'],remaining_weapon_PC_reference_audit=receipts[names[5]]['manifest'],source_weapon_overlay_and_rollback_checks=receipts[names[6]]['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=list(editable.values()),all_owned_JVMs_terminal=True)
v['implemented_scope']=[
 'Material69 massTeleport retains its complete original normal/exception finally flow. First source proof is copied before the area body and carried through both publication captures and a complete original unlock lambda sibling. Original material costs, portal, massUnlock and exception cleanup remain.',
 'Publication captures require the actual original source action lock and revalidate first role/status/life/Hub/lifetime/application. Third-actor nesting only try-locks missing source ownership. Existing Batch/root drain defers outputs until outer actors release, without new global registry, TLS, queue or worker.',
 'Explicit immutable proof reaches the existing complete final sender/cipher path. Main/STOPPED and replacement role/status/life/lifetime/application reject without advancing cipher; DRAINING is retained. Missing source status/actor token/lifetime are not recreated after waits.',
 '193 new plus15910 inherited ordinary same-SHA checks from124 freshly compiled native plans;7 forced-tie checks on a separate generated test SHA. Frozen common build is byte identical.116 complete original methods inverse across2 owners;0 added original fields,7 original-method ports,2 complete lambda-sibling ports,1 readonly ingress bridge; inherited no-init plus4 explicit owners.',
 'Configured58 material rows execute232 native peer/self/Main/STOPPED calls without unexpected ingress/cleanup exceptions.116 closed calls remain inert;60 cost observations,46 normal rejections/no-output and10 cost-free responses are individually scoped, not proof of all effects.69 retains54MP/1material/138 encrypted bytes; injected original body exception remains original and produces exactly one4-byte unlock.',
 'All12 work groups remain open. Complete normal/Ctrl/friendly/NPC/area effect semantics, other producers/full Source/controls, formal composition, actual backend/Main/memory, Chinese MySQL and game cursor remain unqualified. No deployment, baseline or Source/player SQL/schema/WAL writes.'
]
v['program_checks'].update(pure_mass_teleport_exit_contract_scenarios=193,inherited_and_revalidated_same_SHA_pure_contract_scenarios=15910,inherited_same_SHA_plan_runs=124,ordinary_same_SHA_checks=16103,original_methods_independently_inverted=116,original_changed_owners=2,new_original_class_fields=0,explicit_original_method_ports=7,explicit_complete_lambda_sibling_ports=2,readonly_ingress_bridges=1,new_runtime_class_entries=2,additional_mass_exit_no_initialization_owners=4,actual_mass_exit_source_lock_waits=13,actual_mass_exit_final_monitor_waits=13,nested_original_root_cases=5,configured_material_ingress_attempts=232,configured_material_closed_ingress_attempts=116,candidate_direct_lock_inventory_methods=58,original_native_body_exception_preserved=True,original_mass_unlock_frame_once=True,full_material_effects_qualified=False,byte_identical_frozen_rebuild=True)
v.update(Source_material69_exit_publication_first_role_and_actual_lock_wired=True,Source_material69_original_unlock_explicit_final_cipher_wired=True,material69_exit_program_contract=build['mass_teleport_exit_port_census'],configured_material_native_outcomes=build['material_configured_ingress_outcomes'],material69_original_exception_exit=build['material_native69_exception_exit'],parent_direct_lock_census=build['parent_direct_lock_census'],candidate_direct_lock_census=build['candidate_direct_lock_census'],test_only_forced_tie_fixture=build['test_only_forced_tie_fixture'])
for group in v['remaining_work_groups']:
 if group['id']=='materials':group['work']='Continue full effects for all58 material rows and applicable Source models: normal/Ctrl, friendly/enemy/self/NPC targets, area primary/secondary selection, original immunity/RNG/cost/rollback and final output lifecycle. Configured232 ingress calls are exception-free, but60 cost observations and rejected/no-output cases do not qualify complete effects.69 exit lock/final proof is wired; normal portal destination/invite lifecycle and remaining skills are open.'
 if group['id']=='paired-lifecycle':group['work']='Continue first acceptance/paired commit/final receiver proof across remaining PC/NPC/area producers and nested roles.69 exit uses original action lock, third-actor try-lock and existing Batch/root drain;13 source waits/13 final waits/5 nested cases pass. Full area/invite/remaining producer and actual Main/memory/backend coverage stays open.'
v['remaining']=[g['work']for g in v['remaining_work_groups']];v['historical_remaining_preserved_in']=previous['manifest'];assert len(v['remaining_work_groups'])==12
for k in ['all_doll_paths_complete','all_material_skills_complete','all_Source_consumers_wired','same_SHA_full_regression_passed','actual_client_cursor_verified','user_report_fixed','full_goal_achieved','qualified_for_release','formal_deployment_changed','new_baseline_created','Source_or_player_SQL_or_WAL_written']:assert v[k]is False,k
write(run/'manifest.json',v);write(index,dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),system_goal=v['goal_reset_receipt'],new_game_runtime_cases=0,actual_validation_deferred_by_user=True,full_goal_achieved=False,qualified_for_release=False))
w=work.read_text('utf-8');w=re.sub(r'現行候選：`[0-9a-f]{64}`',f"現行候選：`{runtime['sha256']}`",w)
w=re.sub(r'現行JAR：[^\n]*方法、[^\n]*本地呼叫點、',f"現行JAR：{inv['observations']['current_archive_methods']:,}方法、{inv['observations']['current_archive_local_call_sites']:,}本地呼叫點、",w)
paths={Path(ref['path']).name:Path(ref['path']).as_posix()for ref in inv['artifacts']};w=re.sub(r'(?<=\()D:/[^)\n]*/source-completion-inventory/[^)\n]*/(skills.tsv|controls.tsv|hp-producers.tsv)(?=\))',lambda m:paths[m[1]],w)
a=w.index('## 本次已更新的程式');b=w.index('## 尚未完成與完成門檻',a)
block='## 本次已更新的程式\n\n材料技能69的原massTeleport退出發布已接入實際角色變更鎖，首次角色／status／actor life／lifetime／application證據沿兩條原finally與完整原解除鎖定lambda副本送到既有最終加密監視器。Main／STOPPED／切角／生命替換拒絕，DRAINING保留；第三角色忙碌時不持鎖等待，巢狀補送沿原Batch／root解鎖後排放。沒有新增global registry／TLS／queue／worker，鎖後不重建缺失生命。\n\n193新增＋15910繼承同SHA檢查、124個完整重編native plans及同位元重建通過；7個forced-tie檢查另列測試SHA。116完整原方法inverse、2既有owner、0新增原欄位、7原方法接點＋2完整lambda副本接點、1唯讀入口bridge；13次來源鎖等待、13次最終監視器等待、5種巢狀情境通過。\n\n58材料列的232次完整設定入口無未預期例外／清理失敗，116次Main／STOPPED沒有扣款或送包。其餘60次成本觀察、46次正常拒絕或無輸出、10次無成本回應仍逐項未完整效果資格；69正常保留54MP／1材料及138加密bytes，刻意原body例外保留原例外並僅補一次4-byte unlock。\n\n原73首次入口／最終cipher、原27武器及既有範圍／NPC／HP生命週期進度均承接。完整Source／其餘技能效果／外控／正式隔離合成／後端／中文MySQL／實機仍未完成；沒有部署、建立正式基準或寫入Source／玩家SQL／schema／WAL。詳見[source-mass-teleport-exit.zh-TW.md](source-mass-teleport-exit.zh-TW.md)。\n\n'
work.write_text(w[:a]+block+w[b:],'utf-8')
note=f"\n\n2026-10-27 material69 exit：候選`{runtime['sha256']}`；193新增＋15910繼承同SHA、124重編native plans、同位元重建；原退出發布鎖／首次角色生命／最終cipher補齊，13來源等待／13最終等待／5巢狀。58列232入口無未預期例外；完整效果／Source／後端／MySQL／實機仍開放，未部署。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
detail_text=detail.read_text('utf-8').replace('程式與重建已驗證；共同候選進度／盤點接入仍待完成，目前共同索引仍為 `07a334d7aa8da829af7e4b5543ac12e305b07541706e2b9ae254741766397d9b`。','程式、共同建置／重建、同SHA盤點、ABI及武器稽核已接入共同候選進度。')
detail_text=detail_text.replace('接續先把此確切 SHA 接入共同 builder／checkpoint／inventory、ABI 與武器稽核，保留六個既有 alias 差異；再繼續','共同 builder／checkpoint／inventory、ABI 與武器稽核已接入此確切 SHA，六個既有 alias 差異仍保留；接續繼續')
detail.write_text(detail_text+f"\n共同進度證據：[本次checkpoint]({(run/'manifest.json').as_posix()})；普通同SHA檢查16103、forced-tie另列7，direct-lock census為58方法。\n",'utf-8')
def freeze_previous(obj):
 if isinstance(obj,dict):
  if obj.get('path')==old_index['path']and obj.get('sha256')==old_index['sha256']:obj.update(frozen_index)
  else:
   for x in obj.values():freeze_previous(x)
 elif isinstance(obj,list):
  for x in obj:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=runtime,native_runtime_qualified=False,complete=False,program_wired='material69-exit-first-role-original-lock-final-cipher',full_goal_achieved=False)
continuation['source_mass_teleport_exit_progress']=fresh;proof['source_program_update_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['docs']=[pin(p)for p in [current,doc,work,detail]]
write(C,continuation);write(proofpath,proof);write(auditpath,audit)
pending=D/'source-mass-teleport-exit-pending-integration-current.json';shutil.copyfile(pending,run/'previous-pending-integration.json');write(pending,dict(status='integrated-common-program-further-material-and-Source-semantics-open',runtime=runtime,progress=pin(run/'manifest.json'),full_goal_achieved=False))
cache.clear();verify(anchors)
for k in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[k])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchors,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=runtime,manifest=pin(run/'manifest.json'),ordinary_same_SHA_checks=16103,new_game_runtime_cases=0,new_SQL_connections=0),ensure_ascii=False))
