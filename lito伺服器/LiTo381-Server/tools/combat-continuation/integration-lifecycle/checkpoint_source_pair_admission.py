"""Checkpoint explicit new-action pairs and shared owned tie policy, retaining accepted continuations."""
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
latest=read(D/'source-pair-admission-build-current.json');repro=read(D/'source-pair-admission-reproduction-current.json');inventory=read(D/'source-completion-inventory-current.json');contracts=read(D/'source-next-producer-contracts-current.json')
for ref in [latest,repro,inventory,contracts]:verify(ref)
assert latest['runtime']==repro['runtime']==inventory['runtime']==contracts['runtime']
assert repro['status'].startswith('passed-frozen-byte-identical')
build,rep,inv,contract=[read(Path(x['manifest']['path']))for x in [latest,repro,inventory,contracts]]
for m in [build,rep,inv,contract]:verify(m)
assert build['base']==previous['runtime']
assert build['pure_pair_admission_contract_scenarios']==66
assert build['test_only_forced_tie_fixture_checks']==7
assert build['test_only_forced_tie_fixture']['sha256']!=latest['runtime']['sha256']
assert sum(v for k,v in build.items()if k.endswith('_contract_scenarios')and k!='pure_pair_admission_contract_scenarios')==3426
log=Path(build['log']['path']).read_text('utf-8')
for marker in ['inverse: 78 original methods; one shared already-owned branch and3 typed new-action ports','PASS336','PASS66 pair admission actual-entry','PASS7 pair admission test-only','contentions=16, original-raw-owned-calls=6, preserved-accepted-waits=4']:assert marker in log,marker
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
for name in ['NativeSourcePairAdmission.java','SourcePairAdmissionMethods.java','SourcePairAdmissionDelta.java','SourcePairAdmissionLoad.java','SourcePairAdmissionPlan.java','SourcePairAdmissionTie.java','build_source_pair_admission.py','reproduce_source_pair_admission.py','audit_source_completion.py','audit_source_next_producers.py','verify_source_pair_admission.py',Path(__file__).name]:shutil.copyfile(R/name,inputs/name)
v=copy.deepcopy(old);v.update(status='program-updated-explicit-new-action-pair-admission-accepted-and-owned-tie-full-Source-open',runtime=latest['runtime'],build=latest['manifest'],reproduction=repro['manifest'],inventory=inventory['manifest'],next_producer_contracts=contracts['manifest'],previous_program_progress=dict(index=frozen_index,manifest=previous['manifest'],runtime=previous['runtime']),editable_inputs=[pin(p)for p in sorted(inputs.iterdir())],all_owned_JVMs_terminal=True)
v['implemented_scope']=['Three original typed data ports now acquire a missing peer before scope registration for explicit new actions: weapon buffs148/149, raw-owned actions when accepted=false, and doll player damage without an accepted parent Frame. Original action/aim guards, costs/RNG, HP/receipts and complete try/finally bodies remain. There is no generic drop/defer policy for accepted continuations; raw accepted=true and accepted-parent doll retain exact original synchronous behavior.', 'The common ordered core only adds a BATTLE already-both-owned branch after original null validation and before identity ordering/tie monitor. It executes the original action within the original scope with exact caller-owned hold counts. Neither-owned signed-id/identity-hash/tie order, WORLD/unloaded fallback and original cleanup remain retained. Other accepted HP/status/reward/retirement caller classes are unchanged. No new fields/methods/queues/TLS/registry/workers/SQL/HP authority.', 'Independent inverse of78 original methods;336 no-init classes.66 candidate contracts plus3426 inherited same-SHA checks;16 actual mutex-contention scenarios,6 complete original raw-owned entry/finally calls and4 preserved accepted synchronous waits. Separately7 checks on a test-only forced-hash fixture exercise actual original tie-monitor fallback and BATTLE already-owned bypass. The fixture has its own pin and is not the candidate SHA or a program-progress runtime. Frozen candidate replay byte-identical;20 SQL consumer classes and earlier full-room defense retained.', 'Direct original raw-owned fixtures use prebuilt closed actions: they exercise real entry/phase/finally without constructing Source Context or running HP. No actual148/149/doll cast, RNG/cost/HP/IO/Main/backend/MySQL/game qualification is inferred. Original non-adapted pair callers, accepted nested contention whole-continuation semantics and earlier/manual lock lifetimes remain open.', 'Complete role/life and primary/secondary/normal/Ctrl/area pairing, other material/proc/HP producers, Source math/status/252 controls, NPC-AI/32 classes, final outputs/Main/memory/formal composition and concentrated actual acceptance remain open. Accepted target HP remains independent of later actor/caster liveness. No deployment or Source/player SQL/schema/WAL writes.']
v['program_checks']=dict(JVM_no_initialization_classes=336,original_methods_independently_inverted=78,shared_BATTLE_already_owned_branches=1,typed_new_action_data_ports=3,existing_changed_class_entries=4,new_fieldless_helper_entries=1,new_fields_or_methods_on_existing_classes=0,pure_pair_admission_contract_scenarios=66,inherited_and_revalidated_same_SHA_pure_contract_scenarios=3426,test_only_forced_tie_fixture_checks=7,test_only_fixture_is_not_candidate_SHA=True,actual_mutex_contention_scenarios=16,actual_original_raw_owned_entry_finally_calls=6,preserved_accepted_synchronous_waits=4,original_scope_not_registered_on_new_action_peer_contention=True,manual_no_parent_and_both_lock_orientations_checked=True,caller_owned_hold_counts_and_exception_identity_retained=True,raw_accepted_true_and_accepted_parent_doll_keep_original_pair=True,other_accepted_status_reward_retirement_callers_byte_retained=True,shared_neither_owned_signed_id_identity_tie_order_retained=True,shared_null_validation_before_owned_branch=True,WORLD_unloaded_fallback_retained=True,no_new_acceptance_HP_or_caster_liveness_policy=True,no_new_workers_TLS_registries_queues_or_SQL=True,typed_raw_fixtures_use_prebuilt_closed_action_and_no_Source_Context=True,original_earlier_full_room_defense_Snapshot_byte_retained=True,byte_identical_frozen_rebuild=True,new_SELECTs=0,new_connection_ownership=False,bound_actual_server_game_or_Main_shutdown_cases=0,full_other_pair_phases_life_cost_RNG_HP_IO_Main_Source_qualified=False)
v['test_only_forced_tie_fixture']=build['test_only_forced_tie_fixture']
for key in ['Source_pair_admission_contract','pair_admission_port_census','Source_companion_first_actor_contract','companion_first_actor_port_census','Source_companion_clock_contract','companion_clock_port_census','Source_NPC_HP_output_contract','npc_HP_output_port_census','Source_NPC_player_view_contract','npc_player_view_port_census','Source_NPC_tick_view_contract','npc_tick_view_port_census','Source_NPC_view_action_contract','npc_view_action_port_census','Source_NPC_first_view_contract','npc_first_view_port_census']:
 verify(build[key]);v[key]=build[key]
v.update(Source_explicit_new_action_pair_admission_and_owned_tie_program_wired=True,full_Source_NPC_AI_and_presentation_qualified=False)
reset_request=D/'source-completion-goal-reset-requests/20261026-user-reset-request.json';verify(pin(reset_request));v['latest_goal_reset_request']=pin(reset_request)
updates={'paired-lifecycle':'Three explicit new-action ports now acquire missing peer before scope:148/149, raw accepted=false and doll without accepted parent. Shared BATTLE both-owned core bypasses identity/tie after null validation, retaining original scope/hold counts. Accepted raw/parent-doll and other accepted status/reward/cleanup keep original synchronous semantics. Complete parent scopes holding a third actor while neither requested pair side is held, other direct pair callers, accepted nested contention with whole-continuation policies, no-parent/manual/tie/earlier waits, role/life/primary-secondary/Ctrl/area and actual Main/IO/bounded memory. No blanket accepted drop/defer. Prior first owner/audience/controlled Snapshot/Ref proofs retained; six historical aliases open.', 'materials':'148/149 original pre-admission pair entry now rejects nested missing-peer contention before scope; original Aim/decoded target/normal-Ctrl intent/cost/RNG/body/cleanup retained. No actual buff casts qualified. Complete first role/life/metadata/class/self/none/normal/Ctrl/primary/secondary/range/LOS/status/numeric/proc/cost/RNG/rollback and applicable807/58 material models, target immunity/corpse/PK/clan/party/GM, unknown/manual factories and empty selection consumption.', 'hp-producers':'Explicit raw-owned accepted=false and doll new damage pair entries now resolve missing-peer contention before original scope. Accepted raw=true and accepted parent doll retain original waits; no new HP/admission/output policy. Complete all original producer earlier proofs/phase-specific accepted pair continuations/controls/math and wand/reflect/feedback/poison/cube/ground/controlled/bot/NPC routes, target-owned accepted/final output/IO/Main/memory. Static call counts and closed raw-owned fixtures do not qualify full real HP/cost/RNG.', 'proc-dispatch':'Doll player new-action entry without accepted parent now resolves missing peer before scope; accepted parent doll remains original synchronous pair. Complete doll skill/add/reduce/equipment/stone/weapon/nonhealing68/guardian/talent, Source GMBUFF/PROCS/direct-magic distinctions, earlier pre-RNG/HP/effect accepted proofs and actual NONE versus unknown.'}
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
block='## 本次已更新的程式\n\n本輪補三個明確的新動作成對入口：148／149武器Buff、raw owned的accepted=false，以及沒有accepted parent Frame的娃娃player damage。缺少的peer以tryLock在原scope註冊前取得；原Action／Aim guard、成本／RNG／HP／receipt與完整finally保留。raw accepted=true及accepted parent娃娃保持原同步等待，不對已接受HP後的狀態／獎勵／退休清理新增一律丟棄或延後規則。\n\n共用ordered core只在原null驗證後，為BATTLE且已持有兩側鎖的情況略過identity／tie monitor，仍在原scope執行且保留呼叫者hold counts。未持鎖時的signed-id／identity-hash／tie排序與WORLD／未載入後備保留。無既有class欄位或方法增加，無新worker／TLS／registry／queue／SQL或HP權責；前輪受控Snapshot／首次owner／Ref及原全房間防禦保留。\n\n78原方法獨立inverse、336 no-init、66候選契約＋3426同SHA繼承及同位元重建通過；16次實際競爭包含6次完整原raw-owned entry／finally與4次保持原等待的accepted路徑。另7項test-only強制hash fixture驗證實際原tie monitor後備與BATTLE已持鎖略過，fixture與候選SHA分開，不能當候選實機證據。20 SQL consumer保留。\n\nraw-owned fixture使用預建closed Action測原入口／phase／finally，不建立Source Context、不執行HP，不代表148／149或娃娃實際施法。父scope持有第三角色而本次pair兩側皆未持有、其餘direct pair caller、accepted nested contention的完整continuation、較早／手動持鎖、角色生命期／Ctrl／範圍主次目標、材料／數值／proc／外控／NPC-AI／最終輸出／Main與記憶體仍開放。6歷史current索引未解；全部程式完成後才集中後端／中文MySQL／實機驗收。未部署，不啟動實服或連MySQL，不寫Source／玩家SQL／schema／WAL。\n\n'
work.write_text(text[:a]+block+text[b:],'utf-8')
note=f"\n\n2026-10-26 explicit new-action pair entry：候選`{v['runtime']['sha256']}`，148／149、raw accepted=false及無accepted parent娃娃在scope前處理peer競爭；共用BATTLE已持雙鎖略過tie，accepted／清理保留原同步語意。78原方法inverse、336 no-init、66候選新＋3426同SHA繼承、16競爭／6原raw-owned／4accepted等待；另7項test-only強制tie fixture獨立標記。同位元重建通過；其餘pair phase／完整Source／Main仍開放，未部署／未連MySQL。證據：[{run.name}]({(run/'manifest.json').as_posix()})。\n"
for p in [current,doc]:p.write_text(p.read_text('utf-8')+note,'utf-8')
def freeze_previous(v):
 if isinstance(v,dict):
  if v.get('path')==old_index['path']and v.get('sha256')==old_index['sha256']:v.update(frozen_index)
  else:
   for x in v.values():freeze_previous(x)
 elif isinstance(v,list):
  for x in v:freeze_previous(x)
continuation=read(C);freeze_previous(continuation)
fresh=dict(index=pin(index),runtime=v['runtime'],native_runtime_qualified=False,complete=False,program_wired='explicit-new-action-pair-admission-original-accepted-and-owned-tie-program-only',full_goal_achieved=False)
continuation['source_pair_admission_progress']=fresh;audit['source_program_update_progress']=dict(**fresh,remaining=v['remaining']);proof['source_program_update_progress']=fresh;proof['docs']=[pin(current),pin(doc),pin(work)]
write(C,continuation);write(auditpath,audit);write(proofpath,proof)
cache.clear();verify(anchor_refs)
for key in ['protected','live_helpers','experimental_live_helpers','docs']:verify(proof[key])
verify(v['additional_protected'])
write(run/'post-checkpoint-receipt.json',dict(index=pin(index),proof=pin(proofpath),audit=pin(auditpath),continuation=pin(C),docs=proof['docs'],protected=proof['protected'],additional_protected=v['additional_protected'],unchanged_stage_anchors=anchor_refs,formal_deployment_changed=False,Source_or_player_SQL_or_WAL_written=False))
print(json.dumps(dict(status=v['status'],runtime=v['runtime'],manifest=pin(run/'manifest.json'),new_game_runtime_cases=0,new_SQL_connections=0)))
